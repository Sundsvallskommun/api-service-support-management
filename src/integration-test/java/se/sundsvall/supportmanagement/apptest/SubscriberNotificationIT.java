package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.SubscriberNotificationRepository;

@WireMockAppTestSuite(files = "classpath:/SubscriberNotificationIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class SubscriberNotificationIT extends AbstractAppTest {

	private static final String RESPONSE_FILE = "response.json";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String NOTIFICATION_ID_1 = "a1b2c3d4-0000-0000-0000-000000000001";
	private static final String NOTIFICATION_ID_2 = "a1b2c3d4-0000-0000-0000-000000000002";
	private static final String BASE_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/notifications";

	@Autowired
	private SubscriberNotificationRepository repository;

	@Test
	void test01_getNotifications() {
		setupCall()
			.withServicePath(BASE_PATH + "/adAccount/joe01doe")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_acknowledgeNotification() {
		assertThat(repository.findById(NOTIFICATION_ID_1)).isPresent()
			.get()
			.extracting("acknowledged")
			.isNull();

		setupCall()
			.withServicePath(BASE_PATH + "/" + NOTIFICATION_ID_1 + "/acknowledge")
			.withHttpMethod(PUT)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(repository.findById(NOTIFICATION_ID_1)).isPresent()
			.get()
			.extracting("acknowledged")
			.isNotNull();
	}

	@Test
	void test03_deleteNotification() {
		assertThat(repository.existsById(NOTIFICATION_ID_2)).isTrue();

		setupCall()
			.withServicePath(BASE_PATH + "/" + NOTIFICATION_ID_2)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(repository.existsById(NOTIFICATION_ID_2)).isFalse();
	}
}
