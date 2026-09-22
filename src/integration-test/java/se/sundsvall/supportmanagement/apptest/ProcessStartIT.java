package se.sundsvall.supportmanagement.apptest;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.scheduler.processevent.ProcessEventRelay;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Objects.isNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

/**
 * Starting the handling of an errand by hand over the wire: what GET .../processes says about whether a start is
 * possible, the start command with every answer it gives, and the start reaching pw-alkt.
 * <p>
 * PROCESS-NAMESPACE names no process triggers, and the direct run is off, so every row here is written by a start
 * command and stays undelivered until a test delivers it.
 */
@WireMockAppTestSuite(files = "classpath:/ProcessStartIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-process-start.sql"
})
class ProcessStartIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "PROCESS-NAMESPACE";
	private static final String NAMESPACE_WITHOUT_PROCESSES = "NAMESPACE-1";

	private static final String SUPERVISION_ERRAND_ID = "ab000000-0000-0000-0000-0000000000b1";
	private static final String AMBIGUOUS_ERRAND_ID = "ab000000-0000-0000-0000-0000000000b2";
	private static final String FAILED_START_ERRAND_ID = "ab000000-0000-0000-0000-0000000000b3";
	private static final String COMPLETED_ERRAND_ID = "ab000000-0000-0000-0000-0000000000b4";
	private static final String BUSY_ERRAND_ID = "ab000000-0000-0000-0000-0000000000b5";
	private static final String LIVE_ERRAND_ID = "aa000000-0000-0000-0000-0000000000a1";
	private static final String UNLABELLED_ERRAND_ID = "aa000000-0000-0000-0000-0000000000a2";
	private static final String UNKNOWN_ERRAND_ID = "ab000000-0000-0000-0000-00000000dead";
	private static final String ERRAND_OF_NAMESPACE_WITHOUT_PROCESSES_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";

	private static final String SUPERVISION = "alkt-tillsyn";
	private static final String APPLICATION = "alkt-ansokan";

	private static final String HANDLER = "joe01doe";
	private static final String HANDLER_IDENTITY = HANDLER + "; type=adAccount";
	private static final String PROCESS_ENGINE = "pw-alkt; type=processEngine";

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String PROCESSES_RESPONSE_FILE = "response-processes.json";
	private static final String ACTIVITIES_RESPONSE_FILE = "response-activities.json";

	@Autowired
	private ProcessEventOutboxRepository outboxRepository;

	@Autowired
	private ProcessEventRelay processEventRelay;

	@Autowired
	private NamespaceConfigService namespaceConfigService;

	@Autowired
	private ProcessEngineProperties processEngineProperties;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static String errandPath(final String errandId) {
		return "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + errandId;
	}

	private static String processesPath(final String namespace, final String errandId) {
		return "/" + MUNICIPALITY_ID + "/" + namespace + "/errands/" + errandId + "/processes";
	}

	private static String processesPath(final String errandId) {
		return processesPath(NAMESPACE, errandId);
	}

	private static String startPath(final String namespace, final String errandId) {
		return processesPath(namespace, errandId) + "/start";
	}

	private static String startPath(final String errandId) {
		return startPath(NAMESPACE, errandId);
	}

	private static String activitiesPath(final String errandId) {
		return errandPath(errandId) + "/process-activities";
	}

	/**
	 * The errand is assigned to someone other than the handler starting it, so a notification would have someone to go
	 * to. Until the process registers the start, the errand still reads as startable.
	 */
	@Test
	@DisplayName("Verification that a start by hand gives an activity entry without an instance naming the sender, and an event carrying the chosen key and the permission all the way to pw-alkt")
	void test01_aStartReachesTheProcessWithItsKey() {
		assertThat(namespaceConfigService.getProcessTriggers(NAMESPACE, MUNICIPALITY_ID))
			.as("no trigger may be named for the test to show that the triggers have no say over a start")
			.isEmpty();

		setupCall()
			.withServicePath(processesPath(SUPERVISION_ERRAND_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(PROCESSES_RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(startPath(SUPERVISION_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		assertThat(outboxRepository.findAll()).singleElement().satisfies(row -> {
			assertThat(row.getErrandId()).isEqualTo(SUPERVISION_ERRAND_ID);
			assertThat(row.getEventType()).isEqualTo("UPDATE");
			assertThat(row.getEventSubType()).isEqualTo("PROCESS");
			assertThat(row.getProcessKey()).isEqualTo(SUPERVISION);
			assertThat(row.isStartAllowed()).isTrue();
			assertThat(row.getSignalName()).isNull();
			assertThat(row.getExecutedBy()).isEqualTo(HANDLER);
			assertThat(row.getDeliveredAt()).isNull();
		});
		assertThat(jdbcTemplate.queryForObject("select count(*) from notification_dispatch where errand_id = ?", Integer.class, SUPERVISION_ERRAND_ID)).isZero();

		setupCall()
			.withServicePath(errandPath(SUPERVISION_ERRAND_ID) + "/notifications")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-notifications.json")
			.sendRequest();

		setupCall()
			.withServicePath(activitiesPath(SUPERVISION_ERRAND_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(ACTIVITIES_RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(processesPath(SUPERVISION_ERRAND_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(PROCESSES_RESPONSE_FILE)
			.sendRequest();

		processEventRelay.relayErrand(SUPERVISION_ERRAND_ID);

		verifyStubs();
	}

	@Test
	@DisplayName("Verification that every press with the same key is recorded and answered 202, while the process is handed one start")
	void test02_aDoubleClickIsRecordedButStartsOnce() {
		setupCall()
			.withServicePath(startPath(SUPERVISION_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		setupCall()
			.withServicePath(startPath(SUPERVISION_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		setupCall()
			.withServicePath(startPath(SUPERVISION_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		assertThat(outboxRepository.findAll()).singleElement().satisfies(row -> {
			assertThat(row.getErrandId()).isEqualTo(SUPERVISION_ERRAND_ID);
			assertThat(row.getProcessKey()).isEqualTo(SUPERVISION);
		});
		wiremock.verify(3, postRequestedFor(urlPathEqualTo("/api-eventlog/" + MUNICIPALITY_ID + "/" + SUPERVISION_ERRAND_ID)));

		setupCall()
			.withServicePath(activitiesPath(SUPERVISION_ERRAND_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@DisplayName("Verification that a start of another process while one is on its way is refused with 409, and leaves the first start untouched")
	void test03_aChangedChoiceWhileAStartIsOnItsWayIsRefused() {
		setupCall()
			.withServicePath(startPath(AMBIGUOUS_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-application.json")
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		setupCall()
			.withServicePath(startPath(AMBIGUOUS_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-supervision.json")
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		assertThat(outboxRepository.findAll()).singleElement().satisfies(row -> {
			assertThat(row.getProcessKey()).isEqualTo(APPLICATION);
			assertThat(row.getDeliveredAt()).isNull();
		});

		setupCall()
			.withServicePath(activitiesPath(AMBIGUOUS_ERRAND_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(ACTIVITIES_RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@DisplayName("Verification that an errand whose labels point at two processes offers both, refuses a start choosing none or another, and starts the one chosen")
	void test04_anAmbiguousErrandStartsTheChosenProcess() {
		setupCall()
			.withServicePath(processesPath(AMBIGUOUS_ERRAND_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(PROCESSES_RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(startPath(AMBIGUOUS_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse("response-no-key.json")
			.sendRequest();

		setupCall()
			.withServicePath(startPath(AMBIGUOUS_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-unknown-key.json")
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse("response-unknown-key.json")
			.sendRequest();

		assertThat(outboxRepository.findAll()).isEmpty();

		setupCall()
			.withServicePath(startPath(AMBIGUOUS_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).singleElement().satisfies(row -> {
			assertThat(row.getProcessKey()).isEqualTo(SUPERVISION);
			assertThat(row.isStartAllowed()).isTrue();
		});
	}

	@Test
	@DisplayName("Verification that a start from a caller that is not an ad account is refused with 403 and writes nothing")
	void test05_aStartFromAMachineIsRefused() {
		setupCall()
			.withServicePath(startPath(SUPERVISION_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(startPath(SUPERVISION_ERRAND_ID))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("Verification that an errand running a process is offered no start, and that a start is refused with 409 without anything written")
	void test06_aLiveProcessRefusesTheStart() {
		setupCall()
			.withServicePath(processesPath(LIVE_ERRAND_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(PROCESSES_RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(startPath(LIVE_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("Verification that an errand whose process ran to its end is offered no start, and that a start is refused with 409 without anything written")
	void test07_aCompletedProcessRefusesTheStart() {
		setupCall()
			.withServicePath(processesPath(COMPLETED_ERRAND_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(PROCESSES_RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(startPath(COMPLETED_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("Verification that an errand without a label naming a process is offered no start, and that a start is refused with 400 without anything written")
	void test08_anErrandWithoutAProcessLabelRefusesTheStart() {
		setupCall()
			.withServicePath(processesPath(UNLABELLED_ERRAND_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(PROCESSES_RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(startPath(UNLABELLED_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("Verification that an errand of a namespace without a process engine is offered no start, and that a start is refused with 400 without anything written")
	void test09_aNamespaceWithoutAProcessEngineRefusesTheStart() {
		setupCall()
			.withServicePath(processesPath(NAMESPACE_WITHOUT_PROCESSES, ERRAND_OF_NAMESPACE_WITHOUT_PROCESSES_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(PROCESSES_RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(startPath(NAMESPACE_WITHOUT_PROCESSES, ERRAND_OF_NAMESPACE_WITHOUT_PROCESSES_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("Verification that a start of an errand that is not there, or not in the namespace named, is refused with 404 without anything written")
	void test10_aStartOfAnErrandThatIsNotThereIsRefused() {
		setupCall()
			.withServicePath(startPath(UNKNOWN_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(startPath(NAMESPACE_WITHOUT_PROCESSES, SUPERVISION_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse("response-other-namespace.json")
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("Verification that an errand whose only start failed, and whose label starts on its own, is started again by hand")
	void test11_aFailedStartIsStartedAgainInAutomaticMode() {
		setupCall()
			.withServicePath(processesPath(FAILED_START_ERRAND_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(PROCESSES_RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(startPath(FAILED_START_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).singleElement().satisfies(row -> {
			assertThat(row.getEventSubType()).isEqualTo("PROCESS");
			assertThat(row.getProcessKey()).isEqualTo(APPLICATION);
			assertThat(row.isStartAllowed()).isTrue();
		});
	}

	@Test
	@DisplayName("Verification that a start reaches the process though the emergency brake has tripped for the errand")
	void test12_aStartPassesATrippedBrake() {
		tripTheBrake(BUSY_ERRAND_ID);

		setupCall()
			.withServicePath(startPath(BUSY_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).filteredOn(row -> isNull(row.getDeliveredAt())).singleElement().satisfies(row -> {
			assertThat(row.getEventSubType()).isEqualTo("PROCESS");
			assertThat(row.getProcessKey()).isEqualTo(SUPERVISION);
			assertThat(row.isStartAllowed()).isTrue();
		});
	}

	@Test
	@DisplayName("Verification that a start naming a key longer than a process key can be is a bad request, and that the process is not told of it")
	void test13_anOversizedKeyIsRejected() {
		setupCall()
			.withServicePath(startPath(SUPERVISION_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("Verification that a blank key is no key named, and starts the one process the labels offer")
	void test14_aBlankKeyStartsTheOnlyProcessOffered() {
		setupCall()
			.withServicePath(startPath(SUPERVISION_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).singleElement().satisfies(row -> {
			assertThat(row.getEventSubType()).isEqualTo("PROCESS");
			assertThat(row.getProcessKey()).isEqualTo(SUPERVISION);
		});
	}

	@Test
	@DisplayName("Verification that a blank key on an errand whose labels point at two processes chooses neither, as no key would")
	void test15_aBlankKeyOnAnAmbiguousErrandChoosesNothing() {
		setupCall()
			.withServicePath(startPath(AMBIGUOUS_ERRAND_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	/**
	 * Writes as many delivered rows within the window as the emergency brake allows, and verifies that the brake counts
	 * them.
	 */
	private void tripTheBrake(final String errandId) {
		final var guard = processEngineProperties.loopGuard();

		for (var i = 0; i < guard.maxEventsPerErrand(); i++) {
			outboxRepository.save(ProcessEventOutboxEntity.create()
				.withMunicipalityId(MUNICIPALITY_ID)
				.withNamespace(NAMESPACE)
				.withErrandId(errandId)
				.withProcessService("pw-alkt")
				.withProcessKey(SUPERVISION)
				.withEventType("UPDATE")
				.withEventSubType("ERRAND")
				.withDeliveredAt(OffsetDateTime.now()));
		}

		assertThat(outboxRepository.countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter(errandId, OffsetDateTime.now().minus(guard.window())))
			.isGreaterThanOrEqualTo(guard.maxEventsPerErrand());
	}
}
