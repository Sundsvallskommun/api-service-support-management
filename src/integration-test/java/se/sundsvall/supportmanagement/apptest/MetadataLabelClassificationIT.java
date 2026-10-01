package se.sundsvall.supportmanagement.apptest;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.LabelClassificationRepository;
import se.sundsvall.supportmanagement.integration.db.model.LabelClassificationEntity;

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

/**
 * Label classification metadata IT tests. Runs against NAMESPACE-2584, whose two labels share the classification CLASS.
 */
@WireMockAppTestSuite(files = "classpath:/MetadataLabelClassificationIT/", classes = Application.class, sharedContext = true)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class MetadataLabelClassificationIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String NAMESPACE = "NAMESPACE-2584";
	private static final String MUNICIPALITY_ID = "2584";
	private static final String PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/label-classifications";
	private static final String LABELS_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/labels";
	private static final String CLASSIFICATION = "CLASS";

	@Autowired
	private LabelClassificationRepository repository;

	@Test
	void test01_createLabelClassification() {
		assertThat(repository.existsByNamespaceAndMunicipalityIdAndClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION)).isFalse();

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(PATH + "/" + CLASSIFICATION))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(repository.findByNamespaceAndMunicipalityIdAndClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION))
			.hasValueSatisfying(entity -> assertThat(entity.getDisplayName()).isEqualTo("Klass"));
	}

	@Test
	void test02_getLabelClassification() {
		saveClassification(CLASSIFICATION, "Klass");

		setupCall()
			.withServicePath(PATH + "/" + CLASSIFICATION)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_getLabelClassifications() {
		saveClassification(CLASSIFICATION, "Klass");
		saveClassification("OTHER", "Annan");

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_updateLabelClassificationRenamesEveryLabel() {
		saveClassification(CLASSIFICATION, "Klass");

		setupCall()
			.withServicePath(PATH + "/" + CLASSIFICATION)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		// Every label of the classification carries the new display name
		setupCall()
			.withServicePath(LABELS_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse("labels-response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_deleteLabelClassification() {
		saveClassification(CLASSIFICATION, "Klass");

		setupCall()
			.withServicePath(PATH + "/" + CLASSIFICATION)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(repository.existsByNamespaceAndMunicipalityIdAndClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION)).isFalse();

		// The labels are left in place, without a classification display name
		setupCall()
			.withServicePath(LABELS_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse("labels-response.json")
			.sendRequestAndVerifyResponse();
	}

	private void saveClassification(final String classification, final String displayName) {
		repository.saveAndFlush(LabelClassificationEntity.create()
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withClassification(classification)
			.withDisplayName(displayName));
	}
}
