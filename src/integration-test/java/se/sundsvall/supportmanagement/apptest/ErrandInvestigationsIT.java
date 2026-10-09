package se.sundsvall.supportmanagement.apptest;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.InvestigationRepository;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.SectionAssessment;

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
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

/**
 * Errand Investigations IT tests, including the sections an investigation is assessed in, the attachments linked to it,
 * the JSON parameters it and its sections own and the parameters it carries.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandInvestigationsIT/", classes = Application.class, sharedContext = true)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandInvestigationsIT extends AbstractAppTest {

	private static final String NAMESPACE = "NAMESPACE-ARTEFACT";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "a0000000-0000-0000-0000-000000000001";
	private static final String INVESTIGATION_ID = "f2000000-0000-0000-0000-000000000001";
	private static final String DECISION_ID = "f4000000-0000-0000-0000-000000000001";
	private static final String SECTION_ID = "f3000000-0000-0000-0000-000000000002";
	private static final String LINKED_ATTACHMENT_ID = "a5000000-0000-0000-0000-000000000001";
	private static final String UNLINKED_ATTACHMENT_ID = "a5000000-0000-0000-0000-000000000002";

	private static final String ERRAND_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID;
	private static final String PATH = ERRAND_PATH + "/investigations";
	private static final String INVESTIGATION_PATH = PATH + "/" + INVESTIGATION_ID;
	private static final String SECTION_PATH = INVESTIGATION_PATH + "/sections/" + SECTION_ID;
	private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String AD_ACCOUNT = "joe01doe; type=adAccount";

	@Autowired
	private InvestigationRepository investigationRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void test01_createErrandInvestigation() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(PATH + "/" + UUID_PATTERN))
			.sendRequestAndVerifyResponse();

		assertThat(investigationRepository.findByNamespaceAndMunicipalityIdAndErrandEntityIdOrderByCreated(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID))
			.filteredOn(investigation -> "Utredning av lokalen".equals(investigation.getTitle()))
			.singleElement()
			.satisfies(investigation -> {
				assertThat(investigation.getStatus()).isEqualTo(ItemStatus.DRAFT);
				assertThat(investigation.getNamespace()).isEqualTo(NAMESPACE);
			});
		assertThat(jdbcTemplate.queryForList("""
			select concat_ws('|', p.parameters_key, p.display_name, p.parameter_group, v.value) from investigation_parameter p
			join investigation i on i.id = p.investigation_id
			join investigation_parameter_values v on v.investigation_parameter_id = p.id
			where i.title = 'Utredning av lokalen'
			order by p.parameters_key, v.value_order""", String.class))
			.as("the values sent for the same key are held under one parameter")
			.containsExactly("checkedSources|underlag|Skatteverket", "riskLevel|Risknivå|low", "riskLevel|Risknivå|medium");
	}

	@Test
	void test02_readErrandInvestigation() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_findErrandInvestigations() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_updateErrandInvestigation() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(investigationRepository.findById(INVESTIGATION_ID)).get()
			.satisfies(investigation -> {
				assertThat(investigation.getStatus()).isEqualTo(ItemStatus.COMPLETED);
				assertThat(investigation.getRecommendation()).isEqualTo("APPROVAL");
				assertThat(investigation.getVersion()).isEqualTo(1L);
			});
		assertThat(parameterValues()).as("the sent parameters replaced the stored ones").containsExactly("riskLevel=medium");
	}

	/**
	 * The location names the section that was created, by the id of the section.
	 */
	@Test
	void test05_createInvestigationSection() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/sections")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(INVESTIGATION_PATH + "/sections/" + UUID_PATTERN))
			.sendRequestAndVerifyResponse();

		assertThat(sectionsWithKey("personal")).isOne();
	}

	@Test
	void test06_findInvestigationSections() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/sections")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test07_readInvestigationSection() {
		setupCall()
			.withServicePath(SECTION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_updateInvestigationSection() {
		setupCall()
			.withServicePath(SECTION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(assessmentOf(SECTION_ID)).isEqualTo(SectionAssessment.DEFICIENCY.name());
		assertThat(investigationRepository.findById(INVESTIGATION_ID)).get()
			.extracting(InvestigationEntity::getVersion).as("the section is part of the investigation, so its version moved").isEqualTo(1L);
	}

	/**
	 * A section takes the JSON parameters it owns with it.
	 */
	@Test
	void test09_deleteInvestigationSection() {
		setupCall()
			.withServicePath(SECTION_PATH)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(sections(SECTION_ID)).isZero();
		assertThat(sectionParametersWithKey("sectionForm")).as("the parameter of the section went with it").isZero();
		assertThat(investigationParametersWithKey("investigationForm")).as("the one of the investigation stayed").isOne();
	}

	/**
	 * The section key is unique per investigation, and a section reusing one is answered with 409.
	 */
	@Test
	void test10_reusingASectionKeyIsAConflict() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/sections")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CONFLICT)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The investigation takes what it owns - its sections, the JSON parameters of both and its parameters - and leaves the
	 * attachments linked to it on the errand.
	 */
	@Test
	void test11_deleteErrandInvestigation() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(investigationRepository.existsById(INVESTIGATION_ID)).isFalse();
		assertThat(investigationParametersWithKey("investigationForm")).isZero();
		assertThat(sectionParametersWithKey("sectionForm")).isZero();
		assertThat(jdbcTemplate.queryForObject("select count(*) from investigation_parameter where investigation_id = ?", Integer.class, INVESTIGATION_ID))
			.as("its parameters went with it").isZero();
		assertThat(jdbcTemplate.queryForObject("select count(*) from investigation_parameter_values", Integer.class)).as("with their values").isZero();
		assertThat(attachmentLinks()).as("the link went").isZero();
		assertThat(attachments(LINKED_ATTACHMENT_ID)).as("the attachment stayed on the errand").isOne();
		assertThat(jdbcTemplate.queryForObject("select investigation_id from decision where id = ?", String.class, DECISION_ID))
			.as("the decision resting on it stayed, without the reference").isNull();
	}

	@Test
	void test12_readingUnknownSectionGives404() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/sections/f3000000-0000-0000-0000-0000000000ff")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test13_uploadInvestigationAttachment() throws Exception {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/attachments")
			.withHttpMethod(POST)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("attachment", "test.txt")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(ERRAND_PATH + "/attachments/" + UUID_PATTERN))
			.sendRequestAndVerifyResponse();

		assertThat(attachmentLinks()).as("the uploaded attachment is linked to the investigation").isEqualTo(2);
	}

	@Test
	void test14_linkInvestigationAttachment() {
		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(INVESTIGATION_PATH + "/attachments/" + UNLINKED_ATTACHMENT_ID)
			.withHttpMethod(POST)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(ERRAND_PATH + "/attachments/" + UNLINKED_ATTACHMENT_ID))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(attachmentLinks()).isEqualTo(2);
	}

	@Test
	void test16_unlinkInvestigationAttachment() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/attachments/" + LINKED_ATTACHMENT_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(attachmentLinks()).as("the link went").isZero();
		assertThat(attachments(LINKED_ATTACHMENT_ID)).as("the attachment stayed on the errand").isOne();
	}

	@Test
	void test17_readInvestigationJsonParameters() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/json-parameters")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test18_readInvestigationJsonParameter() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/json-parameters/investigationForm")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test19_createInvestigationJsonParameter() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/json-parameters/dispatchLog")
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(INVESTIGATION_PATH + "/json-parameters/dispatchLog"))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(investigationParametersWithKey("dispatchLog")).isOne();
	}

	@Test
	void test20_deleteInvestigationJsonParameter() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/json-parameters/investigationForm")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(investigationParametersWithKey("investigationForm")).isZero();
		assertThat(investigationRepository.existsById(INVESTIGATION_ID)).as("the investigation stayed").isTrue();
	}

	@Test
	void test21_readSectionJsonParameters() {
		setupCall()
			.withServicePath(SECTION_PATH + "/json-parameters")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test22_readSectionJsonParameter() {
		setupCall()
			.withServicePath(SECTION_PATH + "/json-parameters/sectionForm")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test23_createSectionJsonParameter() {
		setupCall()
			.withServicePath(SECTION_PATH + "/json-parameters/dispatchLog")
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(SECTION_PATH + "/json-parameters/dispatchLog"))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(sectionParametersWithKey("dispatchLog")).isOne();
	}

	@Test
	void test24_deleteSectionJsonParameter() {
		setupCall()
			.withServicePath(SECTION_PATH + "/json-parameters/sectionForm")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(sectionParametersWithKey("sectionForm")).isZero();
		assertThat(sections(SECTION_ID)).as("the section stayed").isOne();
	}

	/**
	 * The recommendation proposes a decision, and is held to the decision outcomes the namespace has registered.
	 */
	@Test
	void test25_aRecommendationTheNamespaceHasNotRegisteredIsRejected() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jdbcTemplate.queryForObject("select recommendation from investigation where id = ?", String.class, INVESTIGATION_ID))
			.as("the recommendation was not written").isNull();
	}

	/**
	 * A change to the parameters alone moves the version of the investigation once, and the ETag it answers with is the
	 * version it was stored with. The caller already stands as the last to modify it, so nothing but the parameters
	 * changes.
	 */
	@Test
	void test26_updatingOnlyTheParametersMovesTheVersion() {
		jdbcTemplate.update("update investigation set modified_by = 'joe01doe' where id = ?", INVESTIGATION_ID);

		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(PATCH)
			.withHeader(IF_MATCH, "\"0\"")
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(ETAG, List.of("1"))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jdbcTemplate.queryForObject("select version from investigation where id = ?", Long.class, INVESTIGATION_ID)).isOne();
		assertThat(parameterValues()).containsExactly("checkedSources=Skatteverket", "checkedSources=Polisen");
	}

	/**
	 * Parameters that come out the same as the stored ones, in another order and with the values of a key split, leave the
	 * investigation as it stands.
	 */
	@Test
	void test27_sameParametersLeaveTheInvestigationAsItStands() {
		jdbcTemplate.update("update investigation set modified_by = 'joe01doe' where id = ?", INVESTIGATION_ID);

		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(ETAG, List.of("0"))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jdbcTemplate.queryForList("select id from investigation_parameter where investigation_id = ? order by id", String.class, INVESTIGATION_ID))
			.as("the stored parameters were not written again")
			.containsExactly("fa000000-0000-0000-0000-000000000001", "fa000000-0000-0000-0000-000000000002");
	}

	@Test
	void test28_anEmptyListRemovesTheParameters() {
		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jdbcTemplate.queryForObject("select count(*) from investigation_parameter where investigation_id = ?", Integer.class, INVESTIGATION_ID)).isZero();
		assertThat(jdbcTemplate.queryForObject("select count(*) from investigation_parameter_values", Integer.class)).as("the values went with them").isZero();
	}

	@Test
	void test29_aParameterWithoutKeyIsRejected() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(parameterValues()).containsExactly("checkedSources=Skatteverket", "checkedSources=Kronofogden", "riskLevel=low");
	}

	/**
	 * Keys the database orders differently from the service, one with a letter outside a-z and two that differ only in
	 * case, are sent again in another order and with a padded key. The investigation is left as it stands, and answers with
	 * its parameters in the order of their keys.
	 */
	@Test
	void test30_theSameParametersInAnotherOrderLeaveTheInvestigationAsItStands() {
		jdbcTemplate.update("update investigation set modified_by = 'joe01doe' where id = ?", INVESTIGATION_ID);
		jdbcTemplate.update("delete from investigation_parameter where investigation_id = ?", INVESTIGATION_ID);
		jdbcTemplate.update("""
			insert into investigation_parameter(id, investigation_id, parameters_key)
			values ('fa000000-0000-0000-0000-000000000011', ?, 'zon'),
			       ('fa000000-0000-0000-0000-000000000012', ?, 'ärende'),
			       ('fa000000-0000-0000-0000-000000000013', ?, 'Zon')""", INVESTIGATION_ID, INVESTIGATION_ID, INVESTIGATION_ID);
		jdbcTemplate.update("""
			insert into investigation_parameter_values(investigation_parameter_id, value_order, value)
			values ('fa000000-0000-0000-0000-000000000011', 0, 'a'),
			       ('fa000000-0000-0000-0000-000000000012', 0, 'b'),
			       ('fa000000-0000-0000-0000-000000000013', 0, 'c')""");

		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(ETAG, List.of("0"))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jdbcTemplate.queryForList("select id from investigation_parameter where investigation_id = ? order by id", String.class, INVESTIGATION_ID))
			.as("the stored parameters were not written again")
			.containsExactly("fa000000-0000-0000-0000-000000000011", "fa000000-0000-0000-0000-000000000012", "fa000000-0000-0000-0000-000000000013");
	}

	/** The parameter values of the investigation as key=value, in the order of the keys and then of the values. */
	private List<String> parameterValues() {
		return jdbcTemplate.queryForList("""
			select concat(p.parameters_key, '=', v.value) from investigation_parameter p
			join investigation_parameter_values v on v.investigation_parameter_id = p.id
			where p.investigation_id = ?
			order by p.parameters_key, v.value_order""", String.class, INVESTIGATION_ID);
	}

	/** Counts the investigation sections with the given id, read with SQL. */
	private int sections(final String sectionId) {
		return jdbcTemplate.queryForObject("select count(*) from investigation_section where id = ?", Integer.class, sectionId);
	}

	private int sectionsWithKey(final String sectionKey) {
		return jdbcTemplate.queryForObject("select count(*) from investigation_section where investigation_id = ? and section_key = ?", Integer.class, INVESTIGATION_ID, sectionKey);
	}

	private String assessmentOf(final String sectionId) {
		return jdbcTemplate.queryForObject("select assessment from investigation_section where id = ?", String.class, sectionId);
	}

	private int investigationParametersWithKey(final String key) {
		return jdbcTemplate.queryForObject("select count(*) from investigation_json_parameter where parameter_key = ?", Integer.class, key);
	}

	private int sectionParametersWithKey(final String key) {
		return jdbcTemplate.queryForObject("select count(*) from investigation_section_json_parameter where parameter_key = ?", Integer.class, key);
	}

	private int attachmentLinks() {
		return jdbcTemplate.queryForObject("select count(*) from investigation_attachment where investigation_id = ?", Integer.class, INVESTIGATION_ID);
	}

	private int attachments(final String attachmentId) {
		return jdbcTemplate.queryForObject("select count(*) from attachment where id = ?", Integer.class, attachmentId);
	}
}
