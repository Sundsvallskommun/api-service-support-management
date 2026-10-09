package se.sundsvall.supportmanagement.apptest;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.NotificationDispatchRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

/**
 * Errands created as drafts over the wire: a draft tells the process nothing whatever its labels say, nothing is
 * communicated and no one is notified about it, a search leaves it out unless asked for it, and made active it is
 * handled as an errand just created.
 * <p>
 * A changed errand wakes the process of PROCESS-NAMESPACE (testdata-process-loop-guard.sql), the label the drafts wear
 * starts its process on its own (testdata-process-start-mode.sql), and the direct run is off, so every event written
 * stays in the outbox.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandDraftIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-process-loop-guard.sql",
	"/db/scripts/testdata-process-start-mode.sql"
})
class ErrandDraftIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "PROCESS-NAMESPACE";
	private static final String ERRANDS_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands";
	private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

	private static final String ACTIVE_ERRAND_ID = "ac000000-0000-0000-0000-0000000000c2";

	private static final String APPLICATION = "alkt-ansokan";
	private static final String HANDLER_IDENTITY = "joe01doe; type=adAccount";

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	@Autowired
	private ProcessEventOutboxRepository outboxRepository;

	@Autowired
	private NotificationDispatchRepository notificationDispatchRepository;

	@Test
	@DisplayName("Verification that a draft wearing a label that starts its process on its own tells the process nothing, and cannot be started by hand")
	void test01_aDraftTellsTheProcessNothing() {
		final var errandId = createDraft();

		setupCall()
			.withServicePath(ERRANDS_PATH + "/" + errandId + "/processes")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-processes.json")
			.sendRequest();

		setupCall()
			.withServicePath(ERRANDS_PATH + "/" + errandId + "/processes/start")
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-start.json")
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("Verification that a draft made active is logged as activated and publishes the permission to start its process")
	void test02_aDraftMadeActiveMayStartItsProcess() {
		final var errandId = createDraft();

		setupCall()
			.withServicePath(ERRANDS_PATH + "/" + errandId)
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-activate.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		setupCall()
			.withServicePath(ERRANDS_PATH + "/" + errandId + "/processes")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-processes.json")
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).singleElement().satisfies(row -> {
			assertThat(row.getErrandId()).isEqualTo(errandId);
			assertThat(row.getEventType()).isEqualTo("UPDATE");
			assertThat(row.getEventSubType()).isEqualTo("ERRAND");
			assertThat(row.getProcessKey()).isEqualTo(APPLICATION);
			assertThat(row.isStartAllowed()).isTrue();
		});
	}

	@Test
	@DisplayName("Verification that an active errand is never made a draft again")
	void test03_anActiveErrandIsNeverMadeADraft() {
		setupCall()
			.withServicePath(ERRANDS_PATH + "/" + ACTIVE_ERRAND_ID)
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("Verification that the drafts of a namespace are found by their life cycle")
	void test04_draftsAreFoundByTheirLifecycle() {
		createDraft();

		setupCall()
			.withServicePath(ERRANDS_PATH + "/count?filter=lifecycle:'DRAFT'")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@DisplayName("Verification that a life cycle not spelled exactly as one is refused")
	void test05_anUnknownLifecycleIsRefused() {
		setupCall()
			.withServicePath(ERRANDS_PATH)
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("Verification that a search leaves drafts out unless its filter names the life cycle")
	void test06_aSearchLeavesDraftsOut() {
		createDraft();

		setupCall()
			.withServicePath(ERRANDS_PATH + "/count?filter=title:'Utkast som inte syns'")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-default.json")
			.sendRequest();

		setupCall()
			.withServicePath(ERRANDS_PATH + "/count?filter=title:'Utkast som inte syns' and lifecycle:'DRAFT'")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-drafts.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	@DisplayName("Verification that nothing is communicated about a draft")
	void test07_nothingIsCommunicatedAboutADraft() {
		final var errandId = createDraft();

		setupCall()
			.withServicePath(ERRANDS_PATH + "/" + errandId + "/communication/email")
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-email.json")
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The draft has a handler, so a change to an active errand would notify them and its subscribers.
	 */
	@Test
	@DisplayName("Verification that no one is notified about a draft, whether by a change to it or by hand")
	void test08_noOneIsNotifiedAboutADraft() {
		final var errandId = createDraft();

		setupCall()
			.withServicePath(ERRANDS_PATH + "/" + errandId)
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-patch.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		setupCall()
			.withServicePath(ERRANDS_PATH + "/" + errandId + "/notifications")
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-notification.json")
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-notification.json")
			.sendRequest();

		setupCall()
			.withServicePath(ERRANDS_PATH + "/" + errandId + "/notifications")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-notifications.json")
			.sendRequestAndVerifyResponse();

		assertThat(notificationDispatchRepository.findAll()).isEmpty();
	}

	private String createDraft() {
		return setupCall()
			.withServicePath(ERRANDS_PATH)
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(ERRANDS_PATH + "/" + UUID_PATTERN))
			.sendRequest()
			.getResponseHeaders()
			.getLocation()
			.getPath()
			.substring(ERRANDS_PATH.length() + 1);
	}
}
