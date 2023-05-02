package se.sundsvall.supportmanagement.apptest;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;

/**
 * Revisions IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/RevisionsIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class RevisionsIT extends AbstractAppTest {

	private static final String RESPONSE_FILE = "response.json";

	@Test
	void test01_readErrandRevisions() throws Exception {
		setupCall()
			.withServicePath("/errands/147d355f-dc94-4fde-a4cb-9ddd16cb1946/revisions")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_readNoteRevisions() throws Exception {
		setupCall()
			.withServicePath("/errands/ec677eb3-604c-4935-bff7-f8f0b500c8f4/notes/d1f2c8d4-d234-4504-a483-b74570a7941d/revisions")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_compareErrandRevisionsAfterModifiedErrand() throws Exception {
		setupCall()
			.withServicePath("/errands/147d355f-dc94-4fde-a4cb-9ddd16cb1946/revisions/difference?source=0&target=1")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_compareErrandRevisionsAfterAddedFile() throws Exception {
		setupCall()
			.withServicePath("/errands/147d355f-dc94-4fde-a4cb-9ddd16cb1946/revisions/difference?source=1&target=2")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_compareNoteRevisions() throws Exception {
		setupCall()
		.withServicePath("/errands/ec677eb3-604c-4935-bff7-f8f0b500c8f4/notes/d1f2c8d4-d234-4504-a483-b74570a7941d/revisions/difference?source=0&target=1")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
