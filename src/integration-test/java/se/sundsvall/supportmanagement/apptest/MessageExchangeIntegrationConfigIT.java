package se.sundsvall.supportmanagement.apptest;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;

@WireMockAppTestSuite(files = "classpath:/MessageExchangeIntegrationConfigIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class MessageExchangeIntegrationConfigIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String NAMESPACE_1 = "NAMESPACE-1";
	private static final String NAMESPACE_2 = "NAMESPACE-2";
	private static final String NAMESPACE_3 = "NAMESPACE-3";
	private static final String MUNICIPALITY_ID = "2281";

	private static final UnaryOperator<String> PATH = namespace -> "/" + MUNICIPALITY_ID + "/" + namespace + "/messageexchange-integration-config";

	@Test
	void test01_getConfig() {
		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_1))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_createConfig() {
		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_3))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequest();

		final var location = setupCall()
			.withServicePath(PATH.apply(NAMESPACE_3))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(PATH.apply(NAMESPACE_3)))
			.withExpectedResponseBodyIsNull()
			.sendRequest()
			.getResponseHeaders()
			.getFirst(LOCATION);

		setupCall()
			.withServicePath(location)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_createMinimalConfig() {
		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_3))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequest();

		final var location = setupCall()
			.withServicePath(PATH.apply(NAMESPACE_3))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(PATH.apply(NAMESPACE_3)))
			.withExpectedResponseBodyIsNull()
			.sendRequest()
			.getResponseHeaders()
			.getFirst(LOCATION);

		setupCall()
			.withServicePath(location)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_updateConfig() {
		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_1))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse("response_before.json")
			.sendRequest();

		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_1))
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_1))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse("response_after.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_deleteConfig() {
		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_1))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest();

		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_1))
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_1))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test06_getConfigNotFound() {
		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_3))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test07_deleteConfigNotFound() {
		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_3))
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_updateConfigNotFound() {
		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_3))
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test09_createConfigFailsWithoutNamespaceConfig() {
		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_2))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequest();

		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_2))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(INTERNAL_SERVER_ERROR)
			.sendRequest();

		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_2))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();
	}
}
