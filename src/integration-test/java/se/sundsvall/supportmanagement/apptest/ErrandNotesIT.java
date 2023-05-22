package se.sundsvall.supportmanagement.apptest;

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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;

/**
 * ErrandNotes IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandNotesIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandNotesIT extends AbstractAppTest {

	private static final String PATH = "/NAMESPACE.1/2281/errands/"; // 2281 is the municipalityId of Sundsvalls kommun
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	@Test
	void test01_findErrandNotes() throws Exception {
		setupCall()
			.withServicePath(PATH + "ec677eb3-604c-4935-bff7-f8f0b500c8f4/notes?context=SUPPORT&role=FIRST_LINE_SUPPORT&partyId=81471222-5798-11e9-ae24-57fa13b361e1&page=1&limit=100")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_readErrandNote() throws Exception {
		setupCall()
			.withServicePath(PATH + "ec677eb3-604c-4935-bff7-f8f0b500c8f4/notes/d1f2c8d4-d234-4504-a483-b74570a7941d")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_createErrandNote() throws Exception {
		setupCall()
			.withHeader("sentbyuser", "cre03ate")
			.withServicePath(PATH + "ec677eb3-604c-4935-bff7-f8f0b500c8f4/notes")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("^http://(.*)/errands/(.*)$"))
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_updateErrandNote() throws Exception {
		setupCall()
			.withServicePath(PATH + "ec677eb3-604c-4935-bff7-f8f0b500c8f4/notes/d1f2c8d4-d234-4504-a483-b74570a7941d")
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_deleteErrandNote() throws Exception {
		setupCall()
			.withHeader("sentbyuser", "del05ete")
			.withServicePath(PATH + "ec677eb3-604c-4935-bff7-f8f0b500c8f4/notes/d1f2c8d4-d234-4504-a483-b74570a7941d")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}
}
