package se.sundsvall.supportmanagement.service.scheduler.emaildispatch;

import generated.se.sundsvall.employee.PortalPersonData;
import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.EmailSender;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.EmailDispatchOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.EmailDispatchOutboxEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.NotificationChannelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.integration.messaging.MessagingClient;
import se.sundsvall.supportmanagement.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.supportmanagement.service.EmployeeService;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Optional.ofNullable;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_DISPLAY_NAME;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.getNullableValue;

@Service
public class SubscriberEmailService {

	private static final Logger LOG = LoggerFactory.getLogger(SubscriberEmailService.class);
	private static final String SUBJECT_TEMPLATE = "Händelse kopplad till ärende %s";
	private static final String IDENTIFIER_TYPE_AD_ACCOUNT = "AD_ACCOUNT";

	private final EmailDispatchOutboxRepository outboxRepository;
	private final NamespaceConfigRepository namespaceConfigRepository;
	private final MessagingSettingsIntegration messagingSettingsIntegration;
	private final MessagingClient messagingClient;
	private final EmployeeService employeeService;

	@Value("${scheduler.email-dispatch.max-attempts:5}")
	private int maxAttempts;

	public SubscriberEmailService(
		final EmailDispatchOutboxRepository outboxRepository,
		final NamespaceConfigRepository namespaceConfigRepository,
		final MessagingSettingsIntegration messagingSettingsIntegration,
		final MessagingClient messagingClient,
		final EmployeeService employeeService) {
		this.outboxRepository = outboxRepository;
		this.namespaceConfigRepository = namespaceConfigRepository;
		this.messagingSettingsIntegration = messagingSettingsIntegration;
		this.messagingClient = messagingClient;
		this.employeeService = employeeService;
	}

	public void enqueue(final String errandId, final String errandNumber, final NotificationChannelEmbeddable channel,
		final SubscriberEntity subscriber, final List<NotificationDispatchEntity> events) {

		final var eventSummary = events.stream()
			.map(e -> ofNullable(e.getDescription()).orElse(e.getEventType()))
			.collect(Collectors.joining("; "));

		final var identifier = subscriber.getIdentifier();

		outboxRepository.save(EmailDispatchOutboxEntity.create()
			.withMunicipalityId(subscriber.getMunicipalityId())
			.withNamespace(subscriber.getNamespace())
			.withErrandId(errandId)
			.withErrandNumber(errandNumber)
			.withSubscriberId(subscriber.getId())
			.withRecipientEmail(channel.getDestination())
			.withIdentifierType(ofNullable(identifier).map(id -> id.getType()).orElse(null))
			.withIdentifierValue(ofNullable(identifier).map(id -> id.getValue()).orElse(null))
			.withEventSummary(eventSummary));
	}

	public List<EmailDispatchOutboxEntity> fetchPending() {
		return outboxRepository.findByAttemptsLessThanOrderByCreatedAsc(maxAttempts);
	}

	@Transactional(propagation = REQUIRES_NEW)
	public void processEntry(final EmailDispatchOutboxEntity entry) {
		try {
			final var email = resolveEmail(entry);
			if (email == null) {
				LOG.warn("Could not resolve email for outbox entry {}, subscriber {}, skipping", entry.getId(), entry.getSubscriberId());
				outboxRepository.delete(entry);
				return;
			}

			final var displayName = resolveDisplayName(entry.getMunicipalityId(), entry.getNamespace());
			final var messagingSettings = messagingSettingsIntegration.getMessagingsettings(entry.getMunicipalityId(), entry.getNamespace(), displayName);

			final var subject = SUBJECT_TEMPLATE.formatted(ofNullable(entry.getErrandNumber()).orElse(entry.getErrandId()));
			final var body = ofNullable(messagingSettings.supportText())
				.map(text -> text.formatted(
					"",
					ofNullable(entry.getErrandNumber()).orElse(entry.getErrandId()),
					messagingSettings.contactInformationUrl(),
					ofNullable(entry.getErrandNumber()).orElse(entry.getErrandId())))
				.orElse(ofNullable(entry.getEventSummary()).orElse(""));

			final var emailRequest = new EmailRequest()
				.recipients(List.of(email))
				.subject(subject)
				.message(body)
				.sender(new EmailSender()
					.name(ofNullable(messagingSettings.contactInformationEmailName()).orElse(messagingSettings.contactInformationEmail()))
					.address(messagingSettings.contactInformationEmail()));

			messagingClient.sendEmail(entry.getMunicipalityId(), true, emailRequest);

			outboxRepository.delete(entry);
			LOG.info("Successfully sent subscriber email for outbox entry {}", entry.getId());

		} catch (final Exception e) {
			LOG.error("Failed to process outbox entry {}: {}", entry.getId(), e.getMessage(), e);
			entry.setAttempts(entry.getAttempts() + 1);
			entry.setLastAttempted(now(systemDefault()).truncatedTo(MILLIS));
			outboxRepository.save(entry);
		}
	}

	private String resolveEmail(final EmailDispatchOutboxEntity entry) {
		if (entry.getRecipientEmail() != null) {
			return entry.getRecipientEmail();
		}
		if (IDENTIFIER_TYPE_AD_ACCOUNT.equals(entry.getIdentifierType()) && entry.getIdentifierValue() != null) {
			return ofNullable(employeeService.getEmployeeByLoginName(entry.getMunicipalityId(), entry.getIdentifierValue()))
				.map(PortalPersonData::getEmail)
				.orElse(null);
		}
		return null;
	}

	private String resolveDisplayName(final String municipalityId, final String namespace) {
		return namespaceConfigRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId)
			.map(config -> (String) getNullableValue(config, PROPERTY_DISPLAY_NAME))
			.orElse(namespace);
	}
}
