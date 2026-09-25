package se.sundsvall.supportmanagement.service.scheduler.emaildispatch;

import generated.se.sundsvall.employee.PortalPersonData;
import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.EmailSender;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.integration.db.EmailDispatchOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.EmailDispatchOutboxEntity;
import se.sundsvall.supportmanagement.integration.db.model.EmailDispatchOutboxEventEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.NotificationChannelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.integration.messaging.MessagingClient;
import se.sundsvall.supportmanagement.service.EmployeeService;
import se.sundsvall.supportmanagement.service.mapper.NamespaceConfigMapper;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static se.sundsvall.supportmanagement.api.model.identifier.IdentifierTypeValues.AD_ACCOUNT;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.EMAIL;

@Service
public class SubscriberEmailService {

	private static final Logger LOG = LoggerFactory.getLogger(SubscriberEmailService.class);
	private static final String SUBJECT_TEMPLATE = "Uppdatering i %s";
	private static final String ERRAND_HEADING_TEMPLATE = "Ärende %s:";
	private static final String EVENT_LINE_TEMPLATE = "    - %s";
	private static final String LINK_LINE_TEMPLATE = "    Länk: %s/%s";
	private static final String ERRANDS_URL_TEMPLATE = "%s/%s/%s/errands";
	private static final boolean ASYNCHRONOUSLY = true;

	private final EmailDispatchOutboxRepository outboxRepository;
	private final NamespaceConfigRepository namespaceConfigRepository;
	private final NamespaceConfigMapper namespaceConfigMapper;
	private final MessagingClient messagingClient;
	private final EmployeeService employeeService;
	private final String senderAddress;
	private final String senderName;
	private final Duration maxAge;

	public SubscriberEmailService(
		final EmailDispatchOutboxRepository outboxRepository,
		final NamespaceConfigRepository namespaceConfigRepository,
		final NamespaceConfigMapper namespaceConfigMapper,
		final MessagingClient messagingClient,
		final EmployeeService employeeService,
		@Value("${scheduler.notification-dispatch.email.sender-address}") final String senderAddress,
		@Value("${scheduler.notification-dispatch.email.sender-name}") final String senderName,
		@Value("${scheduler.notification-dispatch.max-age:P30D}") final Duration maxAge) {
		this.outboxRepository = outboxRepository;
		this.namespaceConfigRepository = namespaceConfigRepository;
		this.namespaceConfigMapper = namespaceConfigMapper;
		this.messagingClient = messagingClient;
		this.employeeService = employeeService;
		this.senderAddress = senderAddress;
		this.senderName = senderName;
		this.maxAge = maxAge;
	}

	/**
	 * Stores the events a subscriber should be emailed about for an errand. Runs in the caller's transaction, so the
	 * row is written together with the internal notifications of the same dispatch or not at all.
	 */
	public void enqueue(final String errandId, final String errandNumber, final SubscriberEntity subscriber, final List<NotificationDispatchEntity> events) {
		outboxRepository.save(EmailDispatchOutboxEntity.create()
			.withSubscriber(subscriber)
			.withErrandId(errandId)
			.withErrandNumber(errandNumber)
			.withEvents(events.stream()
				.map(event -> EmailDispatchOutboxEventEmbeddable.create()
					.withEventId(event.getEventId())
					.withDescription(event.getDescription()))
				.toList()));
	}

	public List<String> findPendingSubscriberIds() {
		return outboxRepository.findDistinctSubscriberIds();
	}

