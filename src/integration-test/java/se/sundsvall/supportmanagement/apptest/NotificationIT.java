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

	private static final String NAMESPACE = "namespace_1";

	private static final String MUNICIPALITY_2281 = "2281";

	private static final String PATH = "/" + NAMESPACE + "/" + MUNICIPALITY_2281 + "/notifications";

	private static final String OWNER_ID = "owner_id-1";

	private static final String NOTIFICATION_ID = "3ec421e9-56d1-4e47-9160-259d8dbe6a50";

	@Autowired
	private NotificationRepository notificationRepository;

	@Test
	void test01_createNotification() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/" + NAMESPACE + "/" + MUNICIPALITY_2281 + "/notifications/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(notificationRepository.existsByIdAndNamespaceAndMunicipalityId("3ec421e9-56d1-4e47-9160-259d8dbe6a50", NAMESPACE, MUNICIPALITY_2281)).isTrue();

	}

	@Test
	void test02_getNotification() {
		setupCall()
			.withServicePath(PATH + "?ownerId=" + OWNER_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_updateNotification() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

	}

	@Test
	void test04_deleteNotification() {
		setupCall()
			.withServicePath(PATH + "/" + NOTIFICATION_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

}
