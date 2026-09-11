package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.DecisionRepository;
import se.sundsvall.supportmanagement.integration.db.model.DecisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionOutcome;

/**
 * Errand Decisions IT tests, including the terms a decision carries, the attachments linked to it and the JSON
 * parameters it owns.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandDecisionsIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandDecisionsIT extends AbstractAppTest {

	private static final String NAMESPACE = "NAMESPACE-ARTEFACT";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "a0000000-0000-0000-0000-000000000001";
	private static final String DECISION_ID = "f4000000-0000-0000-0000-000000000001";
	private static final String TERM_ID = "f5000000-0000-0000-0000-000000000002";
	private static final String LINKED_ATTACHMENT_ID = "a5000000-0000-0000-0000-000000000001";
	private static final String UNLINKED_ATTACHMENT_ID = "a5000000-0000-0000-0000-000000000002";
	private static final int NAMESPACE_CONFIG_ID = 6;

	private static final String ERRAND_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID;
	private static final String PATH = ERRAND_PATH + "/decisions";
	private static final String DECISION_PATH = PATH + "/" + DECISION_ID;
	private static final String TERM_PATH = DECISION_PATH + "/terms/" + TERM_ID;
	private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String AD_ACCOUNT = "joe01doe; type=adAccount";

	@Autowired
	private DecisionRepository decisionRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * Several decisions per errand are allowed unless the namespace says otherwise - interim decisions, partial
	 * decisions and reconsideration are ordinary where one line of business expects exactly one.
	 */
	@Test
	void test01_createErrandDecision() {
		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(PATH + "/" + UUID_PATTERN))
			.sendRequestAndVerifyResponse();

		assertThat(decisionRepository.findByNamespaceAndMunicipalityIdAndErrandEntityIdOrderByCreated(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID))
			.as("the errand now holds two decisions, which the namespace has not forbidden")
			.hasSize(2)
			.filteredOn(decision -> DecisionOutcome.PARTIAL_APPROVAL.equals(decision.getOutcome()))
			.singleElement()
			.satisfies(decision -> assertThat(decision.getCreatedBy()).isEqualTo("joe01doe"));
	}

	@Test
	void test02_readErrandDecision() {
		setupCall()
			.withServicePath(DECISION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_findErrandDecisions() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_updateErrandDecision() {
		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(DECISION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(decisionRepository.findById(DECISION_ID)).get()
			.satisfies(decision -> {
				assertThat(decision.getOutcome()).isEqualTo(DecisionOutcome.PARTIAL_APPROVAL);
				assertThat(decision.getJustification()).isEqualTo("Tillstånd ges för del av lokalen.");
				assertThat(decision.getVersion()).isEqualTo(1L);
			});
	}

	/**
	 * The location names the term that was created, which takes the id of the term rather than of a copy of it.
	 */
	@Test
	void test05_createDecisionTerm() {
		setupCall()
			.withServicePath(DECISION_PATH + "/terms")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(DECISION_PATH + "/terms/" + UUID_PATTERN))
			.sendRequestAndVerifyResponse();

		assertThat(terms()).isEqualTo(3);
	}

	@Test
	void test06_findDecisionTerms() {
		setupCall()
			.withServicePath(DECISION_PATH + "/terms")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test07_readDecisionTerm() {
		setupCall()
			.withServicePath(TERM_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_updateDecisionTerm() {
		setupCall()
			.withServicePath(TERM_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(textOf(TERM_ID)).isEqualTo("Högst 90 gäster får vistas i lokalen samtidigt.");
		assertThat(decisionRepository.findById(DECISION_ID)).get()
			.extracting(DecisionEntity::getVersion).as("the term is part of the decision, so its version moved").isEqualTo(1L);
	}

	@Test
	void test09_deleteDecisionTerm() {
		setupCall()
			.withServicePath(TERM_PATH)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(terms()).isOne();
	}

	/**
	 * The decision rests on an investigation of the same errand. One belonging to another errand is not reachable, and
	 * is answered as the 404 it is rather than written as a reference across errands.
	 */
	@Test
	void test10_restingOnAnInvestigationOfAnotherErrandGives404() {
		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * A caseworker cannot stamp their own decision as automatic. That difference has to be answerable afterwards, which
	 * is why it is checked when the decision comes in rather than trusted.
	 */
	@Test
	void test11_anAdAccountCannotWriteAnAutomaticDecision() {
		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(FORBIDDEN)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The decision takes its terms and the JSON parameters it owns, and leaves the attachments linked to it on the errand.
	 */
	@Test
	void test12_deleteErrandDecision() {
		setupCall()
			.withServicePath(DECISION_PATH)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(decisionRepository.existsById(DECISION_ID)).isFalse();
		assertThat(terms()).as("the terms went with the decision").isZero();
		assertThat(parametersWithKey("decisionForm")).as("and so did its parameter").isZero();
		assertThat(attachments(LINKED_ATTACHMENT_ID)).as("the attachment stayed on the errand").isOne();
	}

	@Test
	void test13_uploadDecisionAttachment() throws Exception {
		setupCall()
			.withServicePath(DECISION_PATH + "/attachments?sortOrder=2")
			.withHttpMethod(POST)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("attachment", "test.txt")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(ERRAND_PATH + "/attachments/" + UUID_PATTERN))
			.sendRequestAndVerifyResponse();

		assertThat(attachmentLinks()).as("the uploaded attachment is linked to the decision").isEqualTo(2);
	}

	@Test
	void test14_linkDecisionAttachment() {
		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(DECISION_PATH + "/attachments/" + UNLINKED_ATTACHMENT_ID)
			.withHttpMethod(POST)
			.withRequest("{\"sortOrder\":2}")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(ERRAND_PATH + "/attachments/" + UNLINKED_ATTACHMENT_ID))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(attachmentLinks()).isEqualTo(2);
	}

	@Test
	void test15_updateDecisionAttachment() {
		setupCall()
			.withServicePath(DECISION_PATH + "/attachments/" + LINKED_ATTACHMENT_ID)
			.withHttpMethod(PATCH)
			.withRequest("{\"sortOrder\":5}")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jdbcTemplate.queryForObject("select sort_order from decision_attachment where decision_id = ? and attachment_id = ?", Integer.class, DECISION_ID, LINKED_ATTACHMENT_ID))
			.as("the new order reached the database").isEqualTo(5);
	}

	@Test
	void test16_unlinkDecisionAttachment() {
		setupCall()
			.withServicePath(DECISION_PATH + "/attachments/" + LINKED_ATTACHMENT_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(attachmentLinks()).as("the link went").isZero();
		assertThat(attachments(LINKED_ATTACHMENT_ID)).as("the attachment stayed on the errand").isOne();
	}

	@Test
	void test17_readDecisionJsonParameters() {
		setupCall()
			.withServicePath(DECISION_PATH + "/json-parameters")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test18_readDecisionJsonParameter() {
		setupCall()
			.withServicePath(DECISION_PATH + "/json-parameters/decisionForm")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test19_createDecisionJsonParameter() {
		setupCall()
			.withServicePath(DECISION_PATH + "/json-parameters/dispatchLog")
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(DECISION_PATH + "/json-parameters/dispatchLog"))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(parametersWithKey("dispatchLog")).isOne();
	}

	@Test
	void test20_deleteDecisionJsonParameter() {
		setupCall()
			.withServicePath(DECISION_PATH + "/json-parameters/decisionForm")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(parametersWithKey("decisionForm")).isZero();
		assertThat(decisionRepository.existsById(DECISION_ID)).as("the decision stayed").isTrue();
	}

	/**
	 * A namespace that expects exactly one decision per errand says so in its configuration, and a second one is then
	 * the conflict it is rather than a decision nobody can tell apart from the first.
	 */
	@Test
	void test21_aSecondDecisionIsAConflictWhereTheNamespaceAllowsOne() {
		jdbcTemplate.update("insert into namespace_config_value(namespace_config_id, `key`, `value`, `type`) values (?, 'SINGLE_DECISION_PER_ERRAND', 'true', 'BOOLEAN')",
			NAMESPACE_CONFIG_ID);

		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CONFLICT)
			.sendRequestAndVerifyResponse();

		assertThat(decisionRepository.findByNamespaceAndMunicipalityIdAndErrandEntityIdOrderByCreated(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).hasSize(1);
	}

	/** Read with SQL rather than through JPA: the collections of a loaded entity are lazy, and the test has no session. */
	private int terms() {
		return jdbcTemplate.queryForObject("select count(*) from decision_term where decision_id = ?", Integer.class, DECISION_ID);
	}

	private String textOf(final String termId) {
		return jdbcTemplate.queryForObject("select text from decision_term where id = ?", String.class, termId);
	}

	private int parametersWithKey(final String key) {
		return jdbcTemplate.queryForObject("select count(*) from json_parameter where errand_id = ? and parameter_key = ?", Integer.class, ERRAND_ID, key);
	}

	private int attachmentLinks() {
		return jdbcTemplate.queryForObject("select count(*) from decision_attachment where decision_id = ?", Integer.class, DECISION_ID);
	}

	private int attachments(final String attachmentId) {
		return jdbcTemplate.queryForObject("select count(*) from attachment where id = ?", Integer.class, attachmentId);
	}
}
