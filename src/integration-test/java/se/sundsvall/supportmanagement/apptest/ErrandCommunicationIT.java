package se.sundsvall.supportmanagement.apptest;

import static java.util.UUID.randomUUID;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.util.MimeTypeUtils.IMAGE_JPEG_VALUE;
import static se.sundsvall.dept44.support.Identifier.HEADER_NAME;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
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

	private static final String PATH = "/2281/NAMESPACE-1/errands";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	@Test
	void test01_sendEmail() {
		setupCall()
			.withServicePath(PATH + "/ec677eb3-604c-4935-bff7-f8f0b500c8f4/communication/email")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_sendEmailWithAttachments() {
		setupCall()
			.withServicePath(PATH + "/cc236cf1-c00f-4479-8341-ecf5dd90b5b9/communication/email")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		setupCall()
			.withServicePath(PATH + "/cc236cf1-c00f-4479-8341-ecf5dd90b5b9/communication")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_sendSms() {
		setupCall()
			.withServicePath(PATH + "/1be673c0-6ba3-4fb0-af4a-43acf23389f6/communication/sms")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_readCommunications() {
		setupCall()
			.withServicePath(PATH + "/ec677eb3-604c-4935-bff7-f8f0b500c8f4/communication")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_updateViewedStatus() {
		setupCall()
			.withServicePath(PATH + "/ec677eb3-604c-4935-bff7-f8f0b500c8f4/communication/cc236cf1-c00f-4479-8341-ecf5dd90b5b9/viewed/true")
			.withHttpMethod(PUT)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test06_sendEmailWithErrandAttachment() {
		setupCall()
			.withServicePath(PATH + "/cc236cf1-c00f-4479-8341-ecf5dd90b5b9/communication/email")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		setupCall()
			.withServicePath(PATH + "/cc236cf1-c00f-4479-8341-ecf5dd90b5b9/communication")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test07_getMessageAttachmentNotFound() {

		final var errandId = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
		final var communicationId = "59328e70-4297-4bb5-ba69-cb17f2d15a17";
		final var attachmentId = randomUUID().toString();

		setupCall()
			.withHttpMethod(GET)
			.withServicePath(PATH + "/" + errandId + "/communication/" + communicationId + "/attachments/" + attachmentId)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_getCommunicationAttachment() throws Exception {

		final var errandId = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
		final var communicationId = "59328e70-4297-4bb5-ba69-cb17f2d15a17";
		final var attachmentId = "05b29c30-4512-46c0-9d82-d0f11cb04bae";

		setupCall()
			.withHttpMethod(GET)
			.withServicePath(PATH + "/" + errandId + "/communication/" + communicationId + "/attachments/" + attachmentId)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(IMAGE_JPEG_VALUE))
			.withExpectedBinaryResponse("Test_image.jpg")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test09_sendWebMessageUserUnknown() {
		// external
		setupCall()
			.withServicePath(PATH + "/cad8ec4e-0b6b-473a-800d-feb063f59094/communication/webmessage")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test10_sendWebMessageWithAttachments() {
		// internal
		setupCall()
			.withServicePath(PATH + "/b481b191-dd37-47ca-b417-ed3a56ba724c/communication/webmessage")
			.withHttpMethod(POST)
			.withHeader("x-sent-by", "someUserId; type=adAccount")
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		// fetch attachment
		setupCall()
			.withServicePath(PATH + "/b481b191-dd37-47ca-b417-ed3a56ba724c/communication")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test11_sendWebMessage() {
		// external
		setupCall()
			.withServicePath(PATH + "/cad8ec4e-0b6b-473a-800d-feb063f59094/communication/webmessage")
			.withHttpMethod(POST)
			.withHeader("x-sent-by", "someUserId; type=adAccount")
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test12_readExternalCommunications() {
		setupCall()
			.withServicePath(PATH + "/ec677eb3-604c-4935-bff7-f8f0b500c8f4/communication/external")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test13_getConversations() {
		setupCall()
			.withHttpMethod(GET)
			.withServicePath(PATH + "/ec677eb3-604c-4935-bff7-f8f0b500c8f4/communication/conversations")
			.withHeader(HEADER_NAME, "type=adAccount; joe01doe")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test14_getConversationById() {
		setupCall()
			.withHttpMethod(GET)
			.withServicePath(PATH + "/ec677eb3-604c-4935-bff7-f8f0b500c8f4/communication/conversations/7a772d18-a588-41bc-91ec-13b7421c9bb8")
			.withHeader(HEADER_NAME, "type=adAccount; joe01doe")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test15_updateConversationById() {
		setupCall()
			.withHttpMethod(PATCH)
			.withServicePath(PATH + "/ec677eb3-604c-4935-bff7-f8f0b500c8f4/communication/conversations/7a772d18-a588-41bc-91ec-13b7421c9bb8")
			.withHeader(HEADER_NAME, "type=adAccount; joe01doe")
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test16_createConversation() {
		final var location = setupCall()
			.withHttpMethod(POST)
			.withServicePath(PATH + "/ec677eb3-604c-4935-bff7-f8f0b500c8f4/communication/conversations")
			.withHeader(HEADER_NAME, "type=adAccount; joe01doe")
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.sendRequest()
			.getResponseHeaders().get(LOCATION).getFirst();

		setupCall()
			.withServicePath(location)
			.withHttpMethod(GET)
			.withHeader(HEADER_NAME, "type=adAccount; joe01doe")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test17_getConversationMessages() {
		setupCall()
			.withHttpMethod(GET)
			.withServicePath(PATH + "/ec677eb3-604c-4935-bff7-f8f0b500c8f4/communication/conversations/f4524497-a592-4618-a746-b59a60a76f13/messages")
			.withHeader(HEADER_NAME, "type=adAccount; joe01doe")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test18_createConversationMessage() throws FileNotFoundException {
		setupCall()
			.withHttpMethod(POST)
			.withServicePath(PATH + "/ec677eb3-604c-4935-bff7-f8f0b500c8f4/communication/conversations/f4524497-a592-4618-a746-b59a60a76f13/messages")
			.withHeader(HEADER_NAME, "type=adAccount; joe01doe")
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("message", REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test19_getConversationMessageAttachment() throws IOException {
		setupCall()
			.withServicePath(PATH + "/ec677eb3-604c-4935-bff7-f8f0b500c8f4/communication/conversations/f4524497-a592-4618-a746-b59a60a76f13/messages/d82bd8ac-1507-4d9a-958d-369261eecc15/attachments/05b29c30-4512-46c0-9d82-d0f11cb04bae")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(IMAGE_JPEG_VALUE))
			.withExpectedBinaryResponse("Test_image.jpg")
			.sendRequestAndVerifyResponse();
	}
}
