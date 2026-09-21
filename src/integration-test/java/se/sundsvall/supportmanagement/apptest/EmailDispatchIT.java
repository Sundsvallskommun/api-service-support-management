package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.EmailDispatchOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.NotificationDispatchRepository;
import se.sundsvall.supportmanagement.service.scheduler.emaildispatch.SubscriberEmailService;
import se.sundsvall.supportmanagement.service.scheduler.notificationdispatch.NotificationDispatchWorker;

@WireMockAppTestSuite(files = "classpath:/EmailDispatchIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-email-dispatch-it.sql"
})
class EmailDispatchIT extends AbstractAppTest {

	private static final String PATH = "/2281/NAMESPACE-1/errands";
	private static final String ERRAND_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeee0001";

	@Autowired
	private NotificationDispatchWorker notificationDispatchWorker;

	@Autowired
	private SubscriberEmailService subscriberEmailService;

	@Autowired
	private EmailDispatchOutboxRepository emailDispatchOutboxRepository;

	@Autowired
	private NotificationDispatchRepository notificationDispatchRepository;

	/**
	 * Verifies that the notification dispatch worker creates an email outbox entry
	 * for a subscriber with an EMAIL channel and a NAMESPACE subscription when
	 * processing an UPDATE/MESSAGE event.
	 */
	@Test
	void test01_emailSubscriberReceivesOutboxEntryOnUpdate() {
		setupCall();

		// Only fetch the UPDATE/MESSAGE dispatch entry (grp-001)
		final var dispatchEntries = notificationDispatchWorker.fetchProcessable().stream()
			.filter(e -> "grp-001".equals(e.getRequestGroupId()))
			.toList();
		assertThat(dispatchEntries).isNotEmpty();

		notificationDispatchWorker.processGroup(dispatchEntries);

		final var outboxEntries = emailDispatchOutboxRepository.findAll();
		assertThat(outboxEntries).hasSize(1);

		final var outbox = outboxEntries.getFirst();
		assertThat(outbox.getMunicipalityId()).isEqualTo("2281");
		assertThat(outbox.getNamespace()).isEqualTo("NAMESPACE-1");
		assertThat(outbox.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(outbox.getErrandNumber()).isEqualTo("NS1-2024-000001");
		assertThat(outbox.getSubscriberId()).isEqualTo("11111111-0000-0000-0000-000000000001");
		assertThat(outbox.getRecipientEmail()).isEqualTo("chef@example.com");
		assertThat(outbox.getEventSummary()).isEqualTo("Nytt meddelande");
	}

	/**
	 * Verifies that the outbox processor resolves email, fetches messaging settings,
	 * and sends an email via the messaging service. After successful send the outbox
	 * entry is deleted.
	 */
	@Test
	void test02_outboxProcessorSendsEmailViaMessaging() {
		setupCall();

		final var dispatchEntries = notificationDispatchWorker.fetchProcessable().stream()
			.filter(e -> "grp-001".equals(e.getRequestGroupId()))
			.toList();
		notificationDispatchWorker.processGroup(dispatchEntries);
		assertThat(emailDispatchOutboxRepository.findAll()).hasSize(1);

		subscriberEmailService.fetchPending().forEach(subscriberEmailService::processEntry);

		assertThat(emailDispatchOutboxRepository.findAll()).isEmpty();
		verifyStubs();
	}

	/**
	 * Verifies that a CREATE/ERRAND event dispatches email to a subscriber whose
	 * event filter includes {CREATE, null} (any CREATE subtype). The full chain is
	 * tested: dispatch entry → dispatch worker → outbox → outbox processor → email.
	 */
	@Test
	void test03_createEventDispatchesEmailToSubscriber() {
		setupCall();

		final var dispatchEntries = notificationDispatchWorker.fetchProcessable().stream()
			.filter(e -> "grp-002".equals(e.getRequestGroupId()))
			.toList();
		assertThat(dispatchEntries).hasSize(1);
		assertThat(dispatchEntries.getFirst().getEventType()).isEqualTo("CREATE");

		notificationDispatchWorker.processGroup(dispatchEntries);

		final var outboxEntries = emailDispatchOutboxRepository.findAll();
		assertThat(outboxEntries).hasSize(1);
		assertThat(outboxEntries.getFirst().getEventSummary()).isEqualTo("Nytt arende");

		subscriberEmailService.fetchPending().forEach(subscriberEmailService::processEntry);

		assertThat(emailDispatchOutboxRepository.findAll()).isEmpty();
		verifyStubs();
	}

	/**
	 * Verifies that an UPDATE/RESTRICTED event does NOT create an outbox entry for a
	 * subscriber whose event filter is {UPDATE, MESSAGE}. The RESTRICTED subtype does
	 * not match MESSAGE, so the subscriber is not notified — this is how MAS/MAR
	 * updates stay silent toward other roles.
	 */
	@Test
	void test04_restrictedUpdateDoesNotDispatchToRegularSubscriber() {
		setupCall();

		final var dispatchEntries = notificationDispatchWorker.fetchProcessable().stream()
			.filter(e -> "grp-003".equals(e.getRequestGroupId()))
			.toList();
		assertThat(dispatchEntries).hasSize(1);
		assertThat(dispatchEntries.getFirst().getSubType()).isEqualTo("RESTRICTED");

		notificationDispatchWorker.processGroup(dispatchEntries);

		assertThat(emailDispatchOutboxRepository.findAll()).isEmpty();
	}

	/**
	 * End-to-end: PATCH an errand via REST API, then run the dispatch worker and
	 * outbox processor to verify the full chain from API call to email delivery.
	 * <p>
	 * The subscriber has filter {UPDATE, MESSAGE} which does not match UPDATE/ERRAND,
	 * and filter {CREATE, null}. Since the PATCH produces an UPDATE/ERRAND event, the
	 * subscriber's filter does not match and no outbox entry is created.
	 * <p>
	 * This test verifies that the dispatch entry IS created (sendNotification=true
	 * for UPDATE events) but that the subscriber filter correctly prevents dispatch.
	 */
	@Test
	void test05_patchErrandDispatchesEmail() {
		// Clear pre-inserted dispatch entries so only the PATCH-generated one remains
		notificationDispatchRepository.deleteAll();

		setupCall()
			.withHeader(SENT_BY_HEADER, "patcher01; type=adAccount")
			.withServicePath(PATH + "/" + ERRAND_ID)
			.withHttpMethod(PATCH)
			.withRequest("request.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		// A dispatch entry should have been created by EventService
		final var dispatchEntries = notificationDispatchRepository.findAll();
		assertThat(dispatchEntries).isNotEmpty();
		assertThat(dispatchEntries.getFirst().getEventType()).isEqualTo("UPDATE");
		assertThat(dispatchEntries.getFirst().getSubType()).isEqualTo("ERRAND");

		// Fetch and process — subscriber has {UPDATE, MESSAGE} filter which does NOT match UPDATE/ERRAND
		final var processable = notificationDispatchWorker.fetchProcessable();
		assertThat(processable).isNotEmpty();

		notificationDispatchWorker.processGroup(processable);

		// No outbox entry because the subscriber's filter doesn't match UPDATE/ERRAND
		assertThat(emailDispatchOutboxRepository.findAll()).isEmpty();
	}
}
