package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.ETAG;
import static org.springframework.http.HttpHeaders.IF_MATCH;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
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
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionMethod;

/**
 * Errand Decisions IT tests, including the terms a decision carries, the attachments linked to it, the JSON parameters
 * it owns and the parameters it carries.
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
	private static final String MEASURE_ID = "ee000000-0000-0000-0000-000000000200";
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
	 * Several decisions per errand are allowed unless the namespace says otherwise, so a second decision is created.
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
			.filteredOn(decision -> "PARTIAL_APPROVAL".equals(decision.getOutcome()))
			.singleElement()
			.satisfies(decision -> assertThat(decision.getCreatedBy()).isEqualTo("joe01doe"));
		assertThat(jdbcTemplate.queryForList("""
			select concat_ws('|', p.parameters_key, p.display_name, p.parameter_group, v.value) from decision_parameter p
			join decision d on d.id = p.decision_id
			join decision_parameter_values v on v.decision_parameter_id = p.id
			where d.title = 'Interimistiskt beslut'
			order by p.parameters_key, v.value_order""", String.class))
			.as("the values sent for the same key are held under one parameter")
			.containsExactly("area|lokal|north", "zone|Zon|A", "zone|Zon|B");
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
				assertThat(decision.getOutcome()).isEqualTo("PARTIAL_APPROVAL");
				assertThat(decision.getJustification()).isEqualTo("Tillstånd ges för del av lokalen.");
				assertThat(decision.getVersion()).isEqualTo(1L);
			});
		assertThat(parameterValues()).as("the sent parameters replaced the stored ones").containsExactly("servingArea=inomhus");
	}

	/**
	 * The location names the id of the term that was created.
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
	 * is answered with 404.
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
	 * A caseworker cannot stamp their own decision as automatic, and is refused with 403.
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
		jdbcTemplate.update("update measure set decision_id = ? where id = ?", DECISION_ID, MEASURE_ID);

		setupCall()
			.withServicePath(DECISION_PATH)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(decisionRepository.existsById(DECISION_ID)).isFalse();
		assertThat(terms()).as("the terms went with the decision").isZero();
		assertThat(parametersWithKey("decisionForm")).as("and so did its parameter").isZero();
		assertThat(jdbcTemplate.queryForObject("select count(*) from decision_parameter where decision_id = ?", Integer.class, DECISION_ID))
			.as("and its parameters").isZero();
		assertThat(jdbcTemplate.queryForObject("select count(*) from decision_parameter_values", Integer.class)).as("with their values").isZero();
		assertThat(attachmentLinks()).as("the link went").isZero();
		assertThat(attachments(LINKED_ATTACHMENT_ID)).as("the attachment stayed on the errand").isOne();
		assertThat(jdbcTemplate.queryForObject("select decision_id from measure where id = ?", String.class, MEASURE_ID))
			.as("the measure stayed, without the reference").isNull();
	}

	@Test
	void test13_uploadDecisionAttachment() throws Exception {
		setupCall()
			.withServicePath(DECISION_PATH + "/attachments")
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
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(ERRAND_PATH + "/attachments/" + UNLINKED_ATTACHMENT_ID))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(attachmentLinks()).isEqualTo(2);
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
	 * refused with 409 and not written.
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

	/**
	 * The outcomes are the namespace's to register, and one it has not registered is refused with 400 and not written.
	 */
	@Test
	void test22_anOutcomeTheNamespaceHasNotRegisteredIsRejected() {
		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(decisionRepository.findByNamespaceAndMunicipalityIdAndErrandEntityIdOrderByCreated(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).hasSize(1);
	}

	/**
	 * Verifies that in a namespace without a process consumer an automatic decision is taken from a caller that is not an
	 * ad account.
	 */
	@Test
	void test23_aServiceWritesAnAutomaticDecisionWhereNoProcessRuns() {
		setupCall()
			.withHeader(SENT_BY_HEADER, "e-service; type=processEngine")
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(PATH + "/" + UUID_PATTERN))
			.sendRequestAndVerifyResponse();

		assertThat(decisionRepository.findByNamespaceAndMunicipalityIdAndErrandEntityIdOrderByCreated(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID))
			.filteredOn(decision -> "Automatiskt beslut".equals(decision.getTitle()))
			.singleElement()
			.satisfies(decision -> {
				assertThat(decision.getMethod()).isEqualTo(DecisionMethod.AUTOMATIC);
				assertThat(decision.getCreatedBy()).isEqualTo("e-service");
			});
	}

	/**
	 * A change to the parameters alone moves the version of the decision once, and the ETag it answers with is the version
	 * it was stored with. The caller already stands as the last to modify it, so nothing but the parameters changes.
	 */
	@Test
	void test24_updatingOnlyTheParametersMovesTheVersion() {
		jdbcTemplate.update("update decision set modified_by = 'joe01doe' where id = ?", DECISION_ID);

		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(DECISION_PATH)
			.withHttpMethod(PATCH)
			.withHeader(IF_MATCH, "\"0\"")
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(ETAG, List.of("1"))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jdbcTemplate.queryForObject("select version from decision where id = ?", Long.class, DECISION_ID)).isOne();
		assertThat(parameterValues()).containsExactly("zone=A", "zone=B");
	}

	/**
	 * Parameters that come out the same as the stored ones, in another order and with the values of a key split, leave the
	 * decision as it stands.
	 */
	@Test
	void test25_sameParametersLeaveTheDecisionAsItStands() {
		jdbcTemplate.update("update decision set modified_by = 'joe01doe' where id = ?", DECISION_ID);

		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(DECISION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(ETAG, List.of("0"))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jdbcTemplate.queryForList("select id from decision_parameter where decision_id = ? order by id", String.class, DECISION_ID))
			.as("the stored parameters were not written again")
			.containsExactly("f7000000-0000-0000-0000-000000000001", "f7000000-0000-0000-0000-000000000002");
	}

	@Test
	void test26_anEmptyListRemovesTheParameters() {
		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(DECISION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jdbcTemplate.queryForObject("select count(*) from decision_parameter where decision_id = ?", Integer.class, DECISION_ID)).isZero();
		assertThat(jdbcTemplate.queryForObject("select count(*) from decision_parameter_values", Integer.class)).as("the values went with them").isZero();
	}

	@Test
	void test27_aParameterWithoutKeyIsRejected() {
		setupCall()
			.withServicePath(DECISION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(parameterValues()).containsExactly("maxGuests=120", "servingArea=inomhus", "servingArea=uteservering");
	}

	/**
	 * Keys the database orders differently from the service, one with a letter outside a-z and two that differ only in
	 * case, are sent again in another order and with a padded key. The decision is left as it stands, and answers with its
	 * parameters in the order of their keys.
	 */
	@Test
	void test28_theSameParametersInAnotherOrderLeaveTheDecisionAsItStands() {
		jdbcTemplate.update("update decision set modified_by = 'joe01doe' where id = ?", DECISION_ID);
		jdbcTemplate.update("delete from decision_parameter where decision_id = ?", DECISION_ID);
		jdbcTemplate.update("""
			insert into decision_parameter(id, decision_id, parameters_key)
			values ('f7000000-0000-0000-0000-000000000011', ?, 'zon'),
			       ('f7000000-0000-0000-0000-000000000012', ?, 'ärende'),
			       ('f7000000-0000-0000-0000-000000000013', ?, 'Zon')""", DECISION_ID, DECISION_ID, DECISION_ID);
		jdbcTemplate.update("""
			insert into decision_parameter_values(decision_parameter_id, value_order, value)
			values ('f7000000-0000-0000-0000-000000000011', 0, 'a'),
			       ('f7000000-0000-0000-0000-000000000012', 0, 'b'),
			       ('f7000000-0000-0000-0000-000000000013', 0, 'c')""");

		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(DECISION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(ETAG, List.of("0"))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jdbcTemplate.queryForList("select id from decision_parameter where decision_id = ? order by id", String.class, DECISION_ID))
			.as("the stored parameters were not written again")
			.containsExactly("f7000000-0000-0000-0000-000000000011", "f7000000-0000-0000-0000-000000000012", "f7000000-0000-0000-0000-000000000013");
	}

	/** The parameter values of the decision as key=value, in the order of the keys and then of the values. */
	private List<String> parameterValues() {
		return jdbcTemplate.queryForList("""
			select concat(p.parameters_key, '=', v.value) from decision_parameter p
			join decision_parameter_values v on v.decision_parameter_id = p.id
			where p.decision_id = ?
			order by p.parameters_key, v.value_order""", String.class, DECISION_ID);
	}

	/** Counts the terms of the decision, read with SQL. */
	private int terms() {
		return jdbcTemplate.queryForObject("select count(*) from decision_term where decision_id = ?", Integer.class, DECISION_ID);
	}

	private String textOf(final String termId) {
		return jdbcTemplate.queryForObject("select text from decision_term where id = ?", String.class, termId);
	}

	private int parametersWithKey(final String key) {
		return jdbcTemplate.queryForObject("select count(*) from decision_json_parameter where parameter_key = ?", Integer.class, key);
	}

	private int attachmentLinks() {
		return jdbcTemplate.queryForObject("select count(*) from decision_attachment where decision_id = ?", Integer.class, DECISION_ID);
	}

	private int attachments(final String attachmentId) {
		return jdbcTemplate.queryForObject("select count(*) from attachment where id = ?", Integer.class, attachmentId);
	}
}
