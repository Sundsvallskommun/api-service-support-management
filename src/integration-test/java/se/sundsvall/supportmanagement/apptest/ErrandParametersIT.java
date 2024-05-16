package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;

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

	private static final String PATH = "/NAMESPACE.1/2281/errands/"; // 2281 is the municipalityId of Sundsvalls kommun
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";

	@Autowired
	private ParameterRepository parameterRepository;

	@Test
	void test01_createErrandParameter() {
		setupCall()
			.withServicePath(PATH + ERRAND_ID + "/parameters")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("^/NAMESPACE.1/2281/errands/" + ERRAND_ID + "/parameters/(.*)$"))
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_readErrandParameter() {
		setupCall()
			.withServicePath(PATH + ERRAND_ID + "/parameters/55d266a7-1ff2-4bf4-b6f3-0473b2b86fcd")
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
			.withServicePath(PATH + ERRAND_ID + "/parameters/55d266a7-1ff2-4bf4-b6f3-0473b2b86fcd")
			.withHttpMethod(PATCH)
			.withExpectedResponseStatus(OK)
			.withRequest(REQUEST_FILE)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_deleteErrandParameter() {
		final var errandId = "cc236cf1-c00f-4479-8341-ecf5dd90b5b9";
		final var parameterId = "45d266a7-1ff2-4bf4-b6f3-0473b2b86fcd";

		assertThat(parameterRepository.findById(parameterId)).isPresent();

		setupCall()
			.withServicePath(PATH + errandId + "/parameters/" + parameterId)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(parameterRepository.findById(parameterId)).isEmpty();
	}
}
