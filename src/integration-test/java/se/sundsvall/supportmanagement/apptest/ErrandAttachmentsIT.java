package se.sundsvall.supportmanagement.apptest;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;

import java.util.List;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * ErrandAttachments IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandAttachmentsIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandAttachmentsIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	@Test
	void test01_readErrandAttachment() throws Exception {
		setupCall()
			.withServicePath("/errands/ec677eb3-604c-4935-bff7-f8f0b500c8f4/attachments/25d266a7-1ff2-4bf4-b6f3-0473b2b86fcd")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_readErrandAttachments() throws Exception {
		setupCall()
			.withServicePath("/errands/cc236cf1-c00f-4479-8341-ecf5dd90b5b9/attachments")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_createErrandAttachment() throws Exception {
		setupCall()
			.withServicePath("/errands/1be673c0-6ba3-4fb0-af4a-43acf23389f6/attachments")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("^http://(.*)/errands/(.*)$"))
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_deleteErrandAttachment() throws Exception {
		setupCall()
			.withServicePath("/errands/1be673c0-6ba3-4fb0-af4a-43acf23389f6/attachments/99fa4dd0-9308-4d45-bb8e-4bb881a9a536")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}
}
