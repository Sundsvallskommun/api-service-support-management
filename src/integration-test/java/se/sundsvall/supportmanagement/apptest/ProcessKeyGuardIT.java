package se.sundsvall.supportmanagement.apptest;

import java.util.List;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

/**
 * Verifies the process key guard over the wire: a label change that would leave an errand naming a process it does not
 * belong to.
 * <p>
 * The refusal reaches the caller as a 400 whose detail names what is wrong, and is decided by the process rows in the
 * database - a live process, one that has run to its end, and none at all.
 */
@WireMockAppTestSuite(files = "classpath:/ProcessKeyGuardIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-process-key-guard.sql"
})
class ProcessKeyGuardIT extends AbstractAppTest {

	private static final String NAMESPACE = "PROCESS-NAMESPACE";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_WITH_LIVE_PROCESS = "aa000000-0000-0000-0000-0000000000a1";
	private static final String ERRAND_WITHOUT_PROCESS = "aa000000-0000-0000-0000-0000000000a2";
	private static final String ERRAND_WITH_FINISHED_PROCESS = "aa000000-0000-0000-0000-0000000000a3";

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	private static String errandPath(final String errandId) {
		return "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + errandId;
	}

	@Test
	@DisplayName("Verification that relabelling an errand which runs a process into another process is refused")
	void test01_relabellingAnErrandRunningAProcessIsRefused() {
		setupCall()
			.withServicePath(errandPath(ERRAND_WITH_LIVE_PROCESS))
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@DisplayName("Verification that a process which has run to its end holds the labels just as still: the process life of the errand is over")
	void test02_relabellingAnErrandWhoseProcessHasFinishedIsRefused() {
		setupCall()
			.withServicePath(errandPath(ERRAND_WITH_FINISHED_PROCESS))
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@DisplayName("Verification that an errand which has never had a process is not held by the guard at all")
	void test03_relabellingAnErrandWithoutAProcessGoesThrough() {
		setupCall()
			.withServicePath(errandPath(ERRAND_WITHOUT_PROCESS))
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequest();

		setupCall()
			.withServicePath(errandPath(ERRAND_WITHOUT_PROCESS))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withJsonAssertOptions(List.of(Option.IGNORING_EXTRA_FIELDS))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@DisplayName("Verification that a label saying nothing about a process goes onto an errand running one as any other label would")
	void test04_addingALabelThatNamesNoProcessGoesThrough() {
		setupCall()
			.withServicePath(errandPath(ERRAND_WITH_LIVE_PROCESS))
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequest();

		setupCall()
			.withServicePath(errandPath(ERRAND_WITH_LIVE_PROCESS))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withJsonAssertOptions(List.of(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@DisplayName("Verification that an errand may not be labelled into two processes at once, whether it runs one or not")
	void test05_labellingAnErrandWithTwoProcessesIsRefused() {
		setupCall()
			.withServicePath(errandPath(ERRAND_WITHOUT_PROCESS))
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
