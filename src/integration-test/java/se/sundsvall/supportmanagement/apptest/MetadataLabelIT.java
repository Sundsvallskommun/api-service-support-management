package se.sundsvall.supportmanagement.apptest;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
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
 * Label Metadata IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/MetadataLabelIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class MetadataLabelIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String NAMESPACE_2584 = "NAMESPACE-2584";
	private static final String MUNICIPALITY_2281 = "2281";
	private static final String MUNICIPALITY_2282 = "2282";
	private static final String MUNICIPALITY_2309 = "2309";
	private static final String MUNICIPALITY_2584 = "2584";

	@Test
	void test01_createLabels() {
		final var path = "/" + MUNICIPALITY_2282 + "/" + NAMESPACE + "/metadata/labels";

		setupCall()
			.withServicePath(path)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(path)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_getLabels() {
		setupCall()
			.withServicePath("/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/metadata/labels")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_getLabelsWhenEmpty() {
		final var path = "/" + MUNICIPALITY_2309 + "/" + NAMESPACE + "/metadata/labels";

		setupCall()
			.withServicePath(path)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_deleteLabels() {

		final var path = "/" + MUNICIPALITY_2584 + "/" + NAMESPACE_2584 + "/metadata/labels";

		setupCall()
			.withServicePath(path)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(path)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(path)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_updateLabels() {
		final var path = "/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/metadata/labels";

		setupCall()
			.withServicePath(path)
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(path)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
