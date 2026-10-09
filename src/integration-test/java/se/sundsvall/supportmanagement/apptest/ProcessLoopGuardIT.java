package se.sundsvall.supportmanagement.apptest;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import generated.se.sundsvall.eventlog.EventType;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import se.sundsvall.dept44.scheduling.health.Dept44CompositeHealthContributor;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType;
import se.sundsvall.supportmanagement.service.EventService;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.model.ProcessCommand;
import se.sundsvall.supportmanagement.service.scheduler.processevent.ProcessEventScheduler;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.PROCESS;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SIGNAL;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.TRIGGER_PROCESS_HEADER;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.clearTriggerProcess;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.setTriggerProcess;

/**
 * The whole round between SupportManagement and its process, with WireMock standing in for pw-alkt, run to verify that
 * the two services stop waking each other.
 * <p>
 * Every change to the errand below is held to having become an errand event in the event log, made by the sender, and
 * every row written is held to reaching pw-alkt.
 * <p>
 * The test plays the part of pw-alkt that calls back: it answers the way pw-alkt does - registering the start,
 * reporting on its work step and patching the errand. SupportManagement delivers on its own, through the direct run,
 * and the test waits for rows to be delivered.
 * <p>
 * A handler steps the process on and starts it through the signal and start endpoints. Commands sent as a process
 * engine, which those endpoints refuse, are issued through {@link EventService#createProcessCommandEvent}, which is
 * where the endpoints hand them over.
 */
