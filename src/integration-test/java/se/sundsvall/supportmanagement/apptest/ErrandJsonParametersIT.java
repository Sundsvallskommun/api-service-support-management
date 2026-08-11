package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.JsonParameterRepository;

/**
 * ErrandJsonParameters IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandJsonParametersIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandJsonParametersIT extends AbstractAppTest {

	private static final String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
	private static final String PATH = "/2281/NAMESPACE-1/errands/" + ERRAND_ID + "/json-parameters";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	@Autowired
	private JsonParameterRepository jsonParameterRepository;

	@Test
	void test01_readJsonParameter() {
		setupCall()
			.withServicePath(PATH + "/formData")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_updateJsonParameter() {
		final var initialId = jsonParameterRepository.findAll().stream()
			.filter(p -> "formData".equals(p.getKey()))
			.findFirst()
			.map(e -> e.getId())
			.orElseThrow();

		setupCall()
			.withServicePath(PATH + "/formData")
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		final var updated = jsonParameterRepository.findById(initialId);
		assertThat(updated).isPresent();
		assertThat(updated.get().getSchemaId()).isEqualTo("test-schema-1.0");
	}

	@Test
	void test03_updateJsonParameterWithWrongIfMatch() {
		setupCall()
			.withServicePath(PATH + "/formData")
			.withHttpMethod(PUT)
			.withHeader("If-Match", "\"999\"")
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(PRECONDITION_FAILED)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_deleteJsonParameterWithWrongIfMatch() {
		setupCall()
			.withServicePath(PATH + "/formData")
			.withHttpMethod(DELETE)
			.withHeader("If-Match", "\"999\"")
			.withExpectedResponseStatus(PRECONDITION_FAILED)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_deleteJsonParameter() {
		assertThat(jsonParameterRepository.findAll().stream()
			.anyMatch(p -> "formData".equals(p.getKey()))).isTrue();

		setupCall()
			.withServicePath(PATH + "/formData")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(jsonParameterRepository.findAll().stream()
			.anyMatch(p -> "formData".equals(p.getKey()))).isFalse();
	}

	@Test
	void test06_createJsonParameter() {
		setupCall()
			.withServicePath(PATH + "/newKey")
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jsonParameterRepository.findAll().stream()
			.anyMatch(p -> "newKey".equals(p.getKey()))).isTrue();
	}

	@Test
	void test07_readNonexistentJsonParameter() {
		setupCall()
			.withServicePath(PATH + "/nonExistentKey")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_readAllJsonParameters() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test09_createJsonParameterWithInvalidSchema() {
		setupCall()
			.withServicePath(PATH + "/newKey")
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test10_deleteNonexistentJsonParameter() {
		setupCall()
			.withServicePath(PATH + "/nonExistentKey")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test11_readEmptyJsonParameterList() {
		setupCall()
			.withServicePath("/2281/NAMESPACE-1/errands/cc236cf1-c00f-4479-8341-ecf5dd90b5b9/json-parameters")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test12_createJsonParameterWithSchemaServiceError() {
		// The json-schema service responds with 500 → the validator rethrows the ServerProblem → 502 Bad Gateway.
		setupCall()
			.withServicePath(PATH + "/newKey")
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_GATEWAY)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
