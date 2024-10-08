package se.sundsvall.supportmanagement.apptest;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;

/**
 * ErrandCommunication IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandCommunicationIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
@DirtiesContext
class ErrandCommunicationIT extends AbstractAppTest {

	private static final String PATH = "/2281/NAMESPACE.1/errands/";

	private static final String REQUEST_FILE = "request.json";

	private static final String RESPONSE_FILE = "response.json";

	@Test
	void test01_sendEmail() {
		setupCall()
			.withServicePath(PATH + "ec677eb3-604c-4935-bff7-f8f0b500c8f4/communication/email")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_sendEmailWithAttachments() {
		setupCall()
			.withServicePath(PATH + "cc236cf1-c00f-4479-8341-ecf5dd90b5b9/communication/email")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		setupCall()
			.withServicePath(PATH + "cc236cf1-c00f-4479-8341-ecf5dd90b5b9/communication")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_sendSms() {
		setupCall()
			.withServicePath(PATH + "1be673c0-6ba3-4fb0-af4a-43acf23389f6/communication/sms")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_readCommunications() {
		setupCall()
			.withServicePath(PATH + "ec677eb3-604c-4935-bff7-f8f0b500c8f4/communication")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_updateViewedStatus() {
		setupCall()
			.withServicePath(PATH + "ec677eb3-604c-4935-bff7-f8f0b500c8f4/communication/cc236cf1-c00f-4479-8341-ecf5dd90b5b9/viewed/true")
			.withHttpMethod(PUT)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test06_sendEmailWithErrandAttachment() {
		setupCall()
			.withServicePath(PATH + "cc236cf1-c00f-4479-8341-ecf5dd90b5b9/communication/email")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		setupCall()
			.withServicePath(PATH + "cc236cf1-c00f-4479-8341-ecf5dd90b5b9/communication")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

	}

}