	/**
	 * Sends one email covering everything in the outbox for the subscriber and removes it. A failure rolls back this
	 * subscriber only, leaving the rows in place to be retried on the next run. Since that retry has no end of its own,
	 * rows older than {@code maxAge} are left out of the email and removed along with the rest, the same way stale
	 * notification dispatch entries are.
	 */
	@Transactional(propagation = REQUIRES_NEW)
	public void sendPending(final String subscriberId) {
		final var allEntries = outboxRepository.findBySubscriberIdOrderByCreatedAsc(subscriberId);
		if (allEntries.isEmpty()) {
			return;
		}

		final var cutoff = now(systemDefault()).minus(maxAge);
		final var entries = allEntries.stream()
			.filter(entry -> ofNullable(entry.getCreated()).map(created -> created.isAfter(cutoff)).orElse(true))
			.toList();
		if (entries.size() < allEntries.size()) {
			LOG.warn("Discarding {} outbox entries older than {} for subscriber {}", allEntries.size() - entries.size(), maxAge, subscriberId);
		}
		if (entries.isEmpty()) {
			outboxRepository.deleteAll(allEntries);
			return;
		}

		final var subscriber = entries.getFirst().getSubscriber();
		final var recipients = resolveRecipients(subscriber);
		if (recipients.isEmpty()) {
			LOG.warn("No email address to send to for subscriber {}, discarding {} outbox entries", subscriberId, entries.size());
			outboxRepository.deleteAll(allEntries);
			return;
		}

		final var municipalityId = subscriber.getMunicipalityId();
		final var namespace = subscriber.getNamespace();
		final var namespaceConfig = namespaceConfigRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId)
			.map(namespaceConfigMapper::toNamespaceConfig);
		final var displayName = namespaceConfig
			.map(NamespaceConfig::getDisplayName)
			.orElse(namespace);
		final boolean excludeDescriptions = namespaceConfig
			.map(NamespaceConfig::isExcludeEventDescriptionsInEmail)
			.orElse(false);
		final var errandsUrl = namespaceConfig
			.map(NamespaceConfig::getBaseUrl)
			.map(baseUrl -> ERRANDS_URL_TEMPLATE.formatted(Strings.CS.removeEnd(baseUrl, "/"), municipalityId, namespace));

		final var subject = SUBJECT_TEMPLATE.formatted(displayName);

		final var emailRequest = new EmailRequest()
			.recipients(recipients)
			.subject(subject)
			.message(buildBody(subject, entries, excludeDescriptions, errandsUrl))
			.sender(new EmailSender()
				.name(senderName)
				.address(senderAddress));

		messagingClient.sendEmail(municipalityId, ASYNCHRONOUSLY, emailRequest);
		outboxRepository.deleteAll(allEntries);
	}

	private String buildBody(final String heading, final List<EmailDispatchOutboxEntity> entries, final boolean excludeDescriptions, final Optional<String> errandsUrl) {
		final var sections = entries.stream()
			.collect(Collectors.groupingBy(EmailDispatchOutboxEntity::getErrandId, LinkedHashMap::new, Collectors.toList()))
			.values().stream()
			.map(errandEntries -> toSection(errandEntries, excludeDescriptions, errandsUrl))
			.collect(Collectors.joining("\n\n"));

		return heading + "\n\n" + sections;
	}

	private String toSection(final List<EmailDispatchOutboxEntity> errandEntries, final boolean excludeDescriptions, final Optional<String> errandsUrl) {
		final var errandNumber = errandEntries.stream()
			.map(EmailDispatchOutboxEntity::getErrandNumber)
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(errandEntries.getFirst().getErrandId());

		final var link = errandsUrl.map(url -> LINK_LINE_TEMPLATE.formatted(url, errandNumber)).stream();
		final var descriptions = errandEntries.stream()
			.filter(_ -> !excludeDescriptions)
			.flatMap(entry -> ofNullable(entry.getEvents()).orElse(emptyList()).stream())
			.map(EmailDispatchOutboxEventEmbeddable::getDescription)
			.filter(Objects::nonNull)
			.map(EVENT_LINE_TEMPLATE::formatted);

		return Stream.of(Stream.of(ERRAND_HEADING_TEMPLATE.formatted(errandNumber)), link, descriptions)
			.flatMap(identity())
			.collect(Collectors.joining("\n"));
	}

	/**
	 * The addresses of the subscriber's email channels as they are now, so a subscriber who has since dropped email gets
	 * nothing. An email channel without an address stands for the subscriber's own address in the employee directory.
	 */
	private List<String> resolveRecipients(final SubscriberEntity subscriber) {
		final var emailChannels = ofNullable(subscriber.getChannels()).orElse(emptyList()).stream()
			.filter(channel -> channel.getType() == EMAIL)
			.toList();

		final var employeeEmail = emailChannels.stream()
			.filter(channel -> isNull(channel.getDestination()))
			.findAny()
			.flatMap(_ -> ofNullable(subscriber.getIdentifier()))
			.filter(identifier -> AD_ACCOUNT.equals(identifier.getType()))
			.map(identifier -> employeeService.getEmployeeByLoginName(subscriber.getMunicipalityId(), identifier.getValue()))
			.map(PortalPersonData::getEmail);

		return Stream.concat(emailChannels.stream().map(NotificationChannelEmbeddable::getDestination).filter(Objects::nonNull), employeeEmail.stream())
			.distinct()
			.toList();
	}
}
