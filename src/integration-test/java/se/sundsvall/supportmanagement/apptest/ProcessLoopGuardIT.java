package se.sundsvall.supportmanagement.apptest;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import generated.se.sundsvall.eventlog.EventType;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
import se.sundsvall.supportmanagement.integration.db.ErrandProcessActivityRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType;
import se.sundsvall.supportmanagement.service.EventService;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.model.ProcessCommand;
import se.sundsvall.supportmanagement.service.scheduler.processevent.ProcessEventScheduler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.ERROR;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.PROCESS;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SIGNAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.RUNNING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.WAITING;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.TRIGGER_PROCESS_HEADER;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.clearTriggerProcess;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.setTriggerProcess;

/**
 * The whole round between SupportManagement and its process, with WireMock standing in for pw-alkt, and the question
 * the round is run for: do the two services stop waking each other?
 * <p>
 * Two of the three layers of the loop guard leave nothing behind when they work - a row never written looks exactly
 * like an event that never happened. The danger is therefore not a guard that is missing but one that is too wide: a
 * filter silencing more than it should stops the process from being woken, and nobody notices until someone asks why an
 * errand stands still. Every write below is therefore held to having become an errand event in the event log, so that a
 * missing row is always the doing of publication, and every row written is held to reaching pw-alkt.
 * <p>
 * The test plays the part of pw-alkt that calls back: it reads what reached the stub, and answers the way pw-alkt does
 * - registering the start, reporting on its work step and patching the errand. SupportManagement delivers on its own,
 * through the direct run, so the test waits for rows to be delivered rather than for time to pass.
 * <p>
 * Commands are issued through {@link EventService#createProcessCommandEvent}, which is where the start and signal
 * endpoints hand them over. The endpoints and their checks - the 403 for a caller without an ad account among them - are
 * not what this test asks about: the signal endpoint is tried over the wire in {@link ProcessSignalIT}, and the start
 * endpoint is built in a task of its own.
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

	private static final String PROCESS_LABEL_ID = "dd000000-0000-0000-0000-0000000000d2";
	private static final String LABELLED_ERRAND_ID = "aa000000-0000-0000-0000-0000000000a4";
	private static final String PROCESS_KEY = "alkt-ansokan";
	private static final String PROCESS_INSTANCE_ID = "pi-loop-guard";
	private static final String SIGNAL_NAME = "granskning-godkand";

	private static final String PROCESS_SERVICE = "pw-alkt";
	private static final String PROCESS_ENGINE = PROCESS_SERVICE + "; type=processEngine";
	private static final String HANDLER = "joe01doe";
	private static final String HANDLER_IDENTITY = HANDLER + "; type=adAccount";
	private static final String DO_NOT_WAKE = "false";

	private static final Duration DELIVERY_TIMEOUT = Duration.ofSeconds(10);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Autowired
	private ProcessEventOutboxRepository outboxRepository;

	@Autowired
	private ErrandProcessRepository processRepository;

	@Autowired
	private ErrandProcessActivityRepository activityRepository;

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

	@BeforeEach
	void setUp() {
		wiremock.resetAll();
		healthContributor.getOrCreateIndicator(RELAY_JOB).setHealthy();

		wiremock.stubFor(post(urlPathEqualTo("/api-gateway/token"))
			.willReturn(aResponse()
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("common/responses/api-gateway-token-response.json")));
		wiremock.stubFor(post(urlPathMatching("/api-eventlog/.*")).willReturn(aResponse().withStatus(202)));
		wiremock.stubFor(post(urlPathEqualTo(PW_ALKT_PATH)).willReturn(aResponse().withStatus(202)));
	}

	@Test
	@DisplayName("Verification that the round between SupportManagement and its process comes to rest, while neither a handler's change nor a command is silenced on the way")
	void test01_theRoundComesToRest() {
		assertThat(namespaceConfigService.getProcessTriggers(NAMESPACE, MUNICIPALITY_ID))
			.as("the commands have to be left out of the triggers for the test to show that they pass them")
			.containsOnly(ERRAND);

		final var errandId = aHandlerCreatesAnErrandWearingTheProcessLabel();

		theProcessStartsAndItsWorkStepReportsRunning(errandId);
		theProcessPatchesTheErrandAskingNotToBeWoken(errandId);
		theWorkStepReportsThatTheProcessWaits(errandId);
		aHandlerSendsTheSamePatchWithTheHeaderKept(errandId);
		theProcessForgetsTheHeaderUntilTheBrakeCatchesIt(errandId);
		theTrippedBrakeHoldsBackOrdinaryChanges(errandId);
		aHandlerSignalsTheWaitingProcessPastTheBrake(errandId);
		aHandlerStartsTheFailedProcessAgainPastTheBrake(errandId);

		assertThat(wiremock.findAllUnmatchedRequests()).isEmpty();
	}

	/**
	 * Commands pass layer 1 because publication waives it for them, and not merely because only handlers reach them.
	 * <p>
	 * The endpoints are to refuse a caller without an ad account, and the header is not honoured for an ad account, so a
	 * command would get past layer 1 even without a waiver. Leaning on that chain would make the buttons depend on the 403
	 * staying in place: were it lost, a command sent with the header every process engine sets would be silenced rather
	 * than refused, which is the quietest failure there is. Here the commands reach publication exactly that way, and an
	 * ordinary change in the same context shows that layer 1 is armed there.
	 */
	@Test
	@DisplayName("Verification that a command passes layer 1 of its own accord, so the buttons do not lean on the check keeping machines away from them")
	void test02_aCommandPassesLayerOneOfItsOwnAccord() {
		final var processEngine = Identifier.parse(PROCESS_ENGINE);

		assertThat(rowsWrittenDuring(() -> asCaller(processEngine, DO_NOT_WAKE, LABELLED_ERRAND_ID,
			errand -> eventService.createErrandEvent(EventType.UPDATE, "Ärendet har uppdaterats.", errand, null, null, false, ERRAND)))).isEmpty();

		final var signal = rowsWrittenDuring(() -> issueCommand(processEngine, DO_NOT_WAKE, LABELLED_ERRAND_ID, SIGNAL, new ProcessCommand(null, SIGNAL_NAME)));
		final var start = rowsWrittenDuring(() -> issueCommand(processEngine, DO_NOT_WAKE, LABELLED_ERRAND_ID, PROCESS, new ProcessCommand(PROCESS_KEY, null)));

		assertThat(signal).singleElement().satisfies(row -> {
			assertThat(row.getEventSubType()).isEqualTo("SIGNAL");
			assertThat(row.getSignalName()).isEqualTo(SIGNAL_NAME);
		});
		assertThat(start).singleElement().satisfies(row -> {
			assertThat(row.getEventSubType()).isEqualTo("PROCESS");
			assertThat(row.getProcessKey()).isEqualTo(PROCESS_KEY);
			assertThat(row.isStartAllowed()).isTrue();
		});
		awaitDeliveryOf(signal.getFirst());
		awaitDeliveryOf(start.getFirst());
		assertThat(wiremock.findAllUnmatchedRequests()).isEmpty();
	}

	/**
	 * An identity header that cannot be parsed leaves the request without an identity at all. Layer 1 never reads the
	 * identity other than to exempt an ad account, so the process is still obeyed, and the price is only that the row
	 * written without the header names no one.
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
		awaitDeliveryOf(rows.getFirst());
		assertThat(wiremock.findAllUnmatchedRequests()).isEmpty();
	}

	/**
	 * The creation is the event that starts the process: the label says nothing about the start mode, which reads as
	 * automatic, and the errand has never had a process. It is also the one write whose labels have never been read from
	 * the database, and a publication reading only what Hibernate has filled in would find no process on them at all.
	 */
	private String aHandlerCreatesAnErrandWearingTheProcessLabel() {
		final var response = restTemplate.exchange(ERRANDS_PATH, POST, new HttpEntity<>("""
			{
			  "title": "Ansokan om serveringstillstand",
			  "priority": "MEDIUM",
			  "status": "STATUS-1",
			  "reporterUserId": "joe01doe",
			  "classification": {"category": "CATEGORY-1", "type": "TYPE-1"},
			  "labels": [{"id": "%s"}]
			}""".formatted(PROCESS_LABEL_ID), headers(HANDLER_IDENTITY, null)), String.class);

		assertThat(response.getStatusCode()).isEqualTo(CREATED);
		final var location = response.getHeaders().getLocation().getPath();
		final var errandId = location.substring(location.lastIndexOf('/') + 1);

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

		final var event = awaitDeliveryOf(rows.getFirst());
		assertThat(event.path("errandId").asString()).isEqualTo(errandId);
		assertThat(event.path("eventType").asString()).isEqualTo("CREATE");
		assertThat(event.path("processKey").asString()).isEqualTo(PROCESS_KEY);
		assertThat(event.path("startAllowed").booleanValue()).isTrue();

		return errandId;
	}

	/**
	 * What pw-alkt does once the event has started the process: register the start, and have the first work step say that
	 * it is at work. Neither is a change to the errand, and neither wakes anything.
	 */
	private void theProcessStartsAndItsWorkStepReportsRunning(final String errandId) {
		assertThat(rowsWrittenDuring(() -> {
			reportAsProcess(POST, processesPath(errandId), """
				{"processService": "pw-alkt", "processKey": "alkt-ansokan", "processInstanceId": "%s", "processStatus": "RUNNING"}"""
				.formatted(PROCESS_INSTANCE_ID), CREATED);
			reportAsProcess(PUT, instancePath(errandId), workStepReport(RUNNING.name()), OK);
		})).isEmpty();

		assertThat(processRepository.findByErrandIdOrderByCreatedDesc(errandId)).singleElement().satisfies(process -> {
			assertThat(process.getProcessInstanceId()).isEqualTo(PROCESS_INSTANCE_ID);
			assertThat(process.getProcessKey()).isEqualTo(PROCESS_KEY);
			assertThat(process.getProcessStatus()).isEqualTo(RUNNING);
			assertThat(process.getActiveMarker()).isTrue();
		});
		assertThat(readErrand(errandId).path("process").path("processStatus").asString()).isEqualTo("RUNNING");
	}

	/**
	 * Layer 1. The patch became an errand event like any other, so the missing row is the doing of publication - and the
	 * identity of the process is nowhere among the rows, which is how it shows in operation that the header works.
	 */
	private void theProcessPatchesTheErrandAskingNotToBeWoken(final String errandId) {
		assertThat(rowsWrittenDuring(() -> patchErrand(errandId, PROCESS_ENGINE, DO_NOT_WAKE))).isEmpty();

		assertThat(outboxRepository.findAll()).extracting(ProcessEventOutboxEntity::getExecutedBy).doesNotContain(PROCESS_SERVICE);
	}

	private void theWorkStepReportsThatTheProcessWaits(final String errandId) {
		assertThat(rowsWrittenDuring(() -> reportAsProcess(PUT, instancePath(errandId), workStepReport(WAITING.name()), OK))).isEmpty();

		assertThat(processRepository.findByErrandIdOrderByCreatedDesc(errandId)).singleElement().satisfies(process -> {
			assertThat(process.getProcessStatus()).isEqualTo(WAITING);
			assertThat(process.getActiveMarker()).isTrue();
			assertThat(process.getEnded()).isNull();
		});
		assertThat(readErrand(errandId).path("process").path("processStatus").asString()).isEqualTo("WAITING");
	}

	/**
	 * The step that tells a working filter from one that silences everything. The request is the one the process sent,
	 * header and all, and only the identity differs: the header is not honoured for an ad account, so a handler's change
	 * wakes the process however the client sets it. Without that rule anyone could make their writes invisible to the
	 * process. The event asks for no start, since the process is alive.
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
		assertThat(awaitDeliveryOf(rows.getFirst()).path("startAllowed").booleanValue()).isFalse();
	}

	/**
	 * Layer 1 rests on a header the sender sets itself. A process that forgets it patches the errand, is woken by its own
	 * change and patches again, round after round, until layer 2 or 3 catches it. A changed errand is a trigger of the
	 * namespace, so here it is the brake: the loop stops once as many events as the brake allows have reached the
	 * process within its window, those of the handler's writes included. The rows the loop did write carry the identity
	 * of the process, which is how the fault shows in operation.
	 * <p>
	 * The rounds are bounded, so that a brake that never trips fails the test rather than running it for ever.
	 */
	private void theProcessForgetsTheHeaderUntilTheBrakeCatchesIt(final String errandId) {
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
		assertThat(eventsReceivedByProcess()).hasSize(maxEvents);
	}

	/**
	 * Layer 3 now holds back every ordinary change of the errand, a handler's included, and says so once on the errand
	 * however many events it drops. The brake is about one errand and not about the service, so the health of the relay
	 * is left alone.
	 * <p>
	 * The health is read before the scheduled run as well as after it. The run resets the indicator before it judges the
	 * relay, so only the reading before it could show a brake that had marked the service unhealthy.
	 */
	private void theTrippedBrakeHoldsBackOrdinaryChanges(final String errandId) {
		assertThat(rowsWrittenDuring(() -> patchErrand(errandId, HANDLER_IDENTITY, null))).isEmpty();

		assertThat(activityRepository.findByErrandId(errandId, Pageable.unpaged()))
			.filteredOn(entry -> ERROR == entry.getSeverity())
			.singleElement()
			.satisfies(entry -> {
				assertThat(entry.getActivityType()).isEqualTo("LOOP_GUARD");
				assertThat(entry.getErrorCode()).isEqualTo("EVENT_RATE_EXCEEDED");
				assertThat(entry.getErrandProcessId()).isNull();
			});

		assertThat(relayHealth()).isEqualTo("UP");

		processEventScheduler.relay();

		assertThat(relayHealth()).isEqualTo("UP");
	}

	/**
	 * A signal is a handler pressing a button at a gate the process waits at, not something that happened to the errand.
	 * It passes the brake, and the triggers of the namespace, which do not name it.
	 */
	private void aHandlerSignalsTheWaitingProcessPastTheBrake(final String errandId) {
		final var rows = rowsWrittenDuring(() -> issueCommand(Identifier.parse(HANDLER_IDENTITY), null, errandId, SIGNAL, new ProcessCommand(null, SIGNAL_NAME)));

		assertThat(rows).singleElement().satisfies(row -> {
			assertThat(row.getEventSubType()).isEqualTo("SIGNAL");
			assertThat(row.getSignalName()).isEqualTo(SIGNAL_NAME);
			assertThat(row.getProcessKey()).isEqualTo(PROCESS_KEY);
			assertThat(row.isStartAllowed()).isFalse();
			assertThat(row.getExecutedBy()).isEqualTo(HANDLER);
		});

		final var event = awaitDeliveryOf(rows.getFirst());
		assertThat(event.path("eventSubType").asString()).isEqualTo("SIGNAL");
		assertThat(event.path("signalName").asString()).isEqualTo(SIGNAL_NAME);
	}

	/**
	 * The button that must never be swallowed. The brake lies before the triggers, so without the exception an errand with
	 * lively traffic would take the handler's start command, answer 202 and start nothing - on exactly the errands with
	 * the most to do.
	 * <p>
	 * The process fails first, since a start is only offered for an errand without a live process, and starting again
	 * after a failure is what the button is for. An ordinary change right before the command shows that the brake still
	 * holds at that moment, the failure notwithstanding.
	 */
	private void aHandlerStartsTheFailedProcessAgainPastTheBrake(final String errandId) {
		reportAsProcess(PUT, instancePath(errandId), """
			{"processService": "pw-alkt", "processKey": "alkt-ansokan", "processStatus": "FAILED", "error": {"code": "INCIDENT", "message": "The work step gave up"}}""", OK);

		assertThat(processRepository.findByErrandIdOrderByCreatedDesc(errandId)).singleElement().satisfies(process -> {
			assertThat(process.getProcessStatus()).isEqualTo(FAILED);
			assertThat(process.getActiveMarker()).isNull();
		});
		assertThat(rowsWrittenDuring(() -> patchErrand(errandId, HANDLER_IDENTITY, null))).isEmpty();

		final var rows = rowsWrittenDuring(() -> issueCommand(Identifier.parse(HANDLER_IDENTITY), null, errandId, PROCESS, new ProcessCommand(PROCESS_KEY, null)));

		assertThat(rows).singleElement().satisfies(row -> {
			assertThat(row.getEventSubType()).isEqualTo("PROCESS");
			assertThat(row.getProcessKey()).isEqualTo(PROCESS_KEY);
			assertThat(row.isStartAllowed()).isTrue();
			assertThat(row.getSignalName()).isNull();
			assertThat(row.getExecutedBy()).isEqualTo(HANDLER);
		});

		final var event = awaitDeliveryOf(rows.getFirst());
		assertThat(event.path("eventSubType").asString()).isEqualTo("PROCESS");
		assertThat(event.path("processKey").asString()).isEqualTo(PROCESS_KEY);
		assertThat(event.path("startAllowed").booleanValue()).isTrue();
	}

	/**
	 * Patches the errand the way both the process and the handler do in this test: the same field, the same headers but
	 * for those sent in. Each patch writes a value the errand has not had before, since a patch that changes nothing
	 * normally writes no revision and no event, and would leave no row for a reason that has nothing to do with the loop
	 * guard.
	 * That the change became an event, made by the sender, is checked here for every patch.
	 */
	private void patchErrand(final String errandId, final String sentBy, final String triggerProcess) {
		final var body = """
			{"description": "Looked at in round %d"}""".formatted(++patches);

		final var logged = requestsMadeDuring(postRequestedFor(urlPathEqualTo("/api-eventlog/" + MUNICIPALITY_ID + "/" + errandId)),
			() -> assertThat(restTemplate.exchange(ERRANDS_PATH + "/" + errandId, PATCH, new HttpEntity<>(body, headers(sentBy, triggerProcess)), String.class).getStatusCode()).isEqualTo(OK));

		assertThat(logged).singleElement().satisfies(event -> {
			assertThat(event.path("type").asString()).isEqualTo("UPDATE");
			assertThat(event.path("subType").asString()).isEqualTo("ERRAND");
			assertThat(event.at("/executingUser/value").stringValueOpt()).isEqualTo(ofNullable(Identifier.parse(sentBy)).map(Identifier::getValue));
		});
	}

	private void reportAsProcess(final HttpMethod method, final String path, final String report, final HttpStatus expectedStatus) {
		assertThat(restTemplate.exchange(path, method, new HttpEntity<>(report, headers(PROCESS_ENGINE, DO_NOT_WAKE)), String.class).getStatusCode())
			.isEqualTo(expectedStatus);
	}

	private static String workStepReport(final String processStatus) {
		return """
			{
			  "processService": "pw-alkt",
			  "processKey": "alkt-ansokan",
			  "processStatus": "%s",
			  "currentActivityId": "granska-ansokan",
			  "currentActivityName": "Granska ansokan",
			  "externalTaskId": "task-granska-ansokan"
			}""".formatted(processStatus);
	}

	private void issueCommand(final Identifier identity, final String triggerProcess, final String errandId, final EventSubType subType, final ProcessCommand command) {
		asCaller(identity, triggerProcess, errandId,
			errand -> eventService.createProcessCommandEvent(EventType.UPDATE, "Kommando till processen", errand, false, subType, command));
	}

	/**
	 * Runs a write the way a request does: in a transaction, with the identity and the header of the caller held for the
	 * thread.
	 */
	private void asCaller(final Identifier identity, final String triggerProcess, final String errandId, final Consumer<ErrandEntity> write) {
		Identifier.set(identity);
		setTriggerProcess(triggerProcess);
		try {
			new TransactionTemplate(transactionManager).executeWithoutResult(_ -> write.accept(errandsRepository.findById(errandId).orElseThrow()));
		} finally {
			Identifier.remove();
			clearTriggerProcess();
		}
	}

	private JsonNode readErrand(final String errandId) {
		final var response = restTemplate.getForEntity(ERRANDS_PATH + "/" + errandId, String.class);

		assertThat(response.getStatusCode()).isEqualTo(OK);
		return OBJECT_MAPPER.readTree(response.getBody());
	}

	private List<ProcessEventOutboxEntity> rowsWrittenDuring(final Runnable write) {
		final var before = outboxRepository.findAll().stream().map(ProcessEventOutboxEntity::getId).collect(toSet());

		write.run();

		return outboxRepository.findAll().stream()
			.filter(row -> !before.contains(row.getId()))
			.toList();
	}

	/**
	 * The bodies of the requests matching the pattern that reached WireMock while the call ran, told apart by id rather
	 * than by time, since two requests can be logged within the same millisecond.
	 */
	private List<JsonNode> requestsMadeDuring(final RequestPatternBuilder pattern, final Runnable call) {
		final var before = wiremock.findAll(pattern).stream().map(LoggedRequest::getId).collect(toSet());

		call.run();

		return wiremock.findAll(pattern).stream()
			.filter(request -> !before.contains(request.getId()))
			.map(request -> OBJECT_MAPPER.readTree(request.getBodyAsString()))
			.toList();
	}

	/**
	 * Waits for the direct run to deliver the row, and answers with the one event pw-alkt received for it.
	 */
	private JsonNode awaitDeliveryOf(final ProcessEventOutboxEntity row) {
		awaitDelivery().untilAsserted(() -> assertThat(outboxRepository.findById(row.getId()))
			.get()
			.extracting(ProcessEventOutboxEntity::getDeliveredAt)
			.isNotNull());

		final var received = eventsReceivedByProcess().stream()
			.filter(event -> row.getId().equals(event.path("eventId").asString()))
			.toList();

		assertThat(received).hasSize(1);
		return received.getFirst();
	}

	private void awaitEverythingDelivered() {
		awaitDelivery().untilAsserted(() -> assertThat(outboxRepository.findAll()).allSatisfy(row -> assertThat(row.getDeliveredAt()).isNotNull()));
	}

	private static ConditionFactory awaitDelivery() {
		return await().atMost(DELIVERY_TIMEOUT).pollDelay(Duration.ZERO).pollInterval(Duration.ofMillis(20));
	}

	private List<JsonNode> eventsReceivedByProcess() {
		return wiremock.findAll(postRequestedFor(urlPathEqualTo(PW_ALKT_PATH))).stream()
			.map(request -> OBJECT_MAPPER.readTree(request.getBodyAsString()))
			.toList();
	}

	private String relayHealth() {
		return healthContributor.getOrCreateIndicator(RELAY_JOB).health().getStatus().getCode();
	}

	private static String processesPath(final String errandId) {
		return ERRANDS_PATH + "/" + errandId + "/processes";
	}

	private static String instancePath(final String errandId) {
		return processesPath(errandId) + "/" + PROCESS_INSTANCE_ID;
	}

	private static HttpHeaders headers(final String sentBy, final String triggerProcess) {
		final var headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		ofNullable(sentBy).ifPresent(value -> headers.add(SENT_BY_HEADER, value));
		ofNullable(triggerProcess).ifPresent(value -> headers.add(TRIGGER_PROCESS_HEADER, value));
		return headers;
	}
}
