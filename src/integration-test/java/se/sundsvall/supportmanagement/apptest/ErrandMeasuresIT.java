package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
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
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.MeasureEntity;

/**
 * Errand Measures IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandMeasuresIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandMeasuresIT extends AbstractAppTest {

	private static final String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
	private static final String MEASURE_ID = "ee000000-0000-0000-0000-000000000100";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String MUNICIPALITY_2281 = "2281";
	private static final String PATH = "/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/measures";
	private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	// The measure on the errand the handling artefacts share, which is where the attachments and parameters of all four live.
	private static final String ARTEFACT_ERRAND_ID = "a0000000-0000-0000-0000-000000000001";
	private static final String ARTEFACT_ERRAND_PATH = "/" + MUNICIPALITY_2281 + "/NAMESPACE-ARTEFACT/errands/" + ARTEFACT_ERRAND_ID;
	private static final String ARTEFACT_MEASURE_ID = "ee000000-0000-0000-0000-000000000200";
	private static final String ARTEFACT_MEASURE_PATH = ARTEFACT_ERRAND_PATH + "/measures/" + ARTEFACT_MEASURE_ID;
	private static final String LINKED_ATTACHMENT_ID = "a5000000-0000-0000-0000-000000000001";
	private static final String UNLINKED_ATTACHMENT_ID = "a5000000-0000-0000-0000-000000000002";
	private static final String AD_ACCOUNT = "joe01doe; type=adAccount";

	@Autowired
	private ErrandsRepository errandsRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void test01_createErrandMeasure() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(PATH + "/" + UUID_PATTERN))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		final var errand = errandsRepository.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_2281).orElseThrow();
		assertThat(errand.getMeasures()).hasSize(3);
		assertThat(errand.getMeasures())
			.filteredOn(m -> "MEASURE-3".equals(m.getType()))
			.singleElement()
			.satisfies(m -> {
				assertThat(m.getResponsibleUser()).isEqualTo("new01user");
				assertThat(m.getGoal()).isEqualTo("New assessment goal");
			});
	}

	@Test
	void test02_readErrandMeasure() {
		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_findErrandMeasures() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_updateErrandMeasure() {
		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_deleteErrandMeasure() {
		final var errand = errandsRepository.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_2281).orElseThrow();
		assertThat(errand.getMeasures()).hasSize(2);

		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		final var updatedErrand = errandsRepository.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_2281).orElseThrow();
		assertThat(updatedErrand.getMeasures()).hasSize(1);
	}

	/**
	 * Patching the errand with the measures it was just served must leave them exactly where they were. They are
	 * addressable in their own right, so regenerating their ids would break every Location handed out by a create.
	 */
	@Test
	void test06_patchErrandKeepsMeasureIds() {
		final var measuresBefore = errandsRepository.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_2281).orElseThrow().getMeasures();
		assertThat(measuresBefore).hasSize(2);
		final var createdBefore = measuresBefore.stream().filter(measure -> MEASURE_ID.equals(measure.getId())).findFirst().orElseThrow().getCreated();

		setupCall()
			.withServicePath("/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/errands/" + ERRAND_ID)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequest();

		final var measuresAfter = errandsRepository.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_2281).orElseThrow().getMeasures();
		assertThat(measuresAfter)
			.extracting(MeasureEntity::getId)
			.containsExactlyInAnyOrder(MEASURE_ID, "ee000000-0000-0000-0000-000000000101");
		assertThat(measuresAfter.stream().filter(measure -> MEASURE_ID.equals(measure.getId())).findFirst().orElseThrow().getCreated()).isEqualTo(createdBefore);

		// The id previously handed out still resolves
		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test07_uploadMeasureAttachment() throws Exception {
		setupCall()
			.withServicePath(ARTEFACT_MEASURE_PATH + "/attachments?sortOrder=2")
			.withHttpMethod(POST)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("attachment", "test.txt")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(ARTEFACT_ERRAND_PATH + "/attachments/" + UUID_PATTERN))
			.sendRequestAndVerifyResponse();

		assertThat(attachmentLinks()).as("the uploaded attachment is linked to the measure").isEqualTo(2);
	}

	@Test
	void test08_linkMeasureAttachment() {
		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(ARTEFACT_MEASURE_PATH + "/attachments/" + UNLINKED_ATTACHMENT_ID)
			.withHttpMethod(POST)
			.withRequest("{\"sortOrder\":2}")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(ARTEFACT_ERRAND_PATH + "/attachments/" + UNLINKED_ATTACHMENT_ID))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(attachmentLinks()).isEqualTo(2);
	}

	@Test
	void test09_updateMeasureAttachment() {
		setupCall()
			.withServicePath(ARTEFACT_MEASURE_PATH + "/attachments/" + LINKED_ATTACHMENT_ID)
			.withHttpMethod(PATCH)
			.withRequest("{\"sortOrder\":5}")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jdbcTemplate.queryForObject("select sort_order from measure_attachment where measure_id = ? and attachment_id = ?", Integer.class, ARTEFACT_MEASURE_ID, LINKED_ATTACHMENT_ID))
			.as("the new order reached the database").isEqualTo(5);
	}

	@Test
	void test10_unlinkMeasureAttachment() {
		setupCall()
			.withServicePath(ARTEFACT_MEASURE_PATH + "/attachments/" + LINKED_ATTACHMENT_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(attachmentLinks()).as("the link went").isZero();
		assertThat(attachments(LINKED_ATTACHMENT_ID)).as("the attachment stayed on the errand").isOne();
	}

	@Test
	void test11_readMeasureJsonParameters() {
		setupCall()
			.withServicePath(ARTEFACT_MEASURE_PATH + "/json-parameters")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test12_readMeasureJsonParameter() {
		setupCall()
			.withServicePath(ARTEFACT_MEASURE_PATH + "/json-parameters/measureForm")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test13_createMeasureJsonParameter() {
		setupCall()
			.withServicePath(ARTEFACT_MEASURE_PATH + "/json-parameters/dispatchLog")
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(ARTEFACT_MEASURE_PATH + "/json-parameters/dispatchLog"))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(parametersWithKey("dispatchLog")).isOne();
	}

	@Test
	void test14_deleteMeasureJsonParameter() {
		setupCall()
			.withServicePath(ARTEFACT_MEASURE_PATH + "/json-parameters/measureForm")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(parametersWithKey("measureForm")).isZero();
		assertThat(measures(ARTEFACT_MEASURE_ID)).as("the measure stayed").isOne();
	}

	/**
	 * The measure takes the JSON parameters it owns, and leaves the attachments linked to it on the errand.
	 */
	@Test
	void test15_deleteMeasureTakesItsParameterButNotItsAttachments() {
		setupCall()
			.withServicePath(ARTEFACT_MEASURE_PATH)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(measures(ARTEFACT_MEASURE_ID)).isZero();
		assertThat(parametersWithKey("measureForm")).as("the parameter went with the measure").isZero();
		assertThat(attachmentLinks()).as("the link went").isZero();
		assertThat(attachments(LINKED_ATTACHMENT_ID)).as("the attachment stayed on the errand").isOne();
	}

	/**
	 * Dropping a measure by patching the errand without it does what deleting it through the measure resource does:
	 * its parameters go too, rather than being left behind to block the next measure asking for the same key.
	 */
	@Test
	void test16_patchingErrandWithoutTheMeasureTakesItsParameter() {
		setupCall()
			.withServicePath(ARTEFACT_ERRAND_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		assertThat(measures(ARTEFACT_MEASURE_ID)).isZero();
		assertThat(parametersWithKey("measureForm")).as("the parameter went with the measure").isZero();
		assertThat(parametersWithKey("formData")).as("the errand kept its own").isOne();
	}

	private int attachmentLinks() {
		return jdbcTemplate.queryForObject("select count(*) from measure_attachment where measure_id = ?", Integer.class, ARTEFACT_MEASURE_ID);
	}

	private int attachments(final String attachmentId) {
		return jdbcTemplate.queryForObject("select count(*) from attachment where id = ?", Integer.class, attachmentId);
	}

	private int measures(final String measureId) {
		return jdbcTemplate.queryForObject("select count(*) from measure where id = ?", Integer.class, measureId);
	}

	private int parametersWithKey(final String key) {
		return jdbcTemplate.queryForObject("select count(*) from json_parameter where errand_id = ? and parameter_key = ?", Integer.class, ARTEFACT_ERRAND_ID, key);
	}
}
