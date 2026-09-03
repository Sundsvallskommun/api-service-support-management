package se.sundsvall.supportmanagement.apptest;

import se.sundsvall.supportmanagement.service.util.ETagUtil;
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
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
import se.sundsvall.supportmanagement.integration.db.MeasureTypeRepository;
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
	private static final String OTHER_MEASURE_ID = "ee000000-0000-0000-0000-000000000101";
	private static final String MEASURE_ID = "ee000000-0000-0000-0000-000000000100";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String MUNICIPALITY_2281 = "2281";
	private static final String PATH = "/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/measures";
	private static final String ERRAND_PATH = "/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/errands/" + ERRAND_ID;
	private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	@Autowired
	private ErrandsRepository errandsRepository;

	@Autowired
	private MeasureTypeRepository measureTypeRepository;

	@Test
	void test01_createErrandMeasure() {
		final var parentEtag = currentErrandEtag();
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
		assertThat(currentErrandEtag()).isNotEqualTo(parentEtag);
		assertThat(errand.getMeasures())
			.filteredOn(m -> "MEASURE-3".equals(m.getType()))
			.singleElement()
			.satisfies(m -> {
				assertThat(m.getVersion()).isZero();
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
			.withHeader("If-Match", currentMeasureEtag(MEASURE_ID))
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_deleteErrandMeasure() {
		final var parentEtag = currentErrandEtag();
		final var errand = errandsRepository.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_2281).orElseThrow();
		assertThat(errand.getMeasures()).hasSize(2);

		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(DELETE)
			.withHeader("If-Match", currentMeasureEtag(MEASURE_ID))
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		final var updatedErrand = errandsRepository.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_2281).orElseThrow();
		assertThat(updatedErrand.getMeasures()).hasSize(1);
		assertThat(currentErrandEtag()).isNotEqualTo(parentEtag);
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
			.withHeader("If-Match", currentErrandEtag())
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

	private String currentErrandEtag() {
		return ETagUtil.format(errandsRepository.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_2281).orElseThrow().getVersion());
	}

	@Test
	void test07_clearDecisionAndDates() {
		final var id = "ee000000-0000-0000-0000-000000000101";
		final var response = restTemplate.exchange(PATH + "/" + id, PATCH,
			measureRequest("{\"executed\":null,\"accept\":null,\"plannedComplete\":null}", currentMeasureEtag(id)), String.class);
		assertThat(response.getStatusCode()).isEqualTo(OK);
		final var measure = storedMeasure(id);
		assertThat(measure.getExecuted()).isNull();
		assertThat(measure.getAccept()).isNull();
		assertThat(measure.getPlannedComplete()).isNull();
		assertThat(measure.getPlannedStart()).isNotNull();
		assertThat(measure.getGoal()).isEqualTo("Follow up on progress");
	}

	@Test
	void test08_rejectStaleMeasureVersions() {
		final var originalEtag = currentMeasureEtag(MEASURE_ID);
		final var parentEtag = currentErrandEtag();
		final var updated = restTemplate.exchange(PATH + "/" + MEASURE_ID, PATCH,
			measureRequest("{\"goal\":\"Fresh goal\"}", originalEtag), Measure.class);
		assertThat(updated.getStatusCode()).isEqualTo(OK);
		assertThat(updated.getBody().getVersion()).isEqualTo(1L);
		assertThat(updated.getHeaders().getETag()).isEqualTo(currentMeasureEtag(MEASURE_ID)).isNotEqualTo(originalEtag);
		assertThat(currentErrandEtag()).isNotEqualTo(parentEtag);
		final var parentAfterUpdate = currentErrandEtag();
		assertThat(restTemplate.exchange(PATH + "/" + MEASURE_ID, PATCH, measureRequest("{\"goal\":\"Stale goal\"}", originalEtag), String.class).getStatusCode())
			.isEqualTo(HttpStatus.PRECONDITION_FAILED);
		assertThat(restTemplate.exchange(PATH + "/" + MEASURE_ID, DELETE, measureRequest(null, originalEtag), String.class).getStatusCode())
			.isEqualTo(HttpStatus.PRECONDITION_FAILED);
		assertThat(storedMeasure(MEASURE_ID).getGoal()).isEqualTo("Fresh goal");
		assertThat(currentErrandEtag()).isEqualTo(parentAfterUpdate);
	}

	@Test
	void test09_deprecatedTypesRemainEditableButCannotBeSelected() {
		final var type = measureTypeRepository.findById("dd000000-0000-0000-0000-000000000100").orElseThrow();
		type.setDeprecated(true);
		measureTypeRepository.save(type);
		assertThat(restTemplate.exchange(PATH + "/" + MEASURE_ID, PATCH, measureRequest("{\"type\":\"MEASURE-1\",\"goal\":\"Updated\"}", null), String.class).getStatusCode()).isEqualTo(OK);
		assertThat(restTemplate.exchange(PATH, POST, measureRequest("{\"type\":\"MEASURE-1\",\"addedByUser\":\"joe01doe\",\"addedByRole\":\"ROLE-1\"}", null), String.class).getStatusCode())
			.isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(restTemplate.exchange(PATH + "/ee000000-0000-0000-0000-000000000101", PATCH, measureRequest("{\"type\":\"MEASURE-1\"}", null), String.class).getStatusCode())
			.isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(storedMeasure(MEASURE_ID).getGoal()).isEqualTo("Updated");
	}

	@Test
	void test10_errandPatchCannotRewriteTheCreator() {
		final var body = "{\"measures\":[{\"id\":\"" + MEASURE_ID + "\",\"type\":\"MEASURE-1\",\"addedByUser\":\"someone-else\",\"addedByRole\":\"ROLE-1\"}]}";
		final var response = restTemplate.exchange(PATH.substring(0, PATH.length() - "/measures".length()), PATCH, measureRequest(body, currentErrandEtag()), String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(storedMeasure(MEASURE_ID).getAddedByUser()).isEqualTo("joe01doe");
		assertThat(errandsRepository.findById(ERRAND_ID).orElseThrow().getMeasures()).hasSize(2);
	}

	@Test
	void test11_typeCannotBeCleared() {
		assertThat(restTemplate.exchange(PATH + "/" + MEASURE_ID, PATCH, measureRequest("{\"type\":null}", currentMeasureEtag(MEASURE_ID)), String.class).getStatusCode())
			.isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(storedMeasure(MEASURE_ID).getType()).isEqualTo("MEASURE-1");
	}

	private MeasureEntity storedMeasure(final String id) {
		return errandsRepository.findById(ERRAND_ID).orElseThrow().getMeasures().stream().filter(measure -> id.equals(measure.getId())).findFirst().orElseThrow();
	}

	private HttpEntity<String> measureRequest(final String body, final String etag) {
		final var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		if (etag != null) {
			headers.setIfMatch(etag);
		}
		return new HttpEntity<>(body, headers);
	}

	@Test
	void test12_errandPatchRejectsAStaleParentVersion() {
		final var parentEtag = currentErrandEtag();
		assertThat(restTemplate.exchange(PATH + "/" + MEASURE_ID, PATCH, measureRequest("{\"goal\":\"Changed\"}", null), String.class).getStatusCode()).isEqualTo(OK);
		final var response = restTemplate.exchange(ERRAND_PATH, PATCH, measureRequest("{\"measures\":[]}", parentEtag), String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
		assertThat(errandsRepository.findById(ERRAND_ID).orElseThrow().getMeasures()).hasSize(2);
	}

	@Test
	void test13_measureEtagSurvivesChangesToTheParentAndAnotherMeasure() {
		final var originalEtag = currentMeasureEtag(MEASURE_ID);
		assertThat(restTemplate.exchange(ERRAND_PATH, PATCH, measureRequest("{\"title\":\"Another title\"}", currentErrandEtag()), String.class).getStatusCode()).isEqualTo(OK);
		assertThat(restTemplate.exchange(PATH + "/" + OTHER_MEASURE_ID, PATCH, measureRequest("{\"goal\":\"Another goal\"}", currentMeasureEtag(OTHER_MEASURE_ID)), String.class).getStatusCode()).isEqualTo(OK);
		assertThat(currentMeasureEtag(MEASURE_ID)).isEqualTo(originalEtag);
		final var otherEtag = currentMeasureEtag(OTHER_MEASURE_ID);
		final var parentEtag = currentErrandEtag();
		final var response = restTemplate.exchange(PATH + "/" + MEASURE_ID, PATCH, measureRequest("{\"goal\":\"My goal\"}", originalEtag), Measure.class);
		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(response.getBody().getGoal()).isEqualTo("My goal");
		assertThat(response.getHeaders().getETag()).isNotEqualTo(originalEtag);
		assertThat(currentMeasureEtag(OTHER_MEASURE_ID)).isEqualTo(otherEtag);
		assertThat(currentErrandEtag()).isNotEqualTo(parentEtag);
	}

	@Test
	void test14_measurePreconditionsAreOptional() {
		assertThat(restTemplate.exchange(PATH + "/" + MEASURE_ID, PATCH, measureRequest("{\"goal\":\"Updated without a precondition\"}", null), String.class).getStatusCode()).isEqualTo(OK);
		assertThat(storedMeasure(MEASURE_ID).getGoal()).isEqualTo("Updated without a precondition");
		assertThat(restTemplate.exchange(PATH + "/" + MEASURE_ID, DELETE, measureRequest(null, null), String.class).getStatusCode()).isEqualTo(NO_CONTENT);
		assertThat(errandsRepository.findById(ERRAND_ID).orElseThrow().getMeasures()).extracting(MeasureEntity::getId).doesNotContain(MEASURE_ID);
	}

	@Test
	void test15_errandPatchUpdatesTheMeasureVersion() {
		final var originalEtag = currentMeasureEtag(MEASURE_ID);
		final var body = "{\"measures\":[{\"id\":\"" + MEASURE_ID + "\",\"type\":\"MEASURE-1\",\"addedByUser\":\"joe01doe\",\"addedByRole\":\"ROLE-1\",\"goal\":\"Changed through the errand\",\"version\":999}]}";
		assertThat(restTemplate.exchange(ERRAND_PATH, PATCH, measureRequest(body, currentErrandEtag()), String.class).getStatusCode()).isEqualTo(OK);
		assertThat(storedMeasure(MEASURE_ID).getVersion()).isEqualTo(1L);
		assertThat(currentMeasureEtag(MEASURE_ID)).isNotEqualTo(originalEtag);
		assertThat(restTemplate.exchange(PATH + "/" + MEASURE_ID, PATCH, measureRequest("{\"goal\":\"Stale goal\"}", originalEtag), String.class).getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
	}

	@Test
	void test16_weakTagsAreRejectedAndWildcardMatchesExistingMeasures() {
		final var etag = currentMeasureEtag(MEASURE_ID);
		assertThat(restTemplate.exchange(PATH + "/" + MEASURE_ID, PATCH, measureRequest("{\"goal\":\"Weak tag\"}", "W/" + etag), String.class).getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
		assertThat(restTemplate.exchange(PATH + "/" + MEASURE_ID, DELETE, measureRequest(null, "W/" + etag), String.class).getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
		assertThat(currentMeasureEtag(MEASURE_ID)).isEqualTo(etag);
		assertThat(restTemplate.exchange(PATH + "/" + MEASURE_ID, PATCH, measureRequest("{\"goal\":\"Wildcard update\"}", "*"), String.class).getStatusCode()).isEqualTo(OK);
		assertThat(restTemplate.exchange(PATH + "/" + MEASURE_ID, DELETE, measureRequest(null, "*"), String.class).getStatusCode()).isEqualTo(NO_CONTENT);
	}

	@Test
	void test17_errandPreconditionRemainsOptionalForAnEmptyMeasureList() {
		final var parentEtag = currentErrandEtag();
		assertThat(restTemplate.exchange(ERRAND_PATH, PATCH, measureRequest("{\"measures\":[]}", null), String.class).getStatusCode()).isEqualTo(OK);
		assertThat(errandsRepository.findById(ERRAND_ID).orElseThrow().getMeasures()).isEmpty();
		assertThat(currentErrandEtag()).isNotEqualTo(parentEtag);
	}

	@Test
	void test18_createdMeasureReturnsItsOwnEtag() {
		final var body = "{\"type\":\"MEASURE-3\",\"addedByUser\":\"new01user\",\"addedByRole\":\"ROLE-1\",\"version\":999}";
		final var created = restTemplate.exchange(PATH, POST, measureRequest(body, null), Void.class);
		assertThat(created.getStatusCode()).isEqualTo(CREATED);
		assertThat(created.getHeaders().getETag()).isEqualTo("\"0\"");
		assertThat(created.getHeaders().getLocation()).isNotNull();
		final var location = created.getHeaders().getLocation().getPath();
		final var read = restTemplate.getForEntity(location, Measure.class);
		assertThat(read.getStatusCode()).isEqualTo(OK);
		assertThat(read.getBody().getVersion()).isZero();
		assertThat(read.getHeaders().getETag()).isEqualTo(created.getHeaders().getETag());
		final var updated = restTemplate.exchange(location, PATCH, measureRequest("{\"goal\":\"Next edit\",\"version\":999}", created.getHeaders().getETag()), Measure.class);
		assertThat(updated.getStatusCode()).isEqualTo(OK);
		assertThat(updated.getBody().getVersion()).isEqualTo(1L);
		assertThat(updated.getHeaders().getETag()).isEqualTo("\"1\"");
		assertThat(restTemplate.exchange(location, DELETE, measureRequest(null, updated.getHeaders().getETag()), Void.class).getStatusCode()).isEqualTo(NO_CONTENT);
	}

	private String currentMeasureEtag(final String id) {
		final var response = restTemplate.getForEntity(PATH + "/" + id, Measure.class);
		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(response.getHeaders().getETag()).isEqualTo(ETagUtil.format(response.getBody().getVersion()));
		return response.getHeaders().getETag();
	}
}
