package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.NotificationRepository;

@WireMockAppTestSuite(files = "classpath:/NotificationIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class NotificationIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String NAMESPACE = "NAMESPACE.1";
	private static final String MUNICIPALITY_2281 = "2281";
	private static final String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
	private static final String GLOBAL_NOTIFICATIONS_PATH = "/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/notifications";
	private static final String ERRAND_NOTIFICATIONS_PATH = "/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/notifications";
	private static final String OWNER_ID = "owner_id-1";
	private static final String NOTIFICATION_ID = "3ec421e9-56d1-4e47-9160-259d8dbe6a50";

	@Autowired
	private NotificationRepository notificationRepository;

	@Test
	void test01_createNotification() {

		final var result = setupCall()
			.withServicePath(ERRAND_NOTIFICATIONS_PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/notifications/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse()
			.getResponseHeaders()
			.getFirst(LOCATION);

		assertThat(result).isNotBlank();
		final var createdNotificationId = result.substring(result.lastIndexOf('/') + 1);
		assertThat(notificationRepository.existsByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(createdNotificationId, NAMESPACE, MUNICIPALITY_2281, ERRAND_ID)).isTrue();

	}

	@Test
	void test02_getNotification() {
		setupCall()
			.withServicePath(GLOBAL_NOTIFICATIONS_PATH + "?ownerId=" + OWNER_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_updateNotification() {
		setupCall()
			.withServicePath(GLOBAL_NOTIFICATIONS_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

	}

	@Test
	void test04_deleteNotification() {
		setupCall()
			.withServicePath(ERRAND_NOTIFICATIONS_PATH + "/" + NOTIFICATION_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_getNotification() {
		setupCall()
			.withServicePath(ERRAND_NOTIFICATIONS_PATH + "/" + NOTIFICATION_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
