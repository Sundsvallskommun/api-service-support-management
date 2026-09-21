package se.sundsvall.supportmanagement.apptest;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.scheduler.processevent.ProcessEventRelay;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.TRIGGER_PROCESS_HEADER;

/**
 * Manual stepping over the wire: the process reports what it waits for, the errand shows it, a handler presses one of
 * the buttons, and the name of the gate reaches the process.
 * <p>
 * The outbox row and the event pw-alkt receives are both verified to carry the name of the signal. A signal that is
 * refused is verified to have written nothing at all: no activity entry, no outbox row, no errand event.
 * <p>
 * The namespace of testdata-it.sql names no process triggers at all, so every signal published here is published
 * without one. The direct run is off, so rows stay undelivered until a test delivers them.
 */
@WireMockAppTestSuite(files = "classpath:/ProcessSignalIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ProcessSignalIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "PROCESS-NAMESPACE";
	private static final String ERRANDS_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/";
	private static final String PW_ALKT_PATH = "/api-pw-alkt/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/process/errand-events";

	private static final String ERRAND_ID = "aa000000-0000-0000-0000-0000000000a1";
	private static final String ERRAND_WITHOUT_PROCESS_ID = "aa000000-0000-0000-0000-0000000000a2";
	private static final String PROCESS_INSTANCE_ID = "pi-it-live";
	private static final String PROCESS_KEY = "alkt-ansokan";

	private static final String APPROVE = "granskning-godkand";
	private static final String REJECT = "granskning-avvisad";

	private static final String HANDLER = "joe01doe";
	private static final String HANDLER_IDENTITY = HANDLER + "; type=adAccount";
	private static final String PROCESS_ENGINE = "pw-alkt; type=processEngine";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Autowired
	private ProcessEventOutboxRepository outboxRepository;

	@Autowired
	private ProcessEventRelay processEventRelay;

	@Autowired
	private NamespaceConfigService namespaceConfigService;

	@Autowired
	private ProcessEngineProperties processEngineProperties;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		wiremock.resetAll();
		wiremock.stubFor(post(urlPathEqualTo("/api-gateway/token"))
			.willReturn(aResponse()
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("common/responses/api-gateway-token-response.json")));
		wiremock.stubFor(post(urlPathMatching("/api-eventlog/.*")).willReturn(aResponse().withStatus(202)));
		wiremock.stubFor(post(urlPathEqualTo(PW_ALKT_PATH)).willReturn(aResponse().withStatus(202)));
	}

	/**
	 * The whole chain, from the report of the process to the event pw-alkt receives. Also verifies that the signal
	 * notifies no one, although the errand has a handler.
	 */
	@Test
	@DisplayName("Verification that an awaited signal gives an activity entry naming the sender, and an event carrying the name of the gate all the way to pw-alkt")
	void test01_anAwaitedSignalReachesTheProcessWithItsName() {
		assertThat(namespaceConfigService.getProcessTriggers(NAMESPACE, MUNICIPALITY_ID))
			.as("no trigger may name the signal for the test to show that the triggers have no say over it")
			.isEmpty();
		jdbcTemplate.update("update errand set assigned_user_id = 'ann01doe' where id = ?", ERRAND_ID);

		reportWaitingFor(APPROVE, REJECT);

		assertThat(awaitingSignalsOnTheErrand()).containsExactly(APPROVE, REJECT);

		final var rows = rowsWrittenBySignal(APPROVE);

		assertThat(rows).singleElement().satisfies(row -> {
			assertThat(row.getErrandId()).isEqualTo(ERRAND_ID);
			assertThat(row.getEventType()).isEqualTo("UPDATE");
			assertThat(row.getEventSubType()).isEqualTo("SIGNAL");
			assertThat(row.getSignalName()).isEqualTo(APPROVE);
			assertThat(row.getProcessKey()).isEqualTo(PROCESS_KEY);
			assertThat(row.isStartAllowed()).isFalse();
			assertThat(row.getExecutedBy()).isEqualTo(HANDLER);
		});

		assertThat(signalEntries()).singleElement().satisfies(entry -> {
			assertThat(entry.path("activityId").asString()).isEqualTo(APPROVE);
			assertThat(entry.path("activityName").asString()).isEqualTo("Godkänn granskning");
			assertThat(entry.path("severity").asString()).isEqualTo("INFO");
			assertThat(entry.path("processInstanceId").asString()).isEqualTo(PROCESS_INSTANCE_ID);
			assertThat(entry.path("message").asString()).contains(HANDLER);
		});

		assertThat(jdbcTemplate.queryForObject("select count(*) from notification where errand_id = ?", Integer.class, ERRAND_ID)).isZero();
		assertThat(jdbcTemplate.queryForObject("select count(*) from notification_dispatch where errand_id = ?", Integer.class, ERRAND_ID)).isZero();

		processEventRelay.relayErrand(ERRAND_ID);

		assertThat(outboxRepository.findById(rows.getFirst().getId())).get().extracting(ProcessEventOutboxEntity::getDeliveredAt).isNotNull();
		assertThat(eventsReceivedByProcess()).singleElement().satisfies(event -> {
			assertThat(event.path("eventId").asString()).isEqualTo(rows.getFirst().getId());
			assertThat(event.path("eventSubType").asString()).isEqualTo("SIGNAL");
			assertThat(event.path("signalName").asString()).isEqualTo(APPROVE);
			assertThat(event.path("processKey").asString()).isEqualTo(PROCESS_KEY);
			assertThat(event.path("startAllowed").booleanValue()).isFalse();
		});
		assertThat(wiremock.findAllUnmatchedRequests()).isEmpty();
	}

	/**
	 * A report replaces the list as a whole: a signal left out of it is gone, and pressing its button after that report
	 * is refused and writes nothing.
	 */
	@Test
	@DisplayName("Verification that a signal the latest report left out is gone from the errand, and refused with 409 without anything written")
	void test02_aSignalNoLongerAwaitedIsRefused() {
		reportWaitingFor(APPROVE, REJECT);
		reportWaitingFor(REJECT);

		assertThat(awaitingSignalsOnTheErrand()).containsExactly(REJECT);
		assertNothingWrittenBy(() -> assertThat(signal(ERRAND_ID, PROCESS_INSTANCE_ID, APPROVE, HANDLER_IDENTITY)).isEqualTo(CONFLICT));

		reportWaitingFor();

		assertThat(awaitingSignalsOnTheErrand()).as("an empty report means the process waits for no person").isEmpty();
		assertNothingWrittenBy(() -> assertThat(signal(ERRAND_ID, PROCESS_INSTANCE_ID, REJECT, HANDLER_IDENTITY)).isEqualTo(CONFLICT));
	}

	/**
	 * Covers a caller with an identity of another type as well as one without any identity.
	 */
	@Test
	@DisplayName("Verification that a signal from a caller that is not an ad account is refused with 403 and writes nothing")
	void test03_aSignalFromAMachineIsRefused() {
		reportWaitingFor(APPROVE);

		assertNothingWrittenBy(() -> {
			assertThat(signal(ERRAND_ID, PROCESS_INSTANCE_ID, APPROVE, PROCESS_ENGINE)).isEqualTo(FORBIDDEN);
			assertThat(signal(ERRAND_ID, PROCESS_INSTANCE_ID, APPROVE, null)).isEqualTo(FORBIDDEN);
		});
	}

	@Test
	@DisplayName("Verification that a signal to an errand without that live process instance is not found, and one to an ended process is a conflict")
	void test04_aSignalWithoutALiveProcessToReachIsRefused() {
		reportWaitingFor(APPROVE);

		assertNothingWrittenBy(() -> {
			assertThat(signal(ERRAND_WITHOUT_PROCESS_ID, PROCESS_INSTANCE_ID, APPROVE, HANDLER_IDENTITY)).isEqualTo(NOT_FOUND);
			assertThat(signal(ERRAND_ID, "pi-unknown", APPROVE, HANDLER_IDENTITY)).isEqualTo(NOT_FOUND);
			assertThat(signal("aa000000-0000-0000-0000-00000000dead", PROCESS_INSTANCE_ID, APPROVE, HANDLER_IDENTITY)).isEqualTo(NOT_FOUND);
		});

		report("""
			{"processService": "pw-alkt", "processKey": "alkt-ansokan", "processStatus": "COMPLETED",
			 "awaitingSignals": [{"name": "granskning-godkand"}]}""");

		assertThat(awaitingSignalsOnTheErrand()).as("an ended process waits for no one, whatever its report says").isEmpty();
		assertNothingWrittenBy(() -> assertThat(signal(ERRAND_ID, PROCESS_INSTANCE_ID, APPROVE, HANDLER_IDENTITY)).isEqualTo(CONFLICT));
	}

	@Test
	@DisplayName("Verification that a signal reaches the process though the emergency brake has tripped for the errand")
	void test05_aSignalPassesATrippedBrake() {
		reportWaitingFor(APPROVE);
		tripTheBrake();

		assertThat(rowsWrittenBySignal(APPROVE)).singleElement().satisfies(row -> {
			assertThat(row.getEventSubType()).isEqualTo("SIGNAL");
			assertThat(row.getSignalName()).isEqualTo(APPROVE);
			assertThat(row.getDeliveredAt()).isNull();
		});
	}

	@Test
	@DisplayName("Verification that a signal without a name is a bad request, and that the process is not told of it")
	void test06_aSignalWithoutANameIsRejected() {
		reportWaitingFor(APPROVE);

		assertNothingWrittenBy(() -> {
			assertThat(exchange(POST, signalsPath(ERRAND_ID, PROCESS_INSTANCE_ID), "{}", HANDLER_IDENTITY).getStatusCode()).isEqualTo(BAD_REQUEST);
			assertThat(exchange(POST, signalsPath(ERRAND_ID, PROCESS_INSTANCE_ID), """
				{"signal": " "}""", HANDLER_IDENTITY).getStatusCode()).isEqualTo(BAD_REQUEST);
		});
	}

	/**
	 * Sends the signal, holds it to having been logged as an errand event with the sub type SIGNAL, and hands back the
	 * rows it wrote.
	 */
	private List<ProcessEventOutboxEntity> rowsWrittenBySignal(final String name) {
		final var before = outboxIds();
		final var logged = requestsMadeDuring(eventLogRequests(ERRAND_ID),
			() -> assertThat(signal(ERRAND_ID, PROCESS_INSTANCE_ID, name, HANDLER_IDENTITY)).isEqualTo(ACCEPTED));

		assertThat(logged).singleElement().satisfies(event -> {
			assertThat(event.path("type").asString()).isEqualTo("UPDATE");
			assertThat(event.path("subType").asString()).isEqualTo("SIGNAL");
			assertThat(event.path("message").asString()).startsWith("En signal har skickats till processen i ärendet");
			assertThat(event.path("historyReference").isMissingNode() || event.path("historyReference").isNull()).isTrue();
		});
		return rowsWrittenSince(before);
	}

	private void assertNothingWrittenBy(final Runnable calls) {
		final var before = outboxIds();
		final var entriesBefore = signalEntries().size();
		final var logged = requestsMadeDuring(postRequestedFor(urlPathMatching("/api-eventlog/.*")), calls);

		assertThat(logged).isEmpty();
		assertThat(rowsWrittenSince(before)).isEmpty();
		assertThat(signalEntries()).hasSize(entriesBefore);
	}

	private void reportWaitingFor(final String... names) {
		final var signals = String.join(",", Arrays.stream(names)
			.map(name -> """
				{"name": "%s", "label": "%s"}""".formatted(name, APPROVE.equals(name) ? "Godkänn granskning" : "Skicka tillbaka för komplettering"))
			.toList());

		report("""
			{"processService": "pw-alkt", "processKey": "alkt-ansokan", "processStatus": "WAITING",
			 "currentActivityId": "granskning", "currentActivityName": "Granskning", "awaitingSignals": [%s]}""".formatted(signals));
	}

	private void report(final String body) {
		final var headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		headers.add(SENT_BY_HEADER, PROCESS_ENGINE);
		headers.add(TRIGGER_PROCESS_HEADER, "false");

		assertThat(restTemplate.exchange(ERRANDS_PATH + ERRAND_ID + "/processes/" + PROCESS_INSTANCE_ID, PUT, new HttpEntity<>(body, headers), String.class).getStatusCode())
			.isEqualTo(OK);
	}

	private List<String> awaitingSignalsOnTheErrand() {
		final var response = exchange(GET, ERRANDS_PATH + ERRAND_ID, null, HANDLER_IDENTITY);

		assertThat(response.getStatusCode()).isEqualTo(OK);
		final var signals = OBJECT_MAPPER.readTree(response.getBody()).path("process").path("awaitingSignals");
		assertThat(signals.isArray()).as("the errand says what its process waits for, an empty list included").isTrue();

		return signals.valueStream().map(signal -> signal.path("name").asString()).toList();
	}

	private List<JsonNode> signalEntries() {
		final var response = exchange(GET, ERRANDS_PATH + ERRAND_ID + "/process-activities", null, HANDLER_IDENTITY);

		assertThat(response.getStatusCode()).isEqualTo(OK);
		return OBJECT_MAPPER.readTree(response.getBody()).path("content").valueStream()
			.filter(entry -> "SIGNAL".equals(entry.path("activityType").asString()))
			.toList();
	}

	private HttpStatus signal(final String errandId, final String processInstanceId, final String name, final String sentBy) {
		return HttpStatus.valueOf(exchange(POST, signalsPath(errandId, processInstanceId), """
			{"signal": "%s"}""".formatted(name), sentBy).getStatusCode().value());
	}

	private ResponseEntity<String> exchange(final HttpMethod method, final String path, final String body, final String sentBy) {
		final var headers = new HttpHeaders();
		ofNullable(body).ifPresent(_ -> headers.setContentType(APPLICATION_JSON));
		ofNullable(sentBy).ifPresent(value -> headers.add(SENT_BY_HEADER, value));
		return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
	}

	/**
	 * Trips the emergency brake for the errand by writing as many delivered rows within the window as the configured
	 * limit allows, and verifies that the count the brake asks for reaches that limit.
	 */
	private void tripTheBrake() {
		final var guard = processEngineProperties.loopGuard();

		for (var i = 0; i < guard.maxEventsPerErrand(); i++) {
			outboxRepository.save(ProcessEventOutboxEntity.create()
				.withMunicipalityId(MUNICIPALITY_ID)
				.withNamespace(NAMESPACE)
				.withErrandId(ERRAND_ID)
				.withProcessService("pw-alkt")
				.withProcessKey(PROCESS_KEY)
				.withEventType("UPDATE")
				.withEventSubType("ERRAND")
				.withDeliveredAt(OffsetDateTime.now()));
		}

		assertThat(outboxRepository.countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter(ERRAND_ID, OffsetDateTime.now().minus(guard.window())))
			.isGreaterThanOrEqualTo(guard.maxEventsPerErrand());
	}

	private List<JsonNode> requestsMadeDuring(final RequestPatternBuilder pattern, final Runnable call) {
		final var before = wiremock.findAll(pattern).stream().map(LoggedRequest::getId).collect(toSet());

		call.run();

		return wiremock.findAll(pattern).stream()
			.filter(request -> !before.contains(request.getId()))
			.map(request -> OBJECT_MAPPER.readTree(request.getBodyAsString()))
			.toList();
	}

	private List<JsonNode> eventsReceivedByProcess() {
		return wiremock.findAll(postRequestedFor(urlPathEqualTo(PW_ALKT_PATH))).stream()
			.map(request -> OBJECT_MAPPER.readTree(request.getBodyAsString()))
			.toList();
	}

	private List<String> outboxIds() {
		return outboxRepository.findAll().stream().map(ProcessEventOutboxEntity::getId).toList();
	}

	private List<ProcessEventOutboxEntity> rowsWrittenSince(final List<String> before) {
		return outboxRepository.findAll().stream()
			.filter(row -> !before.contains(row.getId()))
			.toList();
	}

	private static RequestPatternBuilder eventLogRequests(final String errandId) {
		return postRequestedFor(urlPathEqualTo("/api-eventlog/" + MUNICIPALITY_ID + "/" + errandId));
	}

	private static String signalsPath(final String errandId, final String processInstanceId) {
		return ERRANDS_PATH + errandId + "/processes/" + processInstanceId + "/signals";
	}
}
