package se.sundsvall.supportmanagement.apptest;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;

/**
 * Handover IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/HandoverIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class HandoverIT extends AbstractAppTest {

	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands";
	private static final String PREVIEW = "/handover/preview";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	@Test
	void test01_previewHandover() {
		setupCall()
			.withServicePath(PATH + "/ec677eb3-604c-4935-bff7-f8f0b500c8f4" + PREVIEW)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_previewHandoverWithLabelMappings() {
		setupCall()
			.withServicePath(PATH + "/1be673c0-6ba3-4fb0-af4a-43acf23389f6" + PREVIEW)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_previewHandoverWithSchemaWarning() {
		// Create an errand carrying json parameters (the source data does not contain any)
		final var headers = setupCall()
			.withHeader(SENT_BY_HEADER, "joe01doe; type=adAccount")
			.withServicePath("/" + MUNICIPALITY_ID + "/CONTACTCENTER/errands")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.sendRequest()
			.getResponseHeaders();

		// Preview a handover to NAMESPACE-1; 'test-schema-1.0' is registered in the target while
		// 'test-schema-2.0' is not (responds with 404), which renders a PARAMETER_SCHEMA_MISMATCH warning
		setupCall()
			.withServicePath(headers.get(LOCATION).stream().findFirst().orElseThrow() + PREVIEW)
			.withHttpMethod(POST)
			.withRequest("request-preview.json")
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_previewHandoverToSameNamespace() {
		setupCall()
			.withServicePath(PATH + "/ec677eb3-604c-4935-bff7-f8f0b500c8f4" + PREVIEW)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_PROBLEM_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_previewHandoverToNonExistingTargetNamespace() {
		setupCall()
			.withServicePath(PATH + "/ec677eb3-604c-4935-bff7-f8f0b500c8f4" + PREVIEW)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_PROBLEM_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test06_previewHandoverForNonExistingErrand() {
		setupCall()
			.withServicePath(PATH + "/4756ab93-2c64-4f36-a916-0a06ffa77ad9" + PREVIEW)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_PROBLEM_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test07_previewHandoverUnauthorized() {
		// NAMESPACE-2506 has access control enabled and the stubbed accessmapper response does not
		// grant the user access to any label on the errand
		setupCall()
			.withServicePath("/2506/NAMESPACE-2506/errands/7c57b4c3-9ef6-472d-8f03-5c15e9458ad6" + PREVIEW)
			.withHeader(SENT_BY_HEADER, "joe01doe; type=adAccount")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(UNAUTHORIZED)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_PROBLEM_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
