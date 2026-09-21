package se.sundsvall.supportmanagement.apptest;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.time.OffsetDateTime;
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
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

/**
 * Starting the handling of an errand by hand over the wire: what GET .../processes says about whether a start is
 * possible, the start command with every answer it gives, and the start reaching pw-alkt.
 * <p>
 * PROCESS-NAMESPACE names no process triggers, and the direct run is off, so every row here is written by a start
 * command and stays undelivered until a test delivers it.
 */
@WireMockAppTestSuite(files = "classpath:/ProcessStartIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-process-start.sql"
})
class ProcessStartIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "PROCESS-NAMESPACE";
	private static final String ERRANDS_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/";
	private static final String PW_ALKT_PATH = "/api-pw-alkt/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/process/errand-events";

	private static final String SUPERVISION_ERRAND_ID = "ab000000-0000-0000-0000-0000000000b1";
	private static final String AMBIGUOUS_ERRAND_ID = "ab000000-0000-0000-0000-0000000000b2";
	private static final String FAILED_START_ERRAND_ID = "ab000000-0000-0000-0000-0000000000b3";
	private static final String COMPLETED_ERRAND_ID = "ab000000-0000-0000-0000-0000000000b4";
	private static final String BUSY_ERRAND_ID = "ab000000-0000-0000-0000-0000000000b5";
	private static final String LIVE_ERRAND_ID = "aa000000-0000-0000-0000-0000000000a1";
	private static final String UNLABELLED_ERRAND_ID = "aa000000-0000-0000-0000-0000000000a2";
	private static final String ERRAND_OF_NAMESPACE_WITHOUT_PROCESSES_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";

	private static final String SUPERVISION = "alkt-tillsyn";
	private static final String APPLICATION = "alkt-ansokan";

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

	@Test
	@DisplayName("Verification that a start by hand gives an activity entry without an instance naming the sender, and an event carrying the chosen key and the permission all the way to pw-alkt")
	void test01_aStartReachesTheProcessWithItsKey() {
		assertThat(namespaceConfigService.getProcessTriggers(NAMESPACE, MUNICIPALITY_ID))
			.as("no trigger may be named for the test to show that the triggers have no say over a start")
			.isEmpty();
		jdbcTemplate.update("update errand set assigned_user_id = 'ann01doe' where id = ?", SUPERVISION_ERRAND_ID);

		assertThat(startable(SUPERVISION_ERRAND_ID)).isEqualTo(startableJson("AVAILABLE", SUPERVISION));
		assertThat(processesOf(SUPERVISION_ERRAND_ID)).isEmpty();

		final var rows = rowsWrittenByStart(SUPERVISION_ERRAND_ID, null, SUPERVISION);

		assertThat(rows).singleElement().satisfies(row -> {
			assertThat(row.getErrandId()).isEqualTo(SUPERVISION_ERRAND_ID);
			assertThat(row.getEventType()).isEqualTo("UPDATE");
			assertThat(row.getEventSubType()).isEqualTo("PROCESS");
			assertThat(row.getProcessKey()).isEqualTo(SUPERVISION);
			assertThat(row.isStartAllowed()).isTrue();
			assertThat(row.getSignalName()).isNull();
			assertThat(row.getExecutedBy()).isEqualTo(HANDLER);
		});

		assertThat(startEntries(SUPERVISION_ERRAND_ID)).singleElement().satisfies(entry -> {
			assertThat(entry.path("activityId").asString()).isEqualTo(SUPERVISION);
			assertThat(entry.path("severity").asString()).isEqualTo("INFO");
			assertThat(entry.path("processInstanceId").isMissingNode() || entry.path("processInstanceId").isNull()).isTrue();
			assertThat(entry.path("message").asString()).contains(HANDLER);
		});

		assertThat(jdbcTemplate.queryForObject("select count(*) from notification where errand_id = ?", Integer.class, SUPERVISION_ERRAND_ID)).isZero();
		assertThat(jdbcTemplate.queryForObject("select count(*) from notification_dispatch where errand_id = ?", Integer.class, SUPERVISION_ERRAND_ID)).isZero();
		assertThat(startable(SUPERVISION_ERRAND_ID)).as("the start is on its way until the process registers it").isEqualTo(startableJson("AVAILABLE", SUPERVISION));

		processEventRelay.relayErrand(SUPERVISION_ERRAND_ID);

		assertThat(outboxRepository.findById(rows.getFirst().getId())).get().extracting(ProcessEventOutboxEntity::getDeliveredAt).isNotNull();
		assertThat(eventsReceivedByProcess()).singleElement().satisfies(event -> {
			assertThat(event.path("eventId").asString()).isEqualTo(rows.getFirst().getId());
			assertThat(event.path("eventSubType").asString()).isEqualTo("PROCESS");
			assertThat(event.path("processKey").asString()).isEqualTo(SUPERVISION);
			assertThat(event.path("startAllowed").booleanValue()).isTrue();
		});
		assertThat(wiremock.findAllUnmatchedRequests()).isEmpty();
	}

	@Test
	@DisplayName("Verification that two presses with the same key write one start, and are both answered 202")
	void test02_aDoubleClickWritesOneStart() {
		final var rows = rowsWrittenByStart(SUPERVISION_ERRAND_ID, SUPERVISION, SUPERVISION);

		assertNothingWrittenBy(SUPERVISION_ERRAND_ID, () -> assertThat(start(SUPERVISION_ERRAND_ID, SUPERVISION, HANDLER_IDENTITY)).isEqualTo(ACCEPTED));
		assertNothingWrittenBy(SUPERVISION_ERRAND_ID, () -> assertThat(start(SUPERVISION_ERRAND_ID, null, HANDLER_IDENTITY)).isEqualTo(ACCEPTED));

		assertThat(outboxRepository.findAll()).extracting(ProcessEventOutboxEntity::getId).containsExactly(rows.getFirst().getId());
		assertThat(startEntries(SUPERVISION_ERRAND_ID)).hasSize(1);
	}

	@Test
	@DisplayName("Verification that a start of another process while one is on its way is refused with 409, and leaves the first start untouched")
	void test03_aChangedChoiceWhileAStartIsOnItsWayIsRefused() {
		final var first = rowsWrittenByStart(AMBIGUOUS_ERRAND_ID, APPLICATION, APPLICATION).getFirst();

		assertNothingWrittenBy(AMBIGUOUS_ERRAND_ID, () -> assertThat(start(AMBIGUOUS_ERRAND_ID, SUPERVISION, HANDLER_IDENTITY)).isEqualTo(CONFLICT));

		assertThat(outboxRepository.findAll()).singleElement().satisfies(row -> {
			assertThat(row.getId()).isEqualTo(first.getId());
			assertThat(row.getProcessKey()).isEqualTo(APPLICATION);
			assertThat(row.getDeliveredAt()).isNull();
		});
	}

	@Test
	@DisplayName("Verification that an errand whose labels point at two processes offers both, refuses a start choosing none or another, and starts the one chosen")
	void test04_anAmbiguousErrandStartsTheChosenProcess() {
		assertThat(startable(AMBIGUOUS_ERRAND_ID)).isEqualTo(startableJson("AVAILABLE", APPLICATION, SUPERVISION));

		assertNothingWrittenBy(AMBIGUOUS_ERRAND_ID, () -> {
			assertThat(start(AMBIGUOUS_ERRAND_ID, null, HANDLER_IDENTITY)).isEqualTo(BAD_REQUEST);
			assertThat(start(AMBIGUOUS_ERRAND_ID, "alkt-serveringstillstand", HANDLER_IDENTITY)).isEqualTo(BAD_REQUEST);
		});

		assertThat(rowsWrittenByStart(AMBIGUOUS_ERRAND_ID, SUPERVISION, SUPERVISION)).singleElement().satisfies(row -> {
			assertThat(row.getProcessKey()).isEqualTo(SUPERVISION);
			assertThat(row.isStartAllowed()).isTrue();
		});
	}

	@Test
	@DisplayName("Verification that a start from a caller that is not an ad account is refused with 403 and writes nothing")
	void test05_aStartFromAMachineIsRefused() {
		assertNothingWrittenBy(SUPERVISION_ERRAND_ID, () -> {
			assertThat(start(SUPERVISION_ERRAND_ID, SUPERVISION, PROCESS_ENGINE)).isEqualTo(FORBIDDEN);
			assertThat(start(SUPERVISION_ERRAND_ID, SUPERVISION, null)).isEqualTo(FORBIDDEN);
		});
	}

	@Test
	@DisplayName("Verification that a start is refused, without anything written, for every obstacle startable names, and for an errand that is not there")
	void test06_eachObstacleRefusesTheStart() {
		assertThat(startable(LIVE_ERRAND_ID)).isEqualTo(startableJson("LIVE_INSTANCE"));
		assertThat(startable(COMPLETED_ERRAND_ID)).isEqualTo(startableJson("PROCESS_COMPLETED"));
		assertThat(startable(UNLABELLED_ERRAND_ID)).isEqualTo(startableJson("NO_PROCESS_KEY"));
		assertThat(startable("/2281/NAMESPACE-1/errands/", ERRAND_OF_NAMESPACE_WITHOUT_PROCESSES_ID)).isEqualTo(startableJson("NO_PROCESS_ENGINE"));

		assertNothingWrittenBy(LIVE_ERRAND_ID, () -> assertThat(start(LIVE_ERRAND_ID, null, HANDLER_IDENTITY)).isEqualTo(CONFLICT));
		assertNothingWrittenBy(COMPLETED_ERRAND_ID, () -> assertThat(start(COMPLETED_ERRAND_ID, APPLICATION, HANDLER_IDENTITY)).isEqualTo(CONFLICT));
		assertNothingWrittenBy(UNLABELLED_ERRAND_ID, () -> assertThat(start(UNLABELLED_ERRAND_ID, null, HANDLER_IDENTITY)).isEqualTo(BAD_REQUEST));
		assertThat(exchange(POST, "/2281/NAMESPACE-1/errands/" + ERRAND_OF_NAMESPACE_WITHOUT_PROCESSES_ID + "/processes/start", null, HANDLER_IDENTITY).getStatusCode())
			.isEqualTo(BAD_REQUEST);
		assertNothingWrittenBy(SUPERVISION_ERRAND_ID, () -> {
			assertThat(start("ab000000-0000-0000-0000-00000000dead", null, HANDLER_IDENTITY)).isEqualTo(NOT_FOUND);
			assertThat(exchange(POST, "/2281/NAMESPACE-1/errands/" + SUPERVISION_ERRAND_ID + "/processes/start", null, HANDLER_IDENTITY).getStatusCode()).isEqualTo(NOT_FOUND);
		});
		assertThat(outboxRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("Verification that an errand whose only start failed, and whose label starts on its own, is started again by hand")
	void test07_aFailedStartIsStartedAgainInAutomaticMode() {
		assertThat(startable(FAILED_START_ERRAND_ID)).isEqualTo(startableJson("AVAILABLE", APPLICATION));

		assertThat(rowsWrittenByStart(FAILED_START_ERRAND_ID, null, APPLICATION)).singleElement().satisfies(row -> {
			assertThat(row.getEventSubType()).isEqualTo("PROCESS");
			assertThat(row.getProcessKey()).isEqualTo(APPLICATION);
			assertThat(row.isStartAllowed()).isTrue();
		});
	}

	@Test
	@DisplayName("Verification that a start reaches the process though the emergency brake has tripped for the errand")
	void test08_aStartPassesATrippedBrake() {
		tripTheBrake(BUSY_ERRAND_ID);

		assertThat(rowsWrittenByStart(BUSY_ERRAND_ID, null, SUPERVISION)).singleElement().satisfies(row -> {
			assertThat(row.getEventSubType()).isEqualTo("PROCESS");
			assertThat(row.isStartAllowed()).isTrue();
			assertThat(row.getDeliveredAt()).isNull();
		});
	}

	@Test
	@DisplayName("Verification that a start naming a key longer than a process key can be is a bad request, and that the process is not told of it")
	void test09_anOversizedKeyIsRejected() {
		assertNothingWrittenBy(SUPERVISION_ERRAND_ID, () -> assertThat(start(SUPERVISION_ERRAND_ID, "k".repeat(129), HANDLER_IDENTITY)).isEqualTo(BAD_REQUEST));
	}

	/**
	 * Sends the start, verifies that it was logged as an errand event with the sub type PROCESS naming the key, and
	 * returns the rows it wrote.
	 */
	private List<ProcessEventOutboxEntity> rowsWrittenByStart(final String errandId, final String processKey, final String expectedKey) {
		final var before = outboxIds();
		final var logged = requestsMadeDuring(eventLogRequests(errandId),
			() -> assertThat(start(errandId, processKey, HANDLER_IDENTITY)).isEqualTo(ACCEPTED));

		assertThat(logged).singleElement().satisfies(event -> {
			assertThat(event.path("type").asString()).isEqualTo("UPDATE");
			assertThat(event.path("subType").asString()).isEqualTo("PROCESS");
			assertThat(event.path("message").asString()).isEqualTo("En start av processen har begärts i ärendet: " + expectedKey + ".");
		});
		return rowsWrittenSince(before);
	}

	/**
	 * Runs the calls and verifies that they wrote no errand event, no outbox row and no start entry on the errand.
	 */
	private void assertNothingWrittenBy(final String errandId, final Runnable calls) {
		final var before = outboxIds();
		final var entriesBefore = startEntries(errandId).size();
		final var logged = requestsMadeDuring(postRequestedFor(urlPathMatching("/api-eventlog/.*")), calls);

		assertThat(logged).isEmpty();
		assertThat(rowsWrittenSince(before)).isEmpty();
		assertThat(startEntries(errandId)).hasSize(entriesBefore);
	}

	private HttpStatus start(final String errandId, final String processKey, final String sentBy) {
		final var body = ofNullable(processKey).map("""
			{"processKey": "%s"}"""::formatted).orElse(null);

		return HttpStatus.valueOf(exchange(POST, ERRANDS_PATH + errandId + "/processes/start", body, sentBy).getStatusCode().value());
	}

	private JsonNode startable(final String errandId) {
		return startable(ERRANDS_PATH, errandId);
	}

	private JsonNode startable(final String errandsPath, final String errandId) {
		final var response = exchange(GET, errandsPath + errandId + "/processes", null, HANDLER_IDENTITY);

		assertThat(response.getStatusCode()).isEqualTo(OK);
		return OBJECT_MAPPER.readTree(response.getBody()).path("startable");
	}

	private List<JsonNode> processesOf(final String errandId) {
		final var response = exchange(GET, ERRANDS_PATH + errandId + "/processes", null, HANDLER_IDENTITY);

		assertThat(response.getStatusCode()).isEqualTo(OK);
		return OBJECT_MAPPER.readTree(response.getBody()).path("processes").valueStream().toList();
	}

	private static JsonNode startableJson(final String status, final String... processKeys) {
		final var node = OBJECT_MAPPER.createObjectNode().put("status", status);
		final var keys = node.putArray("processKeys");
		List.of(processKeys).forEach(keys::add);
		return node;
	}

	private List<JsonNode> startEntries(final String errandId) {
		final var response = exchange(GET, ERRANDS_PATH + errandId + "/process-activities", null, HANDLER_IDENTITY);

		assertThat(response.getStatusCode()).isEqualTo(OK);
		return OBJECT_MAPPER.readTree(response.getBody()).path("content").valueStream()
			.filter(entry -> "START".equals(entry.path("activityType").asString()))
			.toList();
	}

	private ResponseEntity<String> exchange(final HttpMethod method, final String path, final String body, final String sentBy) {
		final var headers = new HttpHeaders();
		ofNullable(body).ifPresent(_ -> headers.setContentType(APPLICATION_JSON));
		ofNullable(sentBy).ifPresent(value -> headers.add(SENT_BY_HEADER, value));
		return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
	}

	/**
	 * Writes as many delivered rows within the window as the emergency brake allows, and verifies that the brake counts
	 * them.
	 */
	private void tripTheBrake(final String errandId) {
		final var guard = processEngineProperties.loopGuard();

		for (var i = 0; i < guard.maxEventsPerErrand(); i++) {
			outboxRepository.save(ProcessEventOutboxEntity.create()
				.withMunicipalityId(MUNICIPALITY_ID)
				.withNamespace(NAMESPACE)
				.withErrandId(errandId)
				.withProcessService("pw-alkt")
				.withProcessKey(SUPERVISION)
				.withEventType("UPDATE")
				.withEventSubType("ERRAND")
				.withDeliveredAt(OffsetDateTime.now()));
		}

		assertThat(outboxRepository.countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter(errandId, OffsetDateTime.now().minus(guard.window())))
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
}
