package se.sundsvall.supportmanagement.apptest;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.scheduler.action.ActionScheduler;
import se.sundsvall.supportmanagement.service.scheduler.emailreader.EmailReaderScheduler;
import se.sundsvall.supportmanagement.service.scheduler.processevent.ProcessEventRelay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;

/**
 * The start mode of the labels over the wire: the permission an ordinary errand event carries to pw-alkt, and the checks
 * a label write is held to. Also the publication from the writes that reach an errand through neither the API nor the
 * errand service: the email intake, which writes no revision, and a scheduled action, which has no request behind it,
 * and the deletion of an errand through the API, which is published whatever its labels say.
 * <p>
 * A changed errand wakes the process of PROCESS-NAMESPACE (testdata-process-loop-guard.sql), and the direct run is off,
 * so every event written stays undelivered until a test delivers it. The tests of the intake, the action and the
 * deletion turn NAMESPACE-1 into a namespace that runs a process as well (testdata-process-event.sql).
 */
@WireMockAppTestSuite(files = "classpath:/ProcessStartModeIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-process-loop-guard.sql",
	"/db/scripts/testdata-process-start-mode.sql"
})
@SqlMergeMode(MERGE)
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

	private static final String INTAKE_NAMESPACE = "NAMESPACE-1";
	private static final String INTAKE_ERRANDS_PATH = "/" + MUNICIPALITY_ID + "/" + INTAKE_NAMESPACE + "/errands";
	private static final String EMAILED_ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
	private static final String ERRAND_WITHOUT_LABELS_ID = "cc236cf1-c00f-4479-8341-ecf5dd90b5b9";
	private static final String INTAKE_PROCESS_SQL = "/db/scripts/testdata-process-event.sql";

	@Autowired
	private EmailReaderScheduler emailReaderScheduler;

	@Autowired
	private ActionScheduler actionScheduler;

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

	/**
	 * The creation has put an automatic start on its way, and a handler presses start before it has been delivered.
	 */
	@Test
	@DisplayName("Verification that a start pressed while an automatic start is on its way is recorded, but hands the process no second start")
	void test11_aStartPressedWhileAnAutomaticStartIsOnItsWayIsRecorded() {
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

		setupCall()
			.withServicePath(ERRANDS_PATH + "/" + errandId + "/processes/start")
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		assertThat(outboxRepository.findAll()).singleElement().satisfies(row -> {
			assertThat(row.getErrandId()).isEqualTo(errandId);
			assertThat(row.getEventType()).isEqualTo("CREATE");
			assertThat(row.getEventSubType()).isEqualTo("ERRAND");
			assertThat(row.isStartAllowed()).isTrue();
		});

		setupCall()
			.withServicePath(ERRANDS_PATH + "/" + errandId + "/process-activities")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@DisplayName("Verification that an email arriving on an errand reaches the process of that errand")
	@Sql(INTAKE_PROCESS_SQL)
	void test12_anEmailIntakeGivesAnOutboxRow() {
		setupCall();

		emailReaderScheduler.getAndProcessEmails();

		assertThat(outboxRepository.findAll())
			.singleElement()
			.satisfies(row -> {
				assertThat(row.getErrandId()).isEqualTo(EMAILED_ERRAND_ID);
				assertThat(row.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
				assertThat(row.getNamespace()).isEqualTo(INTAKE_NAMESPACE);
				assertThat(row.getProcessService()).isEqualTo("pw-alkt");
				assertThat(row.getProcessKey()).isEqualTo(APPLICATION);
				assertThat(row.getEventType()).isEqualTo("UPDATE");
				assertThat(row.getEventSubType()).isEqualTo("MESSAGE");
				assertThat(row.getCreated()).isNotNull();
				assertThat(row.getDeliveredAt()).isNull();
			});

		// The label says nothing about the start mode, which reads as AUTOMATIC, and the errand has no process yet
		assertThat(outboxRepository.findAll()).extracting(ProcessEventOutboxEntity::isStartAllowed).containsExactly(true);

		verifyStubs();
	}

	/**
	 * The label is added with no request behind it, and it is the label naming the process. The action records its
	 * change as a new revision, and the executed action is removed.
	 */
	@Test
	@DisplayName("Verification that the process label a scheduled action gives an errand reaches the process, with the permission to start it")
	@Sql({
		INTAKE_PROCESS_SQL, "/db/scripts/testdata-process-event-action.sql"
	})
	void test13_aScheduledLabelActionGivesAnOutboxRow() {
		setupCall();

		actionScheduler.processActions();

		assertThat(outboxRepository.findAll())
			.singleElement()
			.satisfies(row -> {
				assertThat(row.getErrandId()).isEqualTo(ERRAND_WITHOUT_LABELS_ID);
				assertThat(row.getProcessKey()).isEqualTo(APPLICATION);
				assertThat(row.getEventType()).isEqualTo("UPDATE");
				assertThat(row.getEventSubType()).isEqualTo("ERRAND");
				assertThat(row.isStartAllowed()).isTrue();
				assertThat(row.getExecutedBy()).isNull();
			});

		setupCall()
			.withServicePath(INTAKE_ERRANDS_PATH + "/" + ERRAND_WITHOUT_LABELS_ID + "/revisions")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-revisions.json")
			.sendRequest();

		setupCall()
			.withServicePath(INTAKE_ERRANDS_PATH + "/" + ERRAND_WITHOUT_LABELS_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-errand.json")
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The errand is created without a label naming a process, so its creation reaches none. Its deletion is published all
	 * the same, without a process key, since the process consumer finds the instance by the errand and not by the key.
	 */
	@Test
	@DisplayName("Verification that deleting an errand whose labels name no process still reaches the process consumer, without a key")
	@Sql(INTAKE_PROCESS_SQL)
	void test14_aDeletionWithoutAProcessKeyGivesAnOutboxRow() {
		final var location = setupCall()
			.withServicePath(INTAKE_ERRANDS_PATH)
			.withHttpMethod(POST)
			.withRequest("request-create.json")
			.withExpectedResponseStatus(CREATED)
			.sendRequest()
			.getResponseHeaders()
			.getLocation()
			.getPath();
		final var errandId = location.substring(location.lastIndexOf('/') + 1);

		setupCall()
			.withServicePath(location)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequest();

		setupCall()
			.withServicePath(location)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll())
			.singleElement()
			.satisfies(row -> {
				assertThat(row.getErrandId()).isEqualTo(errandId);
				assertThat(row.getProcessService()).isEqualTo("pw-alkt");
				assertThat(row.getProcessKey()).isNull();
				assertThat(row.getEventType()).isEqualTo("DELETE");
				assertThat(row.getEventSubType()).isEqualTo("ERRAND");
				assertThat(row.isStartAllowed()).isFalse();
			});
	}
}
