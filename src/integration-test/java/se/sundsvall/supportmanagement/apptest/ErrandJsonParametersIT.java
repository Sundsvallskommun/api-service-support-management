package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;

/**
 * ErrandJsonParameter IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandJsonParametersIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandJsonParametersIT extends AbstractAppTest {

	private static final String PATH = "/2281/NAMESPACE-1/errands/ec677eb3-604c-4935-bff7-f8f0b500c8f4/json-parameters";
	private static final String KEY = "formData";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	@Test
	void test01_createJsonParameter() {
		setupCall()
			.withServicePath(PATH + "/newKey")
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_readJsonParameter() {
		setupCall()
			.withServicePath(PATH + "/" + KEY)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_updateJsonParameter() {
		setupCall()
			.withServicePath(PATH + "/" + KEY)
			.withHttpMethod(PUT)
			.withHeader("If-Match", "\"0\"")
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_updateJsonParameterWithWrongIfMatch() {
		setupCall()
			.withServicePath(PATH + "/" + KEY)
			.withHttpMethod(PUT)
			.withHeader("If-Match", "\"99\"")
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(PRECONDITION_FAILED)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_deleteJsonParameter() {
		setupCall()
			.withServicePath(PATH + "/" + KEY)
			.withHttpMethod(DELETE)
			.withHeader("If-Match", "\"0\"")
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(PATH + "/" + KEY)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test06_readNonexistentJsonParameter() {
		setupCall()
			.withServicePath(PATH + "/nonexistent")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
