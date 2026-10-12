package se.sundsvall.supportmanagement.apptest;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Objects.isNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.ETAG;
import static org.springframework.http.HttpHeaders.IF_MATCH;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;
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
	private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

	private static final String RUNNING_ERRAND_ID = "aa000000-0000-0000-0000-0000000000a1";
	private static final String ERRAND_WITHOUT_PROCESS_ID = "aa000000-0000-0000-0000-0000000000a2";
	private static final String ENDED_ERRAND_ID = "aa000000-0000-0000-0000-0000000000a5";
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

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String DECISIONS_RESPONSE_FILE = "response-decisions.json";

	@Autowired
	private ProcessEventOutboxRepository outboxRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ProcessEngineProperties processEngineProperties;

	private static String errandPath(final String errandId) {
		return "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + errandId;
	}

	private static String decisionsPath(final String errandId) {
		return errandPath(errandId) + "/decisions";
	}

	private static String decisionPath(final String errandId, final String decisionId) {
		return decisionsPath(errandId) + "/" + decisionId;
	}

	private static String attachmentPath(final String errandId, final String attachmentId) {
		return errandPath(errandId) + "/attachments/" + attachmentId;
	}

	private static String investigationPath(final String errandId, final String investigationId) {
		return errandPath(errandId) + "/investigations/" + investigationId;
	}

	/**
	 * The whole chain from the caseworker: the decision is logged as an errand event, the event becomes a row addressed to
	 * the process of the errand, and the version of the errand moves. The process row sent along is not taken, since a
	 * manual decision is made by no process whatever the body says. No label of the errand gives it a start mode.
	 */
	@Test
	@DisplayName("Verification that a caseworker writing a decision gives an outbox row with the sub type DECISION")
	void test01_aCaseworkersDecisionReachesTheProcess() {
		setupCall()
			.withServicePath(errandPath(RUNNING_ERRAND_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(ETAG, List.of("0"))
			.sendRequest();

		final var location = setupCall()
			.withServicePath(decisionsPath(RUNNING_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(decisionsPath(RUNNING_ERRAND_ID) + "/" + UUID_PATTERN))
			.sendRequest()
			.getResponseHeaders()
			.getLocation();

		assertThat(outboxRepository.findAll()).singleElement().satisfies(row -> {
			assertThat(row.getErrandId()).isEqualTo(RUNNING_ERRAND_ID);
			assertThat(row.getProcessService()).isEqualTo(PROCESS_SERVICE);
			assertThat(row.getProcessKey()).isEqualTo(PROCESS_KEY);
			assertThat(row.getEventType()).isEqualTo("UPDATE");
			assertThat(row.getEventSubType()).isEqualTo("DECISION");
			assertThat(row.getExecutedBy()).isEqualTo(HANDLER);
			assertThat(row.isStartAllowed()).isFalse();
		});

		setupCall()
			.withServicePath(location.getPath())
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(errandPath(RUNNING_ERRAND_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(ETAG, List.of("1"))
			.sendRequestAndVerifyResponse();
	}

	/**
	 * Loop guard layer 1 holds for a decision the process concludes itself: no row is written, although a concluded
	 * decision passes the emergency brake. The decision names the live process as the one that made it, whatever the body
	 * says.
	 */
	@Test
	@DisplayName("Verification that the process concluding its own decision is not woken by it, and is recorded as the one that made it")
	void test02_theProcessConcludingItsOwnDecisionIsNotWokenByIt() {
		final var location = setupCall()
			.withServicePath(decisionsPath(RUNNING_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withHeader(TRIGGER_PROCESS_HEADER, DO_NOT_WAKE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(decisionsPath(RUNNING_ERRAND_ID) + "/" + UUID_PATTERN))
			.sendRequest()
			.getResponseHeaders()
			.getLocation();

		assertThat(outboxRepository.findAll()).isEmpty();

		setupCall()
			.withServicePath(location.getPath())
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * Both directions are refused with 403, on create as well as on update, and nothing is written.
	 */
	@Test
	@DisplayName("Verification that a decision cannot claim the method of the other kind of writer")
	void test03_aDecisionCannotClaimTheMethodOfTheOtherKindOfWriter() {
		setupCall()
			.withServicePath(decisionsPath(RUNNING_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, OTHER_ENGINE)
			.withRequest("request-automatic-by-other-engine.json")
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse("response-automatic-by-other-engine.json")
			.sendRequest();

		setupCall()
			.withServicePath(decisionsPath(RUNNING_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-automatic-by-handler.json")
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse("response-automatic-by-handler.json")
			.sendRequest();

		setupCall()
			.withServicePath(decisionsPath(RUNNING_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withRequest("request-manual-by-process.json")
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse("response-manual-by-process.json")
			.sendRequest();

		setupCall()
			.withServicePath(decisionPath(RUNNING_ERRAND_ID, DRAFT_DECISION_ID))
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-patch-automatic-by-handler.json")
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse("response-patch-automatic-by-handler.json")
			.sendRequest();

		setupCall()
			.withServicePath(decisionsPath(RUNNING_ERRAND_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(DECISIONS_RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	/**
	 * While the process lives a decision can be written again, until it is concluded. After that it is locked as it
	 * stands - taking it back to a draft and deleting it included - and so are its terms, its attachments and the
	 * attachments of the errand it rests on.
	 */
	@Test
	@DisplayName("Verification that a decision is written again while the process lives, and locked once concluded")
	void test04_aDecisionIsLockedOnceConcluded() {
		final var path = decisionPath(RUNNING_ERRAND_ID, DRAFT_DECISION_ID);

		setupCall()
			.withServicePath(path)
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withHeader(IF_MATCH, "\"0\"")
			.withRequest("request-title.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		setupCall()
			.withServicePath(path)
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withHeader(IF_MATCH, "\"1\"")
			.withRequest("request-conclude.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		assertThat(outboxRepository.findAll()).hasSize(2).allSatisfy(row -> assertThat(row.getEventSubType()).isEqualTo("DECISION"));

		setupCall()
			.withServicePath(path)
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withHeader(IF_MATCH, "\"2\"")
			.withRequest("request-retitle.json")
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-locked.json")
			.sendRequest();

		setupCall()
			.withServicePath(path)
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-draft.json")
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-locked.json")
			.sendRequest();

		setupCall()
			.withServicePath(path)
			.withHttpMethod(DELETE)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-locked.json")
			.sendRequest();

		setupCall()
			.withServicePath(path + "/terms")
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-term.json")
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-locked.json")
			.sendRequest();

		setupCall()
			.withServicePath(path + "/attachments/" + DRAFT_ATTACHMENT_ID)
			.withHttpMethod(DELETE)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-locked.json")
			.sendRequest();

		setupCall()
			.withServicePath(path + "/attachments/" + COMPLETED_ATTACHMENT_ID)
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-locked.json")
			.sendRequest();

		setupCall()
			.withServicePath(attachmentPath(RUNNING_ERRAND_ID, DRAFT_ATTACHMENT_ID))
			.withHttpMethod(DELETE)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-attachment-locked.json")
			.sendRequest();

		setupCall()
			.withServicePath(investigationPath(RUNNING_ERRAND_ID, DRAFT_INVESTIGATION_ID))
			.withHttpMethod(DELETE)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-investigation-locked.json")
			.sendRequest();

		assertThat(outboxRepository.findAll()).hasSize(2);
		wiremock.verify(2, postRequestedFor(urlPathEqualTo("/api-eventlog/" + MUNICIPALITY_ID + "/" + RUNNING_ERRAND_ID)));

		setupCall()
			.withServicePath(decisionsPath(RUNNING_ERRAND_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(DECISIONS_RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
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

		setupCall()
			.withServicePath(decisionPath(RUNNING_ERRAND_ID, DRAFT_DECISION_ID))
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-title.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		assertThat(outboxRepository.findAll()).filteredOn(row -> isNull(row.getDeliveredAt())).isEmpty();

		setupCall()
			.withServicePath(decisionsPath(RUNNING_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withRequest("request-concluded-by-process.json")
			.withExpectedResponseStatus(CREATED)
			.sendRequest();

		assertThat(outboxRepository.findAll()).filteredOn(row -> isNull(row.getDeliveredAt())).isEmpty();

		setupCall()
			.withServicePath(decisionPath(RUNNING_ERRAND_ID, DRAFT_DECISION_ID))
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-conclude.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		assertThat(outboxRepository.findAll()).filteredOn(row -> isNull(row.getDeliveredAt())).singleElement()
			.extracting(ProcessEventOutboxEntity::getEventSubType)
			.isEqualTo("DECISION");
		wiremock.verify(3, postRequestedFor(urlPathEqualTo("/api-eventlog/" + MUNICIPALITY_ID + "/" + RUNNING_ERRAND_ID)));
		verifyStubs();
	}

	/**
	 * A completed process, which is never started again, locks every decision of its errand whatever their status, and a
	 * new decision too. An attachment no decision rests on, and the JSON parameters of the decision, stay open.
	 */
	@Test
	@DisplayName("Verification that no decision of an errand whose process has run to its end can be written")
	void test06_noDecisionOfAnEndedProcessCanBeWritten() {
		final var path = decisionPath(ENDED_ERRAND_ID, ENDED_DECISION_ID);

		setupCall()
			.withServicePath(decisionsPath(ENDED_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-locked.json")
			.sendRequest();

		setupCall()
			.withServicePath(path)
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-retitle.json")
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-locked.json")
			.sendRequest();

		setupCall()
			.withServicePath(path)
			.withHttpMethod(DELETE)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-locked.json")
			.sendRequest();

		setupCall()
			.withServicePath(path + "/terms")
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-term.json")
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-locked.json")
			.sendRequest();

		setupCall()
			.withServicePath(path + "/terms/" + ENDED_TERM_ID)
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-edit-term.json")
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-locked.json")
			.sendRequest();

		setupCall()
			.withServicePath(path + "/terms/" + ENDED_TERM_ID)
			.withHttpMethod(DELETE)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-locked.json")
			.sendRequest();

		setupCall()
			.withServicePath(path + "/attachments/" + UNLINKED_ENDED_ATTACHMENT_ID)
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-locked.json")
			.sendRequest();

		setupCall()
			.withServicePath(path + "/attachments/" + ENDED_ATTACHMENT_ID)
			.withHttpMethod(DELETE)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-locked.json")
			.sendRequest();

		setupCall()
			.withServicePath(attachmentPath(ENDED_ERRAND_ID, ENDED_ATTACHMENT_ID))
			.withHttpMethod(DELETE)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-attachment-locked.json")
			.sendRequest();

		setupCall()
			.withServicePath(investigationPath(ENDED_ERRAND_ID, ENDED_INVESTIGATION_ID))
			.withHttpMethod(DELETE)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-investigation-locked.json")
			.sendRequest();

		assertThat(outboxRepository.findAll()).isEmpty();

		setupCall()
			.withServicePath(path)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-decision.json")
			.sendRequest();

		setupCall()
			.withServicePath(attachmentPath(ENDED_ERRAND_ID, UNLINKED_ENDED_ATTACHMENT_ID))
			.withHttpMethod(DELETE)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequest();

		setupCall()
			.withServicePath(path + "/json-parameters/legalForce")
			.withHttpMethod(DELETE)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * An errand that has never had a process is never locked. Every change is written to the event log and moves the
	 * version of the errand.
	 */
	@Test
	@DisplayName("Verification that a completed decision on an errand without a process can be changed and deleted")
	void test07_anErrandWithoutAProcessIsNeverLocked() {
		final var path = decisionPath(ERRAND_WITHOUT_PROCESS_ID, DECISION_WITHOUT_PROCESS_ID);

		setupCall()
			.withServicePath(path)
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withHeader(IF_MATCH, "\"0\"")
			.withRequest("request-retitle.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		setupCall()
			.withServicePath(path)
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withHeader(IF_MATCH, "\"1\"")
			.withRequest("request-draft.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		setupCall()
			.withServicePath(path)
			.withHttpMethod(DELETE)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withHeader(IF_MATCH, "\"2\"")
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequest();

		setupCall()
			.withServicePath(path)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		wiremock.verify(3, postRequestedFor(urlPathEqualTo("/api-eventlog/" + MUNICIPALITY_ID + "/" + ERRAND_WITHOUT_PROCESS_ID)));

		setupCall()
			.withServicePath(errandPath(ERRAND_WITHOUT_PROCESS_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(ETAG, List.of("3"))
			.sendRequestAndVerifyResponse();
	}

	/**
	 * A work step reads the errand, and a decision written meanwhile changes what it read. The step is told so with 412
	 * and runs again on the errand as it now is.
	 */
	@Test
	@DisplayName("Verification that a work step holding the errand from before a decision was written gets 412")
	void test08_aWorkStepHoldingAnOlderErrandIsToldItHasChanged() {
		setupCall()
			.withServicePath(errandPath(RUNNING_ERRAND_ID))
			.withHttpMethod(GET)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(ETAG, List.of("0"))
			.sendRequest();

		setupCall()
			.withServicePath(decisionPath(RUNNING_ERRAND_ID, DRAFT_DECISION_ID))
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-title.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		setupCall()
			.withServicePath(errandPath(RUNNING_ERRAND_ID))
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withHeader(TRIGGER_PROCESS_HEADER, DO_NOT_WAKE)
			.withHeader(IF_MATCH, "\"0\"")
			.withRequest("request-errand.json")
			.withExpectedResponseStatus(PRECONDITION_FAILED)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(errandPath(RUNNING_ERRAND_ID))
			.withHttpMethod(GET)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(ETAG, List.of("1"))
			.sendRequest();

		setupCall()
			.withServicePath(errandPath(RUNNING_ERRAND_ID))
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withHeader(TRIGGER_PROCESS_HEADER, DO_NOT_WAKE)
			.withHeader(IF_MATCH, "\"1\"")
			.withRequest("request-errand.json")
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * Two writers holding the same version of the decision: the first one wins, and the second is told the decision has
	 * changed instead of overwriting it.
	 */
	@Test
	@DisplayName("Verification that the second of two writes made with the same If-Match gets 412")
	void test09_theSecondOfTwoWritesWithTheSameVersionGets412() {
		final var path = decisionPath(RUNNING_ERRAND_ID, DRAFT_DECISION_ID);

		setupCall()
			.withServicePath(path)
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withHeader(IF_MATCH, "\"0\"")
			.withRequest("request-first.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		setupCall()
			.withServicePath(path)
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withHeader(IF_MATCH, "\"0\"")
			.withRequest("request-second.json")
			.withExpectedResponseStatus(PRECONDITION_FAILED)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(path)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-decision.json")
			.sendRequestAndVerifyResponse();
	}

	/**
	 * Deleting the errand deletes its decisions. The process row a decision names is no foreign key of it, so removing the
	 * row on its own leaves the decision and its reference to the row as they were.
	 * <p>
	 * Both are deleted straight in the database, since it is the schema that is tested, and a decision of a deleted errand
	 * is only visible there: over the wire the errand itself is not found.
	 */
	@Test
	@DisplayName("Verification that deleting the errand takes its decisions along, and that removing a process row does not")
	void test10_theDecisionGoesWithTheErrandAndStaysWithoutItsProcessRow() {
		jdbcTemplate.update("delete from errand_process where id = ?", "ep-it-completed");

		setupCall()
			.withServicePath(decisionPath(ENDED_ERRAND_ID, ENDED_DECISION_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-ended-decision.json")
			.sendRequest();

		jdbcTemplate.update("delete from errand where id = ?", ERRAND_WITHOUT_PROCESS_ID);

		assertThat(jdbcTemplate.queryForObject("select count(*) from decision where id = ?", Integer.class, DECISION_WITHOUT_PROCESS_ID)).isZero();

		setupCall()
			.withServicePath(decisionPath(RUNNING_ERRAND_ID, DRAFT_DECISION_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-draft-decision.json")
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The justification is masked in the payload log: the masked value is found where the justification was, in the
	 * request and in the response alike.
	 */
	@Test
	@DisplayName("Verification that the justification of a decision appears in no log line, going in or coming out")
	void test11_theJustificationIsNeverLogged(final CapturedOutput output) {
		setupCall()
			.withServicePath(decisionsPath(RUNNING_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.sendRequest();

		setupCall()
			.withServicePath(decisionsPath(RUNNING_ERRAND_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(DECISIONS_RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(output.getAll())
			.doesNotContain(JUSTIFICATION)
			.doesNotContain("Utkast till motivering");
		assertThat(MASKED_JUSTIFICATION.matcher(output.getAll()).results().count())
			.as("the justification of the request and the three of the response, masked")
			.isGreaterThanOrEqualTo(4);
	}

	/**
	 * An errand whose process has ended holds a decision that can no longer be changed, so the errand is kept, together
	 * with the decision, and no row is written for its process.
	 */
	@Test
	@DisplayName("Verification that an errand holding a decision that can no longer be changed is not deleted, and the decision stays")
	void test12_anErrandHoldingALockedDecisionCannotBeDeleted() {
		setupCall()
			.withServicePath(errandPath(ENDED_ERRAND_ID))
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-locked.json")
			.sendRequest();

		setupCall()
			.withServicePath(decisionPath(ENDED_ERRAND_ID, ENDED_DECISION_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-ended-decision.json")
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	/**
	 * A decision concluded on an errand whose process still runs is locked as well, so the errand is kept, together with
	 * its decisions, and no row is written for its process.
	 */
	@Test
	@DisplayName("Verification that an errand with a live process and a concluded decision is not deleted, and its decisions stay")
	void test13_anErrandWithALiveProcessAndAConcludedDecisionCannotBeDeleted() {
		setupCall()
			.withServicePath(errandPath(RUNNING_ERRAND_ID))
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-locked.json")
			.sendRequest();

		setupCall()
			.withServicePath(decisionsPath(RUNNING_ERRAND_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(DECISIONS_RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	/**
	 * Trips the emergency brake of the errand by writing as many delivered rows inside its window as it allows.
	 */
	private void tripTheBrake(final String errandId) {
		EmergencyBrake.trip(outboxRepository, processEngineProperties, MUNICIPALITY_ID, NAMESPACE, errandId, PROCESS_KEY);
	}
}
