package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.ParameterRepository;

/**
 * ErrandParameter IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandParametersIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
@DirtiesContext
class ErrandParametersIT extends AbstractAppTest {

	private static final String PATH = "/2281/NAMESPACE.1/errands/";

	private static final String REQUEST_FILE = "request.json";

	private static final String RESPONSE_FILE = "response.json";

	private static final String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";

	@Autowired
	private ParameterRepository parameterRepository;

	@Test
	void test01_updateErrandParameters() {
		setupCall()
			.withServicePath(PATH + ERRAND_ID + "/parameters")
			.withHttpMethod(PATCH)
			.withExpectedResponseStatus(OK)
			.withRequest(REQUEST_FILE)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_readErrandParameter() {
		setupCall()
			.withServicePath(PATH + ERRAND_ID + "/parameters/key1")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_findErrandParameters() {
		setupCall()
			.withServicePath(PATH + ERRAND_ID + "/parameters")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_updateErrandParameter() {
		setupCall()
			.withServicePath(PATH + ERRAND_ID + "/parameters/key1")
			.withHttpMethod(PATCH)
			.withExpectedResponseStatus(OK)
			.withRequest(REQUEST_FILE)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_deleteErrandParameter() {
		final var errandId = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
		final var parameterId = "45d266a7-1ff2-4bf4-b6f3-0473b2b86fcd";

		assertThat(parameterRepository.findById(parameterId)).isPresent();

		setupCall()
			.withServicePath(PATH + errandId + "/parameters/key1")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(parameterRepository.findById(parameterId)).isEmpty();
	}

	void test06_updateErrandParametersWithKeyDuplicates() {
		setupCall()
			.withServicePath(PATH + ERRAND_ID + "/parameters")
			.withHttpMethod(PATCH)
			.withExpectedResponseStatus(OK)
			.withRequest(REQUEST_FILE)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

}
