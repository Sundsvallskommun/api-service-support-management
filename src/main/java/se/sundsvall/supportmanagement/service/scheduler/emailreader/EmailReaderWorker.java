package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import generated.se.sundsvall.emailreader.Email;
import generated.se.sundsvall.eventlog.EventType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Strings;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.integration.db.EmailWorkerConfigRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.EmailWorkerConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType;
import se.sundsvall.supportmanagement.integration.emailreader.EmailReaderClient;
import se.sundsvall.supportmanagement.service.CommunicationService;
import se.sundsvall.supportmanagement.service.ErrandService;
import se.sundsvall.supportmanagement.service.EventService;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isAnyEmpty;
import static se.sundsvall.supportmanagement.service.scheduler.emailreader.ErrandNumberParser.parseSubject;

@Service
public class EmailReaderWorker {
	private static final Logger LOG = LoggerFactory.getLogger(EmailReaderWorker.class);

	private static final String EVENT_LOG_COMMUNICATION = "Nytt meddelande";
	private static final String EMAIL_NEW_SUBJECT_PREFIX = "Bekräftelse ärende ";

	private final EmailReaderClient emailReaderClient;

	private final ErrandService errandService;

	private final EventService eventService;

	private final CommunicationService communicationService;

	private final EmailReaderMapper emailReaderMapper;

	private final ErrandsRepository errandRepository;

	private final EmailWorkerConfigRepository emailWorkerConfigRepository;

	public EmailReaderWorker(final EmailReaderClient emailReaderClient, final EventService eventService,
		final ErrandsRepository errandRepository, final ErrandService errandService, final CommunicationService communicationService,
		final EmailReaderMapper emailReaderMapper, final EmailWorkerConfigRepository emailWorkerConfigRepository) {
		this.emailReaderClient = emailReaderClient;
		this.eventService = eventService;
		this.errandRepository = errandRepository;
		this.errandService = errandService;
		this.communicationService = communicationService;
		this.emailReaderMapper = emailReaderMapper;
		this.emailWorkerConfigRepository = emailWorkerConfigRepository;
	}

	public Set<EmailWorkerConfigEntity> getEnabledEmailConfigs() {
		return emailWorkerConfigRepository.findAll().stream()
			.filter(EmailWorkerConfigEntity::getEnabled)
			.collect(Collectors.toSet());
	}

	public List<Email> getEmailsFromConfig(final EmailWorkerConfigEntity config) {
		return emailReaderClient.getEmails(config.getMunicipalityId(), config.getNamespace());
	}

	@Transactional
	public void processEmail(final Email email, final EmailWorkerConfigEntity config, final Consumer<String> setUnHealthyConsumer) {

		final var errandNumber = parseSubject(email.getSubject());

		getErrand(errandNumber, email, config).ifPresent(errand -> {
			final var emailRequest = processErrand(errand, email, config);

			emailReaderClient.deleteEmail(config.getMunicipalityId(), email.getId());

			try {
				sendEmail(errand, emailRequest);
			} catch (final Exception e) {
				LOG.error("Failed to send confirmation email. Error: {}", e.getMessage());
				setUnHealthyConsumer.accept("Failed to send confirmation email");
			}
		});

	}

	private void sendEmail(final ErrandEntity errand, final EmailRequest emailRequest) {
		if (emailRequest != null) {
			communicationService.sendEmail(errand, emailRequest);
		}
	}

	private Optional<ErrandEntity> getErrand(final String errandNumber, final Email email, final EmailWorkerConfigEntity config) {

		return Optional.ofNullable(errandNumber)
			.flatMap(number -> errandRepository.findByErrandNumberAndNamespace(number, config.getNamespace()))
			.or(() -> errandRepository
				.findById(errandService.createErrand(
					config.getNamespace(),
					config.getMunicipalityId(),
					emailReaderMapper.toErrand(
						email,
						config))));
	}

