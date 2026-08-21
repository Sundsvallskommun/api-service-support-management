package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

import java.util.List;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
class ErrandParametersIT extends AbstractAppTest {

	private static final String PATH = "/2281/NAMESPACE-1/errands/";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
	private static final String ACCESS_CONTROLLED_ERRAND = "/2506/NAMESPACE-2506/errands/58c41b44-0b9f-413d-bd46-406d24bf5ca8";

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

	@Test
	void test06_updateErrandParametersWithKeyDuplicates() {
		setupCall()
			.withServicePath(PATH + ERRAND_ID + "/parameters")
			.withHttpMethod(PATCH)
			.withExpectedResponseStatus(OK)
			.withRequest(REQUEST_FILE)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test07_patchParametersKeepsKeysTheUserMayNotSee() {
		// Namespace 2506 has access control on, with a role restricted to a single parameter key. smo02key holds that
		// role and patches back the list they were served, which cannot be allowed to delete what they never saw.
		setupCall()
			.withServicePath(ACCESS_CONTROLLED_ERRAND + "/parameters")
			.withHeader(SENT_BY_HEADER, "smo02key; type=adAccount")
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		// adm01adm holds no role and is therefore unrestricted, so both keys must come back.
		setupCall()
			.withServicePath(ACCESS_CONTROLLED_ERRAND + "/parameters")
			.withHeader(SENT_BY_HEADER, "adm01adm; type=adAccount")
			.withHttpMethod(GET)
			.withJsonAssertOptions(List.of(Option.IGNORING_ARRAY_ORDER))
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-privileged.json")
			.sendRequestAndVerifyResponse();
	}
}
