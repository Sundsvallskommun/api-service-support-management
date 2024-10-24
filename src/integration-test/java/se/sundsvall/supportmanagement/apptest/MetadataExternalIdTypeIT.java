package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
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
import se.sundsvall.supportmanagement.integration.db.ExternalIdTypeRepository;

/**
 * Status Metadata IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/MetadataExternalIdTypeIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class MetadataExternalIdTypeIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";

	private static final String RESPONSE_FILE = "response.json";

	private static final String NAMESPACE = "NAMESPACE.1";

	private static final String MUNICIPALITY_2281 = "2281";

	private static final String MUNICIPALITY_2309 = "2309";

	private static final String PATH = "/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/metadata/external-id-types";

	@Autowired
	private ExternalIdTypeRepository externalIdTypeRepository;

	@Test
	void test01_createExternalIdType() {
		assertThat(externalIdTypeRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, "A_BRAND_NEW_EXTERNAL_ID_TYPE")).isFalse();

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/metadata/external-id-types/A_BRAND_NEW_EXTERNAL_ID_TYPE"))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(externalIdTypeRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, "A_BRAND_NEW_EXTERNAL_ID_TYPE")).isTrue();
	}

	@Test
	void test02_getExternalIdType() {
		setupCall()
			.withServicePath(PATH + "/EXTERNAL-ID-TYPE-3")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_getExternalIdTypes() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_getExternalIdTypesWhenEmpty() {
		final var path = "/" + MUNICIPALITY_2309 + "/" + NAMESPACE + "/metadata/external-id-types";

		setupCall()
			.withServicePath(path)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_deleteExternalIdType() {
		final var externalIdTypeName = "EXTERNAL-ID-TYPE-3";

		assertThat(externalIdTypeRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, externalIdTypeName)).isTrue();
		assertThat(externalIdTypeRepository.count()).isEqualTo(6);

		setupCall()
			.withServicePath(PATH + "/" + externalIdTypeName)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(externalIdTypeRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, externalIdTypeName)).isFalse();
		assertThat(externalIdTypeRepository.count()).isEqualTo(5);
	}

}
