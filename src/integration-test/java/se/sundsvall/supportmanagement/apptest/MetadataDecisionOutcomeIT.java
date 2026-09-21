package se.sundsvall.supportmanagement.apptest;

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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.DecisionOutcomeRepository;

/**
 * DecisionOutcome Metadata IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/MetadataDecisionOutcomeIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class MetadataDecisionOutcomeIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String NAMESPACE = "NAMESPACE-ARTEFACT";
	private static final String MUNICIPALITY_2281 = "2281";
	private static final String MUNICIPALITY_2309 = "2309";
	private static final String PATH = "/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/metadata/decisionoutcomes";
	private static final String DECISION_ID = "f4000000-0000-0000-0000-000000000001";

	@Autowired
	private DecisionOutcomeRepository decisionOutcomeRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void test01_createDecisionOutcome() {
		assertThat(decisionOutcomeRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, "DISMISSAL")).isFalse();

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(PATH + "/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(decisionOutcomeRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, "DISMISSAL")).isTrue();
	}

	@Test
	void test02_getDecisionOutcome() {
		setupCall()
			.withServicePath(PATH + "/d0000000-0000-0000-0000-000000000002")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_getDecisionOutcomes() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_getDecisionOutcomesWhenEmpty() {
		setupCall()
			.withServicePath("/" + MUNICIPALITY_2309 + "/" + NAMESPACE + "/metadata/decisionoutcomes")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_createExistingDecisionOutcomeIsRejected() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(decisionOutcomeRepository.count()).isEqualTo(3);
	}

	/**
	 * Removing an outcome takes it out of what may be given from now on. A decision already given it keeps it.
	 */
	@Test
	void test06_deleteDecisionOutcome() {
		final var decisionOutcomeId = "d0000000-0000-0000-0000-000000000001";

		assertThat(decisionOutcomeRepository.existsByIdAndNamespaceAndMunicipalityId(decisionOutcomeId, NAMESPACE, MUNICIPALITY_2281)).isTrue();
		assertThat(decisionOutcomeRepository.count()).isEqualTo(3);

		setupCall()
			.withServicePath(PATH + "/" + decisionOutcomeId)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(decisionOutcomeRepository.existsByIdAndNamespaceAndMunicipalityId(decisionOutcomeId, NAMESPACE, MUNICIPALITY_2281)).isFalse();
		assertThat(decisionOutcomeRepository.count()).isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject("select outcome from decision where id = ?", String.class, DECISION_ID))
			.as("the decision given the outcome kept it").isEqualTo("APPROVAL");
	}

	@Test
	void test07_patchDecisionOutcome() {
		setupCall()
			.withServicePath(PATH + "/d0000000-0000-0000-0000-000000000003")
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_getDecisionOutcomeOfAnotherMunicipalityGives404() {
		setupCall()
			.withServicePath("/" + MUNICIPALITY_2309 + "/" + NAMESPACE + "/metadata/decisionoutcomes/d0000000-0000-0000-0000-000000000002")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test09_patchToANameAnotherOutcomeHasIsRejected() {
		setupCall()
			.withServicePath(PATH + "/d0000000-0000-0000-0000-000000000003")
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jdbcTemplate.queryForObject("select name from decision_outcome where id = ?", String.class, "d0000000-0000-0000-0000-000000000003")).isEqualTo("REJECTION");
	}
}
