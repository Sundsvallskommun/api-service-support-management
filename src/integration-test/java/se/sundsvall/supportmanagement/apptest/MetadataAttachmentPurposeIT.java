package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
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
import se.sundsvall.supportmanagement.integration.db.AttachmentPurposeRepository;

/**
 * AttachmentPurpose Metadata IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/MetadataAttachmentPurposeIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class MetadataAttachmentPurposeIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String NAMESPACE = "NAMESPACE-ARTEFACT";
	private static final String MUNICIPALITY_2281 = "2281";
	private static final String MUNICIPALITY_2309 = "2309";
	private static final String PATH = "/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/metadata/attachmentpurposes";

	@Autowired
	private AttachmentPurposeRepository attachmentPurposeRepository;

	@Test
	void test01_createAttachmentPurpose() {
		assertThat(attachmentPurposeRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, "MINUTES")).isFalse();

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(PATH + "/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(attachmentPurposeRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, "MINUTES")).isTrue();
	}

	@Test
	void test02_getAttachmentPurpose() {
		setupCall()
			.withServicePath(PATH + "/f6000000-0000-0000-0000-000000000002")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_getAttachmentPurposes() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_getAttachmentPurposesWhenEmpty() {
		setupCall()
			.withServicePath("/" + MUNICIPALITY_2309 + "/" + NAMESPACE + "/metadata/attachmentpurposes")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_createExistingAttachmentPurposeIsRejected() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(attachmentPurposeRepository.count()).isEqualTo(5);
	}

	@Test
	void test06_deleteAttachmentPurpose() {
		final var attachmentPurposeId = "f6000000-0000-0000-0000-000000000003";

		assertThat(attachmentPurposeRepository.existsByIdAndNamespaceAndMunicipalityId(attachmentPurposeId, NAMESPACE, MUNICIPALITY_2281)).isTrue();
		assertThat(attachmentPurposeRepository.count()).isEqualTo(5);

		setupCall()
			.withServicePath(PATH + "/" + attachmentPurposeId)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(attachmentPurposeRepository.existsByIdAndNamespaceAndMunicipalityId(attachmentPurposeId, NAMESPACE, MUNICIPALITY_2281)).isFalse();
		assertThat(attachmentPurposeRepository.count()).isEqualTo(4);
	}

	@Test
	void test07_patchAttachmentPurpose() {
		setupCall()
			.withServicePath(PATH + "/f6000000-0000-0000-0000-000000000004")
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_getAttachmentPurposeOfAnotherMunicipalityGives404() {
		setupCall()
			.withServicePath("/" + MUNICIPALITY_2309 + "/" + NAMESPACE + "/metadata/attachmentpurposes/f6000000-0000-0000-0000-000000000002")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * A purpose an attachment still carries is not removed from under it, the same rule the labels of an errand follow.
	 */
	@Test
	void test09_deletingAPurposeInUseIsRejected() {
		final var attachmentPurposeId = "f6000000-0000-0000-0000-000000000001";

		setupCall()
			.withServicePath(PATH + "/" + attachmentPurposeId)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(attachmentPurposeRepository.existsByIdAndNamespaceAndMunicipalityId(attachmentPurposeId, NAMESPACE, MUNICIPALITY_2281)).isTrue();
	}
}
