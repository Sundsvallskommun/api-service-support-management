package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.support.TransactionTemplate;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.EmailDispatchOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.NotificationDispatchRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriberRepository;
import se.sundsvall.supportmanagement.integration.db.model.EmailDispatchOutboxEventEmbeddable;
import se.sundsvall.supportmanagement.service.scheduler.notificationdispatch.NotificationDispatchScheduler;
import se.sundsvall.supportmanagement.service.scheduler.notificationdispatch.NotificationDispatchWorker;

@WireMockAppTestSuite(files = "classpath:/EmailDispatchIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-email-dispatch-it.sql"
})
class EmailDispatchIT extends AbstractAppTest {

	private static final String PATH = "/2281/NAMESPACE-1/errands";
	private static final String ERRAND_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeee0001";
	private static final String EMAIL_SUBSCRIBER_ID = "11111111-0000-0000-0000-000000000001";
	private static final String UPDATE_DISPATCH_ID = "33333333-0000-0000-0000-000000000001";
	private static final String CREATE_DISPATCH_ID = "33333333-0000-0000-0000-000000000002";

	@Autowired
	private NotificationDispatchWorker notificationDispatchWorker;

	@Autowired
	private NotificationDispatchScheduler notificationDispatchScheduler;

	@Autowired
	private EmailDispatchOutboxRepository emailDispatchOutboxRepository;

	@Autowired
	private NotificationDispatchRepository notificationDispatchRepository;

	@Autowired
	private SubscriberRepository subscriberRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	/**
	 * Processing an UPDATE/MESSAGE dispatch for a subscriber with an EMAIL channel stores the subscriber, the errand and
	 * the event in the outbox.
	 */
	@Test
	void test01_emailSubscriberReceivesOutboxEntryOnUpdate() {
		setupCall();

		final var dispatchEntries = notificationDispatchWorker.fetchProcessable().stream()
			.filter(e -> "grp-001".equals(e.getRequestGroupId()))
			.toList();
		assertThat(dispatchEntries).isNotEmpty();

		notificationDispatchWorker.processGroup(dispatchEntries);

		transactionTemplate.executeWithoutResult(_ -> {
			final var outboxEntries = emailDispatchOutboxRepository.findAll();
			assertThat(outboxEntries).hasSize(1);

			final var outbox = outboxEntries.getFirst();
			assertThat(outbox.getSubscriber().getId()).isEqualTo(EMAIL_SUBSCRIBER_ID);
			assertThat(outbox.getErrandId()).isEqualTo(ERRAND_ID);
			assertThat(outbox.getErrandNumber()).isEqualTo("NS1-2024-000001");
			assertThat(outbox.getCreated()).isNotNull();
			assertThat(outbox.getEvents()).containsExactly(EmailDispatchOutboxEventEmbeddable.create().withEventId("evt-001").withDescription("Nytt meddelande"));
		});
	}

	/**
	 * The notification dispatch job sends what it enqueued within the same run, one email per subscriber, and empties
	 * the outbox.
	 */
	@Test
	void test02_outboxProcessorSendsEmailViaMessaging() {
		setupCall();
		notificationDispatchRepository.deleteById(CREATE_DISPATCH_ID);

		notificationDispatchScheduler.processDispatch();

		assertThat(notificationDispatchRepository.findAll()).isEmpty();
		assertThat(emailDispatchOutboxRepository.findAll()).isEmpty();
		verifyStubs();
	}

	/**
	 * A CREATE/ERRAND dispatch reaches a subscriber whose event filter is {CREATE, null}, all the way from dispatch
	 * entry to email.
	 */
	@Test
	void test03_createEventDispatchesEmailToSubscriber() {
		setupCall();
		notificationDispatchRepository.deleteById(UPDATE_DISPATCH_ID);

		notificationDispatchScheduler.processDispatch();

		assertThat(notificationDispatchRepository.findAll()).isEmpty();
		assertThat(emailDispatchOutboxRepository.findAll()).isEmpty();
		verifyStubs();
	}

	/**
	 * Removing a subscriber removes what is waiting in the outbox for them, so nothing is sent to someone who is gone.
	 */
	@Test
	void test04_deletingSubscriberRemovesItsOutboxEntries() {
		setupCall();

		final var dispatchEntries = notificationDispatchWorker.fetchProcessable().stream()
			.filter(e -> "grp-001".equals(e.getRequestGroupId()))
			.toList();
		notificationDispatchWorker.processGroup(dispatchEntries);
		assertThat(emailDispatchOutboxRepository.findAll()).hasSize(1);

		subscriberRepository.deleteById(EMAIL_SUBSCRIBER_ID);

		assertThat(emailDispatchOutboxRepository.findAll()).isEmpty();
	}

	/**
	 * A PATCH creates an UPDATE/ERRAND dispatch entry, which does not match the subscriber's {UPDATE, MESSAGE} filter, so
	 * nothing is put in the outbox.
	 */
	@Test
	void test05_patchErrandDispatchesEmail() {
		notificationDispatchRepository.deleteAll();

		setupCall()
			.withHeader(SENT_BY_HEADER, "patcher01; type=adAccount")
			.withServicePath(PATH + "/" + ERRAND_ID)
			.withHttpMethod(PATCH)
			.withRequest("request.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		final var dispatchEntries = notificationDispatchRepository.findAll();
		assertThat(dispatchEntries).isNotEmpty();
		assertThat(dispatchEntries.getFirst().getEventType()).isEqualTo("UPDATE");
		assertThat(dispatchEntries.getFirst().getSubType()).isEqualTo("ERRAND");

		final var processable = notificationDispatchWorker.fetchProcessable();
		assertThat(processable).isNotEmpty();

		notificationDispatchWorker.processGroup(processable);

		assertThat(emailDispatchOutboxRepository.findAll()).isEmpty();
	}
}
