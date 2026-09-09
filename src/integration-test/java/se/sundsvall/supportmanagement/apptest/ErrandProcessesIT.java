package se.sundsvall.supportmanagement.apptest;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;

import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

/**
 * The process resource over the wire.
 * <p>
 * What this asks that the tests below the resource cannot: that the paths route, that the write only fields of the
 * report never come back in a response, that a created row answers with a Location pointing at itself, and that the
 * refusals reach the caller as the status codes the process engine acts on rather than as exceptions.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandProcessesIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandProcessesIT extends AbstractAppTest {

	private static final String NAMESPACE = "PROCESS-NAMESPACE";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_WITH_PROCESS = "aa000000-0000-0000-0000-0000000000a1";
	private static final String ERRAND_WITHOUT_PROCESS = "aa000000-0000-0000-0000-0000000000a2";
	private static final String LIVE_INSTANCE_ID = "pi-it-live";
	private static final String PROCESS_ENGINE = "pw-alkt; type=processEngine";

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	private static String processesPath(final String errandId) {
		return "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + errandId + "/processes";
	}

	private static String activitiesPath(final String errandId) {
		return "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + errandId + "/process-activities";
	}

	@Test
	void test01_reportProcessCreatesTheRow() {
		final var path = processesPath(ERRAND_WITHOUT_PROCESS) + "/pi-it-new";

		setupCall()
			.withServicePath(path)
			.withHttpMethod(PUT)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(path))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_reportProcessUpdatesTheRow() {
		setupCall()
			.withServicePath(processesPath(ERRAND_WITH_PROCESS) + "/" + LIVE_INSTANCE_ID)
			.withHttpMethod(PUT)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_registerProcessStart() {
		setupCall()
			.withServicePath(processesPath(ERRAND_WITHOUT_PROCESS))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(processesPath(ERRAND_WITHOUT_PROCESS) + "/pi-it-registered"))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * A start that failed carries no instance, so there is nothing to point a location at - and the row is still created.
	 */
	@Test
	void test04_registerStartThatFailedAnswersWithoutALocation() {
		setupCall()
			.withServicePath(processesPath(ERRAND_WITHOUT_PROCESS))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_readErrandProcesses() {
		setupCall()
			.withServicePath(processesPath(ERRAND_WITH_PROCESS))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * An errand that never had a process answers with an empty list rather than 404 - the envelope is the answer, and an
	 * empty list is not an error.
	 */
	@Test
	void test06_readErrandProcessesOfAnErrandWithNone() {
		setupCall()
			.withServicePath(processesPath(ERRAND_WITHOUT_PROCESS))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test07_readProcessActivities() {
		setupCall()
			.withServicePath(activitiesPath(ERRAND_WITH_PROCESS))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * Narrowing the log to one instance leaves the entries belonging to no instance out, which is the point of narrowing
	 * it: those entries explain why no process started and belong to the errand rather than to a process.
	 */
	@Test
	void test08_readProcessActivitiesNarrowedToAnInstance() {
		setupCall()
			.withServicePath(activitiesPath(ERRAND_WITH_PROCESS) + "?processInstanceId=" + LIVE_INSTANCE_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test09_reportFromAnotherProcessServiceIsRejected() {
		setupCall()
			.withServicePath(processesPath(ERRAND_WITH_PROCESS) + "/" + LIVE_INSTANCE_ID)
			.withHttpMethod(PUT)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test10_reportWithoutAnIdentifierIsRejected() {
		setupCall()
			.withServicePath(processesPath(ERRAND_WITH_PROCESS) + "/" + LIVE_INSTANCE_ID)
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The refusal a process engine acts on: a second live instance for an errand that already has one.
	 */
	@Test
	void test11_secondLiveInstanceIsRefused() {
		setupCall()
			.withServicePath(processesPath(ERRAND_WITH_PROCESS) + "/pi-it-second")
			.withHttpMethod(PUT)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The refusal a work step that only reads the errand gets when a handler has been there since. Nothing of the report
	 * survives it: the state of the instance and the log of the errand are read back afterwards and stand where they did.
	 */
	@Test
	void test12_reportReadAtAStaleErrandVersionIsRefused() {
		setupCall()
			.withServicePath(processesPath(ERRAND_WITH_PROCESS) + "/" + LIVE_INSTANCE_ID)
			.withHttpMethod(PUT)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(PRECONDITION_FAILED)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(processesPath(ERRAND_WITH_PROCESS))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-processes.json")
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(activitiesPath(ERRAND_WITH_PROCESS))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-activities.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test13_reportReadAtTheCurrentErrandVersionIsTaken() {
		setupCall()
			.withServicePath(processesPath(ERRAND_WITH_PROCESS) + "/" + LIVE_INSTANCE_ID)
			.withHttpMethod(PUT)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * Two branches of one instance working at the same time. Both reports are taken - refusing one would silence the
	 * entry that reveals the model is breaking the rule - and the warning stands in the log afterwards.
	 */
	@Test
	void test14_twoTasksWorkingAtOnceAreWarnedAboutAndBothReportsAreTaken() {
		final var path = processesPath(ERRAND_WITH_PROCESS) + "/" + LIVE_INSTANCE_ID;

		setupCall()
			.withServicePath(path)
			.withHttpMethod(PUT)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withRequest("request-first-task.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		setupCall()
			.withServicePath(path)
			.withHttpMethod(PUT)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withRequest("request-second-task.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		setupCall()
			.withServicePath(activitiesPath(ERRAND_WITH_PROCESS))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
