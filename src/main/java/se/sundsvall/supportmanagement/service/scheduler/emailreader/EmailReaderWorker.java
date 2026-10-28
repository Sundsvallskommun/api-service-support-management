package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import generated.se.sundsvall.emailreader.Email;
import generated.se.sundsvall.eventlog.EventType;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.integration.db.EmailWorkerConfigRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.EmailWorkerConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType;
import se.sundsvall.supportmanagement.integration.emailreader.EmailReaderClient;
import se.sundsvall.supportmanagement.service.CommunicationService;
import se.sundsvall.supportmanagement.service.ErrandService;
import se.sundsvall.supportmanagement.service.EventService;
import se.sundsvall.supportmanagement.service.TeliaAceNotificationRequestedEvent;
import se.sundsvall.supportmanagement.service.model.ErrandEventOptions;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isAnyEmpty;
import static se.sundsvall.supportmanagement.service.scheduler.emailreader.EmailReaderUtilities.isAutoReply;
import static se.sundsvall.supportmanagement.service.scheduler.emailreader.EmailReaderUtilities.isDeliveryStatusReport;
import static se.sundsvall.supportmanagement.service.scheduler.emailreader.EmailReaderUtilities.isNoReplyAddress;
import static se.sundsvall.supportmanagement.service.scheduler.emailreader.EmailReaderUtilities.shouldSuppressConfirmation;
import static se.sundsvall.supportmanagement.service.scheduler.emailreader.ErrandNumberParser.parseSubject;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getExecutingUser;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.inNewRequestGroup;

@Service
public class EmailReaderWorker {
	private static final Logger LOG = LoggerFactory.getLogger(EmailReaderWorker.class);

	private static final String EVENT_LOG_COMMUNICATION = "Meddelande mottaget via e-post";
	private static final String ERRAND_NOTIFICATION_COMMUNICATION = "Nytt meddelande";
	private static final String EMAIL_NEW_SUBJECT_PREFIX = "Bekräftelse ärende ";

	private final EmailReaderClient emailReaderClient;

	private final ErrandService errandService;

	private final EventService eventService;

	private final CommunicationService communicationService;

	private final EmailReaderMapper emailReaderMapper;

	private final ErrandsRepository errandRepository;

	private final EmailWorkerConfigRepository emailWorkerConfigRepository;

	private final ApplicationEventPublisher eventPublisher;

	public EmailReaderWorker(final EmailReaderClient emailReaderClient, final EventService eventService,
		final ErrandsRepository errandRepository, final ErrandService errandService, final CommunicationService communicationService,
		final EmailReaderMapper emailReaderMapper, final EmailWorkerConfigRepository emailWorkerConfigRepository, final ApplicationEventPublisher eventPublisher) {
		this.emailReaderClient = emailReaderClient;
		this.eventService = eventService;
		this.errandRepository = errandRepository;
		this.errandService = errandService;
		this.communicationService = communicationService;
		this.emailReaderMapper = emailReaderMapper;
		this.emailWorkerConfigRepository = emailWorkerConfigRepository;
		this.eventPublisher = eventPublisher;
	}

	public Set<EmailWorkerConfigEntity> getEnabledEmailConfigs() {
		return emailWorkerConfigRepository.findAll().stream()
			.filter(EmailWorkerConfigEntity::getEnabled)
			.collect(Collectors.toSet());
	}

	public List<Email> getEmailsFromConfig(final EmailWorkerConfigEntity config) {
		return emailReaderClient.getEmails(config.getMunicipalityId(), config.getNamespace());
	}

	/**
	 * Processes the email in a request group of its own, so that the errand it may create and the message it brings reach
	 * the subscribers as one notification.
	 */
	@Transactional
	public void processEmail(final Email email, final EmailWorkerConfigEntity config, final Consumer<String> setUnHealthyConsumer) {
		inNewRequestGroup(() -> doProcessEmail(email, config, setUnHealthyConsumer));
	}

	private void doProcessEmail(final Email email, final EmailWorkerConfigEntity config, final Consumer<String> setUnHealthyConsumer) {

		if (config.isIgnoreAutoReply() && isAutoReply(email) && !isDeliveryStatusReport(email)) {
			LOG.info("Email '{}' is an auto-reply and will be deleted per namespace config", email.getId());
			emailReaderClient.deleteEmail(config.getMunicipalityId(), email.getId());
			return;
		}
		if (config.isIgnoreNoReply() && isNoReplyAddress(email)) {
			LOG.info("Email '{}' is from a no-reply address and will be deleted per namespace config", email.getId());
			emailReaderClient.deleteEmail(config.getMunicipalityId(), email.getId());
			return;
		}

		getErrand(email, config).ifPresent(errand -> {
			final var emailRequest = processErrand(errand, email, config);

			try {
				emailReaderClient.deleteEmail(config.getMunicipalityId(), email.getId());
				if (shouldNotifyTeliaAce(errand, config, email)) {
					notifyTeliaAce(errand, email, setUnHealthyConsumer);
				}
			} catch (final Exception e) {
				LOG.warn("Failed to delete email {} from EmailReader for errand {}: {}", email.getId(), errand.getId(), e.getMessage());
				setUnHealthyConsumer.accept("Failed to delete email from EmailReader — email will be re-processed and may cause duplicate communications");
			}

			try {
				sendEmail(errand, emailRequest);
			} catch (final Exception e) {
				LOG.error("Failed to send confirmation email. Error: {}", e.getMessage());
				setUnHealthyConsumer.accept("Failed to send confirmation email");
			}
		});

	}

