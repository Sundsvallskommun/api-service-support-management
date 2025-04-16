package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static se.sundsvall.supportmanagement.service.scheduler.emailreader.ErrandNumberParser.parseSubject;

import generated.se.sundsvall.emailreader.Email;
import generated.se.sundsvall.eventlog.EventType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.integration.db.EmailWorkerConfigRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.EmailWorkerConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType;
import se.sundsvall.supportmanagement.integration.emailreader.EmailReaderClient;
import se.sundsvall.supportmanagement.service.CommunicationService;
import se.sundsvall.supportmanagement.service.ErrandService;
import se.sundsvall.supportmanagement.service.EventService;

@Service
public class EmailReaderWorker {

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
	public void processEmail(final Email email, final EmailWorkerConfigEntity config) {

		final var errandNumber = parseSubject(email.getSubject());

		getErrand(errandNumber, email, config).ifPresent(errand -> {
			final var emailRequest = processErrand(errand, email, config);
			emailReaderClient.deleteEmail(config.getMunicipalityId(), email.getId());
			sendEmail(config, errand, emailRequest);
		});

	}

	private void sendEmail(final EmailWorkerConfigEntity config, final ErrandEntity errand, final EmailRequest emailRequest) {
		if (emailRequest != null) {
			communicationService.sendEmail(config.getNamespace(), config.getMunicipalityId(), errand.getId(), emailRequest);
		}
	}

	private Optional<ErrandEntity> getErrand(final String errandNumber, final Email email, final EmailWorkerConfigEntity config) {

		return Optional.ofNullable(errandNumber)
			.flatMap(errandRepository::findByErrandNumber)
			.or(() -> errandRepository
				.findById(errandService.createErrand(
					config.getNamespace(),
					config.getMunicipalityId(),
					emailReaderMapper.toErrand(
						email,
						config.getStatusForNew(),
						config.isAddSenderAsStakeholder(),
						config.getStakeholderRole(),
						config.getErrandChannel()))));
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

		if (isInvalidEmailAddress(email.getSender())) {
			// Don't send to a bad email address.
			return null;
		}

		return emailReaderMapper.createEmailRequest(email, config.getErrandClosedEmailSender(), config.getErrandClosedEmailTemplate(), email.getSubject());
	}

	private EmailRequest createEmailNew(final ErrandEntity errand, final Email email, final EmailWorkerConfigEntity config) {

		if (isEmpty(config.getErrandNewEmailSender()) || isEmpty(config.getErrandNewEmailTemplate())) {
			// Is optional to send email on new errand
			return null;
		}

		if (isInvalidEmailAddress(email.getSender())) {
			// Don't send to a bad email address.
			return null;
		}

		final var subject = EMAIL_NEW_SUBJECT_PREFIX + "#" + errand.getErrandNumber() + " " + email.getSubject();

		return emailReaderMapper.createEmailRequest(email, config.getErrandNewEmailSender(), config.getErrandNewEmailTemplate(), subject);
	}

	private boolean isInvalidEmailAddress(final String value) {
		return !EmailValidator.getInstance().isValid(value);
	}

	void addAttachments(final CommunicationEntity communicationEntity) {
		Optional.ofNullable(communicationEntity.getAttachments()).orElse(emptyList()).forEach(this::addAttachment);
	}

	private void addAttachment(final CommunicationAttachmentEntity attachment) {
		final var attachmentData = emailReaderClient.getAttachment(attachment.getMunicipalityId(), Integer.parseInt(attachment.getForeignId()));
		attachment.withAttachmentData(emailReaderMapper.toCommunicationAttachmentDataEntity(attachmentData))
			.withFileSize(attachmentData.length);
	}
}
