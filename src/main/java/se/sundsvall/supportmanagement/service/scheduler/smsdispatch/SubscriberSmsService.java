package se.sundsvall.supportmanagement.service.scheduler.smsdispatch;

import generated.se.sundsvall.employee.PortalPersonData;
import generated.se.sundsvall.messaging.SmsRequest;
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
import se.sundsvall.dept44.common.validators.annotation.impl.ValidMSISDNConstraintValidator;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.NotificationDispatchOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchOutboxEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchOutboxEventEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.NotificationChannelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.integration.messaging.MessagingClient;
import se.sundsvall.supportmanagement.service.EmployeeService;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.mapper.NamespaceConfigMapper;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static se.sundsvall.supportmanagement.api.model.identifier.IdentifierTypeValues.AD_ACCOUNT;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.SMS;

@Service
public class SubscriberSmsService {

	private static final Logger LOG = LoggerFactory.getLogger(SubscriberSmsService.class);
	private static final String HEADING_TEMPLATE = "Uppdatering i %s:";
	private static final String ERRAND_LINE_TEMPLATE = "Ärende %s";
	private static final String ERRAND_LINK_LINE_TEMPLATE = "Ärende %s: %s/%s";
	private static final String MORE_ERRANDS_TEMPLATE = "...och %d ärenden till";
	private static final String SWEDISH_COUNTRY_CODE = "+46";
	private static final int MAX_ERRANDS_LISTED = 5;
	private static final boolean ASYNCHRONOUSLY = true;
	private static final ValidMSISDNConstraintValidator MSISDN_VALIDATOR = new ValidMSISDNConstraintValidator();

	private final NotificationDispatchOutboxRepository outboxRepository;
	private final NamespaceConfigRepository namespaceConfigRepository;
	private final NamespaceConfigMapper namespaceConfigMapper;
	private final NamespaceConfigService namespaceConfigService;
	private final MessagingClient messagingClient;
	private final EmployeeService employeeService;
	private final String sender;
	private final Duration maxAge;

	public SubscriberSmsService(
		final NotificationDispatchOutboxRepository outboxRepository,
		final NamespaceConfigRepository namespaceConfigRepository,
		final NamespaceConfigMapper namespaceConfigMapper,
		final NamespaceConfigService namespaceConfigService,
		final MessagingClient messagingClient,
		final EmployeeService employeeService,
		@Value("${scheduler.notification-dispatch.sms.sender}") final String sender,
		@Value("${scheduler.notification-dispatch.max-age:P30D}") final Duration maxAge) {
		this.outboxRepository = outboxRepository;
		this.namespaceConfigRepository = namespaceConfigRepository;
		this.namespaceConfigMapper = namespaceConfigMapper;
		this.namespaceConfigService = namespaceConfigService;
		this.messagingClient = messagingClient;
		this.employeeService = employeeService;
		this.sender = sender;
		this.maxAge = maxAge;
	}

	/**
	 * Stores the events a subscriber should be texted about for an errand. Runs in the caller's transaction, so the row
	 * is written together with the other deliveries of the same dispatch or not at all. Nothing is stored in a namespace
	 * that has not enabled sms, since sms is a cost the namespace has to opt in to.
	 */
	public void enqueue(final String errandId, final String errandNumber, final SubscriberEntity subscriber, final List<NotificationDispatchEntity> events) {
		if (!namespaceConfigService.isSmsNotificationsEnabled(subscriber.getNamespace(), subscriber.getMunicipalityId())) {
			LOG.debug("Sms is not enabled in namespace {}, skipping sms delivery for errand: {} subscriber: {}", subscriber.getNamespace(), errandId, subscriber.getId());
			return;
		}

		outboxRepository.save(NotificationDispatchOutboxEntity.create()
			.withSubscriber(subscriber)
			.withChannel(SMS)
			.withErrandId(errandId)
			.withErrandNumber(errandNumber)
			.withEvents(events.stream()
				.map(event -> NotificationDispatchOutboxEventEmbeddable.create()
					.withEventId(event.getEventId())
					.withDescription(event.getDescription()))
				.toList()));
	}

	public List<String> findPendingSubscriberIds() {
		return outboxRepository.findDistinctSubscriberIdsByChannel(SMS);
	}

	/**
	 * Sends one sms covering everything in the outbox for the subscriber and removes it. A failure rolls back this
	 * subscriber only, leaving the rows in place to be retried on the next run. Rows older than {@code maxAge} are left
	 * out of the sms and removed along with the rest, the same way stale email outbox entries are.
	 */
	@Transactional(propagation = REQUIRES_NEW)
	public void sendPending(final String subscriberId) {
		final var allEntries = outboxRepository.findBySubscriberIdAndChannelOrderByCreatedAsc(subscriberId, SMS);
		if (allEntries.isEmpty()) {
			return;
		}

		final var cutoff = now(systemDefault()).minus(maxAge);
		final var entries = allEntries.stream()
			.filter(entry -> ofNullable(entry.getCreated()).map(created -> created.isAfter(cutoff)).orElse(true))
			.toList();
		if (entries.size() < allEntries.size()) {
			LOG.warn("Discarding {} sms outbox entries older than {} for subscriber {}", allEntries.size() - entries.size(), maxAge, subscriberId);
		}
		if (entries.isEmpty()) {
			outboxRepository.deleteAll(allEntries);
			return;
		}

		final var subscriber = entries.getFirst().getSubscriber();
		final var recipients = resolveRecipients(subscriber);
		if (recipients.isEmpty()) {
			LOG.warn("No mobile number to send to for subscriber {}, discarding {} sms outbox entries", subscriberId, entries.size());
			outboxRepository.deleteAll(allEntries);
			return;
		}

		final var municipalityId = subscriber.getMunicipalityId();
		final var namespace = subscriber.getNamespace();
		final var namespaceConfig = findNamespaceConfig(subscriber);
		final var displayName = namespaceConfig
			.map(NamespaceConfig::getDisplayName)
			.orElse(namespace);
		// The base url is the address of an errand short of its number, so the number is all that is appended
		final var errandsUrl = namespaceConfig
			.map(NamespaceConfig::getBaseUrl)
			.map(baseUrl -> Strings.CS.removeEnd(baseUrl, "/"));

		final var message = buildMessage(displayName, entries, errandsUrl);
		recipients.forEach(recipient -> messagingClient.sendSms(municipalityId, ASYNCHRONOUSLY, new SmsRequest()
			.sender(sender)
			.mobileNumber(recipient)
			.message(message)));
		outboxRepository.deleteAll(allEntries);
	}