@WireMockAppTestSuite(files = "classpath:/ProcessLoopGuardIT/", classes = Application.class)
@TestPropertySource(properties = "process-engine.direct-run.enabled=true")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-process-loop-guard.sql"
})
class ProcessLoopGuardIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "PROCESS-NAMESPACE";
	private static final String ERRANDS_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands";
	private static final String PW_ALKT_PATH = "/api-pw-alkt/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/process/errand-events";
	private static final String RELAY_JOB = "process_event_relay";
	private static final String RELAY_HEALTH_PATH = "/actuator/health/dept44CompositeScheduler/process_event_relay";
	private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

	private static final String LABELLED_ERRAND_ID = "aa000000-0000-0000-0000-0000000000a4";
	private static final String PROCESS_KEY = "alkt-ansokan";
	private static final String SIGNAL_NAME = "granskning-godkand";

	private static final String PROCESS_SERVICE = "pw-alkt";
	private static final String PROCESS_ENGINE = PROCESS_SERVICE + "; type=processEngine";
	private static final String HANDLER = "joe01doe";
	private static final String HANDLER_IDENTITY = HANDLER + "; type=adAccount";
	private static final String DO_NOT_WAKE = "false";

	private static final String PATCH_FILE = "request-patch.json";
	private static final String ROUND = "{round}";
	private static final String HEALTH_RESPONSE_FILE = "response-health.json";
	private static final Duration DELIVERY_TIMEOUT = Duration.ofSeconds(10);

	@Autowired
	private ProcessEventOutboxRepository outboxRepository;

	@Autowired
	private ErrandsRepository errandsRepository;

	@Autowired
	private EventService eventService;

	@Autowired
	private NamespaceConfigService namespaceConfigService;

	@Autowired
	private ProcessEngineProperties processEngineProperties;

	@Autowired
	private ProcessEventScheduler processEventScheduler;

	@Autowired
	private Dept44CompositeHealthContributor healthContributor;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private int patches;

	private static String processesPath(final String errandId) {
		return ERRANDS_PATH + "/" + errandId + "/processes";
	}

	private static String instancePath(final String errandId) {
		return processesPath(errandId) + "/pi-loop-guard";
	}

	@Test
	@DisplayName("Verification that the round between SupportManagement and its process comes to rest, while neither a handler's change nor a command is silenced on the way")
	void test01_theRoundComesToRest() {
		assertThat(namespaceConfigService.getProcessTriggers(NAMESPACE, MUNICIPALITY_ID))
			.as("the commands have to be left out of the triggers for the test to show that they pass them")
			.containsOnly(ERRAND);
		// The scheduled relay never runs in the tests, so its health is registered the way a healthy run leaves it
		healthContributor.getOrCreateIndicator(RELAY_JOB).setHealthy();

		final var errandId = aHandlerCreatesAnErrandWearingTheProcessLabel();

		theProcessStartsAndItsWorkStepReportsRunning(errandId);
		theProcessPatchesTheErrandAskingNotToBeWoken(errandId);
		theWorkStepReportsThatTheProcessWaitsForAHandler(errandId);
		aHandlerSendsTheSamePatchWithTheHeaderKept(errandId);
		final var rounds = theProcessForgetsTheHeaderUntilTheBrakeCatchesIt(errandId);
		theTrippedBrakeHoldsBackOrdinaryChanges(errandId);
		aHandlerSignalsTheWaitingProcessPastTheBrake(errandId);
		aHandlerStartsTheFailedProcessAgainPastTheBrake(errandId);

		wiremock.verify(3, errandChangesLoggedBy(HANDLER));
		wiremock.verify(1 + rounds, errandChangesLoggedBy(PROCESS_SERVICE));
		verifyStubs();
	}

	/**
	 * Commands sent as a process engine, with the header asking not to be woken, still pass layer 1, while an ordinary
	 * change in the same context is held back by it.
	 */
	@Test
	@DisplayName("Verification that a command passes layer 1 of its own accord, so the buttons do not lean on the check keeping machines away from them")
	void test02_aCommandPassesLayerOneOfItsOwnAccord() {
		final var processEngine = Identifier.parse(PROCESS_ENGINE);
		setupCall();

		assertThat(rowsWrittenDuring(() -> asCaller(processEngine, LABELLED_ERRAND_ID,
			errand -> eventService.createErrandEvent(EventType.UPDATE, "Ärendet har uppdaterats.", errand, null, null, false, ERRAND)))).isEmpty();

		final var signal = rowsWrittenDuring(() -> issueCommand(processEngine, LABELLED_ERRAND_ID, SIGNAL, new ProcessCommand(null, SIGNAL_NAME)));
		final var start = rowsWrittenDuring(() -> issueCommand(processEngine, LABELLED_ERRAND_ID, PROCESS, new ProcessCommand(PROCESS_KEY, null)));

		assertThat(signal).singleElement().satisfies(row -> {
			assertThat(row.getEventSubType()).isEqualTo("SIGNAL");
			assertThat(row.getSignalName()).isEqualTo(SIGNAL_NAME);
		});
		assertThat(start).singleElement().satisfies(row -> {
			assertThat(row.getEventSubType()).isEqualTo("PROCESS");
			assertThat(row.getProcessKey()).isEqualTo(PROCESS_KEY);
			assertThat(row.isStartAllowed()).isTrue();
		});
		awaitEventReachingTheProcess(signal.getFirst(), UnaryOperator.identity());
		awaitEventReachingTheProcess(start.getFirst(), UnaryOperator.identity());
		verifyStubs();
	}

	/**
	 * An identity header that cannot be parsed leaves the request without an identity at all. Layer 1 still holds back a
	 * change sent with the header, and the row written without the header names no one.
	 */
	@Test
	@DisplayName("Verification that a process whose identity cannot be read is still obeyed when it asks not to be woken")
	void test03_anUnreadableIdentityLeavesLayerOneArmed() {
		assertThat(rowsWrittenDuring(() -> patchErrand(LABELLED_ERRAND_ID, PROCESS_SERVICE, DO_NOT_WAKE))).isEmpty();

		final var rows = rowsWrittenDuring(() -> patchErrand(LABELLED_ERRAND_ID, PROCESS_SERVICE, null));

		assertThat(rows).singleElement().satisfies(row -> {
			assertThat(row.getProcessKey()).isEqualTo(PROCESS_KEY);
			assertThat(row.getExecutedBy()).isNull();
		});
		awaitEventReachingTheProcess(rows.getFirst(), UnaryOperator.identity());
		wiremock.verify(2, postRequestedFor(urlPathEqualTo("/api-eventlog/" + MUNICIPALITY_ID + "/" + LABELLED_ERRAND_ID)));
		verifyStubs();
	}

	/**
	 * The creation is the event that starts the process: the label says nothing about the start mode, which reads as
	 * automatic, and the errand has never had a process. It is also the one write whose labels have never been read from
	 * the database, and the row it writes still names the process key.
	 */
	private String aHandlerCreatesAnErrandWearingTheProcessLabel() {
		final var errandId = setupCall()
			.withServicePath(ERRANDS_PATH)
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-create.json")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(ERRANDS_PATH + "/" + UUID_PATTERN))
			.sendRequest()
			.getResponseHeaders()
			.getLocation()
			.getPath()
			.substring(ERRANDS_PATH.length() + 1);

		final var rows = outboxRepository.findAll();
		assertThat(rows).singleElement().satisfies(row -> {
			assertThat(row.getErrandId()).isEqualTo(errandId);
			assertThat(row.getProcessService()).isEqualTo(PROCESS_SERVICE);
			assertThat(row.getProcessKey()).isEqualTo(PROCESS_KEY);
			assertThat(row.getEventType()).isEqualTo("CREATE");
			assertThat(row.getEventSubType()).isEqualTo("ERRAND");
			assertThat(row.isStartAllowed()).isTrue();
			assertThat(row.getExecutedBy()).isEqualTo(HANDLER);
		});
		awaitEventReachingTheProcess(rows.getFirst(), event -> event
			.withRequestBody(matchingJsonPath("$.errandId", equalTo(errandId)))
			.withRequestBody(matchingJsonPath("$.eventType", equalTo("CREATE")))
			.withRequestBody(matchingJsonPath("$.processKey", equalTo(PROCESS_KEY)))
			.withRequestBody(matchingJsonPath("$.startAllowed", equalTo("true"))));

		return errandId;
	}

	/**
	 * What pw-alkt does once the event has started the process: register the start, and have the first work step say that
	 * it is at work. Neither is a change to the errand, and neither wakes anything.
	 */
	private void theProcessStartsAndItsWorkStepReportsRunning(final String errandId) {
		assertThat(rowsWrittenDuring(() -> {
			setupCall()
				.withServicePath(processesPath(errandId))
				.withHttpMethod(POST)
				.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
				.withHeader(TRIGGER_PROCESS_HEADER, DO_NOT_WAKE)
				.withRequest("request-register.json")
				.withExpectedResponseStatus(CREATED)
				.sendRequest();

			reportAsProcess(errandId, "request-report-running.json");
		})).isEmpty();

		setupCall()
			.withServicePath(processesPath(errandId))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-processes-running.json")
			.sendRequest();
	}

	/**
	 * Layer 1. The patch becomes an errand event like any other but writes no row, and the identity of the process is
	 * nowhere among the rows.
	 */
	private void theProcessPatchesTheErrandAskingNotToBeWoken(final String errandId) {
		assertThat(rowsWrittenDuring(() -> patchErrand(errandId, PROCESS_ENGINE, DO_NOT_WAKE))).isEmpty();

		assertThat(outboxRepository.findAll()).extracting(ProcessEventOutboxEntity::getExecutedBy).doesNotContain(PROCESS_SERVICE);
	}

	private void theWorkStepReportsThatTheProcessWaitsForAHandler(final String errandId) {
		assertThat(rowsWrittenDuring(() -> reportAsProcess(errandId, "request-report-waiting.json"))).isEmpty();

		setupCall()
			.withServicePath(processesPath(errandId))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-processes-waiting.json")
			.sendRequest();
	}

	/**
	 * The request is the one the process sent, header and all, and only the identity differs: the header is not honoured
	 * for an ad account, so a handler's change wakes the process however the client sets it. The event asks for no
	 * start, since the process is alive.
	 */
	private void aHandlerSendsTheSamePatchWithTheHeaderKept(final String errandId) {
		final var rows = rowsWrittenDuring(() -> patchErrand(errandId, HANDLER_IDENTITY, DO_NOT_WAKE));

		assertThat(rows).singleElement().satisfies(row -> {
			assertThat(row.getEventType()).isEqualTo("UPDATE");
			assertThat(row.getEventSubType()).isEqualTo("ERRAND");
			assertThat(row.getProcessKey()).isEqualTo(PROCESS_KEY);
			assertThat(row.isStartAllowed()).isFalse();
			assertThat(row.getExecutedBy()).isEqualTo(HANDLER);
		});
		awaitEventReachingTheProcess(rows.getFirst(), event -> event
			.withRequestBody(matchingJsonPath("$.startAllowed", equalTo("false"))));
	}

	/**
	 * A process that forgets the header patches the errand, is woken by its own change and patches again, round after
	 * round. A changed errand is a trigger of the namespace, so here the brake catches it: the loop stops once as many
	 * events as the brake allows have reached the process within its window, those of the handler's writes included. The
	 * rows the loop did write carry the identity of the process.
	 * <p>
	 * The rounds are bounded, so a brake that never trips fails the test.
	 *
	 * @return the number of patches the process sent.
	 */
	private int theProcessForgetsTheHeaderUntilTheBrakeCatchesIt(final String errandId) {
		final var maxEvents = processEngineProperties.loopGuard().maxEventsPerErrand();
		final var deliveredBefore = (int) outboxRepository.count();
		List<ProcessEventOutboxEntity> wakes;
		var rounds = 0;

		do {
			wakes = rowsWrittenDuring(() -> patchErrand(errandId, PROCESS_ENGINE, null));
			awaitEverythingDelivered();
			rounds++;
		} while (!wakes.isEmpty() && rounds <= maxEvents);

		assertThat(wakes).as("the patch after the last event the brake lets through").isEmpty();
		assertThat(rounds).isEqualTo(maxEvents - deliveredBefore + 1);
		assertThat(outboxRepository.findAll()).hasSize(maxEvents)
			.filteredOn(row -> PROCESS_SERVICE.equals(row.getExecutedBy()))
			.hasSize(maxEvents - deliveredBefore);
		wiremock.verify(maxEvents, postRequestedFor(urlPathEqualTo(PW_ALKT_PATH)));

		return rounds;
	}

	/**
	 * Layer 3 now holds back every ordinary change of the errand, a handler's included, and says so once on the errand
	 * however many events it drops. The health of the relay is left alone, and is read both before and after a scheduled
	 * run.
	 */
	private void theTrippedBrakeHoldsBackOrdinaryChanges(final String errandId) {
		assertThat(rowsWrittenDuring(() -> patchErrand(errandId, HANDLER_IDENTITY, null))).isEmpty();

		setupCall()
			.withServicePath(ERRANDS_PATH + "/" + errandId + "/process-activities")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-activities.json")
			.sendRequest();

		setupCall()
			.withServicePath(RELAY_HEALTH_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(HEALTH_RESPONSE_FILE)
			.sendRequest();

		processEventScheduler.relay();

		setupCall()
			.withServicePath(RELAY_HEALTH_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(HEALTH_RESPONSE_FILE)
			.sendRequest();
	}

	/**
	 * A handler's signal to the waiting process passes the brake, and the triggers of the namespace, which do not name
	 * it.
	 */
	private void aHandlerSignalsTheWaitingProcessPastTheBrake(final String errandId) {
		final var rows = rowsWrittenDuring(() -> setupCall()
			.withServicePath(instancePath(errandId) + "/signals")
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest("request-signal.json")
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequest());

		assertThat(rows).singleElement().satisfies(row -> {
			assertThat(row.getEventSubType()).isEqualTo("SIGNAL");
			assertThat(row.getSignalName()).isEqualTo(SIGNAL_NAME);
			assertThat(row.getProcessKey()).isEqualTo(PROCESS_KEY);
			assertThat(row.isStartAllowed()).isFalse();
			assertThat(row.getExecutedBy()).isEqualTo(HANDLER);
		});
		awaitEventReachingTheProcess(rows.getFirst(), event -> event
			.withRequestBody(matchingJsonPath("$.eventSubType", equalTo("SIGNAL")))
			.withRequestBody(matchingJsonPath("$.signalName", equalTo(SIGNAL_NAME))));
	}

	/**
	 * A handler's start passes the brake.
	 * <p>
	 * The process fails first, since a start is only offered for an errand without a live process. An ordinary change
	 * right before the start shows that the brake still holds at that moment, the failure notwithstanding.
	 */
	private void aHandlerStartsTheFailedProcessAgainPastTheBrake(final String errandId) {
		reportAsProcess(errandId, "request-report-failed.json");

		setupCall()
			.withServicePath(processesPath(errandId))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-processes-failed.json")
			.sendRequest();

		assertThat(rowsWrittenDuring(() -> patchErrand(errandId, HANDLER_IDENTITY, null))).isEmpty();

		final var rows = rowsWrittenDuring(() -> setupCall()
			.withServicePath(processesPath(errandId) + "/start")
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequest());

		assertThat(rows).singleElement().satisfies(row -> {
			assertThat(row.getEventSubType()).isEqualTo("PROCESS");
			assertThat(row.getProcessKey()).isEqualTo(PROCESS_KEY);
			assertThat(row.isStartAllowed()).isTrue();
			assertThat(row.getSignalName()).isNull();
			assertThat(row.getExecutedBy()).isEqualTo(HANDLER);
		});
		awaitEventReachingTheProcess(rows.getFirst(), event -> event
			.withRequestBody(matchingJsonPath("$.eventSubType", equalTo("PROCESS")))
			.withRequestBody(matchingJsonPath("$.processKey", equalTo(PROCESS_KEY)))
			.withRequestBody(matchingJsonPath("$.startAllowed", equalTo("true"))));
	}

	/**
	 * Patches the errand the way both the process and the handler do in this test: the same field, the same headers but
	 * for those sent in. Each patch writes a value the errand has not had before, so that every patch writes a revision
	 * and an event.
	 */
	private void patchErrand(final String errandId, final String sentBy, final String triggerProcess) {
		final var call = setupCall()
			.withServicePath(ERRANDS_PATH + "/" + errandId)
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, sentBy)
			.withRequest(PATCH_FILE)
			.withRequestReplacement(ROUND, String.valueOf(++patches))
			.withExpectedResponseStatus(OK);

		ofNullable(triggerProcess).ifPresent(value -> call.withHeader(TRIGGER_PROCESS_HEADER, value));
		call.sendRequest();
	}

	/**
	 * Reports on the work step as pw-alkt does, asking not to be woken by it.
	 */
	private void reportAsProcess(final String errandId, final String reportFile) {
		setupCall()
			.withServicePath(instancePath(errandId))
			.withHttpMethod(PUT)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE)
			.withHeader(TRIGGER_PROCESS_HEADER, DO_NOT_WAKE)
			.withRequest(reportFile)
			.withExpectedResponseStatus(OK)
			.sendRequest();
	}

	private void issueCommand(final Identifier identity, final String errandId, final EventSubType subType, final ProcessCommand command) {
		asCaller(identity, errandId, errand -> eventService.createProcessCommandEvent(EventType.UPDATE, "Kommando till processen", errand, subType, command));
	}

	/**
	 * Runs a write the way a request does: in a transaction, with the identity of the caller and the header asking not to
	 * be woken held for the thread.
	 */
	private void asCaller(final Identifier identity, final String errandId, final Consumer<ErrandEntity> write) {
		Identifier.set(identity);
		setTriggerProcess(DO_NOT_WAKE);
		try {
			new TransactionTemplate(transactionManager).executeWithoutResult(_ -> write.accept(errandsRepository.findById(errandId).orElseThrow()));
		} finally {
			Identifier.remove();
			clearTriggerProcess();
		}
	}

	private List<ProcessEventOutboxEntity> rowsWrittenDuring(final Runnable write) {
		final var before = outboxRepository.findAll().stream().map(ProcessEventOutboxEntity::getId).collect(toSet());

		write.run();

		return outboxRepository.findAll().stream()
			.filter(row -> !before.contains(row.getId()))
			.toList();
	}

	/**
	 * Waits for pw-alkt to have received the event of the row exactly once, holding it to the patterns given.
	 */
	private void awaitEventReachingTheProcess(final ProcessEventOutboxEntity row, final UnaryOperator<RequestPatternBuilder> patterns) {
		final var event = patterns.apply(postRequestedFor(urlPathEqualTo(PW_ALKT_PATH))
			.withRequestBody(matchingJsonPath("$.eventId", equalTo(row.getId()))));

		awaitDelivery().untilAsserted(() -> wiremock.verify(1, event));
	}

	private void awaitEverythingDelivered() {
		awaitDelivery().untilAsserted(() -> assertThat(outboxRepository.findAll()).allSatisfy(row -> assertThat(row.getDeliveredAt()).isNotNull()));
	}

	private static ConditionFactory awaitDelivery() {
		return await().atMost(DELIVERY_TIMEOUT).pollDelay(Duration.ZERO).pollInterval(Duration.ofMillis(20));
	}

	private static RequestPatternBuilder errandChangesLoggedBy(final String executingUser) {
		return postRequestedFor(urlPathMatching("/api-eventlog/.*"))
			.withRequestBody(matchingJsonPath("$.type", equalTo("UPDATE")))
			.withRequestBody(matchingJsonPath("$.subType", equalTo("ERRAND")))
			.withRequestBody(matchingJsonPath("$.executingUser.value", equalTo(executingUser)));
	}
}
