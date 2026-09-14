package se.sundsvall.supportmanagement.apptest;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.scheduling.health.Dept44CompositeHealthContributor;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessActivityRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.service.scheduler.processevent.ProcessEventRelay;
import se.sundsvall.supportmanagement.service.scheduler.processevent.ProcessEventScheduler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;
import static java.util.Comparator.comparing;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.ERROR;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.RUNNING;

/**
 * The relay against pw-alkt over the wire, with WireMock standing in for it.
 * <p>
 * What the tests below the relay cannot show: which answers leave a row as it was and which consume it, read back from
 * the database rather than from a log; that what pw-alkt receives is the row as it stands; that the order within an
 * errand survives both the batch limit and a failure; and that the direct run, the scheduled run and a full pool can
 * meet without delivering anything twice or failing the write of an errand.
 * <p>
 * Rows are written straight into the table, aged by setting when they were written, so that the scheduled run takes
 * them without waiting out the transaction buffer. The ones that come from a write to an errand are there to exercise
 * the direct run, which only a committed publication starts.
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
class ProcessEventRelayIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
	private static final String OTHER_ERRAND_ID = "cc236cf1-c00f-4479-8341-ecf5dd90b5b9";
	private static final String THIRD_ERRAND_ID = "1be673c0-6ba3-4fb0-af4a-43acf23389f6";
	private static final String PROCESS_KEY = "alkt-ansokan";
	private static final String PW_ALKT_PATH = "/api-pw-alkt/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/process/errand-events";
	private static final String RELAY_JOB = "process_event_relay";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Autowired
	private ProcessEventScheduler processEventScheduler;

	@Autowired
	private ProcessEventRelay processEventRelay;

	@Autowired
	private ProcessEventOutboxRepository outboxRepository;

	@Autowired
	private ErrandProcessRepository processRepository;

	@Autowired
	private ErrandProcessActivityRepository activityRepository;

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

	@BeforeEach
	void setUp() {
		wiremock.resetAll();
		circuitBreakerRegistry.circuitBreaker("pw-alkt").reset();
		healthContributor.getOrCreateIndicator(RELAY_JOB).setHealthy();
	}

	@Test
	@DisplayName("Verification that pw-alkt receives each row as it stands, the start permission and the signal name included, and that the rows are acknowledged")
	void test01_theEventIsTheRowAsItStands() {
		givenRow("row-signal", ERRAND_ID, Duration.ofMinutes(2), "SIGNAL", true, "granskning-godkand");
		givenRow("row-message", OTHER_ERRAND_ID, Duration.ofMinutes(1), "MESSAGE", false, null);
		pwAlktAnswers(aResponse().withStatus(202));

		processEventScheduler.relay();

		assertThat(outboxRepository.findAllById(List.of("row-signal", "row-message"))).hasSize(2).allSatisfy(row -> assertThat(row.getDeliveredAt()).isNotNull());
		assertThat(eventsReceived()).satisfiesExactly(
			signal -> {
				assertThat(signal.path("eventId").asString()).isEqualTo("row-signal");
				assertThat(signal.path("eventType").asString()).isEqualTo("UPDATE");
				assertThat(signal.path("eventSubType").asString()).isEqualTo("SIGNAL");
				assertThat(signal.path("errandId").asString()).isEqualTo(ERRAND_ID);
				assertThat(signal.path("processKey").asString()).isEqualTo(PROCESS_KEY);
				assertThat(signal.path("startAllowed").booleanValue()).isTrue();
				assertThat(signal.path("signalName").asString()).isEqualTo("granskning-godkand");
				assertThat(signal.path("occurredAt").asString()).isNotBlank();
			},
			message -> {
				assertThat(message.path("eventId").asString()).isEqualTo("row-message");
				assertThat(message.path("eventSubType").asString()).isEqualTo("MESSAGE");
				assertThat(message.path("startAllowed").booleanValue()).isFalse();
				assertThat(message.path("signalName").isNull() || message.path("signalName").isMissingNode()).isTrue();
			});
	}

	@Test
	@DisplayName("Verification that a 422 consumes the row without trying again, fails the live instance and writes an error entry on it")
	void test02_aRefusalForGoodFailsTheProcess() {
		final var instance = processRepository.saveAndFlush(liveInstance());
		givenRow("row-1", ERRAND_ID, Duration.ofMinutes(1));
		pwAlktAnswers(refusalForGood());

		processEventScheduler.relay();
		processEventScheduler.relay();

		assertThat(outboxRepository.findById("row-1")).get().extracting(ProcessEventOutboxEntity::getDeliveredAt).isNotNull();
		assertThat(processRepository.findById(instance.getId())).get().satisfies(process -> {
			assertThat(process.getProcessStatus()).isEqualTo(FAILED);
			assertThat(process.getActiveMarker()).isNull();
			assertThat(process.getErrorCode()).isEqualTo("PROCESS_KEY_NOT_DEPLOYED");
		});
		assertThat(activityRepository.findByErrandId(ERRAND_ID, Pageable.unpaged())).singleElement().satisfies(entry -> {
			assertThat(entry.getErrandProcessId()).isEqualTo(instance.getId());
			assertThat(entry.getActivityType()).isEqualTo("DELIVERY");
			assertThat(entry.getSeverity()).isEqualTo(ERROR);
			assertThat(entry.getErrorCode()).isEqualTo("PROCESS_KEY_NOT_DEPLOYED");
		});
		wiremock.verify(1, postRequestedFor(urlPathEqualTo(PW_ALKT_PATH)));
	}

	@Test
	@DisplayName("Verification that a 422 on an errand without a process writes the error entry alone, without an instance")
	void test03_aRefusalForGoodWithoutAProcess() {
		givenRow("row-1", ERRAND_ID, Duration.ofMinutes(1));
		pwAlktAnswers(refusalForGood());

		processEventScheduler.relay();

		assertThat(outboxRepository.findById("row-1")).get().extracting(ProcessEventOutboxEntity::getDeliveredAt).isNotNull();
		assertThat(activityRepository.findByErrandId(ERRAND_ID, Pageable.unpaged())).singleElement().satisfies(entry -> {
			assertThat(entry.getErrandProcessId()).isNull();
			assertThat(entry.getSeverity()).isEqualTo(ERROR);
		});
	}

	@Test
	@DisplayName("Verification that a 503 leaves the row exactly as it was, and that the next run delivers it")
	void test04_aServerErrorLeavesTheRowForTheNextRun() {
		givenRow("row-1", ERRAND_ID, Duration.ofMinutes(1));
		final var before = outboxRepository.findById("row-1").orElseThrow();
		pwAlktAnswers(aResponse().withStatus(503));

		processEventScheduler.relay();

		assertThat(outboxRepository.findById("row-1")).contains(before);
		wiremock.verify(1, postRequestedFor(urlPathEqualTo(PW_ALKT_PATH)));

		pwAlktNowAnswers(aResponse().withStatus(202));

		processEventScheduler.relay();

		assertThat(outboxRepository.findById("row-1")).get().extracting(ProcessEventOutboxEntity::getDeliveredAt).isNotNull();
	}

	@Test
	@DisplayName("Verification that a pw-alkt that does not answer in time leaves the row exactly as it was")
	void test05_aTimeoutLeavesTheRowAsItWas() {
		givenRow("row-1", ERRAND_ID, Duration.ofMinutes(1));
		final var before = outboxRepository.findById("row-1").orElseThrow();
		pwAlktAnswers(aResponse().withStatus(202).withFixedDelay(5000));

		processEventScheduler.relay();

		assertThat(outboxRepository.findById("row-1")).contains(before);
		// Twice, since the token retryer every client of the service is given takes a timeout for a reason to try again
		wiremock.verify(2, postRequestedFor(urlPathEqualTo(PW_ALKT_PATH)));
	}

	@Test
	@DisplayName("Verification that pw-alkt refusing events keeps the circuit breaker closed, since only calls that never got an answer open it")
	void test15_refusedEventsDoNotOpenTheCircuitBreaker() {
		givenRow("row-1", ERRAND_ID, Duration.ofMinutes(3));
		givenRow("row-2", OTHER_ERRAND_ID, Duration.ofMinutes(2));
		pwAlktAnswers(aResponse().withStatus(503));

		processEventScheduler.relay();
		processEventScheduler.relay();
		processEventScheduler.relay();

		assertThat(circuitBreakerRegistry.circuitBreaker("pw-alkt").getState()).isEqualTo(CLOSED);
		wiremock.verify(6, postRequestedFor(urlPathEqualTo(PW_ALKT_PATH)));
	}

	@Test
	@DisplayName("Verification that a token that cannot be fetched opens the circuit breaker, since pw-alkt is then as far out of reach as when it does not answer")
	void test16_aTokenThatCannotBeFetchedOpensTheCircuitBreaker() {
		givenRow("row-1", ERRAND_ID, Duration.ofMinutes(2));
		givenRow("row-2", OTHER_ERRAND_ID, Duration.ofMinutes(1));
		wiremock.stubFor(post(urlPathEqualTo("/api-gateway/token")).willReturn(aResponse().withStatus(500)));
		wiremock.stubFor(post(urlPathEqualTo(PW_ALKT_PATH)).willReturn(aResponse().withStatus(202)));

		processEventScheduler.relay();
		processEventScheduler.relay();

		assertThat(circuitBreakerRegistry.circuitBreaker("pw-alkt").getState()).isEqualTo(OPEN);
		assertThat(wiremock.findAll(postRequestedFor(urlPathEqualTo(PW_ALKT_PATH)))).isEmpty();
		assertThat(outboxRepository.findAllById(List.of("row-1", "row-2"))).allSatisfy(row -> assertThat(row.getDeliveredAt()).isNull());
	}

	@Test
	@DisplayName("Verification that the rows of an errand reach pw-alkt in the order they were written, also when the batch limit splits them over two runs")
	void test06_theOrderWithinAnErrandHolds() {
		givenRow("row-1", ERRAND_ID, Duration.ofMinutes(3));
		givenRow("row-2", ERRAND_ID, Duration.ofMinutes(2));
		givenRow("row-3", ERRAND_ID, Duration.ofMinutes(1));
		pwAlktAnswers(aResponse().withStatus(202));

		processEventScheduler.relay();

		assertThat(eventIdsReceived()).containsExactly("row-1", "row-2");
		assertThat(outboxRepository.findById("row-3")).get().extracting(ProcessEventOutboxEntity::getDeliveredAt).isNull();

		processEventScheduler.relay();

		assertThat(eventIdsReceived()).containsExactly("row-1", "row-2", "row-3");
	}

	@Test
	@DisplayName("Verification that a group that fails is rolled back in full, and that pw-alkt is given the very same event again on the next run")
	void test07_aFailingGroupIsRolledBackInFull() {
		givenRow("row-1", ERRAND_ID, Duration.ofMinutes(2));
		givenRow("row-2", ERRAND_ID, Duration.ofMinutes(1));
		tokenIsIssued();
		wiremock.stubFor(post(urlPathEqualTo(PW_ALKT_PATH)).atPriority(1)
			.withRequestBody(matchingJsonPath("$.eventId", equalTo("row-2")))
			.willReturn(aResponse().withStatus(503)));
		wiremock.stubFor(post(urlPathEqualTo(PW_ALKT_PATH)).atPriority(5).willReturn(aResponse().withStatus(202)));

		processEventScheduler.relay();

		// pw-alkt took the first row, but the transaction it was acknowledged in went down with the second
		assertThat(eventIdsReceived()).containsExactly("row-1", "row-2");
		assertThat(outboxRepository.findAllById(List.of("row-1", "row-2"))).allSatisfy(row -> assertThat(row.getDeliveredAt()).isNull());

		pwAlktNowAnswers(aResponse().withStatus(202));

		processEventScheduler.relay();

		assertThat(outboxRepository.findAllById(List.of("row-1", "row-2"))).allSatisfy(row -> assertThat(row.getDeliveredAt()).isNotNull());
		final var events = eventsReceived();
		assertThat(events).extracting(event -> event.path("eventId").asString()).containsExactly("row-1", "row-2", "row-1", "row-2");
		assertThat(events.get(2)).isEqualTo(events.get(0));
	}

	@Test
	@DisplayName("Verification that a run takes no more than the batch size, oldest first, and leaves the rest for the next run")
	void test08_aRunTakesAtMostABatch() {
		givenRow("row-1", ERRAND_ID, Duration.ofMinutes(3));
		givenRow("row-2", OTHER_ERRAND_ID, Duration.ofMinutes(2));
		givenRow("row-3", THIRD_ERRAND_ID, Duration.ofMinutes(1));
		pwAlktAnswers(aResponse().withStatus(202));

		processEventScheduler.relay();

		assertThat(eventIdsReceived()).containsExactly("row-1", "row-2");
		assertThat(outboxRepository.findById("row-3")).get().extracting(ProcessEventOutboxEntity::getDeliveredAt).isNull();
	}

	@Test
	@DisplayName("Verification that a write to an errand reaches pw-alkt through the direct run, with no scheduled run involved")
	void test09_aDirectRunDeliversOnceTheWriteIsCommitted() {
		pwAlktAnswers(aResponse().withStatus(202));
		errandWritesAreLogged();

		assertThat(patchErrand().getStatusCode()).isEqualTo(OK);

		await().atMost(10, SECONDS).untilAsserted(() -> assertThat(outboxRepository.findAll())
			.singleElement()
			.satisfies(row -> assertThat(row.getDeliveredAt()).isNotNull()));
		assertThat(eventsReceived()).singleElement().satisfies(event -> {
			assertThat(event.path("errandId").asString()).isEqualTo(ERRAND_ID);
			assertThat(event.path("eventSubType").asString()).isEqualTo("ERRAND");
		});
		assertThat(wiremock.findAllUnmatchedRequests()).isEmpty();
	}

	@Test
	@DisplayName("Verification that a full pool drops the direct run without failing the write, and that the scheduled run delivers the row instead")
	void test10_aFullPoolDropsTheDirectRun() {
		pwAlktAnswers(aResponse().withStatus(202));
		errandWritesAreLogged();
		final var release = new CountDownLatch(1);

		try {
			final var capacity = processEventExecutor.getMaxPoolSize() + processEventExecutor.getQueueCapacity();
			for (var i = 0; i < capacity; i++) {
				processEventExecutor.execute(() -> awaitQuietly(release));
			}
			await().atMost(10, SECONDS).until(() -> processEventExecutor.getActiveCount() == processEventExecutor.getMaxPoolSize());
			assertThat(processEventExecutor.getQueueSize()).isEqualTo(processEventExecutor.getQueueCapacity());

			assertThat(patchErrand().getStatusCode()).isEqualTo(OK);
		} finally {
			release.countDown();
		}

		await().atMost(10, SECONDS).until(() -> processEventExecutor.getActiveCount() == 0 && processEventExecutor.getQueueSize() == 0);

		// Dropped rather than queued: the pool has drained, and still nothing has reached pw-alkt
		assertThat(wiremock.findAll(postRequestedFor(urlPathEqualTo(PW_ALKT_PATH)))).isEmpty();
		final var row = outboxRepository.findAll().getFirst();
		assertThat(row.getDeliveredAt()).isNull();

		ageRow(row.getId(), Duration.ofMinutes(1));
		processEventScheduler.relay();

		assertThat(outboxRepository.findById(row.getId())).get().extracting(ProcessEventOutboxEntity::getDeliveredAt).isNotNull();
		assertThat(wiremock.findAllUnmatchedRequests()).isEmpty();
	}

	@Test
	@DisplayName("Verification that a direct run and a scheduled run reaching for the same row deliver it once: the second waits for the first and finds it delivered")
	void test11_twoRunsReachingForTheSameRowDeliverItOnce() {
		givenRow("row-1", ERRAND_ID, Duration.ofMinutes(1));
		pwAlktAnswers(aResponse().withStatus(202).withFixedDelay(1500));

		final var directRun = CompletableFuture.runAsync(() -> processEventRelay.relayErrand(ERRAND_ID));
		await().atMost(10, SECONDS).until(() -> !wiremock.findAll(postRequestedFor(urlPathEqualTo(PW_ALKT_PATH))).isEmpty());

		processEventScheduler.relay();
		directRun.join();

		wiremock.verify(1, postRequestedFor(urlPathEqualTo(PW_ALKT_PATH)));
		assertThat(outboxRepository.findById("row-1")).get().extracting(ProcessEventOutboxEntity::getDeliveredAt).isNotNull();
	}

	@Test
	@DisplayName("Verification that the health indicator stays green right after a publication and turns only once the oldest undelivered row has passed the limit")
	void test12_theHealthIndicatorGoesByAge() {
		givenRow("row-1", ERRAND_ID, Duration.ofSeconds(10));
		pwAlktAnswers(aResponse().withStatus(503));

		processEventScheduler.relay();

		assertThat(relayHealth()).isEqualTo("UP");

		ageRow("row-1", Duration.ofMinutes(16));
		processEventScheduler.relay();

		assertThat(relayHealth()).isEqualTo("RESTRICTED");

		pwAlktNowAnswers(aResponse().withStatus(202));
		processEventScheduler.relay();

		assertThat(relayHealth()).isEqualTo("UP");
	}

	@Test
	@DisplayName("Verification that a row that has passed the age limit is dropped undelivered, while the rest are delivered as usual")
	void test13_aRowThatHasAgedOutIsDropped() {
		givenRow("row-aged", ERRAND_ID, Duration.ofDays(31));
		givenRow("row-fresh", OTHER_ERRAND_ID, Duration.ofMinutes(1));
		pwAlktAnswers(aResponse().withStatus(202));

		processEventScheduler.relay();

		assertThat(outboxRepository.findById("row-aged")).isEmpty();
		assertThat(eventIdsReceived()).containsExactly("row-fresh");
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

	private void givenRow(final String id, final String errandId, final Duration age) {
		givenRow(id, errandId, age, "MESSAGE", false, null);
	}

	private void givenRow(final String id, final String errandId, final Duration age, final String eventSubType, final boolean startAllowed, final String signalName) {
		jdbcTemplate.update("""
			insert into process_event_outbox(id, municipality_id, namespace, errand_id, process_service, process_key, event_type,
			                                 event_sub_type, start_allowed, signal_name, executed_by, created)
			values (?, ?, ?, ?, 'pw-alkt', ?, 'UPDATE', ?, ?, ?, 'joe01doe', ?)""",
			id, MUNICIPALITY_ID, NAMESPACE, errandId, PROCESS_KEY, eventSubType, startAllowed, signalName, ago(age));
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

	private ErrandProcessEntity liveInstance() {
		final var instance = ErrandProcessEntity.create()
			.withErrandId(ERRAND_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withProcessService("pw-alkt")
			.withProcessKey(PROCESS_KEY)
			.withProcessInstanceId("pi-relay-it");
		instance.applyStatus(RUNNING, clock);

		return instance;
	}

	private void pwAlktAnswers(final ResponseDefinitionBuilder response) {
		tokenIsIssued();
		wiremock.stubFor(post(urlPathEqualTo(PW_ALKT_PATH)).willReturn(response));
	}

	/**
	 * Replaces the stubs while keeping the requests received so far, so that a test can count across runs.
	 */
	private void pwAlktNowAnswers(final ResponseDefinitionBuilder response) {
		wiremock.resetMappings();
		pwAlktAnswers(response);
	}

	private void tokenIsIssued() {
		wiremock.stubFor(post(urlPathEqualTo("/api-gateway/token"))
			.willReturn(aResponse()
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.toString())
				.withBodyFile("common/responses/api-gateway-token-response.json")));
	}

	private void errandWritesAreLogged() {
		wiremock.stubFor(post(urlPathMatching("/api-eventlog/.*")).willReturn(aResponse().withStatus(202)));
		wiremock.stubFor(get(urlPathMatching("/api-employee/.*")).willReturn(aResponse().withStatus(200)));
	}

	private static ResponseDefinitionBuilder refusalForGood() {
		return aResponse()
			.withStatus(422)
			.withHeader(HttpHeaders.CONTENT_TYPE, "application/problem+json")
			.withBody("""
				{"title": "Unprocessable Entity", "status": 422, "detail": "No process definition is deployed under the key alkt-ansokan"}""");
	}

	private ResponseEntity<String> patchErrand() {
		final var headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);

		return restTemplate.exchange("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID, PATCH, new HttpEntity<>("""
			{"title": "A change the process is to be told about"}""", headers), String.class);
	}

	/**
	 * The events pw-alkt has received, in the order it received them.
	 */
	private List<JsonNode> eventsReceived() {
		return wiremock.findAll(postRequestedFor(urlPathEqualTo(PW_ALKT_PATH))).stream()
			.sorted(comparing(LoggedRequest::getLoggedDate))
			.map(request -> OBJECT_MAPPER.readTree(request.getBodyAsString()))
			.toList();
	}

	private List<String> eventIdsReceived() {
		return eventsReceived().stream()
			.map(event -> event.path("eventId").asString())
			.toList();
	}

	private String relayHealth() {
		return healthContributor.getOrCreateIndicator(RELAY_JOB).health().getStatus().getCode();
	}

	private static void awaitQuietly(final CountDownLatch latch) {
		try {
			latch.await(30, SECONDS);
		} catch (final InterruptedException _) {
			Thread.currentThread().interrupt();
		}
	}
}
