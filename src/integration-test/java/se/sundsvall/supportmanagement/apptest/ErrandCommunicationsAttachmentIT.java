package se.sundsvall.supportmanagement.apptest;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;

@WireMockAppTestSuite(files = "classpath:/ErrandCommunicationsAttachmentIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandCommunicationsAttachmentIT extends AbstractAppTest {

	@Test
	void test01_getMessageAttachmentStreamedNotFound() {
		setupCall()
			.withHttpMethod(HttpMethod.GET)
			.withServicePath("/communication/attachments/59328e70-4297-4bb5-ba69-cb17f2d15a17/streamed")
			.withExpectedResponseStatus(HttpStatus.NOT_FOUND)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_getMessageAttachmentStreamed() throws Exception {
		setupCall()
			.withHttpMethod(HttpMethod.GET)
			.withServicePath("/communication/attachments/05b29c30-4512-46c0-9d82-d0f11cb04bae/streamed")
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(IMAGE_PNG_VALUE))
			.withExpectedBinaryResponse("test_image.png")
			.sendRequestAndVerifyResponse();
	}

}
