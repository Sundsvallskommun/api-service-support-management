package se.sundsvall.supportmanagement.apptest;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.util.LinkedMultiValueMap;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.integration.db.RevisionRepository;
import se.sundsvall.supportmanagement.integration.db.model.RevisionEntity;
import tools.jackson.core.type.TypeReference;

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
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

/**
 * ErrandAttachments IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandAttachmentsIT/", classes = Application.class, sharedContext = true)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandAttachmentsIT extends AbstractAppTest {

	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/";
	private static final String RESPONSE_FILE = "response.json";

	@Autowired
	private RevisionRepository revisionRepository;

	@Test
	void test01_readErrandAttachment() throws Exception {
		setupCall()
			.withServicePath(PATH + "ec677eb3-604c-4935-bff7-f8f0b500c8f4/attachments/25d266a7-1ff2-4bf4-b6f3-0473b2b86fcd")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(IMAGE_JPEG_VALUE))
			.withExpectedBinaryResponse("Test_image.jpg")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_readErrandAttachments() {
		setupCall()
			.withServicePath(PATH + "cc236cf1-c00f-4479-8341-ecf5dd90b5b9/attachments")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_createErrandAttachment() throws Exception {
		final var entityId = "1be673c0-6ba3-4fb0-af4a-43acf23389f6";

		assertThat(revisionRepository.findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(NAMESPACE, MUNICIPALITY_ID, entityId)).hasSize(1)
			.extracting(RevisionEntity::getVersion)
			.containsExactly(0);

		final var headers = setupCall()
			.withHeader(SENT_BY_HEADER, "cre03ate; type=adAccount")
			.withServicePath(PATH + entityId + "/attachments")
			.withHttpMethod(POST)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("errandAttachment", "test.txt")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(PATH + entityId + "/attachments/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
			.sendRequest()
			.getResponseHeaders();

		assertThat(revisionRepository.findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(NAMESPACE, MUNICIPALITY_ID, entityId)).hasSize(2)
			.extracting(RevisionEntity::getVersion)
			.containsExactlyInAnyOrder(0, 1);

		setupCall()
			.withServicePath(headers.get(LOCATION).stream().findFirst().get())
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(TEXT_PLAIN_VALUE))
			.withExpectedBinaryResponse("test.txt")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_deleteErrandAttachment() {
		final var entityId = "1be673c0-6ba3-4fb0-af4a-43acf23389f6";

		assertThat(revisionRepository.findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(NAMESPACE, MUNICIPALITY_ID, entityId)).hasSize(1)
			.extracting(RevisionEntity::getVersion)
			.containsExactly(0);

		setupCall()
			.withHeader(SENT_BY_HEADER, "del04ete; type=adAccount")
			.withServicePath(PATH + entityId + "/attachments/99fa4dd0-9308-4d45-bb8e-4bb881a9a536")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(revisionRepository.findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(NAMESPACE, MUNICIPALITY_ID, entityId)).hasSize(2)
			.extracting(RevisionEntity::getVersion)
			.containsExactlyInAnyOrder(0, 1);
	}

	/**
	 * The number of a removed attachment is not given again, even when it was the highest in the errand, and a given
	 * received date is kept rather than replaced by the time of upload.
	 */
	@Test
	void test05_sequenceNumberIsNotReusedAndReceivedIsKept() throws Exception {
		final var entityId = "1be673c0-6ba3-4fb0-af4a-43acf23389f6";
		final var received = OffsetDateTime.parse("2024-03-01T09:15:30Z");

		setupCall()
			.withHeader(SENT_BY_HEADER, "seq05num; type=adAccount")
			.withServicePath(PATH + entityId + "/attachments/99fa4dd0-9308-4d45-bb8e-4bb881a9a536")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequest();

		final var fields = new LinkedMultiValueMap<String, Object>();
		fields.add("channel", "EMAIL");
		fields.add("received", received.toString());

		setupCall()
			.withHeader(SENT_BY_HEADER, "seq05num; type=adAccount")
			.withServicePath(PATH + entityId + "/attachments")
			.withHttpMethod(POST)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequest(fields)
			.withRequestFile("errandAttachment", "test.txt")
			.withExpectedResponseStatus(CREATED)
			.sendRequest();

		final var attachments = setupCall()
			.withServicePath(PATH + entityId + "/attachments")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse()
			.andReturnBody(new TypeReference<List<ErrandAttachment>>() {});

		assertThat(attachments).singleElement().satisfies(attachment -> assertThat(attachment.getReceived()).isAtSameInstantAs(received));
	}
}
