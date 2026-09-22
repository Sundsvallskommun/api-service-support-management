package se.sundsvall.supportmanagement.apptest;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.scheduler.processevent.ProcessEventRelay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;

/**
 * The start mode of the labels over the wire: the permission an ordinary errand event carries to pw-alkt, and the checks
 * a label write is held to.
 * <p>
 * A changed errand wakes the process of PROCESS-NAMESPACE (testdata-process-loop-guard.sql), and the direct run is off,
 * so every event written stays undelivered until a test delivers it.
 */
@WireMockAppTestSuite(files = "classpath:/ProcessStartModeIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-process-loop-guard.sql",
	"/db/scripts/testdata-process-start-mode.sql"
})
class ProcessStartModeIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "PROCESS-NAMESPACE";
	private static final String ERRANDS_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands";
	private static final String LABELS_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/labels";
	private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

	private static final String FAILED_ERRAND_WEARING_TWO_LABELS_ID = "ac000000-0000-0000-0000-0000000000c1";
	private static final String COMPLETED_ERRAND_ID = "ac000000-0000-0000-0000-0000000000c2";
	private static final String FAILED_ERRAND_ID = "ac000000-0000-0000-0000-0000000000c3";

	private static final String APPLICATION = "alkt-ansokan";
	private static final String HANDLER_IDENTITY = "joe01doe; type=adAccount";

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	@Autowired
	private ProcessEventOutboxRepository outboxRepository;

	@Autowired
	private ProcessEventRelay processEventRelay;

	@Autowired
	private NamespaceConfigService namespaceConfigService;

	@Test
	@DisplayName("Verification that an errand created with a MANUAL label is published without the permission to start")
	void test01_aCreationWithAManualLabelMayNotStartTheProcess() {
		assertThat(namespaceConfigService.getProcessTriggers(NAMESPACE, MUNICIPALITY_ID)).containsOnly(ERRAND);

		final var errandId = setupCall()
			.withServicePath(ERRANDS_PATH)
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(ERRANDS_PATH + "/" + UUID_PATTERN))
			.sendRequestAndVerifyResponse()
			.getResponseHeaders()
			.getLocation()
			.getPath()
			.substring(ERRANDS_PATH.length() + 1);

		assertThat(outboxRepository.findAll()).singleElement().satisfies(row -> {
			assertThat(row.getErrandId()).isEqualTo(errandId);
			assertThat(row.getEventType()).isEqualTo("CREATE");
			assertThat(row.getEventSubType()).isEqualTo("ERRAND");
			assertThat(row.getProcessKey()).isEqualTo(APPLICATION);
			assertThat(row.isStartAllowed()).isFalse();
		});
	}

	@Test
	@DisplayName("Verification that an errand created with a label saying nothing about the start mode is published with the permission to start")
	void test02_aCreationWithALabelWithoutStartModeMayStartTheProcess() {
		final var errandId = setupCall()
			.withServicePath(ERRANDS_PATH)
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(ERRANDS_PATH + "/" + UUID_PATTERN))
			.sendRequestAndVerifyResponse()
			.getResponseHeaders()
			.getLocation()
			.getPath()
			.substring(ERRANDS_PATH.length() + 1);

		assertThat(outboxRepository.findAll()).singleElement().satisfies(row -> {
			assertThat(row.getErrandId()).isEqualTo(errandId);
			assertThat(row.getEventType()).isEqualTo("CREATE");
			assertThat(row.getEventSubType()).isEqualTo("ERRAND");
			assertThat(row.getProcessKey()).isEqualTo(APPLICATION);
			assertThat(row.isStartAllowed()).isTrue();
		});
	}

	@Test
	@DisplayName("Verification that an errand created with an AUTOMATIC label reaches pw-alkt with the permission to start")
	void test03_aCreationWithAnAutomaticLabelReachesTheProcessWithThePermissionToStart() {
		final var errandId = setupCall()
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

		assertThat(outboxRepository.findAll()).singleElement().satisfies(row -> {
			assertThat(row.getErrandId()).isEqualTo(errandId);
			assertThat(row.getEventType()).isEqualTo("CREATE");
			assertThat(row.getEventSubType()).isEqualTo("ERRAND");
			assertThat(row.getProcessKey()).isEqualTo(APPLICATION);
			assertThat(row.isStartAllowed()).isTrue();
		});

		processEventRelay.relayErrand(errandId);

		verifyStubs();
	}

	/**
	 * The errand runs the application process and wears a MANUAL application label and an AUTOMATIC supervision label.
	 * The row carries the key of the instance, and no permission to start, as the MANUAL application label says.
	 */
	@Test
	@DisplayName("Verification that the start mode is read only off the label naming the key the event carries")
	void test04_theKeyAndTheStartModeComeFromTheSameLabel() {
		setupCall()
			.withServicePath(ERRANDS_PATH + "/" + FAILED_ERRAND_WEARING_TWO_LABELS_ID)
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).singleElement().satisfies(row -> {
			assertThat(row.getErrandId()).isEqualTo(FAILED_ERRAND_WEARING_TWO_LABELS_ID);
			assertThat(row.getEventType()).isEqualTo("UPDATE");
			assertThat(row.getEventSubType()).isEqualTo("ERRAND");
			assertThat(row.getProcessKey()).isEqualTo(APPLICATION);
			assertThat(row.isStartAllowed()).isFalse();
		});
	}

	@Test
	@DisplayName("Verification that a change to an errand whose process ran to its end is published without the permission to start")
	void test05_aChangeToAnErrandWhoseProcessRanToItsEndMayNotStartOne() {
		setupCall()
			.withServicePath(ERRANDS_PATH + "/" + COMPLETED_ERRAND_ID)
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).singleElement().satisfies(row -> {
			assertThat(row.getErrandId()).isEqualTo(COMPLETED_ERRAND_ID);
			assertThat(row.getEventType()).isEqualTo("UPDATE");
			assertThat(row.getEventSubType()).isEqualTo("ERRAND");
			assertThat(row.isStartAllowed()).isFalse();
		});
	}

	@Test
	@DisplayName("Verification that a change to an errand whose only start failed is published with the permission to start")
	void test06_aChangeToAnErrandWhoseOnlyStartFailedMayStartOneAgain() {
		setupCall()
			.withServicePath(ERRANDS_PATH + "/" + FAILED_ERRAND_ID)
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).singleElement().satisfies(row -> {
			assertThat(row.getErrandId()).isEqualTo(FAILED_ERRAND_ID);
			assertThat(row.getEventType()).isEqualTo("UPDATE");
			assertThat(row.getEventSubType()).isEqualTo("ERRAND");
			assertThat(row.isStartAllowed()).isTrue();
		});
	}

	@Test
	@DisplayName("Verification that a label write is refused when a start mode is not spelled exactly as one")
	void test07_aLabelWriteWithAStartModeInAnotherCaseIsRefused() {
		setupCall()
			.withServicePath(LABELS_PATH)
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse("response-create.json")
			.sendRequest();

		setupCall()
			.withServicePath(LABELS_PATH)
			.withHttpMethod(PUT)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse("response-update.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	@DisplayName("Verification that a label write is refused when a label has a start mode but no process key")
	void test08_aLabelWriteWithAStartModeButNoProcessKeyIsRefused() {
		setupCall()
			.withServicePath(LABELS_PATH)
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse("response-create.json")
			.sendRequest();

		setupCall()
			.withServicePath(LABELS_PATH)
			.withHttpMethod(PUT)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse("response-update.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	@DisplayName("Verification that a label write is refused when the key of a process attribute is not spelled exactly as the one read")
	void test09_aLabelWriteWithAMisspelledStartModeKeyIsRefused() {
		setupCall()
			.withServicePath(LABELS_PATH)
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse("response-create.json")
			.sendRequest();

		setupCall()
			.withServicePath(LABELS_PATH)
			.withHttpMethod(PUT)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse("response-update.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	@DisplayName("Verification that a label write whose process attributes can be read as written is taken")
	void test10_aLabelWriteWithAProcessKeyAndAStartModeIsTaken() {
		setupCall()
			.withServicePath(LABELS_PATH)
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		setupCall()
			.withServicePath(LABELS_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