	private void sendEmail(final ErrandEntity errand, final EmailRequest emailRequest) {
		if (emailRequest != null && !errand.isDraft()) {
			// The confirmation of an email having been received is not logged
			communicationService.sendEmail(errand, emailRequest, null);
		}
	}

	private Optional<ErrandEntity> getErrand(final Email email, final EmailWorkerConfigEntity config) {
		final var errandNumber = parseSubject(email.getSubject());

		return Optional.ofNullable(errandNumber)
			.flatMap(number -> errandRepository.findByErrandNumberAndNamespaceAndMunicipalityId(number, config.getNamespace(), config.getMunicipalityId()))
			.or(() -> errandRepository
				.findById(errandService.createErrand(
					config.getNamespace(),
					config.getMunicipalityId(),
					emailReaderMapper.toErrand(
						email,
						config),
					null)));
	}

	/**
	 * A draft, an inactive errand, a delivery status report or an auto-reply is not a case worker needing to act, so
	 * none of them should page an agent in Telia ACE to open the errand.
	 */
	private boolean shouldNotifyTeliaAce(final ErrandEntity errand, final EmailWorkerConfigEntity config, final Email email) {
		return !errand.isDraft() && !isErrandInactive(errand, config) && !isDeliveryStatusReport(email) && !isAutoReply(email);
	}

	/**
	 * Publishes rather than calls {@code TeliaAceWorkItemService} directly: the listener is deferred to after this
	 * transaction commits, so the errand it is about is guaranteed to exist by the time it runs. See
	 * {@link TeliaAceNotificationRequestedEvent}.
	 */
	private void notifyTeliaAce(final ErrandEntity errand, final Email email, final Consumer<String> setUnHealthyConsumer) {
		eventPublisher.publishEvent(new TeliaAceNotificationRequestedEvent(errand, email, setUnHealthyConsumer));
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
			logStatusChange(errand);
		}
		saveEmail(email, errand);
		try {
			// The assigned user is notified of the email
			eventService.createErrandEvent(EventType.UPDATE, EVENT_LOG_COMMUNICATION, errand, null, EventSubType.MESSAGE_EMAIL_EXTERNAL,
				new ErrandEventOptions(true, getExecutingUser(), Set.of(), ERRAND_NOTIFICATION_COMMUNICATION));
		} catch (final Exception e) {
			LOG.warn("Failed to log new-message event for errand {}: {}", errand.getId(), e.getMessage());
		}
		return emailRequest;
	}

	/**
	 * Logs that the email changed the status of the errand, so that subscribers may be notified of it along with the
	 * message. No direct notification is made, as the message itself makes one.
	 */
	private void logStatusChange(final ErrandEntity errand) {
		try {
			eventService.createStatusChangeEvent(errand, null, null);
		} catch (final Exception e) {
			LOG.warn("Failed to log status change event for errand {}: {}", errand.getId(), e.getMessage());
		}
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
			&& errand.getTouched().isBefore(OffsetDateTime.now(ZoneId.systemDefault()).minusDays(config.getDaysOfInactivityBeforeReject()));
	}

	private boolean isErrandNew(final ErrandEntity errand, final EmailWorkerConfigEntity config) {
		return (config.getStatusForNew() != null)
			&& errand.getStatus().equals(config.getStatusForNew());
	}

	private EmailRequest createEmailClosed(final Email email, final EmailWorkerConfigEntity config) {

		if (shouldSuppressConfirmation(email)) {
			LOG.info("No confirmation for closed errand will be sent for email from '{}'", email.getSender());
			return null;
		}

		return emailReaderMapper.createEmailRequest(email, config.getErrandClosedEmailSender(), config.getErrandClosedEmailTemplate(), config.getErrandClosedEmailHTMLTemplate(), email.getSubject());
	}

	private EmailRequest createEmailNew(final ErrandEntity errand, final Email email, final EmailWorkerConfigEntity config) {

		if (isAnyEmpty(config.getErrandNewEmailSender(), config.getErrandNewEmailTemplate())) {
			// It's optional to send email on new errand
			return null;
		}

		if (shouldSuppressConfirmation(email)) {
			LOG.info("No confirmation for new errand will be sent for email from '{}'", email.getSender());
			return null;
		}

		final var subject = EMAIL_NEW_SUBJECT_PREFIX + "#" + errand.getErrandNumber() + " " + email.getSubject();

		return emailReaderMapper.createEmailRequest(email, config.getErrandNewEmailSender(), config.getErrandNewEmailTemplate(), config.getErrandNewEmailHTMLTemplate(), subject);
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
