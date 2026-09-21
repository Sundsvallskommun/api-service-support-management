package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcess;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcessReport;
import se.sundsvall.supportmanagement.api.model.process.ProcessActivity;
import se.sundsvall.supportmanagement.api.model.process.ProcessSignal;
import se.sundsvall.supportmanagement.api.model.process.ProcessStartable;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessActivityRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessSignalRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.RevisionRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessSignalEntity;
import se.sundsvall.supportmanagement.integration.db.model.LabelAttributeEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static se.sundsvall.supportmanagement.api.model.process.ProcessError.create;
import static se.sundsvall.supportmanagement.api.model.process.ProcessStartability.AVAILABLE;
import static se.sundsvall.supportmanagement.api.model.process.ProcessStartability.LIVE_INSTANCE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.ERROR;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.RUNNING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.WAITING;

/**
 * The process resource against the migrated schema, where the two unique keys and the errand cascade are real.
 * <p>
 * What can only be asked here is what the database itself decides: that a report leaves the revision table alone, that
 * the race between a work step and the registration of its own start comes out the same in either order, and that the
 * process shown on an errand is read for a whole page in one query rather than once per errand.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
@Transactional
class ErrandProcessPersistenceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "PROCESS-IT";
	private static final String PROCESS_SERVICE = "pw-alkt";
	private static final String PROCESS_KEY = "alkt-ansokan";

	@Autowired
	private ErrandProcessService errandProcessService;

	@Autowired
	private ErrandService errandService;

	@Autowired
	private ErrandsRepository errandsRepository;

	@Autowired
	private ErrandProcessRepository errandProcessRepository;

	@Autowired
	private ErrandProcessActivityRepository errandProcessActivityRepository;

	@Autowired
	private ErrandProcessSignalRepository errandProcessSignalRepository;

	@Autowired
	private RevisionRepository revisionRepository;

	@Autowired
	private MetadataLabelRepository metadataLabelRepository;

	@Autowired
	private NamespaceConfigService namespaceConfigService;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	/**
	 * Every path here reads the configuration of the namespace, since that is what says whether access control applies.
	 * With none, the read answers 404 long before the process is reached.
	 */
	@BeforeEach
	void createNamespaceConfig() {
		namespaceConfigService.create(NamespaceConfig.create()
			.withDisplayName("Process integration")
			.withShortCode("PIT")
			.withAccessControl(false)
			.withNotifyReporter(false)
			.withProcessConsumer(PROCESS_SERVICE)
			.withProcessTriggers(List.of(EventSubType.ERRAND, EventSubType.DECISION))
			.withNotificationTTLInDays(30), NAMESPACE, MUNICIPALITY_ID);

		Identifier.set(Identifier.create().withType(Identifier.Type.CUSTOM).withTypeString("processEngine").withValue(PROCESS_SERVICE));
	}

	@AfterEach
	void clearIdentifier() {
		Identifier.remove();
	}

	// ---------------------------------------------------------------------------------------------------------------
	// The race between a work step and the registration of its own start
	// ---------------------------------------------------------------------------------------------------------------

	@Test
	@DisplayName("Verification that a registration arriving after the work step it belongs to leaves the report of that work step untouched")
	void reportFirstThenRegisterLeavesTheStateAlone() {
		final var errandId = createErrand();
		final var processInstanceId = UUID.randomUUID().toString();

		errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, processInstanceId, report(WAITING).withCurrentActivityId("granska"));

		final var registration = errandProcessService.registerProcess(NAMESPACE, MUNICIPALITY_ID, errandId, report(RUNNING).withProcessInstanceId(processInstanceId));

		assertThat(registration.created()).isFalse();
		assertThat(registration.process().getProcessStatus()).isEqualTo(WAITING.name());
		assertThat(registration.process().getCurrentActivityId()).isEqualTo("granska");
		assertThat(errandProcessRepository.findByProcessInstanceId(processInstanceId)).get()
			.satisfies(entity -> assertThat(entity.getProcessStatus()).isEqualTo(WAITING));
	}

	@Test
	@DisplayName("Verification that the report of a work step updates the row its own registration created")
	void registerFirstThenReportUpdatesTheState() {
		final var errandId = createErrand();
		final var processInstanceId = UUID.randomUUID().toString();

		final var registration = errandProcessService.registerProcess(NAMESPACE, MUNICIPALITY_ID, errandId, report(RUNNING).withProcessInstanceId(processInstanceId));
		final var report = errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, processInstanceId, report(COMPLETED));

		assertThat(registration.created()).isTrue();
		assertThat(report.created()).isFalse();
		assertThat(report.process().getProcessStatus()).isEqualTo(COMPLETED.name());
		assertThat(errandProcessRepository.findByErrandIdAndActiveMarkerIsNotNull(errandId)).isEmpty();
	}

	// ---------------------------------------------------------------------------------------------------------------
	// What the unique keys refuse
	// ---------------------------------------------------------------------------------------------------------------

	@Test
	@DisplayName("Verification that a second live instance is refused rather than surfacing as a constraint violation")
	void aSecondLiveInstanceIsRefused() {
		final var errandId = createErrand();
		errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "first-instance", report(RUNNING));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "second-instance", report(RUNNING)))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(409);
				assertThat(problem.getDetail()).contains("first-instance");
			});
	}

	@Test
	@DisplayName("Verification that an instance of another process than the one the errand already runs is refused")
	void anInstanceOfAnotherProcessIsRefused() {
		final var errandId = createErrand();
		errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "first-instance", report(FAILED));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "second-instance", report(RUNNING).withProcessKey("alkt-tillsyn")))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(409);
				assertThat(problem.getDetail()).contains("already runs a process other than 'alkt-tillsyn'");
			});
	}

	/**
	 * Kept apart from the retry below because a refusal rolls its transaction back, and this test shares one with the
	 * next thing it would do.
	 */
	@Test
	@DisplayName("Verification that a process which ran to its end is never started over")
	void aCompletedProcessLifeIsOver() {
		final var errandId = createErrand();
		errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "completed-instance", report(COMPLETED));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> errandProcessService.registerProcess(NAMESPACE, MUNICIPALITY_ID, errandId, report(RUNNING).withProcessInstanceId("new-instance")))
			.satisfies(problem -> assertThat(problem.getStatus().value()).isEqualTo(409));
	}

	@Test
	@DisplayName("Verification that a start which failed leaves the errand free to try again, since recovering is not a second process")
	void aFailedStartMayBeRetried() {
		final var errandId = createErrand();
		errandProcessService.registerProcess(NAMESPACE, MUNICIPALITY_ID, errandId, report(FAILED).withError(create().withCode("START_FAILED").withMessage("boom")));

		final var retry = errandProcessService.registerProcess(NAMESPACE, MUNICIPALITY_ID, errandId, report(RUNNING).withProcessInstanceId("retry-instance"));

		assertThat(retry.created()).isTrue();
		assertThat(retry.process().getProcessStatus()).isEqualTo(RUNNING.name());
	}

	// ---------------------------------------------------------------------------------------------------------------
	// The revision table
	// ---------------------------------------------------------------------------------------------------------------

	@Test
	@DisplayName("Verification that reporting twice writes no revision, which is what a JPA relation from the errand to its process would silently start doing")
	void twoReportsLeaveTheRevisionTableAlone() {
		final var errandId = createErrand();
		final var revisionsBefore = revisionRepository.count();

		errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "instance", report(RUNNING));
		errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "instance", report(COMPLETED));
		entityManager.flush();

		assertThat(revisionRepository.count()).isEqualTo(revisionsBefore);
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Reading
	// ---------------------------------------------------------------------------------------------------------------

	@Test
	@DisplayName("Verification that the overview lists every process of the errand, newest first, and that a live one stands in the way of a start")
	void theOverviewListsTheProcessesNewestFirst() {
		final var errandId = createErrand();
		errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "older", report(FAILED).withStarted(now(systemDefault()).minusDays(2)));
		errandProcessRepository.findByProcessInstanceId("older").ifPresent(entity -> entity.setCreated(now(systemDefault()).minusDays(2)));
		errandProcessRepository.flush();
		errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "newer", report(RUNNING));

		final var overview = errandProcessService.readProcesses(NAMESPACE, MUNICIPALITY_ID, errandId);

		assertThat(overview.getStartable()).isEqualTo(ProcessStartable.create().withStatus(LIVE_INSTANCE).withProcessKeys(List.of()));
		assertThat(overview.getProcesses())
			.extracting(ErrandProcess::getProcessInstanceId)
			.containsExactly("newer", "older");
	}

	@Test
	@DisplayName("Verification that an errand without a process is offered the key its label names, read from the database, whatever the start mode")
	void anErrandWithoutAProcessIsOfferedTheKeyOfItsLabel() {
		final var errandId = createErrandWearing(processLabel("MANUAL"));
		entityManager.flush();
		entityManager.clear();

		final var overview = errandProcessService.readProcesses(NAMESPACE, MUNICIPALITY_ID, errandId);

		assertThat(overview.getProcesses()).isEmpty();
		assertThat(overview.getStartable()).isEqualTo(ProcessStartable.create().withStatus(AVAILABLE).withProcessKeys(List.of(PROCESS_KEY)));
	}

	@Test
	@DisplayName("Verification that the log carries the entries written before any process existed, and that narrowing it to an instance leaves them out")
	void theLogCarriesInstancelessEntriesUntilItIsNarrowed() {
		final var errandId = createErrand();
		errandProcessActivityRepository.saveAndFlush(ErrandProcessActivityEntity.create()
			.withErrandId(errandId)
			.withActivityType("CONFIG")
			.withSeverity(ERROR)
			.withMessage("Two labels resolve to different process keys")
			.withOccurredAt(now(systemDefault())));
		errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "instance", report(RUNNING)
			.withExternalTaskId("task-1")
			.withActivities(List.of(activity("review_phase"))));

		final var whole = errandProcessService.readProcessActivities(NAMESPACE, MUNICIPALITY_ID, errandId, null, PageRequest.of(0, 50));
		final var narrowed = errandProcessService.readProcessActivities(NAMESPACE, MUNICIPALITY_ID, errandId, "instance", PageRequest.of(0, 50));

		assertThat(whole.getContent())
			.extracting(ProcessActivity::getActivityType, ProcessActivity::getProcessInstanceId)
			.containsExactlyInAnyOrder(
				tuple("CONFIG", null),
				tuple("PHASE", "instance"));
		assertThat(narrowed.getContent())
			.extracting(ProcessActivity::getActivityType)
			.containsExactly("PHASE");
	}

	@Test
	@DisplayName("Verification that a replayed report adds no activity it already wrote, rather than colliding with the idempotency key")
	void aReplayedReportAddsNoDuplicateActivities() {
		final var errandId = createErrand();
		final var report = report(RUNNING).withExternalTaskId("task-1").withActivities(List.of(activity("review_phase")));

		errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "instance", report);
		errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "instance", report);

		assertThat(errandProcessActivityRepository.findByErrandId(errandId, PageRequest.of(0, 50)).getTotalElements()).isEqualTo(1);
	}

	// ---------------------------------------------------------------------------------------------------------------
	// The process field of the errand
	// ---------------------------------------------------------------------------------------------------------------

	@Test
	@DisplayName("Verification that an errand whose start failed shows the failure rather than looking like an errand that never had a process")
	void anErrandWithAFailedStartShowsIt() {
		final var errandId = createErrand();
		errandProcessService.registerProcess(NAMESPACE, MUNICIPALITY_ID, errandId, report(FAILED).withError(create().withCode("START_FAILED").withMessage("Timeout against the process engine")));
		entityManager.flush();
		entityManager.clear();

		final var errand = errandService.readErrand(NAMESPACE, MUNICIPALITY_ID, errandId);

		assertThat(errand.getProcess()).isNotNull();
		assertThat(errand.getProcess().getProcessStatus()).isEqualTo(FAILED.name());
		assertThat(errand.getProcess().getError().getMessage()).isEqualTo("Timeout against the process engine");
	}

	@Test
	@DisplayName("Verification that the processes of a whole page and what they wait for are read in two queries, asked by counting statements rather than by looking at the code")
	void theProcessesOfAPageAndTheirSignalsAreReadInTwoQueries() {
		final var errandIds = List.of(createErrand(), createErrand(), createErrand());
		errandIds.forEach(errandId -> errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "instance-" + errandId, report(WAITING)
			.withAwaitingSignals(List.of(signal("granskning-godkand"), signal("granskning-avvisad")))));
		entityManager.flush();
		entityManager.clear();

		final var statistics = statistics();
		statistics.clear();
		final var processes = errandProcessService.findLatestProcesses(NAMESPACE, MUNICIPALITY_ID, errandIds);

		assertThat(processes.values()).hasSize(3).allSatisfy(process -> assertThat(process.getAwaitingSignals())
			.extracting(ProcessSignal::getName)
			.containsExactly("granskning-godkand", "granskning-avvisad"));
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
	}

	@Test
	@DisplayName("Verification that a page on which no errand has a process costs the one query it always did")
	void aPageWithoutProcessesIsReadInOneQuery() {
		final var errandIds = List.of(createErrand(), createErrand());
		entityManager.flush();
		entityManager.clear();

		final var statistics = statistics();
		statistics.clear();

		assertThat(errandProcessService.findLatestProcesses(NAMESPACE, MUNICIPALITY_ID, errandIds)).isEmpty();
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
	}

	/**
	 * The list view is asked the one question a statement count cannot answer for it: whether the processes of the page
	 * are looked up once or once per errand. Counting statements around the whole read would count the collections of
	 * every errand as well, which are lazy and have always been read one errand at a time - so the answer would say
	 * nothing about this lookup. Counting the executions of the process query alone leaves those out, and answers about
	 * the query that actually reached the database rather than about a method call.
	 */
	@Test
	@DisplayName("Verification that listing errands looks up the processes of the whole page and what they wait for once each, not once per errand")
	void listingErrandsLooksUpTheProcessesOfThePageAndTheirSignalsOnce() {
		final var errandIds = List.of(createErrand(), createErrand(), createErrand());
		errandIds.forEach(errandId -> errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "instance-" + errandId, report(WAITING)
			.withAwaitingSignals(List.of(signal("granskning-godkand")))));
		entityManager.flush();
		entityManager.clear();

		final var statistics = statistics();
		statistics.clear();

		final var page = errandService.findErrands(NAMESPACE, MUNICIPALITY_ID, null, PageRequest.of(0, 50));

		assertThat(page.getContent()).hasSize(3).allSatisfy(errand -> {
			assertThat(errand.getProcess().getProcessStatus()).isEqualTo(WAITING.name());
			assertThat(errand.getProcess().getAwaitingSignals()).extracting(ProcessSignal::getName).containsExactly("granskning-godkand");
		});
		assertThat(queryExecutions(statistics, ErrandProcessEntity.class)).isEqualTo(1);
		assertThat(queryExecutions(statistics, ErrandProcessSignalEntity.class)).isEqualTo(1);
	}

	@Test
	@DisplayName("Verification that listing errands that could be started asks nothing about starting them: one process lookup for the page, and no label or outbox lookup")
	void listingStartableErrandsCostsNoLookupPerErrand() {
		final var label = processLabel("MANUAL");
		final var errandIds = List.of(createErrandWearing(label), createErrandWearing(label), createErrandWearing(label));
		entityManager.flush();
		entityManager.clear();

		final var statistics = statistics();
		statistics.clear();

		final var page = errandService.findErrands(NAMESPACE, MUNICIPALITY_ID, null, PageRequest.of(0, 50));

		assertThat(page.getContent()).extracting(Errand::getId).containsExactlyInAnyOrderElementsOf(errandIds);
		assertThat(queryExecutions(statistics, ErrandProcessEntity.class)).isEqualTo(1);
		assertThat(queryExecutions(statistics, MetadataLabelEntity.class)).isZero();
		assertThat(queryExecutions(statistics, ProcessEventOutboxEntity.class)).isZero();
	}

	// ---------------------------------------------------------------------------------------------------------------
	// What the process waits for from a handler
	// ---------------------------------------------------------------------------------------------------------------

	@Test
	@DisplayName("Verification that every report replaces what the instance waits for, in the order reported, and that an empty one leaves nothing")
	void everyReportReplacesWhatTheInstanceWaitsFor() {
		final var errandId = createErrand();

		errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "instance", report(WAITING)
			.withAwaitingSignals(List.of(signal("granskning-godkand"), signal("granskning-avvisad"))));
		assertThat(awaitedBy(errandId)).containsExactly("granskning-godkand", "granskning-avvisad");

		errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "instance", report(WAITING)
			.withAwaitingSignals(List.of(signal("beslut-fattat"), signal("granskning-avvisad"))));
		assertThat(awaitedBy(errandId)).containsExactly("beslut-fattat", "granskning-avvisad");

		errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "instance", report(WAITING).withAwaitingSignals(List.of()));
		assertThat(awaitedBy(errandId)).isEmpty();
	}

	/**
	 * The trap this guards against sits in the flush: inserts run before deletions, so a signal deleted and inserted
	 * anew in one report would meet its own old row in the unique key and fail the report. Kept rows are what avoids it,
	 * and keeping one is also what shows here - the row keeps its id.
	 */
	@Test
	@DisplayName("Verification that a signal still awaited keeps its row, whatever its label and place become")
	void aSignalStillAwaitedKeepsItsRow() {
		final var errandId = createErrand();

		errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "instance", report(WAITING)
			.withAwaitingSignals(List.of(signal("granskning-avvisad"), signal("granskning-godkand"))));
		entityManager.flush();
		final var rowIdBefore = rowIdOf(errandId, "granskning-godkand");

		errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "instance", report(WAITING)
			.withAwaitingSignals(List.of(signal("beslut-fattat"), signal("granskning-godkand").withLabel("Godkänn"))));
		entityManager.flush();
		entityManager.clear();

		assertThat(rowIdOf(errandId, "granskning-godkand")).isEqualTo(rowIdBefore);
		assertThat(errandProcessService.readProcesses(NAMESPACE, MUNICIPALITY_ID, errandId).getProcesses().getFirst().getAwaitingSignals())
			.containsExactly(signal("beslut-fattat"), signal("granskning-godkand").withLabel("Godkänn"));
	}

	// ---------------------------------------------------------------------------------------------------------------

	private Statistics statistics() {
		final var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.setStatisticsEnabled(true);
		return statistics;
	}

	/**
	 * How many times a query against the table of an entity ran, whatever the query looks like - the point is the count,
	 * and pinning the generated text would break on any rename of the method behind it. Matched on the entity name as a
	 * word, since one entity name can be the start of another.
	 */
	private static long queryExecutions(final Statistics statistics, final Class<?> entity) {
		final var entityName = Pattern.compile("\\b" + entity.getSimpleName() + "\\b");

		return Arrays.stream(statistics.getQueries())
			.filter(query -> entityName.matcher(query).find())
			.mapToLong(query -> statistics.getQueryStatistics(query).getExecutionCount())
			.sum();
	}

	/**
	 * What the instance of an errand waits for, as the database holds it, in the order it was reported.
	 */
	private List<String> awaitedBy(final String errandId) {
		entityManager.flush();

		return errandProcessRepository.findByErrandIdOrderByCreatedDesc(errandId).stream()
			.flatMap(process -> errandProcessSignalRepository.findByErrandProcessIdOrderBySortOrderAsc(process.getId()).stream())
			.map(ErrandProcessSignalEntity::getName)
			.toList();
	}

	private String rowIdOf(final String errandId, final String name) {
		return errandProcessRepository.findByErrandIdOrderByCreatedDesc(errandId).stream()
			.flatMap(process -> errandProcessSignalRepository.findByErrandProcessIdOrderBySortOrderAsc(process.getId()).stream())
			.filter(signal -> signal.getName().equals(name))
			.map(ErrandProcessSignalEntity::getId)
			.findFirst()
			.orElseThrow();
	}

	private static ProcessSignal signal(final String name) {
		return ProcessSignal.create().withName(name);
	}

	private String createErrand() {
		return errandsRepository.saveAndFlush(errand()).getId();
	}

	private String createErrandWearing(final MetadataLabelEntity label) {
		return errandsRepository.saveAndFlush(errand()
			.withLabels(new ArrayList<>(List.of(ErrandLabelEmbeddable.create().withMetadataLabelId(label.getId()))))).getId();
	}

	private static ErrandEntity errand() {
		return ErrandEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandNumber("PPT-" + UUID.randomUUID())
			.withTitle("TITLE")
			.withStatus("STATUS")
			.withPriority("MEDIUM")
			.withReporterUserId("joe01doe");
	}

	private MetadataLabelEntity processLabel(final String startMode) {
		return metadataLabelRepository.saveAndFlush(MetadataLabelEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withClassification("CATEGORY")
			.withResourceName("TILLSYN")
			.withAttributes(new ArrayList<>(List.of(
				LabelAttributeEmbeddable.create().withKey("processKey").withValue(PROCESS_KEY),
				LabelAttributeEmbeddable.create().withKey("processStartMode").withValue(startMode)))));
	}

	private static ErrandProcessReport report(final ProcessStatus status) {
		return ErrandProcessReport.create()
			.withProcessService(PROCESS_SERVICE)
			.withProcessKey(PROCESS_KEY)
			.withProcessStatus(status);
	}

	private static ProcessActivity activity(final String activityId) {
		return ProcessActivity.create()
			.withActivityType("PHASE")
			.withActivityId(activityId)
			.withOccurredAt(now(systemDefault()));
	}
}
