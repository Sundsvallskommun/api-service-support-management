package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.HandoverIdempotencyRepository;

@WireMockAppTestSuite(files = "classpath:/HandoverIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-handover.sql"
})
class HandoverIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String SOURCE_NAMESPACE = "NAMESPACE-1";
	private static final String SOURCE_ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
	private static final String SOURCE_ERRAND_ID_2 = "cc236cf1-c00f-4479-8341-ecf5dd90b5b9";
	private static final String TARGET_NAMESPACE = "NAMESPACE-3";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String SOURCE_PATH = "/" + MUNICIPALITY_ID + "/" + SOURCE_NAMESPACE + "/errands/" + SOURCE_ERRAND_ID;
	private static final String SOURCE_PATH_2 = "/" + MUNICIPALITY_ID + "/" + SOURCE_NAMESPACE + "/errands/" + SOURCE_ERRAND_ID_2;

	@Autowired
	private ErrandsRepository errandsRepository;

	@Autowired
	private HandoverIdempotencyRepository idempotencyRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void test01_executeHandover() {
		setupCall()
			.withServicePath(SOURCE_PATH + "/handover/execute")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/2281/NAMESPACE-3/errands/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		final var idempotency = idempotencyRepository
			.findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(SOURCE_ERRAND_ID, TARGET_NAMESPACE, MUNICIPALITY_ID)
			.orElseThrow();
		assertThat(errandsRepository.findById(idempotency.getNewErrandId())).isPresent();
	}

	@Test
	void test02_executeHandoverIdempotent() {
		final var location = setupCall()
			.withServicePath(SOURCE_PATH + "/handover/execute")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.sendRequest()
			.getResponseHeaders()
			.get(LOCATION)
			.stream().findFirst().orElseThrow();

		setupCall()
			.withServicePath(SOURCE_PATH + "/handover/execute")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(location))
			.sendRequestAndVerifyResponse();

		assertThat(idempotencyRepository.findAll()).hasSize(1);
	}

	@Test
	void test03_executeHandoverWithSourceClose() {
		setupCall()
			.withServicePath(SOURCE_PATH_2 + "/handover/execute")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		final var sourceErrand = errandsRepository.findById(SOURCE_ERRAND_ID_2).orElseThrow();
		assertThat(sourceErrand.getStatus()).isEqualTo("STATUS-2");
		assertThat(sourceErrand.getResolution()).isEqualTo("HANDED_OVER");
	}

	@Test
	void test04_executeHandoverMissingStatus() {
		setupCall()
			.withServicePath(SOURCE_PATH + "/handover/execute")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_PROBLEM_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_executeHandoverStatusNotInTargetNamespace() {
		setupCall()
			.withServicePath(SOURCE_PATH + "/handover/execute")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_PROBLEM_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test07_executeHandoverWithAttachmentsCopiesContent() throws Exception {
		// Use sendRequest() so WireMock stub history is preserved for the final sendRequestAndVerifyResponse()
		setupCall()
			.withServicePath(SOURCE_PATH + "/handover/execute")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/2281/NAMESPACE-3/errands/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
			.sendRequest();

		final var newErrandId = idempotencyRepository
			.findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(SOURCE_ERRAND_ID, TARGET_NAMESPACE, MUNICIPALITY_ID)
			.orElseThrow().getNewErrandId();

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM attachment WHERE errand_id = ?", Integer.class, newErrandId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT file_name FROM attachment WHERE errand_id = ?", String.class, newErrandId)).isEqualTo("Test_image.jpg");
		assertThat(jdbcTemplate.queryForObject("SELECT mime_type FROM attachment WHERE errand_id = ?", String.class, newErrandId)).isEqualTo("image/jpeg");

		final var attachmentId = jdbcTemplate.queryForObject("SELECT id FROM attachment WHERE errand_id = ?", String.class, newErrandId);

		// Verifies binary content and all WireMock stubs called during the handover POST
		setupCall()
			.withServicePath("/" + MUNICIPALITY_ID + "/" + TARGET_NAMESPACE + "/errands/" + newErrandId + "/attachments/" + attachmentId)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(IMAGE_JPEG_VALUE))
			.withExpectedBinaryResponse("Test_image.jpg")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_executeHandoverWithAttachmentsIdempotentDoesNotDuplicate() {
		// First call: use sendRequest() so stub history is available for the second call's verification
		setupCall()
			.withServicePath(SOURCE_PATH + "/handover/execute")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.sendRequest();

		// Second call: idempotent — no new external calls, but stubs from the first call cover verification
		setupCall()
			.withServicePath(SOURCE_PATH + "/handover/execute")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.sendRequestAndVerifyResponse();

		final var newErrandId = idempotencyRepository
			.findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(SOURCE_ERRAND_ID, TARGET_NAMESPACE, MUNICIPALITY_ID)
			.orElseThrow().getNewErrandId();

		assertThat(idempotencyRepository.findAll()).hasSize(1);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM attachment WHERE errand_id = ?", Integer.class, newErrandId)).isEqualTo(1);
	}

	@Test
	void test06_previewHandover() {
		setupCall()
			.withServicePath(SOURCE_PATH + "/handover/preview")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
