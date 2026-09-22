package se.sundsvall.supportmanagement.apptest;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import se.sundsvall.dept44.scheduling.health.Dept44CompositeHealthContributor;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.service.scheduler.processevent.ProcessEventRelay;
import se.sundsvall.supportmanagement.service.scheduler.processevent.ProcessEventScheduler;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

/**
 * The relay against pw-alkt over the wire, with WireMock standing in for it.
 * <p>
 * Verifies which answers leave a row as it was and which consume it; that what pw-alkt receives is the row as it
 * stands; that the order within an errand survives both the batch limit and a failure; and that the direct run, the
 * scheduled run and a full pool can meet without delivering anything twice or failing the write of an errand.
 * <p>
 * Neither whether a row is delivered nor the state of the circuit breaker is shown on its own by any resource, so the
 * first is read from the database and the second from the registry. Rows are written straight into the table, aged by
 * setting when they were written, so that the scheduled run takes them without waiting out the transaction buffer. The
 * rows that come from a write to an errand exercise the direct run, which only a committed publication starts.
 * <p>
 * The order pw-alkt receives events in is held by scenarios in the stubs: each event is only answered in the state the
 * one before it leaves behind.
 */
@WireMockAppTestSuite(files = "classpath:/ProcessEventRelayIT/", classes = Application.class)
@TestPropertySource(properties = {
	"process-engine.direct-run.enabled=true",
	"integration.pw-alkt.read-timeout=3",
	"scheduler.process-event.batch-size=2"
})
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-process-event.sql"
})
@SqlMergeMode(MERGE)
class ProcessEventRelayIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
	private static final String OTHER_ERRAND_ID = "cc236cf1-c00f-4479-8341-ecf5dd90b5b9";
	private static final String THIRD_ERRAND_ID = "1be673c0-6ba3-4fb0-af4a-43acf23389f6";
	private static final String ERRAND_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID;
	private static final String PW_ALKT_PATH = "/api-pw-alkt/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/process/errand-events";
	private static final String PW_ALKT = "pw-alkt";
	private static final String RELAY_JOB = "process_event_relay";
	private static final String RELAY_HEALTH_PATH = "/actuator/health/dept44CompositeScheduler/" + RELAY_JOB;

	@Autowired
	private ProcessEventScheduler processEventScheduler;

	@Autowired
	private ProcessEventRelay processEventRelay;

	@Autowired
	private ProcessEventOutboxRepository outboxRepository;

	@Autowired
	private Dept44CompositeHealthContributor healthContributor;

	@Autowired
	private CircuitBreakerRegistry circuitBreakerRegistry;

	@Autowired
	@Qualifier("processEventExecutor")
	private ThreadPoolTaskExecutor processEventExecutor;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Clock clock;

	/**
	 * The circuit breaker and the health of the relay outlive a test, and some tests leave them open and restricted. The
	 * scheduled relay never runs on its own in the tests, so its health is registered the way a healthy run leaves it.
	 */
	@BeforeEach
	void resetTheStateOfTheRelay() {
		circuitBreakerRegistry.circuitBreaker(PW_ALKT).reset();
		healthContributor.getOrCreateIndicator(RELAY_JOB).setHealthy();
	}

	@Test
	@DisplayName("Verification that pw-alkt receives each row as it stands, the start permission and the signal name included, and that the rows are acknowledged")
	void test01_theEventIsTheRowAsItStands() {
		givenRow("row-signal", ERRAND_ID, Duration.ofMinutes(2), "SIGNAL", true, "granskning-godkand");
		givenRow("row-message", OTHER_ERRAND_ID, Duration.ofMinutes(1), "MESSAGE", false, null);
		setupCall();

		processEventScheduler.relay();

		assertThat(outboxRepository.findAllById(List.of("row-signal", "row-message"))).hasSize(2).allSatisfy(row -> assertThat(row.getDeliveredAt()).isNotNull());
		verifyStubs();
	}

	@Test
	@DisplayName("Verification that a 422 consumes the row without trying again, fails the live instance and writes an error entry on it")
	@Sql("/db/scripts/testdata-process-event-live-instance.sql")
	void test02_aRefusalForGoodFailsTheProcess() {
		givenRow("row-1", ERRAND_ID, Duration.ofMinutes(1));
		setupCall();

		processEventScheduler.relay();
		processEventScheduler.relay();

		assertThat(outboxRepository.findById("row-1")).get().extracting(ProcessEventOutboxEntity::getDeliveredAt).isNotNull();
		wiremock.verify(1, postRequestedFor(urlPathEqualTo(PW_ALKT_PATH)));

		setupCall()
			.withServicePath(ERRAND_PATH + "/processes")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-processes.json")
			.sendRequest();

		setupCall()
			.withServicePath(ERRAND_PATH + "/process-activities")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-activities.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	@DisplayName("Verification that a 422 on an errand without a process writes the error entry alone, without an instance")
	void test03_aRefusalForGoodWithoutAProcess() {
		givenRow("row-1", ERRAND_ID, Duration.ofMinutes(1));
		setupCall();

		processEventScheduler.relay();

		assertThat(outboxRepository.findById("row-1")).get().extracting(ProcessEventOutboxEntity::getDeliveredAt).isNotNull();

		setupCall()
			.withServicePath(ERRAND_PATH + "/process-activities")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-activities.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	@DisplayName("Verification that a 503 leaves the row exactly as it was, and that the next run delivers it")
	void test04_aServerErrorLeavesTheRowForTheNextRun() {
		givenRow("row-1", ERRAND_ID, Duration.ofMinutes(1));
		final var before = outboxRepository.findById("row-1").orElseThrow();
		setupCall();

		processEventScheduler.relay();

		assertThat(outboxRepository.findById("row-1")).contains(before);
		wiremock.verify(1, postRequestedFor(urlPathEqualTo(PW_ALKT_PATH)));

		processEventScheduler.relay();

		assertThat(outboxRepository.findById("row-1")).get().extracting(ProcessEventOutboxEntity::getDeliveredAt).isNotNull();
		verifyStubs();
	}

	/**
	 * pw-alkt is asked twice, since the token retryer every client of the service is given takes a timeout for a reason
	 * to try again.
	 */
	@Test
	@DisplayName("Verification that a pw-alkt that does not answer in time leaves the row exactly as it was")
	void test05_aTimeoutLeavesTheRowAsItWas() {
		givenRow("row-1", ERRAND_ID, Duration.ofMinutes(1));
		final var before = outboxRepository.findById("row-1").orElseThrow();
		setupCall();

		processEventScheduler.relay();

		assertThat(outboxRepository.findById("row-1")).contains(before);
		wiremock.verify(2, postRequestedFor(urlPathEqualTo(PW_ALKT_PATH)));
		verifyStubs();
	}

	@Test
	@DisplayName("Verification that the rows of an errand reach pw-alkt in the order they were written, also when the batch limit splits them over two runs")
	void test06_theOrderWithinAnErrandHolds() {
		givenRow("row-1", ERRAND_ID, Duration.ofMinutes(3));
		givenRow("row-2", ERRAND_ID, Duration.ofMinutes(2));
		givenRow("row-3", ERRAND_ID, Duration.ofMinutes(1));
		setupCall();

		processEventScheduler.relay();

		wiremock.verify(2, postRequestedFor(urlPathEqualTo(PW_ALKT_PATH)));
		assertThat(outboxRepository.findById("row-3")).get().extracting(ProcessEventOutboxEntity::getDeliveredAt).isNull();

		processEventScheduler.relay();

		wiremock.verify(3, postRequestedFor(urlPathEqualTo(PW_ALKT_PATH)));
		verifyStubs();
	}

	/**
	 * pw-alkt takes the first row, but the transaction it is acknowledged in goes down with the second, so the next run
	 * gives it both again, the first as the very event it was given before.
	 */
	@Test
	@DisplayName("Verification that a group that fails is rolled back in full, and that pw-alkt is given the very same event again on the next run")
	void test07_aFailingGroupIsRolledBackInFull() {
		givenRow("row-1", ERRAND_ID, Duration.ofMinutes(2));
		givenRow("row-2", ERRAND_ID, Duration.ofMinutes(1));
		setupCall();

		processEventScheduler.relay();

		assertThat(outboxRepository.findAllById(List.of("row-1", "row-2"))).allSatisfy(row -> assertThat(row.getDeliveredAt()).isNull());

		processEventScheduler.relay();

		assertThat(outboxRepository.findAllById(List.of("row-1", "row-2"))).allSatisfy(row -> assertThat(row.getDeliveredAt()).isNotNull());
		final var eventsOfTheFirstRow = wiremock.findAll(postRequestedFor(urlPathEqualTo(PW_ALKT_PATH)).withRequestBody(matchingJsonPath("$.eventId", equalTo("row-1")))).stream()
			.map(LoggedRequest::getBodyAsString)
			.toList();
		assertThat(eventsOfTheFirstRow).hasSize(2).containsOnly(eventsOfTheFirstRow.getFirst());
		verifyStubs();
	}

	@Test
	@DisplayName("Verification that a run takes no more than the batch size, oldest first, and leaves the rest for the next run")
	void test08_aRunTakesAtMostABatch() {
		givenRow("row-1", ERRAND_ID, Duration.ofMinutes(3));
		givenRow("row-2", OTHER_ERRAND_ID, Duration.ofMinutes(2));
		givenRow("row-3", THIRD_ERRAND_ID, Duration.ofMinutes(1));
		setupCall();

		processEventScheduler.relay();

		assertThat(outboxRepository.findById("row-3")).get().extracting(ProcessEventOutboxEntity::getDeliveredAt).isNull();
		verifyStubs();
	}

	@Test
	@DisplayName("Verification that a write to an errand reaches pw-alkt through the direct run, with no scheduled run involved")
	void test09_aDirectRunDeliversOnceTheWriteIsCommitted() {
		setupCall()
			.withServicePath(ERRAND_PATH)
			.withHttpMethod(PATCH)
			.withRequest("request.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		await().atMost(10, SECONDS).untilAsserted(() -> assertThat(outboxRepository.findAll())
			.singleElement()
			.satisfies(row -> assertThat(row.getDeliveredAt()).isNotNull()));
		verifyStubs();
	}

	/**
	 * The direct run is dropped rather than queued: once the pool has drained, still nothing has reached pw-alkt.
	 */
	@Test
	@DisplayName("Verification that a full pool drops the direct run without failing the write, and that the scheduled run delivers the row instead")
	void test10_aFullPoolDropsTheDirectRun() {
		final var release = new CountDownLatch(1);

		try {
			await().atMost(10, SECONDS).pollInterval(Duration.ofMillis(10)).until(() -> fillPool(release));

			setupCall()
				.withServicePath(ERRAND_PATH)
				.withHttpMethod(PATCH)
				.withRequest("request.json")
				.withExpectedResponseStatus(OK)
				.sendRequest();
		} finally {
			release.countDown();
		}

		await().atMost(10, SECONDS).until(() -> processEventExecutor.getActiveCount() == 0 && processEventExecutor.getQueueSize() == 0);

		wiremock.verify(0, postRequestedFor(urlPathEqualTo(PW_ALKT_PATH)));
		final var row = outboxRepository.findAll().getFirst();
		assertThat(row.getDeliveredAt()).isNull();

		ageRow(row.getId(), Duration.ofMinutes(1));
		processEventScheduler.relay();

		assertThat(outboxRepository.findById(row.getId())).get().extracting(ProcessEventOutboxEntity::getDeliveredAt).isNotNull();
		verifyStubs();
	}

	/**
	 * The second run waits for the first and finds the row delivered.
	 */
	@Test
	@DisplayName("Verification that a direct run and a scheduled run reaching for the same row deliver it once")
	void test11_twoRunsReachingForTheSameRowDeliverItOnce() {
		givenRow("row-1", ERRAND_ID, Duration.ofMinutes(1));
		setupCall();

		final var directRun = CompletableFuture.runAsync(() -> processEventRelay.relayErrand(ERRAND_ID));
		await().atMost(10, SECONDS).untilAsserted(() -> wiremock.verify(1, postRequestedFor(urlPathEqualTo(PW_ALKT_PATH))));

		processEventScheduler.relay();
		directRun.join();

		wiremock.verify(1, postRequestedFor(urlPathEqualTo(PW_ALKT_PATH)));
		assertThat(outboxRepository.findById("row-1")).get().extracting(ProcessEventOutboxEntity::getDeliveredAt).isNotNull();
		verifyStubs();
	}

	@Test
	@DisplayName("Verification that the health indicator stays green right after a publication and turns only once the oldest undelivered row has passed the limit")
	void test12_theHealthIndicatorGoesByAge() {
		givenRow("row-1", ERRAND_ID, Duration.ofSeconds(10));
		setupCall();

		processEventScheduler.relay();

		setupCall()
			.withServicePath(RELAY_HEALTH_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-health-up.json")
			.sendRequest();

		ageRow("row-1", Duration.ofMinutes(16));
		processEventScheduler.relay();

		setupCall()
			.withServicePath(RELAY_HEALTH_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-health-restricted.json")
			.sendRequest();

		processEventScheduler.relay();

		setupCall()
			.withServicePath(RELAY_HEALTH_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-health-up.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	@DisplayName("Verification that a row that has passed the age limit is dropped undelivered, while the rest are delivered as usual")
	void test13_aRowThatHasAgedOutIsDropped() {
		givenRow("row-aged", ERRAND_ID, Duration.ofDays(31));
		givenRow("row-fresh", OTHER_ERRAND_ID, Duration.ofMinutes(1));
		setupCall();

		processEventScheduler.relay();

		assertThat(outboxRepository.findById("row-aged")).isEmpty();
		verifyStubs();
	}

	@Test
	@DisplayName("Verification that the cleanup removes rows delivered long enough ago, and leaves undelivered rows alone however old they are")
	void test14_theCleanupLeavesUndeliveredRowsAlone() {
		givenRow("row-delivered-long-ago", ERRAND_ID, Duration.ofDays(3));
		deliverRow("row-delivered-long-ago", Duration.ofDays(3).minusMinutes(1));
		givenRow("row-delivered-lately", ERRAND_ID, Duration.ofHours(1));
		deliverRow("row-delivered-lately", Duration.ofHours(1).minusMinutes(1));
		givenRow("row-undelivered", OTHER_ERRAND_ID, Duration.ofDays(3));

		processEventScheduler.cleanUp();

		assertThat(outboxRepository.findAll()).extracting(ProcessEventOutboxEntity::getId).containsExactlyInAnyOrder("row-delivered-lately", "row-undelivered");
	}

	@Test
	@DisplayName("Verification that pw-alkt refusing events keeps the circuit breaker closed, since only calls that never got an answer open it")
	void test15_refusedEventsDoNotOpenTheCircuitBreaker() {
		givenRow("row-1", ERRAND_ID, Duration.ofMinutes(3));
		givenRow("row-2", OTHER_ERRAND_ID, Duration.ofMinutes(2));
		setupCall();

		processEventScheduler.relay();
		processEventScheduler.relay();
		processEventScheduler.relay();

		wiremock.verify(6, postRequestedFor(urlPathEqualTo(PW_ALKT_PATH)));
		assertThat(circuitBreakerRegistry.circuitBreaker(PW_ALKT).getState()).isEqualTo(CLOSED);
		verifyStubs();
	}

	/**
	 * pw-alkt is never asked, so any call to it would go unanswered by the stubs.
	 */
	@Test
	@DisplayName("Verification that a token that cannot be fetched opens the circuit breaker, since pw-alkt is then as far out of reach as when it does not answer")
	void test16_aTokenThatCannotBeFetchedOpensTheCircuitBreaker() {
		givenRow("row-1", ERRAND_ID, Duration.ofMinutes(2));
		givenRow("row-2", OTHER_ERRAND_ID, Duration.ofMinutes(1));
		setupCall();

		processEventScheduler.relay();
		processEventScheduler.relay();

		assertThat(outboxRepository.findAllById(List.of("row-1", "row-2"))).allSatisfy(row -> assertThat(row.getDeliveredAt()).isNull());
		assertThat(circuitBreakerRegistry.circuitBreaker(PW_ALKT).getState()).isEqualTo(OPEN);
		verifyStubs();
	}

	private void givenRow(final String id, final String errandId, final Duration age) {
		givenRow(id, errandId, age, "MESSAGE", false, null);
	}

	private void givenRow(final String id, final String errandId, final Duration age, final String eventSubType, final boolean startAllowed, final String signalName) {
		jdbcTemplate.update("""
			insert into process_event_outbox(id, municipality_id, namespace, errand_id, process_service, process_key, event_type,
			                                 event_sub_type, start_allowed, signal_name, executed_by, created)
			values (?, ?, ?, ?, 'pw-alkt', 'alkt-ansokan', 'UPDATE', ?, ?, ?, 'joe01doe', ?)""",
			id, MUNICIPALITY_ID, NAMESPACE, errandId, eventSubType, startAllowed, signalName, ago(age));
	}

	private void ageRow(final String id, final Duration age) {
		jdbcTemplate.update("update process_event_outbox set created = ? where id = ?", ago(age), id);
	}

	private void deliverRow(final String id, final Duration age) {
		jdbcTemplate.update("update process_event_outbox set delivered_at = ? where id = ?", ago(age), id);
	}

	/**
	 * A moment as the entities store one: a wall clock in the default zone of the JVM.
	 */
	private Timestamp ago(final Duration age) {
		return Timestamp.valueOf(LocalDateTime.now(clock).minus(age));
	}

	/**
	 * Adds blocked runs while the pool has room, and answers whether every thread is busy and the queue is full. A run is
	 * only added while there is room, so none of them is ever dropped.
	 */
	private boolean fillPool(final CountDownLatch release) {
		while (processEventExecutor.getQueueSize() < processEventExecutor.getQueueCapacity() || processEventExecutor.getPoolSize() < processEventExecutor.getMaxPoolSize()) {
			processEventExecutor.execute(() -> awaitQuietly(release));
		}

		return processEventExecutor.getActiveCount() == processEventExecutor.getMaxPoolSize() && processEventExecutor.getQueueSize() == processEventExecutor.getQueueCapacity();
	}

	private static void awaitQuietly(final CountDownLatch latch) {
		try {
			latch.await(30, SECONDS);
		} catch (final InterruptedException _) {
			Thread.currentThread().interrupt();
		}
	}
}
