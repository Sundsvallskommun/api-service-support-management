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
import static se.sundsvall.dept44.support.Identifier.HEADER_NAME;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.SubscriberRepository;

/**
 * Subscriber IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/SubscribersIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class SubscribersIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String MUNICIPALITY_2281 = "2281";
	private static final String PATH = "/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/subscribers";

	// Subscriber IDs from testdata-it.sql
	private static final String SUBSCRIBER_SERVICEDESK_ID = "aabbccdd-0000-0000-0000-000000000001";
	private static final String SUBSCRIBER_TICKETS_ID = "aabbccdd-0000-0000-0000-000000000002";
	private static final String SUBSCRIBER_TO_DELETE_ID = "aabbccdd-0000-0000-0000-000000000004";
	private static final String UNKNOWN_SUBSCRIBER_ID = "ffffffff-ffff-ffff-ffff-ffffffffffff";

	@Autowired
	private SubscriberRepository subscriberRepository;

	@Test
	void test01_getSubscribers() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_getSubscribersFilteredByIdentifier() {
		setupCall()
			.withServicePath(PATH + "?identifierType=adAccount&identifierValue=joe01doe")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_getSubscriber() {
		setupCall()
			.withServicePath(PATH + "/" + SUBSCRIBER_TICKETS_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_getSubscriberNotFound() {
		setupCall()
			.withServicePath(PATH + "/" + UNKNOWN_SUBSCRIBER_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_createSubscriber() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withHeader(HEADER_NAME, "type=adAccount; adm01adm")
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("^" + PATH + "/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test06_createSubscriberConflict() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test07_updateSubscriber() {
		setupCall()
			.withServicePath(PATH + "/" + SUBSCRIBER_SERVICEDESK_ID)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_updateSubscriberNotFound() {
		setupCall()
			.withServicePath(PATH + "/" + UNKNOWN_SUBSCRIBER_ID)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test09_deleteSubscriber() {
		assertThat(subscriberRepository.findByIdAndNamespaceAndMunicipalityId(SUBSCRIBER_TO_DELETE_ID, NAMESPACE, MUNICIPALITY_2281)).isPresent();

		setupCall()
			.withServicePath(PATH + "/" + SUBSCRIBER_TO_DELETE_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(subscriberRepository.findByIdAndNamespaceAndMunicipalityId(SUBSCRIBER_TO_DELETE_ID, NAMESPACE, MUNICIPALITY_2281)).isNotPresent();
	}

	@Test
	void test10_deleteSubscriberCascadesSubscriptions() {
		assertThat(subscriberRepository.findByIdAndNamespaceAndMunicipalityId(SUBSCRIBER_SERVICEDESK_ID, NAMESPACE, MUNICIPALITY_2281)).isPresent();

		setupCall()
			.withServicePath(PATH + "/" + SUBSCRIBER_SERVICEDESK_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(subscriberRepository.findByIdAndNamespaceAndMunicipalityId(SUBSCRIBER_SERVICEDESK_ID, NAMESPACE, MUNICIPALITY_2281)).isNotPresent();
	}
}
