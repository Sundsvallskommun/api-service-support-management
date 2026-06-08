package se.sundsvall.supportmanagement.apptest;

import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;

/**
 * Integration tests for errand action processing during create/update.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandActionsIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-it-errand-actions.sql",
	"/db/scripts/testdata-it-send-email-actions.sql"
})
class ErrandActionsIT extends AbstractAppTest {

	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

	@Test
	void test01_createErrandWithScheduledAction() {
		// Create errand with STATUS-1, which matches the action config with 24h duration
		final var location = createErrand(REQUEST_FILE);

		// Verify the errand has a scheduled action and no labels
		setupCall()
			.withServicePath(location)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_createErrandWithImmediateAction() {
		// Create errand with STATUS-2, which matches the action config without duration (immediate execution)
		final var location = createErrand(REQUEST_FILE);
		
		// Verify the action was executed immediately (label added, no pending action)
		setupCall()
			.withServicePath(location)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_updateErrandFulfilledActionRemoved() {
		// Create errand with STATUS-1 to get a scheduled action
		final var location = createErrand("create-request.json");

		// Verify action was created
		setupCall()
			.withServicePath(location)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("create-response.json")
			.sendRequest();

		// Update the errand with the label that the action would add - making the action fulfilled
		setupCall()
			.withServicePath(location)
			.withHttpMethod(PATCH)
			.withRequest("update-request.json")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("update-response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_createErrandNoMatchingCondition() {
		// Create errand with STATUS-3, which doesn't match any active action config condition
		final var location = createErrand(REQUEST_FILE);

		// Verify no actions were created and no labels
		setupCall()
			.withServicePath(location)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_updateErrandNoDuplicateAction() {
		// Create errand with STATUS-1 to get a scheduled action
		final var location = createErrand("create-request.json");

		// Verify one action exists
		setupCall()
			.withServicePath(location)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("create-response.json")
			.sendRequest();

		// Update the errand title - action should not be duplicated
		setupCall()
			.withServicePath(location)
			.withHttpMethod(PATCH)
			.withRequest("update-request.json")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("update-response.json")
			.sendRequestAndVerifyResponse();
	}

	// --- SEND_EMAIL action tests ---

	@Test
	void test06_createErrandWithScheduledSendEmail() {
		// Create errand with STATUS-4, which matches the SEND_EMAIL config with 24h duration
		final var location = createErrand(REQUEST_FILE);

		// Verify the errand has a scheduled SEND_EMAIL action
		setupCall()
			.withServicePath(location)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test07_createErrandWithImmediateSendEmail() {
		// Create errand with STATUS-5, which matches the SEND_EMAIL config without duration (immediate execution)
		final var location = createErrand(REQUEST_FILE);

		// Verify the action was executed immediately (email sent, no pending action)
		setupCall()
			.withServicePath(location)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_createErrandNoMatchingSendEmailCondition() {
		// Create errand with STATUS-3, which doesn't match any SEND_EMAIL action config condition
		final var location = createErrand(REQUEST_FILE);

		// Verify no SEND_EMAIL actions were created
		setupCall()
			.withServicePath(location)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test09_updateErrandNoDuplicateSendEmailAction() {
		// Create errand with STATUS-4 to get a scheduled SEND_EMAIL action
		final var location = createErrand("create-request.json");

		// Verify one SEND_EMAIL action exists
		setupCall()
			.withServicePath(location)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("create-response.json")
			.sendRequest();

		// Update the errand title - action should not be duplicated
		setupCall()
			.withServicePath(location)
			.withHttpMethod(PATCH)
			.withRequest("update-request.json")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("update-response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test10_updateErrandTriggersImmediateSendEmail() {
		// Create errand with STATUS-3 (no matching SEND_EMAIL condition)
		final var location = createErrand("create-request.json");

		// Verify no actions were created
		setupCall()
			.withServicePath(location)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("create-response.json")
			.sendRequest();

		// Update status to STATUS-5 which matches the immediate SEND_EMAIL config
		setupCall()
			.withServicePath(location)
			.withHttpMethod(PATCH)
			.withRequest("update-request.json")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("update-response.json")
			.sendRequestAndVerifyResponse();
	}

	private String createErrand(final String requestFile) {
		return setupCall()
			.withHeader(SENT_BY_HEADER, "joe01doe; type=adAccount")
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(requestFile)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + UUID_PATTERN))
			.sendRequest()
			.getResponseHeaders()
			.get(LOCATION).stream().findFirst().orElseThrow();
	}
}
