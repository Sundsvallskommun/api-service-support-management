package se.sundsvall.supportmanagement.apptest;

import static java.text.MessageFormat.format;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;

@WireMockAppTestSuite(files = "classpath:/ErrandCommunicationsAttachmentIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
@DirtiesContext
class ErrandCommunicationsAttachmentIT extends AbstractAppTest {

	private static final String RESPONSE_FILE = "response.json";

	@Test
	void test01_getMessageAttachmentStreamedNotFound() {
		var attachmentId = "59328e70-4297-4bb5-ba69-cb17f2d15a17";
		setupCall()
			.withHttpMethod(GET)
			.withServicePath(format("/communication/attachments/{0}/streamed", attachmentId))
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_getCommunicationAttachmentStreamed() throws Exception {
		var attachmentId = "05b29c30-4512-46c0-9d82-d0f11cb04bae";

		setupCall()
			.withHttpMethod(GET)
			.withServicePath(format("/communication/attachments/{0}/streamed", attachmentId))
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(IMAGE_PNG_VALUE))
			.withExpectedBinaryResponse("test_image.png")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_getErrandAttachmentStreamed() throws Exception {
		var attachmentId = "b3b3b3b3-b3b3-b3b3-b3b3-b3b3b3b3b3b3";

		setupCall()
			.withHttpMethod(GET)
			.withServicePath(format("/communication/attachments/{0}/streamed", attachmentId))
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(IMAGE_JPEG_VALUE))
			.withExpectedBinaryResponse("Test_image.jpg")
			.sendRequestAndVerifyResponse();

	}

}
