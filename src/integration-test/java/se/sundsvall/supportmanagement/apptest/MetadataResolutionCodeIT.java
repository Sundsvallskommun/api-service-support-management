package se.sundsvall.supportmanagement.apptest;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.ResolutionCodeRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * ResolutionCode Metadata IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/MetadataResolutionCodeIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class MetadataResolutionCodeIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String NAMESPACE = "NAMESPACE-ARTEFACT";
	private static final String MUNICIPALITY_2281 = "2281";
	private static final String MUNICIPALITY_2309 = "2309";
	private static final String PATH = "/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/metadata/resolutioncodes";
	private static final String SOLVED_ID = "d2000000-0000-0000-0000-000000000001";
	private static final String HANDED_OVER_ID = "d2000000-0000-0000-0000-000000000002";
	private static final String LEGACY_ID = "d2000000-0000-0000-0000-000000000003";
	private static final String ERRAND_ID = "a0000000-0000-0000-0000-000000000001";

	@Autowired
	private ResolutionCodeRepository resolutionCodeRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void test01_createResolutionCode() {
		assertThat(resolutionCodeRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, "FIXED")).isFalse();

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(PATH + "/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(resolutionCodeRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, "FIXED")).isTrue();
		assertThat(jdbcTemplate.queryForObject("select deprecated from resolution_code where namespace = ? and municipality_id = ? and name = 'FIXED'", Boolean.class, NAMESPACE, MUNICIPALITY_2281))
			.isFalse();
	}

	@Test
	void test02_getResolutionCode() {
		setupCall()
			.withServicePath(PATH + "/" + SOLVED_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_getResolutionCodes() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_getResolutionCodesWhenEmpty() {
		setupCall()
			.withServicePath("/" + MUNICIPALITY_2309 + "/" + NAMESPACE + "/metadata/resolutioncodes")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_createExistingResolutionCodeIsRejected() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(resolutionCodeRepository.count()).isEqualTo(3);
	}

	/**
	 * A code that no errand uses is removed for good.
	 */
	@Test
	void test06_deleteResolutionCode() {
		assertThat(resolutionCodeRepository.existsByIdAndNamespaceAndMunicipalityId(HANDED_OVER_ID, NAMESPACE, MUNICIPALITY_2281)).isTrue();

		setupCall()
			.withServicePath(PATH + "/" + HANDED_OVER_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(resolutionCodeRepository.existsByIdAndNamespaceAndMunicipalityId(HANDED_OVER_ID, NAMESPACE, MUNICIPALITY_2281)).isFalse();
		assertThat(resolutionCodeRepository.count()).isEqualTo(2);
	}

	/**
	 * A patch need not carry the name, which stays as it is.
	 */
	@Test
	void test07_patchResolutionCodeWithoutName() {
		setupCall()
			.withServicePath(PATH + "/" + LEGACY_ID)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_getResolutionCodeOfAnotherMunicipalityGives404() {
		setupCall()
			.withServicePath("/" + MUNICIPALITY_2309 + "/" + NAMESPACE + "/metadata/resolutioncodes/" + SOLVED_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test09_patchToAnotherNameIsRejected() {
		setupCall()
			.withServicePath(PATH + "/" + SOLVED_ID)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jdbcTemplate.queryForObject("select name from resolution_code where id = ?", String.class, SOLVED_ID)).isEqualTo("SOLVED");
	}

	/**
	 * A code an errand uses, soft deleted or not, can only be deprecated.
	 */
	@Test
	void test10_deleteResolutionCodeInUseIsRejected() {
		jdbcTemplate.update("update errand set resolution = 'SOLVED', deleted = now() where id = ?", ERRAND_ID);

		setupCall()
			.withServicePath(PATH + "/" + SOLVED_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(resolutionCodeRepository.existsByIdAndNamespaceAndMunicipalityId(SOLVED_ID, NAMESPACE, MUNICIPALITY_2281)).isTrue();
	}
}
