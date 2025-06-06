package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.ACCEPTED;
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
import se.sundsvall.supportmanagement.integration.db.LabelRepository;

/**
 * Label Metadata IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/MetadataLabelIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class MetadataLabelIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String MUNICIPALITY_2281 = "2281";
	private static final String MUNICIPALITY_2282 = "2282";
	private static final String MUNICIPALITY_2309 = "2309";

	@Autowired
	private LabelRepository labelRepository;

	@Test
	void test01_createLabels() {
		final var path = "/" + MUNICIPALITY_2282 + "/" + NAMESPACE + "/metadata/labels";
		final var json = "[{\"classification\":\"TOP-LEVEL\",\"displayName\":\"Nivå 1\",\"name\":\"LABEL-1\",\"labels\":[{\"classification\":\"MIDDLE-LEVEL\",\"displayName\":\"Nivå 1.1\",\"name\":\"LEVEL-1-1\",\"labels\":[{\"classification\":\"LOWEST-LEVEL\",\"displayName\":\"Nivå 1.1.1\",\"name\":\"LEVEL-1-1-1\"},{\"classification\":\"LOWEST-LEVEL\",\"displayName\":\"Nivå 1.1.2\",\"name\":\"LEVEL-1-1-2\"}]}]}]";

		assertThat(labelRepository.existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_2282)).isFalse();

		setupCall()
			.withServicePath(path)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(labelRepository.existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_2282)).isTrue();
		assertThat(labelRepository.findOneByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_2282).getJsonStructure()).isEqualTo(json);
	}

	@Test
	void test02_getLabels() {
		setupCall()
			.withServicePath("/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/metadata/labels")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_getLabelsWhenEmpty() {
		final var path = "/" + MUNICIPALITY_2309 + "/" + NAMESPACE + "/metadata/labels";

		setupCall()
			.withServicePath(path)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_deleteLabels() {
		assertThat(labelRepository.existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_2281)).isTrue();

		setupCall()
			.withServicePath("/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/metadata/labels")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(labelRepository.existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_2281)).isFalse();
	}

	@Test
	void test05_updateLabels() {
		final var path = "/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/metadata/labels";
		final var json = "[{\"classification\":\"TOP-LEVEL\",\"displayName\":\"TOP 1\",\"name\":\"LABEL-1\",\"labels\":[{\"classification\":\"SUB-LEVEL\",\"displayName\":\"SUB 1.1\",\"name\":\"SUB-1-1\"}]}]";

		assertThat(labelRepository.existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_2281)).isTrue();
		assertThat(labelRepository.findOneByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_2281).getJsonStructure()).isNotEqualTo(json);

		setupCall()
			.withServicePath(path)
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(labelRepository.findOneByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_2281).getJsonStructure()).isEqualTo(json);
	}

}
