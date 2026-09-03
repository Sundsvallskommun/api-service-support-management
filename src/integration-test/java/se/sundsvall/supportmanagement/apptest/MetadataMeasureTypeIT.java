package se.sundsvall.supportmanagement.apptest;

import se.sundsvall.supportmanagement.integration.db.model.MeasureTypeEntity;
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
import se.sundsvall.supportmanagement.integration.db.MeasureTypeRepository;

/**
 * MeasureType Metadata IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/MetadataMeasureTypeIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class MetadataMeasureTypeIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String MUNICIPALITY_2281 = "2281";
	private static final String MUNICIPALITY_2309 = "2309";
	private static final String PATH = "/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/metadata/measuretypes";

	@Autowired
	private MeasureTypeRepository measureTypeRepository;

	@Test
	void test01_createMeasureType() {
		assertThat(measureTypeRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, "A_NEW_MEASURE_TYPE")).isFalse();

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/metadata/measuretypes/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(measureTypeRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, "A_NEW_MEASURE_TYPE")).isTrue();
	}

	@Test
	void test02_getMeasureType() {
		final var measureTypeId = "dd000000-0000-0000-0000-000000000101";
		setupCall()
			.withServicePath(PATH + "/" + measureTypeId)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_getMeasureTypes() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_getMeasureTypesWhenEmpty() {
		final var path = "/" + MUNICIPALITY_2309 + "/" + NAMESPACE + "/metadata/measuretypes";

		setupCall()
			.withServicePath(path)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_getMeasureTypesByGroup() {
		setupCall()
			.withServicePath(PATH + "?measureGroup=GROUP-A")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test06_deleteMeasureType() {
		final var measureTypeId = "dd000000-0000-0000-0000-000000000102";

		assertThat(measureTypeRepository.existsByIdAndNamespaceAndMunicipalityId(measureTypeId, NAMESPACE, MUNICIPALITY_2281)).isTrue();
		assertThat(measureTypeRepository.count()).isEqualTo(3);

		setupCall()
			.withServicePath(PATH + "/" + measureTypeId)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(measureTypeRepository.existsByIdAndNamespaceAndMunicipalityId(measureTypeId, NAMESPACE, MUNICIPALITY_2281)).isFalse();
		assertThat(measureTypeRepository.count()).isEqualTo(2);
	}

	@Test
	void test07_patchMeasureType() {
		final var measureTypeId = "dd000000-0000-0000-0000-000000000102";
		setupCall()
			.withServicePath(PATH + "/" + measureTypeId)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_cannotDeleteReferencedType() {
		final var id = "dd000000-0000-0000-0000-000000000100";
		final var response = restTemplate.exchange(PATH + "/" + id, DELETE, null, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(measureTypeRepository.existsById(id)).isTrue();
	}

	@Test
	void test09_cannotRenameTheReferenceKey() {
		final var id = "dd000000-0000-0000-0000-000000000100";
		final var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		final var response = restTemplate.exchange(PATH + "/" + id, PATCH,
			new HttpEntity<>("{\"name\":\"NEW-NAME\",\"measureGroup\":\"GROUP-A\"}", headers), String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(measureTypeRepository.findById(id).orElseThrow().getName()).isEqualTo("MEASURE-1");
	}

	@Test
	void test10_referenceChecksAreScopedToTheNamespace() {
		final var type = measureTypeRepository.save(MeasureTypeEntity.create()
			.withNamespace("OTHER").withMunicipalityId(MUNICIPALITY_2281).withName("MEASURE-1").withMeasureGroup("GROUP-A"));
		final var response = restTemplate.exchange("/2281/OTHER/metadata/measuretypes/" + type.getId(), DELETE, null, String.class);
		assertThat(response.getStatusCode()).isEqualTo(NO_CONTENT);
		assertThat(measureTypeRepository.existsById(type.getId())).isFalse();
	}
}
