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
import se.sundsvall.supportmanagement.integration.db.InvestigationRepository;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionOutcome;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.SectionAssessment;

/**
 * Errand Investigations IT tests, including the sections an investigation is assessed in, the attachments linked to it
 * and the JSON parameters it and its sections own.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandInvestigationsIT/", classes = Application.class)
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
				assertThat(investigation.getRecommendation()).isEqualTo(DecisionOutcome.APPROVAL);
				assertThat(investigation.getVersion()).isEqualTo(1L);
			});
	}

	/**
	 * The location names the section that was created, which takes the id of the section rather than of a copy of it.
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
		assertThat(parametersWithKey("sectionForm")).as("the parameter of the section went with it").isZero();
		assertThat(parametersWithKey("investigationForm")).as("the one of the investigation stayed").isOne();
	}

	/**
	 * The section key is unique per investigation, and saying so here turns a constraint violation deep in the flush
	 * into an answer the caller can act on.
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
	 * The investigation takes what it owns - its sections and the JSON parameters of both - and leaves the attachments
	 * linked to it on the errand.
	 */
	@Test
	void test11_deleteErrandInvestigation() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(investigationRepository.existsById(INVESTIGATION_ID)).isFalse();
		assertThat(parametersWithKey("investigationForm")).isZero();
		assertThat(parametersWithKey("sectionForm")).isZero();
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
			.withServicePath(INVESTIGATION_PATH + "/attachments?sortOrder=2")
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
			.withRequest("{\"sortOrder\":2}")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(ERRAND_PATH + "/attachments/" + UNLINKED_ATTACHMENT_ID))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(attachmentLinks()).isEqualTo(2);
	}

	@Test
	void test15_updateInvestigationAttachment() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/attachments/" + LINKED_ATTACHMENT_ID)
			.withHttpMethod(PATCH)
			.withRequest("{\"sortOrder\":5}")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jdbcTemplate.queryForObject("select sort_order from investigation_attachment where investigation_id = ? and attachment_id = ?", Integer.class, INVESTIGATION_ID, LINKED_ATTACHMENT_ID))
			.as("the new order reached the database").isEqualTo(5);
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

		assertThat(parametersWithKey("dispatchLog")).isOne();
	}

	@Test
	void test20_deleteInvestigationJsonParameter() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/json-parameters/investigationForm")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(parametersWithKey("investigationForm")).isZero();
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

		assertThat(parametersWithKey("dispatchLog")).isOne();
	}

	@Test
	void test24_deleteSectionJsonParameter() {
		setupCall()
			.withServicePath(SECTION_PATH + "/json-parameters/sectionForm")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(parametersWithKey("sectionForm")).isZero();
		assertThat(sections(SECTION_ID)).as("the section stayed").isOne();
	}

	/** Read with SQL rather than through JPA: the collections of a loaded entity are lazy, and the test has no session. */
	private int sections(final String sectionId) {
		return jdbcTemplate.queryForObject("select count(*) from investigation_section where id = ?", Integer.class, sectionId);
	}

	private int sectionsWithKey(final String sectionKey) {
		return jdbcTemplate.queryForObject("select count(*) from investigation_section where investigation_id = ? and section_key = ?", Integer.class, INVESTIGATION_ID, sectionKey);
	}

	private String assessmentOf(final String sectionId) {
		return jdbcTemplate.queryForObject("select assessment from investigation_section where id = ?", String.class, sectionId);
	}

	private int parametersWithKey(final String key) {
		return jdbcTemplate.queryForObject("select count(*) from json_parameter where errand_id = ? and parameter_key = ?", Integer.class, ERRAND_ID, key);
	}

	private int attachmentLinks() {
		return jdbcTemplate.queryForObject("select count(*) from investigation_attachment where investigation_id = ?", Integer.class, INVESTIGATION_ID);
	}

	private int attachments(final String attachmentId) {
		return jdbcTemplate.queryForObject("select count(*) from attachment where id = ?", Integer.class, attachmentId);
	}
}
