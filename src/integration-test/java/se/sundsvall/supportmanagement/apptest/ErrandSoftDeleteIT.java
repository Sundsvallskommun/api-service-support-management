package se.sundsvall.supportmanagement.apptest;

import java.util.List;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

/**
 * Soft delete and restore of errands IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandSoftDeleteIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandSoftDeleteIT extends AbstractAppTest {

	private static final String ERRAND_ID = "1be673c0-6ba3-4fb0-af4a-43acf23389f6";
	private static final String PATH = "/2281/NAMESPACE-1/errands";
	private static final String ERRAND_PATH = PATH + "/" + ERRAND_ID;
	// Held at read by fro01lin, who therefore may neither soft delete nor restore it
	private static final String ERRAND_HELD_AT_READ_PATH = "/2506/NAMESPACE-2507/errands/9b2a7c14-3d5e-4f60-8a91-2c3d4e5f6a7b";
	// Runs a live process in PROCESS-NAMESPACE, and may therefore not be soft deleted
	private static final String ERRAND_WITH_LIVE_PROCESS = "aa000000-0000-0000-0000-0000000000a1";
	private static final String RESPONSE_FILE = "response.json";
	private static final String USER = "smo02key; type=adAccount";

	@Autowired
	private ErrandsRepository errandsRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void test01_softDeleteAndRestoreErrand() {
		setupCall()
			.withHeader(SENT_BY_HEADER, USER)
			.withServicePath(ERRAND_PATH + "/soft-delete")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequest();

		assertThat(errandsRepository.findById(ERRAND_ID)).hasValueSatisfying(errand -> {
			assertThat(errand.getDeleted()).isNotNull();
			assertThat(errand.getDeletedBy()).isEqualTo("smo02key");
		});

		// Gone from every read, and from what hangs off it
		setupCall()
			.withServicePath(ERRAND_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequest();

		setupCall()
			.withServicePath(ERRAND_PATH + "/revisions")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequest();

		// Found by a listing whose filter names the deletion
		setupCall()
			.withServicePath(PATH + "?filter=deleted is not null")
			.withHttpMethod(GET)
			.withJsonAssertOptions(List.of(Option.IGNORING_EXTRA_FIELDS))
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-deleted.json")
			.sendRequest();

		setupCall()
			.withHeader(SENT_BY_HEADER, USER)
			.withServicePath(ERRAND_PATH + "/restore")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequest();

		setupCall()
			.withServicePath(ERRAND_PATH)
			.withHttpMethod(GET)
			.withJsonAssertOptions(List.of(Option.IGNORING_EXTRA_FIELDS))
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(errandsRepository.findById(ERRAND_ID)).hasValueSatisfying(errand -> {
			assertThat(errand.getDeleted()).isNull();
			assertThat(errand.getDeletedBy()).isNull();
		});
	}

	@Test
	void test02_deleteSoftDeletedErrandForGood() {
		jdbcTemplate.update("update errand set deleted = now(6), deleted_by = 'smo02key' where id = ?", ERRAND_ID);

		setupCall()
			.withHeader(SENT_BY_HEADER, USER)
			.withServicePath(ERRAND_PATH)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(errandsRepository.existsById(ERRAND_ID)).isFalse();
	}

	@Test
	void test03_restoreErrandThatIsNotDeleted() {
		setupCall()
			.withHeader(SENT_BY_HEADER, USER)
			.withServicePath(ERRAND_PATH + "/restore")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_softDeleteErrandHeldAtReadIsNotAllowed() {
		setupCall()
			.withHeader(SENT_BY_HEADER, "fro01lin; type=adAccount")
			.withServicePath(ERRAND_HELD_AT_READ_PATH + "/soft-delete")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(FORBIDDEN)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test06_softDeleteErrandWithALiveProcessIsRefused() {
		setupCall()
			.withHeader(SENT_BY_HEADER, USER)
			.withServicePath("/2281/PROCESS-NAMESPACE/errands/" + ERRAND_WITH_LIVE_PROCESS + "/soft-delete")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(errandsRepository.findById(ERRAND_WITH_LIVE_PROCESS)).hasValueSatisfying(errand -> assertThat(errand.getDeleted()).isNull());
	}

	@Test
	void test05_restoreErrandHeldAtReadIsNotAllowed() {
		jdbcTemplate.update("update errand set deleted = now(6), deleted_by = 'smo02key' where id = ?", "9b2a7c14-3d5e-4f60-8a91-2c3d4e5f6a7b");

		setupCall()
			.withHeader(SENT_BY_HEADER, "fro01lin; type=adAccount")
			.withServicePath(ERRAND_HELD_AT_READ_PATH + "/restore")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(FORBIDDEN)
			.sendRequestAndVerifyResponse();
	}
}
