package se.sundsvall.supportmanagement.apptest;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
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
import static org.springframework.http.HttpHeaders.ETAG;
import static org.springframework.http.HttpHeaders.IF_MATCH;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.TRIGGER_PROCESS_HEADER;

/**
 * The chain that lets a process end: a decision is written, the errand event it gives is published, and the process
 * waiting for the decision is told.
 * <p>
 * Every write expected to reach the process is also held to having been logged as an errand event with the sub type
 * DECISION. The relay is not run, so the rows written stay undelivered.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandDecisionProcessIT/", classes = Application.class)
@ExtendWith(OutputCaptureExtension.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-decision-process.sql"
})
class ErrandDecisionProcessIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "PROCESS-NAMESPACE";
	private static final String ERRANDS_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/";

	private static final String RUNNING_ERRAND_ID = "aa000000-0000-0000-0000-0000000000a1";
	private static final String ERRAND_WITHOUT_PROCESS_ID = "aa000000-0000-0000-0000-0000000000a2";
	private static final String ENDED_ERRAND_ID = "aa000000-0000-0000-0000-0000000000a5";
	private static final String LIVE_PROCESS_ROW_ID = "ep-it-live";
	private static final String PROCESS_KEY = "alkt-ansokan";

	private static final String DRAFT_DECISION_ID = "de000000-0000-0000-0000-000000000001";
	private static final String DECISION_WITHOUT_PROCESS_ID = "de000000-0000-0000-0000-000000000003";
	private static final String ENDED_DECISION_ID = "de000000-0000-0000-0000-000000000004";
	private static final String ENDED_TERM_ID = "df000000-0000-0000-0000-000000000001";
	private static final String DRAFT_ATTACHMENT_ID = "ad000000-0000-0000-0000-000000000001";
	private static final String COMPLETED_ATTACHMENT_ID = "ad000000-0000-0000-0000-000000000002";
	private static final String ENDED_ATTACHMENT_ID = "ad000000-0000-0000-0000-000000000003";
	private static final String UNLINKED_ENDED_ATTACHMENT_ID = "ad000000-0000-0000-0000-000000000004";
	private static final String DRAFT_INVESTIGATION_ID = "e1000000-0000-0000-0000-000000000001";
	private static final String ENDED_INVESTIGATION_ID = "e1000000-0000-0000-0000-000000000002";

	private static final String HANDLER = "joe01doe";
	private static final String HANDLER_IDENTITY = HANDLER + "; type=adAccount";
	private static final String PROCESS_SERVICE = "pw-alkt";
	private static final String PROCESS_ENGINE = PROCESS_SERVICE + "; type=processEngine";
	private static final String OTHER_ENGINE = "other-engine; type=processEngine";
	private static final String DO_NOT_WAKE = "false";

	private static final String JUSTIFICATION = "Sökanden bedöms lämplig efter samlad prövning av vandel och ekonomi";
	private static final Pattern MASKED_JUSTIFICATION = Pattern.compile("\\\\?\"justification\\\\?\"\\s*:\\s*\\\\?\"\\[masked]\\\\?\"");
	private static final String CONCLUDED = "Ett beslut i ärendet har fattats.";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Autowired
	private ProcessEventOutboxRepository outboxRepository;

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
	}

	/**
	 * The whole chain from the caseworker: the decision is logged as an errand event, the event becomes a row addressed to
	 * the process of the errand, and the version of the errand moves. The process row sent along is not taken.
	 */
	@Test
	@DisplayName("Verification that a caseworker writing a decision gives an outbox row with the sub type DECISION")
	void test01_aCaseworkersDecisionReachesTheProcess() {
		final var versionBefore = errandVersion(RUNNING_ERRAND_ID);

		final var decisionId = createDecision(RUNNING_ERRAND_ID, decision("ACTIVE", "MANUAL", HANDLER, LIVE_PROCESS_ROW_ID), HANDLER_IDENTITY, null, "Ett beslut har lagts till i ärendet.",
			rows -> assertThat(rows).singleElement().satisfies(row -> {
				assertThat(row.getErrandId()).isEqualTo(RUNNING_ERRAND_ID);
				assertThat(row.getProcessService()).isEqualTo(PROCESS_SERVICE);
				assertThat(row.getProcessKey()).isEqualTo(PROCESS_KEY);
				assertThat(row.getEventType()).isEqualTo("UPDATE");
				assertThat(row.getEventSubType()).isEqualTo("DECISION");
				assertThat(row.getExecutedBy()).isEqualTo(HANDLER);
				assertThat(row.isStartAllowed()).as("no label of the errand gives it a start mode").isFalse();
			}));

		assertThat(decisionColumn(decisionId, "method")).isEqualTo("MANUAL");
		assertThat(decisionColumn(decisionId, "errand_process_id")).as("a manual decision is made by no process, whatever the body says").isNull();
		assertThat(errandVersion(RUNNING_ERRAND_ID)).isEqualTo(versionBefore + 1);
	}

	/**
	 * Loop guard layer 1 holds for a decision the process concludes itself: no row is written, although a concluded
	 * decision passes the emergency brake. The decision names the live process as the one that made it, whatever the body
	 * says.
	 */
	@Test
	@DisplayName("Verification that the process concluding its own decision is not woken by it, and is recorded as the one that made it")
	void test02_theProcessConcludingItsOwnDecisionIsNotWokenByIt() {
		final var decisionId = createDecision(RUNNING_ERRAND_ID, decision("COMPLETED", "AUTOMATIC", PROCESS_SERVICE, "ep-it-completed"), PROCESS_ENGINE, DO_NOT_WAKE, CONCLUDED,
			rows -> assertThat(rows).isEmpty());

		assertThat(decisionColumn(decisionId, "method")).isEqualTo("AUTOMATIC");
		assertThat(decisionColumn(decisionId, "errand_process_id")).isEqualTo(LIVE_PROCESS_ROW_ID);
	}

	/**
	 * Both directions are refused with 403, on create as well as on update, and nothing is written.
	 */
	@Test
	@DisplayName("Verification that a decision cannot claim the method of the other kind of writer")
	void test03_aDecisionCannotClaimTheMethodOfTheOtherKindOfWriter() {
		final var decisionsBefore = decisionCount();

		assertThat(statusOf(() -> exchange(POST, decisionsPath(RUNNING_ERRAND_ID), decision("ACTIVE", "AUTOMATIC", "other-engine", null), OTHER_ENGINE, null, null))).isEqualTo(FORBIDDEN);
		assertThat(statusOf(() -> exchange(POST, decisionsPath(RUNNING_ERRAND_ID), decision("ACTIVE", "AUTOMATIC", HANDLER, null), HANDLER_IDENTITY, null, null))).isEqualTo(FORBIDDEN);
		assertThat(statusOf(() -> exchange(POST, decisionsPath(RUNNING_ERRAND_ID), decision("ACTIVE", "MANUAL", PROCESS_SERVICE, null), PROCESS_ENGINE, null, null))).isEqualTo(FORBIDDEN);
		assertThat(statusOf(() -> exchange(PATCH, decisionPath(RUNNING_ERRAND_ID, DRAFT_DECISION_ID), """
			{"method": "AUTOMATIC"}""", HANDLER_IDENTITY, null, null))).isEqualTo(FORBIDDEN);

		assertThat(decisionCount()).isEqualTo(decisionsBefore);
		assertThat(decisionColumn(DRAFT_DECISION_ID, "method")).isEqualTo("MANUAL");
	}

	/**
	 * While the process lives a decision can be written again, until it is concluded. After that it is locked as it
	 * stands - taking it back to a draft and deleting it included - and so are its terms, its attachments and the
	 * attachments of the errand it rests on. Its JSON parameters are not.
	 */
	@Test
	@DisplayName("Verification that a decision is written again while the process lives, and locked once concluded")
	void test04_aDecisionIsLockedOnceConcluded() {
		assertThat(decisionPatch(DRAFT_DECISION_ID, """
			{"title": "Beslut om serveringstillstånd"}""", "\"0\"", "Ett beslut i ärendet har uppdaterats.")).hasSize(1);
		assertThat(decisionPatch(DRAFT_DECISION_ID, """
			{"status": "COMPLETED"}""", "\"1\"", CONCLUDED)).hasSize(1);

		final var path = decisionPath(RUNNING_ERRAND_ID, DRAFT_DECISION_ID);
		assertNothingWritten(() -> {
			assertThat(status(PATCH, path, """
				{"title": "Rättat"}""", "\"2\"")).isEqualTo(CONFLICT);
			assertThat(status(PATCH, path, """
				{"status": "DRAFT"}""", null)).isEqualTo(CONFLICT);
			assertThat(status(DELETE, path, null, null)).isEqualTo(CONFLICT);
			assertThat(status(POST, path + "/terms", """
				{"text": "Nytt villkor"}""", null)).isEqualTo(CONFLICT);
			assertThat(status(DELETE, path + "/attachments/" + DRAFT_ATTACHMENT_ID, null, null)).isEqualTo(CONFLICT);
			assertThat(status(POST, path + "/attachments/" + COMPLETED_ATTACHMENT_ID, null, null)).isEqualTo(CONFLICT);
			assertThat(status(DELETE, attachmentPath(RUNNING_ERRAND_ID, DRAFT_ATTACHMENT_ID), null, null)).isEqualTo(CONFLICT);
			assertThat(status(DELETE, investigationPath(RUNNING_ERRAND_ID, DRAFT_INVESTIGATION_ID), null, null)).isEqualTo(CONFLICT);
		});

		assertThat(decisionColumn(DRAFT_DECISION_ID, "status")).isEqualTo("COMPLETED");
		assertThat(decisionColumn(DRAFT_DECISION_ID, "title")).isEqualTo("Beslut om serveringstillstånd");
		assertThat(decisionColumn(DRAFT_DECISION_ID, "investigation_id")).isEqualTo(DRAFT_INVESTIGATION_ID);
		assertThat(attachmentLinks(DRAFT_DECISION_ID)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("select count(*) from decision where errand_id = ?", Integer.class, RUNNING_ERRAND_ID)).isEqualTo(2);
	}

	/**
	 * With the emergency brake tripped by other traffic on the errand, an ordinary change to the decision and a decision
	 * the process concludes itself without asking not to be woken give no row, while a caseworker concluding the decision
	 * gives one.
	 */
	@Test
	@DisplayName("Verification that a decision concluded by a caseworker reaches the process past a tripped emergency brake, and one concluded by the process does not")
	void test05_aConcludedDecisionPassesATrippedBrake() {
		tripTheBrake(RUNNING_ERRAND_ID);

		assertThat(decisionPatch(DRAFT_DECISION_ID, """
			{"title": "Beslut om serveringstillstånd"}""", null, "Ett beslut i ärendet har uppdaterats.")).isEmpty();

		createDecision(RUNNING_ERRAND_ID, decision("COMPLETED", "AUTOMATIC", PROCESS_SERVICE, null), PROCESS_ENGINE, null, CONCLUDED,
			rows -> assertThat(rows).isEmpty());

		assertThat(decisionPatch(DRAFT_DECISION_ID, """
			{"status": "COMPLETED"}""", null, CONCLUDED))
			.singleElement()
			.satisfies(row -> {
				assertThat(row.getEventSubType()).isEqualTo("DECISION");
				assertThat(row.getDeliveredAt()).isNull();
			});
	}

	/**
	 * A completed process, which is never started again, locks every decision of its errand whatever their status, and a
	 * new decision too. The JSON parameters stay open.
	 */
	@Test
	@DisplayName("Verification that no decision of an errand whose process has run to its end can be written")
	void test06_noDecisionOfAnEndedProcessCanBeWritten() {
		final var path = decisionPath(ENDED_ERRAND_ID, ENDED_DECISION_ID);

		assertNothingWritten(() -> {
			assertThat(status(POST, decisionsPath(ENDED_ERRAND_ID), decision("ACTIVE", "MANUAL", HANDLER, null), null)).isEqualTo(CONFLICT);
			assertThat(status(PATCH, path, """
				{"title": "Rättat"}""", null)).isEqualTo(CONFLICT);
			assertThat(status(DELETE, path, null, null)).isEqualTo(CONFLICT);
			assertThat(status(POST, path + "/terms", """
				{"text": "Nytt villkor"}""", null)).isEqualTo(CONFLICT);
			assertThat(status(PATCH, path + "/terms/" + ENDED_TERM_ID, """
				{"text": "Rättat villkor"}""", null)).isEqualTo(CONFLICT);
			assertThat(status(DELETE, path + "/terms/" + ENDED_TERM_ID, null, null)).isEqualTo(CONFLICT);
			assertThat(status(POST, path + "/attachments/" + UNLINKED_ENDED_ATTACHMENT_ID, null, null)).isEqualTo(CONFLICT);
			assertThat(status(DELETE, path + "/attachments/" + ENDED_ATTACHMENT_ID, null, null)).isEqualTo(CONFLICT);
			assertThat(status(DELETE, attachmentPath(ENDED_ERRAND_ID, ENDED_ATTACHMENT_ID), null, null)).isEqualTo(CONFLICT);
			assertThat(status(DELETE, investigationPath(ENDED_ERRAND_ID, ENDED_INVESTIGATION_ID), null, null)).isEqualTo(CONFLICT);
		});

		assertThat(decisionColumn(ENDED_DECISION_ID, "status")).isEqualTo("ACTIVE");
		assertThat(decisionColumn(ENDED_DECISION_ID, "investigation_id")).isEqualTo(ENDED_INVESTIGATION_ID);
		assertThat(jdbcTemplate.queryForObject("select count(*) from decision_term where decision_id = ?", Integer.class, ENDED_DECISION_ID)).isEqualTo(1);
		assertThat(attachmentLinks(ENDED_DECISION_ID)).isEqualTo(1);

		assertThat(status(DELETE, attachmentPath(ENDED_ERRAND_ID, UNLINKED_ENDED_ATTACHMENT_ID), null, null)).as("an attachment no decision rests on").isEqualTo(NO_CONTENT);
		assertThat(status(DELETE, path + "/json-parameters/legalForce", null, null)).as("the JSON parameters stand outside the lock").isEqualTo(NO_CONTENT);
	}

	/**
	 * An errand that has never had a process is never locked. Every change is written to the event log and moves the
	 * version of the errand.
	 */
	@Test
	@DisplayName("Verification that a completed decision on an errand without a process can be changed and deleted")
	void test07_anErrandWithoutAProcessIsNeverLocked() {
		final var versionBefore = errandVersion(ERRAND_WITHOUT_PROCESS_ID);
		final var path = decisionPath(ERRAND_WITHOUT_PROCESS_ID, DECISION_WITHOUT_PROCESS_ID);

		assertThat(status(PATCH, path, """
			{"title": "Rättat"}""", "\"0\"")).isEqualTo(OK);
		assertThat(status(PATCH, path, """
			{"status": "DRAFT"}""", "\"1\"")).isEqualTo(OK);
		assertThat(status(DELETE, path, null, "\"2\"")).isEqualTo(NO_CONTENT);

		assertThat(decisionExists(DECISION_WITHOUT_PROCESS_ID)).isFalse();
		assertThat(errandVersion(ERRAND_WITHOUT_PROCESS_ID)).isEqualTo(versionBefore + 3);
		assertThat(wiremock.findAll(eventLogRequests(ERRAND_WITHOUT_PROCESS_ID))).hasSize(3);
	}

	/**
	 * A work step reads the errand, and a decision written meanwhile changes what it read. The step is told so with 412
	 * and runs again on the errand as it now is.
	 */
	@Test
	@DisplayName("Verification that a work step holding the errand from before a decision was written gets 412")
	void test08_aWorkStepHoldingAnOlderErrandIsToldItHasChanged() {
		final var readBefore = readErrandETag(RUNNING_ERRAND_ID);

		assertThat(status(PATCH, decisionPath(RUNNING_ERRAND_ID, DRAFT_DECISION_ID), """
			{"title": "Beslut om serveringstillstånd"}""", null)).isEqualTo(OK);

		assertThat(statusOf(() -> exchange(PATCH, ERRANDS_PATH + RUNNING_ERRAND_ID, """
			{"description": "Granskad"}""", PROCESS_ENGINE, DO_NOT_WAKE, readBefore))).isEqualTo(PRECONDITION_FAILED);

		final var readAfter = readErrandETag(RUNNING_ERRAND_ID);
		assertThat(readAfter).isNotEqualTo(readBefore);
		assertThat(exchange(PATCH, ERRANDS_PATH + RUNNING_ERRAND_ID, """
			{"description": "Granskad"}""", PROCESS_ENGINE, DO_NOT_WAKE, readAfter).getStatusCode()).isEqualTo(OK);
	}

	/**
	 * Two writers holding the same version of the decision: the first one wins, and the second is told the decision has
	 * changed instead of overwriting it.
	 */
	@Test
	@DisplayName("Verification that the second of two writes made with the same If-Match gets 412")
	void test09_theSecondOfTwoWritesWithTheSameVersionGets412() {
		final var path = decisionPath(RUNNING_ERRAND_ID, DRAFT_DECISION_ID);

		assertThat(status(PATCH, path, """
			{"title": "Första"}""", "\"0\"")).isEqualTo(OK);
		assertThat(status(PATCH, path, """
			{"title": "Andra"}""", "\"0\"")).isEqualTo(PRECONDITION_FAILED);

		assertThat(decisionColumn(DRAFT_DECISION_ID, "title")).isEqualTo("Första");
	}

	/**
	 * Deleting the errand deletes its decisions. The process row a decision names is no foreign key of it, so removing the
	 * row on its own leaves the decision and its reference to the row as they were.
	 */
	@Test
	@DisplayName("Verification that deleting the errand takes its decisions along, and that removing a process row does not")
	void test10_theDecisionGoesWithTheErrandAndStaysWithoutItsProcessRow() {
		jdbcTemplate.update("delete from errand_process where id = ?", "ep-it-completed");
		assertThat(decisionExists(ENDED_DECISION_ID)).isTrue();
		assertThat(decisionColumn(ENDED_DECISION_ID, "errand_process_id")).isEqualTo("ep-it-completed");

		jdbcTemplate.update("delete from errand where id = ?", ERRAND_WITHOUT_PROCESS_ID);
		assertThat(decisionExists(DECISION_WITHOUT_PROCESS_ID)).isFalse();
		assertThat(decisionExists(DRAFT_DECISION_ID)).isTrue();
	}

	/**
	 * The justification is masked in the payload log: the masked value is found where the justification was, in the
	 * request and in the response alike.
	 */
	@Test
	@DisplayName("Verification that the justification of a decision appears in no log line, going in or coming out")
	void test11_theJustificationIsNeverLogged(final CapturedOutput output) {
		assertThat(status(POST, decisionsPath(RUNNING_ERRAND_ID), decision("ACTIVE", "MANUAL", HANDLER, null), null)).isEqualTo(CREATED);
		assertThat(exchange(GET, decisionsPath(RUNNING_ERRAND_ID), null, HANDLER_IDENTITY, null, null).getBody())
			.contains(JUSTIFICATION, "Utkast till motivering");

		assertThat(output.getAll())
			.doesNotContain(JUSTIFICATION)
			.doesNotContain("Utkast till motivering");
		assertThat(MASKED_JUSTIFICATION.matcher(output.getAll()).results().count())
			.as("the justification of the request and the three of the response, masked")
			.isGreaterThanOrEqualTo(4);
	}

	private List<ProcessEventOutboxEntity> decisionPatch(final String decisionId, final String body, final String ifMatch, final String expectedMessage) {
		final var before = outboxIds();
		final var logged = requestsMadeDuring(eventLogRequests(RUNNING_ERRAND_ID),
			() -> assertThat(exchange(PATCH, decisionPath(RUNNING_ERRAND_ID, decisionId), body, HANDLER_IDENTITY, null, ifMatch).getStatusCode()).isEqualTo(OK));

		assertThat(logged).singleElement().satisfies(event -> assertDecisionEvent(event, expectedMessage));
		return rowsWrittenSince(before);
	}

	/**
	 * Creates the decision, holds the write to having been logged as a decision event with the message given, and hands
	 * the rows it wrote to the assertion.
	 */
	private String createDecision(final String errandId, final String body, final String sentBy, final String triggerProcess, final String expectedMessage,
		final Consumer<List<ProcessEventOutboxEntity>> rows) {
		final var before = outboxIds();
		final var responses = new ArrayList<ResponseEntity<String>>();
		final var logged = requestsMadeDuring(eventLogRequests(errandId),
			() -> responses.add(exchange(POST, decisionsPath(errandId), body, sentBy, triggerProcess, null)));

		final var response = responses.getFirst();
		assertThat(response.getStatusCode()).isEqualTo(CREATED);
		assertThat(logged).singleElement().satisfies(event -> assertDecisionEvent(event, expectedMessage));
		rows.accept(rowsWrittenSince(before));

		final var location = response.getHeaders().getLocation().getPath();
		return location.substring(location.lastIndexOf('/') + 1);
	}

	private void assertNothingWritten(final Runnable writes) {
		final var before = outboxIds();
		final var logged = requestsMadeDuring(postRequestedFor(urlPathMatching("/api-eventlog/.*")), writes);

		assertThat(logged).isEmpty();
		assertThat(rowsWrittenSince(before)).isEmpty();
	}

	private static void assertDecisionEvent(final JsonNode event, final String expectedMessage) {
		assertThat(event.path("type").asString()).isEqualTo("UPDATE");
		assertThat(event.path("subType").asString()).isEqualTo("DECISION");
		assertThat(event.path("message").asString()).isEqualTo(expectedMessage);
		assertThat(event.path("historyReference").isMissingNode() || event.path("historyReference").isNull()).isTrue();
	}

	/**
	 * Trips the emergency brake of the errand by writing as many delivered rows inside its window as it allows.
	 */
	private void tripTheBrake(final String errandId) {
		for (var i = 0; i < 20; i++) {
			outboxRepository.save(ProcessEventOutboxEntity.create()
				.withMunicipalityId(MUNICIPALITY_ID)
				.withNamespace(NAMESPACE)
				.withErrandId(errandId)
				.withProcessService(PROCESS_SERVICE)
				.withProcessKey(PROCESS_KEY)
				.withEventType("UPDATE")
				.withEventSubType("ERRAND")
				.withDeliveredAt(OffsetDateTime.now()));
		}
	}

	private HttpStatus status(final HttpMethod method, final String path, final String body, final String ifMatch) {
		return statusOf(() -> exchange(method, path, body, HANDLER_IDENTITY, null, ifMatch));
	}

	private static HttpStatus statusOf(final ExchangeCall call) {
		return HttpStatus.valueOf(call.exchange().getStatusCode().value());
	}

	private ResponseEntity<String> exchange(final HttpMethod method, final String path, final String body, final String sentBy, final String triggerProcess, final String ifMatch) {
		final var headers = new HttpHeaders();
		ofNullable(body).ifPresent(_ -> headers.setContentType(APPLICATION_JSON));
		ofNullable(sentBy).ifPresent(value -> headers.add(SENT_BY_HEADER, value));
		ofNullable(triggerProcess).ifPresent(value -> headers.add(TRIGGER_PROCESS_HEADER, value));
		ofNullable(ifMatch).ifPresent(value -> headers.add(IF_MATCH, value));
		return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
	}

	private String readErrandETag(final String errandId) {
		final var response = exchange(GET, ERRANDS_PATH + errandId, null, PROCESS_ENGINE, null, null);

		assertThat(response.getStatusCode()).isEqualTo(OK);
		return response.getHeaders().getFirst(ETAG);
	}

	private List<JsonNode> requestsMadeDuring(final RequestPatternBuilder pattern, final Runnable call) {
		final var before = wiremock.findAll(pattern).stream().map(LoggedRequest::getId).collect(toSet());

		call.run();

		return wiremock.findAll(pattern).stream()
			.filter(request -> !before.contains(request.getId()))
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

	private long errandVersion(final String errandId) {
		return jdbcTemplate.queryForObject("select version from errand where id = ?", Long.class, errandId);
	}

	private String decisionColumn(final String decisionId, final String column) {
		return jdbcTemplate.queryForObject("select " + column + " from decision where id = ?", String.class, decisionId);
	}

	private boolean decisionExists(final String decisionId) {
		return jdbcTemplate.queryForObject("select count(*) from decision where id = ?", Integer.class, decisionId) > 0;
	}

	private int decisionCount() {
		return jdbcTemplate.queryForObject("select count(*) from decision", Integer.class);
	}

	private int attachmentLinks(final String decisionId) {
		return jdbcTemplate.queryForObject("select count(*) from decision_attachment where decision_id = ?", Integer.class, decisionId);
	}

	private static String decision(final String status, final String method, final String decidedBy, final String errandProcessId) {
		return """
			{
			  "type": "PERMIT",
			  "status": "%s",
			  "outcome": "APPROVAL",
			  "method": "%s",
			  "decidedBy": "%s",
			  "decidedAt": "2026-09-17T10:12:00+02:00",
			  "legalBasis": "8 kap. 12 § alkohollagen",
			  "justification": "%s",
			  "errandProcessId": %s
			}""".formatted(status, method, decidedBy, JUSTIFICATION, errandProcessId == null ? "null" : "\"" + errandProcessId + "\"");
	}

	private static String decisionsPath(final String errandId) {
		return ERRANDS_PATH + errandId + "/decisions";
	}

	private static String decisionPath(final String errandId, final String decisionId) {
		return decisionsPath(errandId) + "/" + decisionId;
	}

	private static String attachmentPath(final String errandId, final String attachmentId) {
		return ERRANDS_PATH + errandId + "/attachments/" + attachmentId;
	}

	private static String investigationPath(final String errandId, final String investigationId) {
		return ERRANDS_PATH + errandId + "/investigations/" + investigationId;
	}

	@FunctionalInterface
	private interface ExchangeCall {
		ResponseEntity<String> exchange();
	}
}
