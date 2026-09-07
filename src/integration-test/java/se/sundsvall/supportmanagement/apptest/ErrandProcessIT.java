package se.sundsvall.supportmanagement.apptest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcess;
import se.sundsvall.supportmanagement.api.model.process.ProcessActivity;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessActivityRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.RevisionRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;
import se.sundsvall.supportmanagement.service.ErrandProcessService;
import se.sundsvall.supportmanagement.service.ErrandService;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.tuple;
import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static se.sundsvall.supportmanagement.api.model.process.ProcessError.create;
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
@ActiveProfiles("it")
@Transactional
class ErrandProcessIT {

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

	@MockitoSpyBean
	private ErrandProcessRepository errandProcessRepository;

	@Autowired
	private ErrandProcessActivityRepository errandProcessActivityRepository;

	@Autowired
	private RevisionRepository revisionRepository;

	@Autowired
	private NamespaceConfigRepository namespaceConfigRepository;

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
		if (!namespaceConfigRepository.existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)) {
			namespaceConfigService.create(NamespaceConfig.create()
				.withDisplayName("Process integration")
				.withShortCode("PIT")
				.withAccessControl(false)
				.withNotifyReporter(false)
				.withNotificationTTLInDays(30), NAMESPACE, MUNICIPALITY_ID);
		}
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
		assertThat(registration.process().getProcessStatus()).isEqualTo(WAITING);
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
		assertThat(report.process().getProcessStatus()).isEqualTo(COMPLETED);
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
		errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "first-instance", report(COMPLETED));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "second-instance", report(RUNNING).withProcessKey("alkt-tillsyn")))
			.satisfies(problem -> assertThat(problem.getStatus().value()).isEqualTo(409));
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
		assertThat(retry.process().getProcessStatus()).isEqualTo(RUNNING);
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
	@DisplayName("Verification that the envelope lists every process of the errand, newest first")
	void theEnvelopeListsTheProcessesNewestFirst() {
		final var errandId = createErrand();
		errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "older", report(COMPLETED).withStarted(now(systemDefault()).minusDays(2)));
		errandProcessRepository.findByProcessInstanceId("older").ifPresent(entity -> entity.setCreated(now(systemDefault()).minusDays(2)));
		errandProcessRepository.flush();
		errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "newer", report(RUNNING));

		final var envelope = errandProcessService.readProcesses(NAMESPACE, MUNICIPALITY_ID, errandId);

		assertThat(envelope.getStartable()).isNull();
		assertThat(envelope.getProcesses())
			.extracting(ErrandProcess::getProcessInstanceId)
			.containsExactly("newer", "older");
	}

	@Test
	@DisplayName("Verification that the log carries the entries written before any process existed, and that narrowing it to an instance leaves them out")
	void theLogCarriesInstancelessEntriesUntilItIsNarrowed() {
		final var errandId = createErrand();
		errandProcessActivityRepository.saveAndFlush(ErrandProcessActivityEntity.create()
			.withErrandId(errandId)
			.withActivityType("CONFIG")
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
		assertThat(errand.getProcess().getProcessStatus()).isEqualTo(FAILED);
		assertThat(errand.getProcess().getError().getMessage()).isEqualTo("Timeout against the process engine");
	}

	@Test
	@DisplayName("Verification that the processes of a whole page are read in one query, asked by counting statements rather than by looking at the code")
	void theProcessesOfAPageAreReadInOneQuery() {
		final var errandIds = List.of(createErrand(), createErrand(), createErrand());
		errandIds.forEach(errandId -> errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "instance-" + errandId, report(RUNNING)));
		entityManager.flush();
		entityManager.clear();

		final var statistics = statistics();
		statistics.clear();
		errandProcessService.findLatestProcesses(errandIds);

		assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
	}

	/**
	 * The list view is asked the one question a statement count cannot answer for it: whether the processes of the page
	 * are looked up once or once per errand. Counting statements around the whole read would count the collections of
	 * every errand as well, which are lazy and have always been read one errand at a time - so the answer would say
	 * nothing about this lookup. What the lookup itself costs is settled by the test above.
	 */
	@Test
	@DisplayName("Verification that listing errands looks up the processes of the whole page once, not once per errand")
	void listingErrandsLooksUpTheProcessesOfThePageOnce() {
		final var errandIds = List.of(createErrand(), createErrand(), createErrand());
		errandIds.forEach(errandId -> errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, errandId, "instance-" + errandId, report(RUNNING)));
		entityManager.flush();
		entityManager.clear();
		clearInvocations(errandProcessRepository);

		final var page = errandService.findErrands(NAMESPACE, MUNICIPALITY_ID, null, PageRequest.of(0, 50));

		assertThat(page.getContent()).hasSize(3).allSatisfy(errand -> assertThat(errand.getProcess().getProcessStatus()).isEqualTo(RUNNING));
		verify(errandProcessRepository, times(1)).findByErrandIdInOrderByCreatedDesc(argThat(ids -> ids.containsAll(errandIds)));
	}

	// ---------------------------------------------------------------------------------------------------------------

	private Statistics statistics() {
		final var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.setStatisticsEnabled(true);
		return statistics;
	}

	private String createErrand() {
		return errandsRepository.saveAndFlush(ErrandEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandNumber("PIT-" + UUID.randomUUID())
			.withTitle("TITLE")
			.withStatus("STATUS")
			.withPriority("MEDIUM")
			.withReporterUserId("joe01doe")).getId();
	}

	private static ErrandProcess report(final ProcessStatus status) {
		return ErrandProcess.create()
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
