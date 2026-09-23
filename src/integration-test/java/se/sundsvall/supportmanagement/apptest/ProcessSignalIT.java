package se.sundsvall.supportmanagement.apptest;

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
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.scheduler.processevent.ProcessEventRelay;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Objects.isNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.TRIGGER_PROCESS_HEADER;

/**
 * Manual stepping over the wire: the process reports what it waits for, the errand shows it, a handler presses one of
 * the buttons, and the name of the gate reaches the process.
 * <p>
 * The outbox row and the event pw-alkt receives are both verified to carry the name of the signal. A signal that is
 * refused is verified to have written nothing at all: no activity entry, no outbox row, no errand event.
 * <p>
 * The namespace of testdata-it.sql names no process triggers at all, so every signal published here is published
 * without one. The direct run is off, so rows stay undelivered until a test delivers them.
 */
@WireMockAppTestSuite(files = "classpath:/ProcessSignalIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-process-signal.sql"
})
class ProcessSignalIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "PROCESS-NAMESPACE";

	private static final String ERRAND_ID = "aa000000-0000-0000-0000-0000000000a1";
	private static final String ERRAND_WITHOUT_PROCESS_ID = "aa000000-0000-0000-0000-0000000000a2";
	private static final String UNKNOWN_ERRAND_ID = "aa000000-0000-0000-0000-00000000dead";
	private static final String PROCESS_INSTANCE_ID = "pi-it-live";
	private static final String UNKNOWN_PROCESS_INSTANCE_ID = "pi-unknown";
	private static final String PROCESS_KEY = "alkt-ansokan";

	private static final String HANDLER = "joe01doe";
	private static final String HANDLER_IDENTITY = HANDLER + "; type=adAccount";
	private static final String PROCESS_ENGINE = "pw-alkt; type=processEngine";
	private static final String DO_NOT_WAKE = "false";

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String REPORT_FILE = "request-report.json";
	private static final String ERRAND_RESPONSE_FILE = "response-errand.json";

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

	private static String instancePath(final String errandId, final String processInstanceId) {
		return errandPath(errandId) + "/processes/" + processInstanceId;
	}

	private static String signalsPath(final String errandId, final String processInstanceId) {
		return instancePath(errandId, processInstanceId) + "/signals";
	}

	/**
	 * The whole chain, from the report of the process to the event pw-alkt receives. The errand is assigned to someone
	 * other than the handler, and the signal still notifies no one.
	 */
	@Test
	@DisplayName("Verification that an awaited signal gives an activity entry naming the sender, and an event carrying the name of the gate all the way to pw-alkt")
	void test01_anAwaitedSignalReachesTheProcessWithItsName() {
		assertThat(namespaceConfigService.getProcessTriggers(NAMESPACE, MUNICIPALITY_ID))
			.as("no trigger may name the signal for the test to show that the triggers have no say over it")
			.isEmpty();

		reportAsProcess(REPORT_FILE);

		setupCall()
			.withServicePath(errandPath(ERRAND_ID))
			.withHttpMethod(GET)
			.withJsonAssertOptions(null)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(ERRAND_RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(signalsPath(ERRAND_ID, PROCESS_INSTANCE_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		assertThat(outboxRepository.findAll()).singleElement().satisfies(row -> {
			assertThat(row.getErrandId()).isEqualTo(ERRAND_ID);
			assertThat(row.getEventType()).isEqualTo("UPDATE");
			assertThat(row.getEventSubType()).isEqualTo("SIGNAL");
			assertThat(row.getSignalName()).isEqualTo("granskning-godkand");
			assertThat(row.getProcessKey()).isEqualTo(PROCESS_KEY);
			assertThat(row.isStartAllowed()).isFalse();
			assertThat(row.getExecutedBy()).isEqualTo(HANDLER);
		});
		assertThat(jdbcTemplate.queryForObject("select count(*) from notification_dispatch where errand_id = ?", Integer.class, ERRAND_ID)).isZero();

		setupCall()
			.withServicePath(errandPath(ERRAND_ID) + "/process-activities")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-activities.json")
			.sendRequest();

		setupCall()
			.withServicePath(errandPath(ERRAND_ID) + "/notifications")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-notifications.json")
			.sendRequest();

		processEventRelay.relayErrand(ERRAND_ID);

		verifyStubs();
	}

	/**
	 * A report replaces the list as a whole: a signal left out of it is gone, and pressing its button after that report
	 * is refused and writes nothing. An empty report means the process waits for no person.
	 */
	@Test
	@DisplayName("Verification that a signal the latest report left out is gone from the errand, and refused with 409 without anything written")
	void test02_aSignalNoLongerAwaitedIsRefused() {
		reportAsProcess("request-report-both.json");
		reportAsProcess("request-report-reject.json");

		setupCall()
			.withServicePath(errandPath(ERRAND_ID))
			.withHttpMethod(GET)
			.withJsonAssertOptions(null)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-errand-reject.json")
			.sendRequest();

		setupCall()
			.withServicePath(signalsPath(ERRAND_ID, PROCESS_INSTANCE_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-approve.json")
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-approve.json")
			.sendRequest();

		reportAsProcess("request-report-none.json");

		setupCall()
			.withServicePath(errandPath(ERRAND_ID))
			.withHttpMethod(GET)
			.withJsonAssertOptions(null)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-errand-none.json")
			.sendRequest();

		setupCall()
			.withServicePath(signalsPath(ERRAND_ID, PROCESS_INSTANCE_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-reject.json")
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-reject.json")
			.sendRequest();

		assertThat(outboxRepository.findAll()).isEmpty();

		setupCall()
			.withServicePath(errandPath(ERRAND_ID) + "/process-activities")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-activities.json")
			.sendRequestAndVerifyResponse();
	}

	/**
	 * Covers a caller with an identity of another type as well as one without any identity.
	 */
	@Test
	@DisplayName("Verification that a signal from a caller that is not an ad account is refused with 403 and writes nothing")
	void test03_aSignalFromAMachineIsRefused() {
		reportAsProcess(REPORT_FILE);

		setupCall()
			.withServicePath(signalsPath(ERRAND_ID, PROCESS_INSTANCE_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(signalsPath(ERRAND_ID, PROCESS_INSTANCE_ID))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	/**
	 * An ended process waits for no one, whatever its report says.
	 */
	@Test
	@DisplayName("Verification that a signal to an errand without that live process instance is not found, and one to an ended process is a conflict")
	void test04_aSignalWithoutALiveProcessToReachIsRefused() {
		reportAsProcess(REPORT_FILE);

		setupCall()
			.withServicePath(signalsPath(ERRAND_WITHOUT_PROCESS_ID, PROCESS_INSTANCE_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse("response-errand-without-process.json")
			.sendRequest();

		setupCall()
			.withServicePath(signalsPath(ERRAND_ID, UNKNOWN_PROCESS_INSTANCE_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse("response-unknown-instance.json")
			.sendRequest();

		setupCall()
			.withServicePath(signalsPath(UNKNOWN_ERRAND_ID, PROCESS_INSTANCE_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse("response-unknown-errand.json")
			.sendRequest();

		reportAsProcess("request-report-completed.json");

		setupCall()
			.withServicePath(errandPath(ERRAND_ID))
			.withHttpMethod(GET)
			.withJsonAssertOptions(null)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(ERRAND_RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(signalsPath(ERRAND_ID, PROCESS_INSTANCE_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse("response-ended.json")
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("Verification that a signal reaches the process though the emergency brake has tripped for the errand")
	void test05_aSignalPassesATrippedBrake() {
		reportAsProcess(REPORT_FILE);
		tripTheBrake();

		setupCall()
			.withServicePath(signalsPath(ERRAND_ID, PROCESS_INSTANCE_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).filteredOn(row -> isNull(row.getDeliveredAt())).singleElement().satisfies(row -> {
			assertThat(row.getEventSubType()).isEqualTo("SIGNAL");
			assertThat(row.getSignalName()).isEqualTo("granskning-godkand");
		});
	}

	@Test
	@DisplayName("Verification that a signal without a name is a bad request, and that the process is not told of it")
	void test06_aSignalWithoutANameIsRejected() {
		reportAsProcess(REPORT_FILE);

		setupCall()
			.withServicePath(signalsPath(ERRAND_ID, PROCESS_INSTANCE_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-without-signal.json")
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(signalsPath(ERRAND_ID, PROCESS_INSTANCE_ID))
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-blank-signal.json")
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	/**
	 * A signal consumes nothing: pressed twice before the process reports again, it is taken both times and reaches the
	 * process twice, and the errand goes on showing every signal the latest report named.
	 */
	@Test
	@DisplayName("Verification that the same signal pressed twice before the next report is taken both times, and leaves the awaited signals as they were")
	void test07_theSameSignalIsTakenTwiceBeforeTheNextReport() {
		reportAsProcess(REPORT_FILE);

		for (var press = 0; press < 2; press++) {
			setupCall()
				.withServicePath(signalsPath(ERRAND_ID, PROCESS_INSTANCE_ID))
				.withHttpMethod(POST)
				.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
				.withRequest(REQUEST_FILE)
				.withExpectedResponseStatus(ACCEPTED)
				.withExpectedResponseBodyIsNull()
				.sendRequest();
		}

		wiremock.verify(2, postRequestedFor(urlPathEqualTo("/api-eventlog/" + MUNICIPALITY_ID + "/" + ERRAND_ID)));

		setupCall()
			.withServicePath(errandPath(ERRAND_ID) + "/process-activities")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-activities.json")
			.sendRequest();

		setupCall()
			.withServicePath(errandPath(ERRAND_ID))
			.withHttpMethod(GET)
			.withJsonAssertOptions(null)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(ERRAND_RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).hasSize(2).allSatisfy(row -> {
			assertThat(row.getEventSubType()).isEqualTo("SIGNAL");
			assertThat(row.getSignalName()).isEqualTo("granskning-godkand");
			assertThat(row.getDeliveredAt()).isNull();
		});
	}

	/**
	 * Reports on the live instance as pw-alkt does, asking not to be woken by it.
	 */
	private void reportAsProcess(final String reportFile) {
		setupCall()
			.withServicePath(instancePath(ERRAND_ID, PROCESS_INSTANCE_ID))
			.withHttpMethod(PUT)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withHeader(TRIGGER_PROCESS_HEADER, DO_NOT_WAKE)
			.withRequest(reportFile)
			.withExpectedResponseStatus(OK)
			.sendRequest();
	}

	/**
	 * Trips the emergency brake for the errand by writing as many delivered rows within the window as the configured
	 * limit allows, and verifies that the count the brake asks for reaches that limit.
	 */
	private void tripTheBrake() {
		EmergencyBrake.trip(outboxRepository, processEngineProperties, MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PROCESS_KEY);
	}
}