	private EmailRequest processErrand(final ErrandEntity errand, final Email email, final EmailWorkerConfigEntity config) {

		EmailRequest emailRequest = null;
		if (isErrandInactive(errand, config)) {
			emailRequest = createEmailClosed(email, config);
		} else if (isErrandNew(errand, config)) {
			emailRequest = createEmailNew(errand, email, config);
		} else if ((config.getTriggerStatusChangeOn() != null) && errand.getStatus().equals(config.getTriggerStatusChangeOn())) {
			errand.setStatus(config.getStatusChangeTo());
			errandRepository.save(errand);
		}
		saveEmail(email, errand);
		eventService.createErrandEvent(EventType.UPDATE, EVENT_LOG_COMMUNICATION, errand, null, null, NotificationSubType.MESSAGE);
		return emailRequest;
	}

	private void saveEmail(final Email email, final ErrandEntity errand) {

		final var communicationEntity = emailReaderMapper.toCommunicationEntity(email, errand);
		addAttachments(communicationEntity);

		communicationService.saveCommunication(communicationEntity);
		communicationService.saveAttachment(communicationEntity, errand);
	}

	private boolean isErrandInactive(final ErrandEntity errand, final EmailWorkerConfigEntity config) {
		return (config.getInactiveStatus() != null)
			&& errand.getStatus().equals(config.getInactiveStatus())
			&& (config.getDaysOfInactivityBeforeReject() != null)
			&& errand.getTouched().isBefore(OffsetDateTime.now().minusDays(config.getDaysOfInactivityBeforeReject()));
	}

	private boolean isErrandNew(final ErrandEntity errand, final EmailWorkerConfigEntity config) {
		return (config.getStatusForNew() != null)
			&& errand.getStatus().equals(config.getStatusForNew());
	}

	private EmailRequest createEmailClosed(final Email email, final EmailWorkerConfigEntity config) {

		if (isNonUsableEmailAddress(email.getSender())) {
			// Don't send to a bad email address or to emails starting with no-reply or noreply.
			LOG.info("No mail for closed errand will be sent as email address '{}' is either invalid or contains a no reply address", email.getSender());
			return null;
		}

		return emailReaderMapper.createEmailRequest(email, config.getErrandClosedEmailSender(), config.getErrandClosedEmailTemplate(), config.getErrandClosedEmailHTMLTemplate(), email.getSubject());
	}

	private EmailRequest createEmailNew(final ErrandEntity errand, final Email email, final EmailWorkerConfigEntity config) {

		if (isAnyEmpty(config.getErrandNewEmailSender(), config.getErrandNewEmailTemplate())) {
			// It's optional to send email on new errand
			return null;
		}

		if (isNonUsableEmailAddress(email.getSender())) {
			// Don't send to a bad email address or to emails starting with no-reply or noreply.
			LOG.info("No mail for new errand will be sent as email address '{}' is either invalid or contains a no reply address", email.getSender());
			return null;
		}

		final var subject = EMAIL_NEW_SUBJECT_PREFIX + "#" + errand.getErrandNumber() + " " + email.getSubject();

		return emailReaderMapper.createEmailRequest(email, config.getErrandNewEmailSender(), config.getErrandNewEmailTemplate(), config.getErrandNewEmailHTMLTemplate(), subject);
	}

	private boolean isNonUsableEmailAddress(final String value) {
		return !EmailValidator.getInstance().isValid(value) || Strings.CI.startsWithAny(value, "no-reply", "noreply");
	}

	void addAttachments(final CommunicationEntity communicationEntity) {
		Optional.ofNullable(communicationEntity.getAttachments()).orElse(emptyList()).forEach(this::addAttachment);
	}

	private void addAttachment(final CommunicationAttachmentEntity attachment) {
		final var attachmentData = Optional.ofNullable(emailReaderClient.getAttachment(attachment.getMunicipalityId(),
			Integer.parseInt(attachment.getForeignId()))).orElse(new byte[0]);
		attachment.withAttachmentData(emailReaderMapper.toAttachmentDataEntity(attachmentData))
			.withFileSize(attachmentData.length);
	}
}
