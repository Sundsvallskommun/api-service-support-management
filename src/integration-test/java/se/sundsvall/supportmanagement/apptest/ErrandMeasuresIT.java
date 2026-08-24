package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
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

	@Autowired
	private ErrandsRepository errandsRepository;

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
}
