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
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.StatementOutcomeRepository;
import se.sundsvall.supportmanagement.integration.db.model.StatementOutcomeEntity;

/**
 * StatementOutcome Metadata IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/MetadataStatementOutcomeIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class MetadataStatementOutcomeIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String NAMESPACE = "NAMESPACE-ARTEFACT";
	private static final String MUNICIPALITY_2281 = "2281";
	private static final String MUNICIPALITY_2309 = "2309";
	private static final String PATH = "/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/metadata/statementoutcomes";

	@Autowired
	private StatementOutcomeRepository statementOutcomeRepository;

	/**
	 * An outcome registered without saying whether it means a response is taken to mean one.
	 */
	@Test
	void test01_createStatementOutcome() {
		assertThat(statementOutcomeRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, "NO_OBJECTION")).isFalse();

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(PATH + "/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(statementOutcomeRepository.findByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, "NO_OBJECTION"))
			.get()
			.extracting(StatementOutcomeEntity::isResponded)
			.isEqualTo(true);
	}

	@Test
	void test02_getStatementOutcome() {
		setupCall()
			.withServicePath(PATH + "/d1000000-0000-0000-0000-000000000003")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_getStatementOutcomes() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_getStatementOutcomesWhenEmpty() {
		setupCall()
			.withServicePath("/" + MUNICIPALITY_2309 + "/" + NAMESPACE + "/metadata/statementoutcomes")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_createExistingStatementOutcomeIsRejected() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(statementOutcomeRepository.count()).isEqualTo(3);
	}

	@Test
	void test06_deleteStatementOutcome() {
		final var statementOutcomeId = "d1000000-0000-0000-0000-000000000002";

		assertThat(statementOutcomeRepository.existsByIdAndNamespaceAndMunicipalityId(statementOutcomeId, NAMESPACE, MUNICIPALITY_2281)).isTrue();
		assertThat(statementOutcomeRepository.count()).isEqualTo(3);

		setupCall()
			.withServicePath(PATH + "/" + statementOutcomeId)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(statementOutcomeRepository.existsByIdAndNamespaceAndMunicipalityId(statementOutcomeId, NAMESPACE, MUNICIPALITY_2281)).isFalse();
		assertThat(statementOutcomeRepository.count()).isEqualTo(2);
	}

	/**
	 * A patch leaving out whether the outcome means a response leaves that as it was.
	 */
	@Test
	void test07_patchStatementOutcome() {
		setupCall()
			.withServicePath(PATH + "/d1000000-0000-0000-0000-000000000003")
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_getStatementOutcomeOfAnotherMunicipalityGives404() {
		setupCall()
			.withServicePath("/" + MUNICIPALITY_2309 + "/" + NAMESPACE + "/metadata/statementoutcomes/d1000000-0000-0000-0000-000000000003")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test09_patchToANameAnotherOutcomeHasIsRejected() {
		setupCall()
			.withServicePath(PATH + "/d1000000-0000-0000-0000-000000000003")
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(statementOutcomeRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, "NO_RESPONSE")).as("the outcome kept its name").isTrue();
	}
}
