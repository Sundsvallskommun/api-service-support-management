package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

import java.util.List;
import net.javacrumbs.jsonunit.core.Option;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.StatementRepository;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.StatementOutcome;

/**
 * Errand Statements IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandStatementsIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandStatementsIT extends AbstractAppTest {

	private static final String NAMESPACE = "NAMESPACE-ARTEFACT";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "a0000000-0000-0000-0000-000000000001";
	private static final String OTHER_ERRAND_ID = "cc236cf1-c00f-4479-8341-ecf5dd90b5b9";
	private static final String STATEMENT_ID = "f1000000-0000-0000-0000-000000000001";
	private static final String DRAFT_STATEMENT_ID = "f1000000-0000-0000-0000-000000000002";
	private static final String MEASURE_ID = "ee000000-0000-0000-0000-000000000200";
	private static final String LINKED_ATTACHMENT_ID = "a5000000-0000-0000-0000-000000000001";
	private static final String UNLINKED_ATTACHMENT_ID = "a5000000-0000-0000-0000-000000000002";

	private static final String PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/statements";
	private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String AD_ACCOUNT = "joe01doe; type=adAccount";

	@Autowired
	private StatementRepository statementRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void test01_createErrandStatement() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(PATH + "/" + UUID_PATTERN))
			.sendRequestAndVerifyResponse();

		assertThat(statementRepository.findByNamespaceAndMunicipalityIdAndErrandEntityIdOrderByCreated(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID))
			.filteredOn(statement -> "Remiss till räddningstjänsten om utrymning".equals(statement.getTitle()))
			.singleElement()
			.satisfies(statement -> {
				assertThat(statement.getStatus()).isEqualTo(ItemStatus.DRAFT);
				assertThat(statement.getCounterpartyName()).isEqualTo("Räddningstjänsten");
				assertThat(statement.getNamespace()).isEqualTo(NAMESPACE);
				assertThat(statement.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
			});
	}

	@Test
	void test02_readErrandStatement() {
		setupCall()
			.withServicePath(PATH + "/" + STATEMENT_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_findErrandStatements() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withJsonAssertOptions(List.of(Option.IGNORING_ARRAY_ORDER))
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_updateErrandStatement() {
		setupCall()
			.withServicePath(PATH + "/" + STATEMENT_ID)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(statementRepository.findById(STATEMENT_ID)).get()
			.satisfies(statement -> {
				assertThat(statement.getStatus()).isEqualTo(ItemStatus.COMPLETED);
				assertThat(statement.getOutcome()).isEqualTo(StatementOutcome.SUPPORTS);
				assertThat(statement.getResponseText()).isEqualTo("Miljökontoret har inget att erinra.");
				assertThat(statement.getVersion()).as("the version moved, which is what the ETag carries").isEqualTo(1L);
			});
	}

	@Test
	void test05_deleteErrandStatement() {
		jdbcTemplate.update("update measure set statement_id = ? where id = ?", DRAFT_STATEMENT_ID, MEASURE_ID);

		setupCall()
			.withServicePath(PATH + "/" + DRAFT_STATEMENT_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(statementRepository.existsById(DRAFT_STATEMENT_ID)).isFalse();
		assertThat(statementRepository.existsById(STATEMENT_ID)).as("the other statement stayed").isTrue();
		assertThat(jdbcTemplate.queryForObject("select statement_id from measure where id = ?", String.class, MEASURE_ID))
			.as("the measure stayed, without the reference").isNull();
	}

	/**
	 * A statement of another errand is not this errand's to reach. The lookup names both, so it finds nothing.
	 */
	@Test
	void test06_readingStatementOfAnotherErrandGives404() {
		setupCall()
			.withServicePath("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + OTHER_ERRAND_ID + "/statements/" + STATEMENT_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The life cycle has to add up: a statement cannot be out with the counterparty without having been sent.
	 */
	@Test
	void test07_activeWithoutSentAtIsRejected() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * And a completed one needs an outcome to be completed with.
	 */
	@Test
	void test08_completedWithoutOutcomeIsRejected() {
		setupCall()
			.withServicePath(PATH + "/" + DRAFT_STATEMENT_ID)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * An ETag that has moved on says so rather than overwriting what somebody else wrote.
	 */
	@Test
	void test09_staleIfMatchIsRejected() {
		setupCall()
			.withServicePath(PATH + "/" + STATEMENT_ID)
			.withHttpMethod(PATCH)
			.withHeader("If-Match", "\"7\"")
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(PRECONDITION_FAILED)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test10_linkStatementAttachment() {
		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(PATH + "/" + STATEMENT_ID + "/attachments/" + UNLINKED_ATTACHMENT_ID)
			.withHttpMethod(POST)
			.withRequest("{\"sortOrder\":2}")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/attachments/" + UNLINKED_ATTACHMENT_ID))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test11_updateStatementAttachment() {
		setupCall()
			.withServicePath(PATH + "/" + STATEMENT_ID + "/attachments/" + LINKED_ATTACHMENT_ID)
			.withHttpMethod(PATCH)
			.withRequest("{\"sortOrder\":5}")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jdbcTemplate.queryForObject("select sort_order from statement_attachment where statement_id = ? and attachment_id = ?", Integer.class, STATEMENT_ID, LINKED_ATTACHMENT_ID))
			.as("the new order reached the database").isEqualTo(5);
	}

	@Test
	void test12_readStatementJsonParameters() {
		setupCall()
			.withServicePath(PATH + "/" + STATEMENT_ID + "/json-parameters")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test13_updateStatementJsonParameter() {
		setupCall()
			.withServicePath(PATH + "/" + STATEMENT_ID + "/json-parameters/responseForm")
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
