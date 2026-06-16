package se.sundsvall.supportmanagement.service.action;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.config.action.Definition;
import se.sundsvall.supportmanagement.api.model.config.action.PossibleValue;
import se.sundsvall.supportmanagement.api.model.config.action.enums.OperationType;
import se.sundsvall.supportmanagement.integration.db.CommunicationRepository;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandActionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;
import se.sundsvall.supportmanagement.service.CommunicationService;
import se.sundsvall.supportmanagement.service.MetadataService;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

@Component
public class SendEmailAction extends AbstractAction {

	private static final Logger LOG = LoggerFactory.getLogger(SendEmailAction.class);

	private static final String RECIPIENT = "recipient";
	private static final String SENDER = "sender";
	private static final String SUBJECT = "subject";
	private static final String BODY = "body";
	private static final String ADD_LINK_TO_ERRAND_IN_BODY = "addLinkToErrandInBody";
	private static final String BASE_URL = "baseUrl";
	private static final String TRUE = "true";
	private static final String FALSE = "false";
	private static final String LINK_TO_ERRAND_HTML_FORMAT = "<br><br><a href=\"%s/arende/%s\">Öppna ärendet direkt i Draken</a>";
	private static final String AUTO_MESSAGE_DISCLAIMER = "<br><br><em>Detta är ett automatiskt meddelande. Svara inte på detta e-postmeddelande</em>";

	private static final Set<String> BOOLEAN_VALUES = Set.of(TRUE, FALSE);
	private static final Set<OperationType> VALID_OPERATION_TYPES = Set.of(OperationType.CREATE, OperationType.UPDATE);

	private final CommunicationService communicationService;
	private final CommunicationRepository communicationRepository;

	public SendEmailAction(final MetadataService metadataService, final CommunicationService communicationService, final CommunicationRepository communicationRepository, final Clock clock) {
		super(metadataService, clock);
		this.communicationService = communicationService;
		this.communicationRepository = communicationRepository;
	}

	@Override
	public String getName() {
		return "SEND_EMAIL";
	}

	@Override
	public String getDescription() {
		return "Sends an email when conditions are met";
	}

	@Override
	public List<Definition> getParameterDefinitions(String municipalityId, String namespace) {
		return List.of(
			Definition.create()
				.withKey(RECIPIENT)
				.withMandatory(true)
				.withDescription("Email address of the recipient"),
			Definition.create()
				.withKey(SENDER)
				.withMandatory(true)
				.withDescription("Email address of the sender"),
			Definition.create()
				.withKey(SUBJECT)
				.withMandatory(true)
				.withDescription("Subject of the email"),
			Definition.create()
				.withKey(BODY)
				.withMandatory(true)
				.withDescription("Body of the email"),
			Definition.create()
				.withKey(ADD_LINK_TO_ERRAND_IN_BODY)
				.withMandatory(true)
				.withDescription("If true, a link to the errand will be appended to the email body")
				.withPossibleValues(List.of(
					PossibleValue.create().withValue(TRUE).withDisplayName(TRUE),
					PossibleValue.create().withValue(FALSE).withDisplayName(FALSE))),
			Definition.create()
				.withKey(BASE_URL)
				.withMandatory(false)
				.withDescription("Base URL for errand links. Required if addLinkToErrandInBody is true"),
			Definition.create()
				.withKey(DURATION)
				.withMandatory(false)
				.withDescription("Delay before the email is sent. Format follows java Duration ISO-8601 (PnDTnHnMn.nS). If null will be sent directly"));
	}

