package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.SubscriptionProfileRepository;

/**
 * Subscription profile IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/SubscriptionProfilesIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class SubscriptionProfilesIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String MUNICIPALITY_2281 = "2281";
	private static final String PATH = "/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/subscription-profiles";

	// Subscription profile IDs from testdata-it.sql
	private static final String PROFILE_NOTICE_ID = "ccddeeff-0000-0000-0000-000000000001";
	private static final String PROFILE_MAIL_ID = "ccddeeff-0000-0000-0000-000000000002";
	private static final String UNKNOWN_PROFILE_ID = "ffffffff-ffff-ffff-ffff-ffffffffffff";

	@Autowired
	private SubscriptionProfileRepository subscriptionProfileRepository;

	@Test
	void test01_getSubscriptionProfiles() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_getSubscriptionProfile() {
		setupCall()
			.withServicePath(PATH + "/" + PROFILE_MAIL_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_getSubscriptionProfileNotFound() {
		setupCall()
			.withServicePath(PATH + "/" + UNKNOWN_PROFILE_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_createSubscriptionProfile() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("^" + PATH + "/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_createSubscriptionProfileConflict() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test06_updateSubscriptionProfile() {
		setupCall()
			.withServicePath(PATH + "/" + PROFILE_MAIL_ID)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test07_deleteSubscriptionProfile() {
		assertThat(subscriptionProfileRepository.findByIdAndNamespaceAndMunicipalityId(PROFILE_NOTICE_ID, NAMESPACE, MUNICIPALITY_2281)).isPresent();

		setupCall()
			.withServicePath(PATH + "/" + PROFILE_NOTICE_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(subscriptionProfileRepository.findByIdAndNamespaceAndMunicipalityId(PROFILE_NOTICE_ID, NAMESPACE, MUNICIPALITY_2281)).isNotPresent();
	}
}
