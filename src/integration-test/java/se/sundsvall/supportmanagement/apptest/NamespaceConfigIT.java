package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;

@WireMockAppTestSuite(files = "classpath:/NamespaceConfigIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class NamespaceConfigIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String NAMESPACE_1 = "NAMESPACE-1";
	private static final String NAMESPACE_2 = "NAMESPACE-2";
	private static final String MUNICIPALITY_ID = "2281";
	private static final Function<String, String> PATH = namespace -> "/" + MUNICIPALITY_ID + "/" + namespace + "/namespace-config";

	@Autowired
	private NamespaceConfigRepository repository;

	@Test
	void test01_getConfig() {
		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_1))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_createConfig() {

		assertThat(repository.existsByNamespaceAndMunicipalityId(NAMESPACE_2, MUNICIPALITY_ID)).isFalse();

		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_2))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(PATH.apply(NAMESPACE_2)))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(repository.existsByNamespaceAndMunicipalityId(NAMESPACE_2, MUNICIPALITY_ID)).isTrue();
	}

	@Test
	void test03_deleteConfig() {
		assertThat(repository.existsByNamespaceAndMunicipalityId(NAMESPACE_1, MUNICIPALITY_ID)).isTrue();

		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_1))
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(repository.existsByNamespaceAndMunicipalityId(NAMESPACE_1, MUNICIPALITY_ID)).isFalse();
	}

	@Test
	void test04_updateConfig() {

		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_1))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse("response_before.json")
			.sendRequest();

		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_1))
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		setupCall()
			.withServicePath(PATH.apply(NAMESPACE_1))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse("response_after.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_getAllConfigsFilteredByMunicipality() {
		setupCall()
			.withServicePath("/namespace-configs?municipalityId=" + MUNICIPALITY_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test06_getAllConfigs() {
		setupCall()
			.withServicePath("/namespace-configs")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
