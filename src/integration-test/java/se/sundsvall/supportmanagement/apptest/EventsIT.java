package se.sundsvall.supportmanagement.apptest;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;

import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

/**
 * Events IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/EventsIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class EventsIT extends AbstractAppTest {

	private static final String RESPONSE_FILE = "response.json";
	private static final String PATH = "/2281/NAMESPACE-1/errands/";

	@Test
	void test01_readErrandEvents() {
		setupCall()
			.withServicePath(PATH + "147d355f-dc94-4fde-a4cb-9ddd16cb1946/events?sort=created,ASC")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_readErrandEventsPaginated() {
		setupCall()
			.withServicePath(PATH + "147d355f-dc94-4fde-a4cb-9ddd16cb1946/events?page=1&size=2&sort=created,ASC")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_readErrandEventsWithoutAccess() {
		// Access control is on for this namespace and the user holds no labels for the errand.
		setupCall()
			.withServicePath("/2506/NAMESPACE-2506/errands/c9efe03d-deff-4828-a043-541fa78ffdeb/events")
			.withHeader(SENT_BY_HEADER, "rob01rep; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(UNAUTHORIZED)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_readErrandEventsForUnknownErrand() {
		setupCall()
			.withServicePath(PATH + "d3a1f2c4-0000-4000-8000-000000000000/events")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();
	}
}
