package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.IF_MATCH;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MeasureTypeRepository;
import se.sundsvall.supportmanagement.integration.db.model.MeasureEntity;
import se.sundsvall.supportmanagement.service.util.ETagUtil;

/**
 * Errand Measures IT tests.
 * <p>
 * The test data is reloaded before every test, so each one starts from an errand holding the two measures MEASURE_ID
 * and OTHER_MEASURE_ID, both at version 0.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandMeasuresIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandMeasuresIT extends AbstractAppTest {

	private static final String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
	private static final String MEASURE_ID = "ee000000-0000-0000-0000-000000000100";
	private static final String OTHER_MEASURE_ID = "ee000000-0000-0000-0000-000000000101";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String MUNICIPALITY_2281 = "2281";
	private static final String ERRAND_PATH = "/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/errands/" + ERRAND_ID;
	private static final String PATH = ERRAND_PATH + "/measures";
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
	void test04_updateErrandMeasure() throws Exception {
		final var etag = currentMeasureEtag(MEASURE_ID);

		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(PATCH)
			.withHeader(IF_MATCH, etag)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_deleteErrandMeasure() throws Exception {
		final var parentEtag = currentErrandEtag();
		final var etag = currentMeasureEtag(MEASURE_ID);
		assertThat(storedMeasures()).hasSize(2);

		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(DELETE)
			.withHeader(IF_MATCH, etag)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(storedMeasures()).hasSize(1);
		assertThat(currentErrandEtag()).isNotEqualTo(parentEtag);
	}

	/**
	 * Patching the errand with the measures it was just served must leave them exactly where they were. They are
	 * addressable in their own right, so regenerating their ids would break every Location handed out by a create.
	 */
	@Test
	void test06_patchErrandKeepsMeasureIds() {
		final var measuresBefore = storedMeasures();
		assertThat(measuresBefore).hasSize(2);
		final var createdBefore = measuresBefore.stream().filter(measure -> MEASURE_ID.equals(measure.getId())).findFirst().orElseThrow().getCreated();
		final var parentEtag = currentErrandEtag();

		setupCall()
			.withServicePath(ERRAND_PATH)
			.withHttpMethod(PATCH)
			.withHeader(IF_MATCH, parentEtag)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequest();

		final var measuresAfter = storedMeasures();
		assertThat(measuresAfter)
			.extracting(MeasureEntity::getId)
			.containsExactlyInAnyOrder(MEASURE_ID, OTHER_MEASURE_ID);
		assertThat(storedMeasure(MEASURE_ID).getCreated()).isEqualTo(createdBefore);

		// The id previously handed out still resolves
		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * An explicit null clears a nullable field, while a field the patch leaves out keeps its value.
	 */
	@Test
	void test07_clearDecisionAndDates() throws Exception {
		final var etag = currentMeasureEtag(OTHER_MEASURE_ID);

		setupCall()
			.withServicePath(PATH + "/" + OTHER_MEASURE_ID)
			.withHttpMethod(PATCH)
			.withHeader(IF_MATCH, etag)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequest();

		final var measure = storedMeasure(OTHER_MEASURE_ID);
		assertThat(measure.getExecuted()).isNull();
		assertThat(measure.getAccept()).isNull();
		assertThat(measure.getPlannedComplete()).isNull();
		assertThat(measure.getPlannedStart()).isNotNull();
		assertThat(measure.getGoal()).isEqualTo("Follow up on progress");
	}

	@Test
	void test08_rejectStaleMeasureVersions() throws Exception {
		final var originalEtag = currentMeasureEtag(MEASURE_ID);
		final var parentEtagBefore = currentErrandEtag();

		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(PATCH)
			.withHeader(IF_MATCH, originalEtag)
			.withRequest("request-fresh.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		assertThat(getResponseHeaders().getETag()).isEqualTo(ETagUtil.format(1L));
		assertThat(getResponseBody(Measure.class).getVersion()).isEqualTo(1L);
		assertThat(currentMeasureEtag(MEASURE_ID)).isNotEqualTo(originalEtag);
		assertThat(currentErrandEtag()).isNotEqualTo(parentEtagBefore);
		final var parentEtagAfter = currentErrandEtag();

		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(PATCH)
			.withHeader(IF_MATCH, originalEtag)
			.withRequest("request-stale.json")
			.withExpectedResponseStatus(PRECONDITION_FAILED)
			.sendRequest();

		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(DELETE)
			.withHeader(IF_MATCH, originalEtag)
			.withExpectedResponseStatus(PRECONDITION_FAILED)
			.sendRequest();

		assertThat(storedMeasure(MEASURE_ID).getGoal()).isEqualTo("Fresh goal");
		assertThat(currentErrandEtag()).isEqualTo(parentEtagAfter);
	}

	@Test
	void test09_deprecatedTypesRemainEditableButCannotBeSelected() {
		final var type = measureTypeRepository.findById("dd000000-0000-0000-0000-000000000100").orElseThrow();
		measureTypeRepository.save(type.withDeprecated(true));

		// A measure that already has the type keeps it and stays editable
		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(PATCH)
			.withRequest("request-keep-type.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		// A new measure cannot select it
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest("request-new-measure.json")
			.withExpectedResponseStatus(BAD_REQUEST)
			.sendRequest();

		// Nor can another measure switch to it
		setupCall()
			.withServicePath(PATH + "/" + OTHER_MEASURE_ID)
			.withHttpMethod(PATCH)
			.withRequest("request-switch-type.json")
			.withExpectedResponseStatus(BAD_REQUEST)
			.sendRequest();

		assertThat(storedMeasure(MEASURE_ID).getGoal()).isEqualTo("Updated");
	}

	@Test
	void test10_errandPatchCannotRewriteTheCreator() {
		final var parentEtag = currentErrandEtag();

		setupCall()
			.withServicePath(ERRAND_PATH)
			.withHttpMethod(PATCH)
			.withHeader(IF_MATCH, parentEtag)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.sendRequest();

		assertThat(storedMeasure(MEASURE_ID).getAddedByUser()).isEqualTo("joe01doe");
		assertThat(storedMeasures()).hasSize(2);
	}

	@Test
	void test11_typeCannotBeCleared() throws Exception {
		final var etag = currentMeasureEtag(MEASURE_ID);

		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(PATCH)
			.withHeader(IF_MATCH, etag)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.sendRequest();

		assertThat(storedMeasure(MEASURE_ID).getType()).isEqualTo("MEASURE-1");
	}

	@Test
	void test12_errandPatchRejectsAStaleParentVersion() {
		final var staleParentEtag = currentErrandEtag();

		// Changing a measure moves the parent version on
		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(PATCH)
			.withRequest("request-measure.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		setupCall()
			.withServicePath(ERRAND_PATH)
			.withHttpMethod(PATCH)
			.withHeader(IF_MATCH, staleParentEtag)
			.withRequest("request-errand.json")
			.withExpectedResponseStatus(PRECONDITION_FAILED)
			.sendRequest();

		assertThat(storedMeasures()).hasSize(2);
	}

	@Test
	void test13_measureEtagSurvivesChangesToTheParentAndAnotherMeasure() throws Exception {
		final var originalEtag = currentMeasureEtag(MEASURE_ID);
		final var parentEtagBefore = currentErrandEtag();

		setupCall()
			.withServicePath(ERRAND_PATH)
			.withHttpMethod(PATCH)
			.withHeader(IF_MATCH, parentEtagBefore)
			.withRequest("request-errand.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		final var otherEtagBefore = currentMeasureEtag(OTHER_MEASURE_ID);

		setupCall()
			.withServicePath(PATH + "/" + OTHER_MEASURE_ID)
			.withHttpMethod(PATCH)
			.withHeader(IF_MATCH, otherEtagBefore)
			.withRequest("request-other-measure.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		assertThat(currentMeasureEtag(MEASURE_ID)).isEqualTo(originalEtag);
		final var otherEtag = currentMeasureEtag(OTHER_MEASURE_ID);
		final var parentEtag = currentErrandEtag();

		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(PATCH)
			.withHeader(IF_MATCH, originalEtag)
			.withRequest("request-measure.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		assertThat(getResponseHeaders().getETag()).isEqualTo(ETagUtil.format(1L));
		assertThat(getResponseBody(Measure.class).getGoal()).isEqualTo("My goal");
		assertThat(currentMeasureEtag(OTHER_MEASURE_ID)).isEqualTo(otherEtag);
		assertThat(currentErrandEtag()).isNotEqualTo(parentEtag);
	}

	@Test
	void test14_measurePreconditionsAreOptional() {
		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequest();

		assertThat(storedMeasure(MEASURE_ID).getGoal()).isEqualTo("Updated without a precondition");

		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(storedMeasures()).extracting(MeasureEntity::getId).doesNotContain(MEASURE_ID);
	}

	/**
	 * A measure changed through the errand gets a new version like any other change. A version sent in the request is
	 * ignored rather than written.
	 */
	@Test
	void test15_errandPatchUpdatesTheMeasureVersion() throws Exception {
		final var originalEtag = currentMeasureEtag(MEASURE_ID);
		final var parentEtag = currentErrandEtag();

		setupCall()
			.withServicePath(ERRAND_PATH)
			.withHttpMethod(PATCH)
			.withHeader(IF_MATCH, parentEtag)
			.withRequest("request-errand.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		assertThat(storedMeasure(MEASURE_ID).getVersion()).isEqualTo(1L);
		assertThat(currentMeasureEtag(MEASURE_ID)).isNotEqualTo(originalEtag);

		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(PATCH)
			.withHeader(IF_MATCH, originalEtag)
			.withRequest("request-stale-measure.json")
			.withExpectedResponseStatus(PRECONDITION_FAILED)
			.sendRequest();
	}

	@Test
	void test16_weakTagsAreRejectedAndWildcardMatchesExistingMeasures() throws Exception {
		final var etag = currentMeasureEtag(MEASURE_ID);

		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(PATCH)
			.withHeader(IF_MATCH, "W/" + etag)
			.withRequest("request-weak.json")
			.withExpectedResponseStatus(PRECONDITION_FAILED)
			.sendRequest();

		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(DELETE)
			.withHeader(IF_MATCH, "W/" + etag)
			.withExpectedResponseStatus(PRECONDITION_FAILED)
			.sendRequest();

		assertThat(currentMeasureEtag(MEASURE_ID)).isEqualTo(etag);

		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(PATCH)
			.withHeader(IF_MATCH, "*")
			.withRequest("request-wildcard.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		setupCall()
			.withServicePath(PATH + "/" + MEASURE_ID)
			.withHttpMethod(DELETE)
			.withHeader(IF_MATCH, "*")
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test17_errandPreconditionRemainsOptionalForAnEmptyMeasureList() {
		final var parentEtag = currentErrandEtag();

		setupCall()
			.withServicePath(ERRAND_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequest();

		assertThat(storedMeasures()).isEmpty();
		assertThat(currentErrandEtag()).isNotEqualTo(parentEtag);
	}

	/**
	 * A create hands out the ETag of the new measure alongside its Location, so a client can go straight on to updating
	 * it. A version sent in a request is ignored.
	 */
	@Test
	void test18_createdMeasureReturnsItsOwnEtag() throws Exception {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(PATH + "/" + UUID_PATTERN))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(getResponseHeaders().getETag()).isEqualTo(ETagUtil.format(0L));
		final var location = getResponseHeaders().getLocation().getPath();

		setupCall()
			.withServicePath(location)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest();

		assertThat(getResponseHeaders().getETag()).isEqualTo(ETagUtil.format(0L));
		assertThat(getResponseBody(Measure.class).getVersion()).isZero();

		setupCall()
			.withServicePath(location)
			.withHttpMethod(PATCH)
			.withHeader(IF_MATCH, ETagUtil.format(0L))
			.withRequest("request-update.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		assertThat(getResponseHeaders().getETag()).isEqualTo(ETagUtil.format(1L));
		assertThat(getResponseBody(Measure.class).getVersion()).isEqualTo(1L);

		setupCall()
			.withServicePath(location)
			.withHttpMethod(DELETE)
			.withHeader(IF_MATCH, ETagUtil.format(1L))
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	private String currentErrandEtag() {
		return ETagUtil.format(errandsRepository.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_2281).orElseThrow().getVersion());
	}

	/**
	 * Reads the measure the way a client would and returns its ETag, checking on the way that the header agrees with the
	 * version in the body. It issues a call of its own, so read it into a variable before setupCall() rather than inside
	 * a chain, which it would otherwise reset.
	 */
	private String currentMeasureEtag(final String id) throws Exception {
		setupCall()
			.withServicePath(PATH + "/" + id)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest();

		final var etag = getResponseHeaders().getETag();
		assertThat(etag).isEqualTo(ETagUtil.format(getResponseBody(Measure.class).getVersion()));
		return etag;
	}

	private List<MeasureEntity> storedMeasures() {
		return errandsRepository.findById(ERRAND_ID).orElseThrow().getMeasures();
	}

	private MeasureEntity storedMeasure(final String id) {
		return storedMeasures().stream()
			.filter(measure -> id.equals(measure.getId()))
			.findFirst()
			.orElseThrow();
	}
}