	@Override
	public void validateParameters(String municipalityId, String namespace, Map<String, List<String>> parameters) throws ThrowableProblem {
		Set<String> validKeys = Set.of(RECIPIENT, SENDER, SUBJECT, BODY, ADD_LINK_TO_ERRAND_IN_BODY, BASE_URL, DURATION);
		validateKeys(parameters, validKeys);

		for (String mandatoryKey : List.of(RECIPIENT, SENDER, SUBJECT, BODY, ADD_LINK_TO_ERRAND_IN_BODY)) {
			if (!parameters.containsKey(mandatoryKey) || parameters.get(mandatoryKey).isEmpty()) {
				throw Problem.valueOf(UNPROCESSABLE_CONTENT, String.format("Key '%s' is mandatory and cannot be empty", mandatoryKey));
			}
			if (parameters.get(mandatoryKey).size() > 1) {
				throw Problem.valueOf(UNPROCESSABLE_CONTENT, String.format("Cannot handle multiple values of key '%s'", mandatoryKey));
			}
		}

		if (!BOOLEAN_VALUES.contains(parameters.get(ADD_LINK_TO_ERRAND_IN_BODY).getFirst())) {
			throw Problem.valueOf(UNPROCESSABLE_CONTENT, String.format("Value '%s' is not valid for key '%s'. Must be 'true' or 'false'", parameters.get(ADD_LINK_TO_ERRAND_IN_BODY).getFirst(), ADD_LINK_TO_ERRAND_IN_BODY));
		}

		if (TRUE.equals(parameters.get(ADD_LINK_TO_ERRAND_IN_BODY).getFirst())
			&& (!parameters.containsKey(BASE_URL) || parameters.get(BASE_URL).isEmpty())) {
			throw Problem.valueOf(UNPROCESSABLE_CONTENT, String.format("Key '%s' is required when '%s' is true", BASE_URL, ADD_LINK_TO_ERRAND_IN_BODY));
		}

		if (parameters.containsKey(BASE_URL) && parameters.get(BASE_URL).size() > 1) {
			throw Problem.valueOf(UNPROCESSABLE_CONTENT, String.format("Cannot handle multiple values of key '%s'", BASE_URL));
		}

		validateDuration(parameters);
	}

	@Override
	public boolean actionFulfilled(ErrandEntity errand, Map<String, List<String>> parameters) {
		final var sender = parameters.get(SENDER).getFirst();
		final var recipient = parameters.get(RECIPIENT).getFirst();
		final var expectedSubject = String.format("%s - %s", parameters.get(SUBJECT).getFirst(), errand.getErrandNumber());

		return communicationRepository.findByErrandNumber(errand.getErrandNumber()).stream()
			.filter(c -> c.getType() == CommunicationType.EMAIL)
			.filter(c -> c.getDirection() == Direction.OUTBOUND)
			.anyMatch(c -> sender.equals(c.getSender())
				&& c.getRecipients().contains(recipient)
				&& expectedSubject.equals(c.getSubject()));
	}

	@Override
	public Optional<ErrandActionEntity> createAction(ErrandEntity errand, ActionConfigEntity actionConfigEntity) {
		return createActionWithDuration(errand, actionConfigEntity);
	}

	@Override
	public void executeAction(ErrandEntity errand, ActionConfigEntity actionConfigEntity) {
		var parameterMap = actionConfigEntity.getParameters().stream()
			.collect(Collectors.toMap(ActionConfigParameterEntity::getKey, ActionConfigParameterEntity::getValues));

		var recipient = parameterMap.get(RECIPIENT).getFirst();
		var sender = parameterMap.get(SENDER).getFirst();
		var subject = String.format(Optional.ofNullable(parameterMap.get(SUBJECT).getFirst()).orElse("").concat(" - %s"), errand.getErrandNumber());
		var addLink = Boolean.parseBoolean(parameterMap.get(ADD_LINK_TO_ERRAND_IN_BODY).getFirst());

		var htmlBody = parameterMap.get(BODY).getFirst();

		if (addLink) {
			var errandBaseUrl = parameterMap.get(BASE_URL).getFirst();
			htmlBody += String.format(LINK_TO_ERRAND_HTML_FORMAT, errandBaseUrl, errand.getErrandNumber());
		}

		htmlBody += AUTO_MESSAGE_DISCLAIMER;

		var emailRequest = EmailRequest.create()
			.withRecipient(recipient)
			.withSender(sender)
			.withSubject(subject)
			.withHtmlMessage(htmlBody);

		communicationService.sendEmail(errand, emailRequest);
		LOG.info("SEND_EMAIL action executed for errand '{}'", errand.getId());
	}

	@Override
	public boolean validForOperationType(OperationType operationType) {
		return VALID_OPERATION_TYPES.contains(operationType);
	}
}
