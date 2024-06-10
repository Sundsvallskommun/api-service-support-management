package se.sundsvall.supportmanagement.apptest;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

import org.junit.jupiter.api.Test;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;

/**
 * Events IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/EventsIT/", classes = Application.class)
class EventsIT extends AbstractAppTest {

	private static final String RESPONSE_FILE = "response.json";

	@Test
	void test01_readErrandEvents() {
		setupCall()
			.withServicePath("/errands/147d355f-dc94-4fde-a4cb-9ddd16cb1946/events?sort=created,ASC")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_readErrandEventsPaginated() {
		setupCall()
			.withServicePath("/errands/147d355f-dc94-4fde-a4cb-9ddd16cb1946/events?page=1&size=2&sort=created,ASC")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

}