	/**
	 * An sms only names the updated errands and links to them, both to keep it short and since an sms is not the place
	 * for what happened to an errand. Past {@code MAX_ERRANDS_LISTED} errands only the number of the rest is told.
	 */
	private String buildMessage(final String displayName, final List<NotificationDispatchOutboxEntity> entries, final Optional<String> errandsUrl) {
		final var errandNumbers = entries.stream()
			.collect(Collectors.groupingBy(NotificationDispatchOutboxEntity::getErrandId, LinkedHashMap::new, Collectors.toList()))
			.values().stream()
			.map(errandEntries -> errandEntries.stream()
				.map(NotificationDispatchOutboxEntity::getErrandNumber)
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(errandEntries.getFirst().getErrandId()))
			.toList();

		final var errandLines = errandNumbers.stream()
			.limit(MAX_ERRANDS_LISTED)
			.map(errandNumber -> errandsUrl
				.map(url -> ERRAND_LINK_LINE_TEMPLATE.formatted(errandNumber, url, errandNumber))
				.orElseGet(() -> ERRAND_LINE_TEMPLATE.formatted(errandNumber)));
		final var moreLine = Optional.of(errandNumbers.size() - MAX_ERRANDS_LISTED)
			.filter(remaining -> remaining > 0)
			.map(MORE_ERRANDS_TEMPLATE::formatted)
			.stream();

		return Stream.of(Stream.of(HEADING_TEMPLATE.formatted(displayName)), errandLines, moreLine)
			.flatMap(identity())
			.collect(Collectors.joining("\n"));
	}

	/**
	 * The mobile numbers of the subscriber's sms channels as they are now. An sms channel without a number stands for the
	 * subscriber's own mobile number in the employee directory, and so does having no sms channel at all: sms is then
	 * only enqueued because a subscription profile routes events to it, and a profile carries no numbers of its own.
	 * Numbers that cannot be made into a valid MSISDN are left out.
	 */
	private List<String> resolveRecipients(final SubscriberEntity subscriber) {
		final var smsChannels = ofNullable(subscriber.getChannels()).orElse(emptyList()).stream()
			.filter(channel -> channel.getType() == SMS)
			.toList();

		final var employeeMobileNumber = Optional.of(smsChannels)
			.filter(channels -> channels.isEmpty() || channels.stream().anyMatch(channel -> isNull(channel.getDestination())))
			.flatMap(_ -> ofNullable(subscriber.getIdentifier()))
			.filter(identifier -> AD_ACCOUNT.equals(identifier.getType()))
			.map(identifier -> employeeService.getEmployeeByLoginName(subscriber.getMunicipalityId(), identifier.getValue()))
			.map(PortalPersonData::getMobilePhone);

		return Stream.concat(smsChannels.stream().map(NotificationChannelEmbeddable::getDestination).filter(Objects::nonNull), employeeMobileNumber.stream())
			.map(mobileNumber -> toMsisdn(subscriber.getId(), mobileNumber))
			.flatMap(Optional::stream)
			.distinct()
			.toList();
	}

	/**
	 * Makes a mobile number written the Swedish way, such as {@code 070-123 45 67} or {@code +46 (0)70 123 45 67}, into an
	 * MSISDN. A number that is still not a valid MSISDN after that is left out, and only the subscriber is logged since
	 * the number is personal.
	 */
	private static Optional<String> toMsisdn(final String subscriberId, final String mobileNumber) {
		final var msisdn = withCountryCode(mobileNumber.replace("(0)", "").replaceAll("[\\s\\-()]", ""));
		if (MSISDN_VALIDATOR.isValid(msisdn)) {
			return Optional.of(msisdn);
		}
		LOG.warn("A mobile number of subscriber {} is not a valid MSISDN and is left out", subscriberId);
		return Optional.empty();
	}

	private static String withCountryCode(final String mobileNumber) {
		if (mobileNumber.startsWith("00")) {
			return "+" + mobileNumber.substring(2);
		}
		if (mobileNumber.startsWith("0")) {
			return SWEDISH_COUNTRY_CODE + mobileNumber.substring(1);
		}
		return mobileNumber;
	}

	private Optional<NamespaceConfig> findNamespaceConfig(final SubscriberEntity subscriber) {
		return namespaceConfigRepository.findByNamespaceAndMunicipalityId(subscriber.getNamespace(), subscriber.getMunicipalityId())
			.map(namespaceConfigMapper::toNamespaceConfig);
	}
}
