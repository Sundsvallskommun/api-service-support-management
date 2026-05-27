package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.dept44.support.Identifier.HEADER_NAME;

import java.util.List;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.SubscriptionRepository;

/**
 * Subscription IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/SubscriptionsIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class SubscriptionsIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String MUNICIPALITY_2281 = "2281";

	// IDs from testdata-it.sql
	private static final String SUBSCRIBER_SERVICEDESK_ID = "aabbccdd-0000-0000-0000-000000000001";
	private static final String SUBSCRIBER_TICKETS_ID = "aabbccdd-0000-0000-0000-000000000002";
	private static final String UNKNOWN_SUBSCRIBER_ID = "ffffffff-ffff-ffff-ffff-ffffffffffff";
	private static final String SUBSCRIPTION_ERRAND_ID = "bbccddee-0000-0000-0000-000000000001";
	private static final String SUBSCRIPTION_NAMESPACE_ID = "bbccddee-0000-0000-0000-000000000002";
	private static final String UNKNOWN_SUBSCRIPTION_ID = "ffffffff-ffff-ffff-ffff-ffffffffffff";
	private static final String PATH = "/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/subscribers/" + SUBSCRIBER_SERVICEDESK_ID + "/subscriptions";
	private static final String TICKETS_PATH = "/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/subscribers/" + SUBSCRIBER_TICKETS_ID + "/subscriptions";

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Test
	void test01_getSubscriptions() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_getSubscriptionsForUnknownSubscriber() {
		setupCall()
			.withServicePath("/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/subscribers/" + UNKNOWN_SUBSCRIBER_ID + "/subscriptions")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_createErrandSubscription() {
		setupCall()
			.withServicePath(TICKETS_PATH)
			.withHttpMethod(POST)
			.withHeader(HEADER_NAME, "type=adAccount; jane11dane")
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("^" + TICKETS_PATH + "/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_createNamespaceSubscription() {
		setupCall()
			.withServicePath(TICKETS_PATH)
			.withHttpMethod(POST)
			.withHeader(HEADER_NAME, "type=adAccount; jane11dane")
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("^" + TICKETS_PATH + "/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_createDuplicateErrandSubscriptionConflict() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test06_createDuplicateNamespaceSubscriptionConflict() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test07_createErrandSubscriptionForUnknownErrand() {
		setupCall()
			.withServicePath(TICKETS_PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_createErrandSubscriptionWithMissingTargetId() {
		setupCall()
			.withServicePath(TICKETS_PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test09_deleteSubscription() {
		assertThat(subscriptionRepository.findByIdAndSubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId(
			SUBSCRIPTION_ERRAND_ID, SUBSCRIBER_SERVICEDESK_ID, NAMESPACE, MUNICIPALITY_2281)).isPresent();

		setupCall()
			.withServicePath(PATH + "/" + SUBSCRIPTION_ERRAND_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(subscriptionRepository.findByIdAndSubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId(
			SUBSCRIPTION_ERRAND_ID, SUBSCRIBER_SERVICEDESK_ID, NAMESPACE, MUNICIPALITY_2281)).isNotPresent();
	}

	@Test
	void test10_deleteSubscriptionNotFound() {
		setupCall()
			.withServicePath(PATH + "/" + UNKNOWN_SUBSCRIPTION_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test11_deleteSubscriptionOwnedByDifferentSubscriber() {
		// SUBSCRIPTION_NAMESPACE_ID belongs to SUBSCRIBER_SERVICEDESK, not SUBSCRIBER_TICKETS
		setupCall()
			.withServicePath(TICKETS_PATH + "/" + SUBSCRIPTION_NAMESPACE_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test12_createAndFetchRoundTrip() {
		// 1. POST a new subscription for SUBSCRIBER_TICKETS (errand 1be673… not previously subscribed by this subscriber).
		setupCall()
			.withServicePath(TICKETS_PATH)
			.withHttpMethod(POST)
			.withHeader(HEADER_NAME, "type=adAccount; jane11dane")
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("^" + TICKETS_PATH + "/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))
			.sendRequestAndVerifyResponse();

		// 2. GET fetches the listing and verifies the new entry was fully persisted (eventFilters, expiresAt, createdBy).
		setupCall()
			.withServicePath(TICKETS_PATH)
			.withHttpMethod(GET)
			.withJsonAssertOptions(List.of(Option.IGNORING_ARRAY_ORDER))
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
