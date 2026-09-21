package se.sundsvall.supportmanagement.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcess;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcessOverview;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcessReport;
import se.sundsvall.supportmanagement.api.model.process.ProcessActivity;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessActivityRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessSignalRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessSignalEntity;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.model.ErrandProcessResult;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.WARN;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.RUNNING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource.PROCESS;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource.PROCESS_ACTIVITY;
import static se.sundsvall.supportmanagement.service.ProcessCommandService.startOptionsOf;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toErrandProcess;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toErrandProcessActivityEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toErrandProcessEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toErrandProcesses;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toProcessActivity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toProcessStartable;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toProcessStatus;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.updateErrandProcessEntity;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getExecutingUser;

/**
 * The state a process reports about an errand, and the reading of it.
 * <p>
 * Two write paths lead here: reporting on an instance creates the row or updates it, while registering a start only
 * ever creates one. A report may arrive before the registration of the start it belongs to, and neither order is an
 * error.
 */
@Service
public class ErrandProcessService {

	private static final String INSTANCE_ID_MISMATCH = "The process instance id '%s' in the body differs from '%s' in the path";
	private static final String MISSING_INSTANCE_ID = "A process instance id is required unless the report registers a start that failed";
	private static final String INSTANCE_ON_OTHER_ERRAND = "The process instance '%s' belongs to another errand and cannot be reported on errand '%s'";
	private static final String OTHER_LIVE_INSTANCE = "The errand '%s' already has a live process instance '%s' and cannot be given another one";
	private static final String OTHER_PROCESS_KEY = "The errand '%s' already runs a process other than '%s', and every instance of an errand runs the same process";
	private static final String PROCESS_LIFE_OVER = "The errand '%s' has a process that ran to its end, and a completed process is never started again";
	static final String NO_PROCESS_CONSUMER = "The namespace '%s' in municipality '%s' has no process consumer configured and runs no process";
	private static final String WRONG_PROCESS_CONSUMER = "The process service '%s' is not the process consumer of namespace '%s', which is '%s'";
	private static final String MISSING_IDENTIFIER = "A report must carry the identifier of its sender in the '%s' header, since it is what the activity log and the notification of the errand name as the author";
	private static final String ERRAND_CHANGED = "The errand has changed since version %s, which the report says it was read at";

	private static final String CONCURRENCY_ACTIVITY_TYPE = "CONCURRENCY";
	private static final String CONCURRENT_TASKS_DETECTED = """
		concurrent external tasks detected: task '%s' reported RUNNING while task '%s' was still working. Two \
		branches of the process instance are changing the errand at once - take the parallel gateway out of the \
		model, or leave the errand writes to one branch alone. Until then the two keep knocking each other out \
		with 412 and neither of them finishes""";
	private static final String CONCURRENT_TASKS_ERROR_CODE = "CONCURRENT_EXTERNAL_TASKS";

	private static final Logger LOG = LoggerFactory.getLogger(ErrandProcessService.class);

	private final ErrandProcessRepository processRepository;
	private final ErrandProcessActivityRepository activityRepository;
	private final ErrandProcessSignalRepository signalRepository;
	private final AccessControlService accessControlService;
	private final NamespaceConfigService namespaceConfigService;
	private final ProcessKeySelector processKeySelector;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;

	public ErrandProcessService(
		final ErrandProcessRepository processRepository,
		final ErrandProcessActivityRepository activityRepository,
		final ErrandProcessSignalRepository signalRepository,
		final AccessControlService accessControlService,
		final NamespaceConfigService namespaceConfigService,
		final ProcessKeySelector processKeySelector,
		final PlatformTransactionManager transactionManager,
		final Clock clock) {

		this.processRepository = processRepository;
		this.activityRepository = activityRepository;
		this.signalRepository = signalRepository;
		this.accessControlService = accessControlService;
		this.namespaceConfigService = namespaceConfigService;
		this.processKeySelector = processKeySelector;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.clock = clock;
	}

	/**
	 * Takes the report of a work step, creating the row for the instance if this is the first word about it.
	 *
	 * @param  namespace         the namespace of the errand.
	 * @param  municipalityId    the municipality of the errand.
	 * @param  errandId          the errand the process runs for.
	 * @param  processInstanceId the instance being reported on, taken from the path.
	 * @param  report            what the process reported.
	 * @return                   the state of the process after the report, and whether the report created it.
	 */
	public ErrandProcessResult reportProcess(final String namespace, final String municipalityId, final String errandId, final String processInstanceId, final ErrandProcessReport report) {
		if (nonNull(report.getProcessInstanceId()) && !processInstanceId.equals(report.getProcessInstanceId())) {
			throw Problem.valueOf(BAD_REQUEST, INSTANCE_ID_MISMATCH.formatted(report.getProcessInstanceId(), processInstanceId));
		}

		verifySenderOfReport(namespace, municipalityId, report);

		return writeWithCollisionRecovery(errandId, processInstanceId, () -> reportInTransaction(namespace, municipalityId, errandId, processInstanceId, report));
	}

	private ErrandProcessResult reportInTransaction(final String namespace, final String municipalityId, final String errandId, final String processInstanceId, final ErrandProcessReport report) {
		verifyErrandVersion(lockErrandForWriting(namespace, municipalityId, errandId), report);

		final var existing = processRepository.findByProcessInstanceId(processInstanceId).orElse(null);

		if (isNull(existing)) {
			return createProcess(namespace, municipalityId, errandId, processInstanceId, report);
		}

		verifyBelongsToErrand(existing, errandId);
		verifySameProcessKey(existing, report);

		// Asked of a row that already exists as well, because a terminal one reporting itself alive again - an incident
		// resolved by hand - asks for the place it gave up when it ended, and may find it taken. The unique key would
		// refuse that too, but only the check can say which instance is standing in the way.
		verifyNoOtherLiveInstance(processRepository.findByErrandIdOrderByCreatedDesc(errandId), errandId, processInstanceId, report);

		final var displaced = trackOutstandingTask(existing, report);

		updateErrandProcessEntity(existing, report, clock);
		final var saved = processRepository.saveAndFlush(existing);

		if (nonNull(displaced)) {
			logConcurrentTasks(saved, errandId, report.getExternalTaskId(), displaced);
		}

		storeActivities(saved, errandId, report);

		final var signals = replaceAwaitingSignals(saved, signalRepository.findByErrandProcessIdOrderBySortOrderAsc(saved.getId()), report);

		return new ErrandProcessResult(toErrandProcess(saved, signals), false);
	}

	/**
	 * Registers a start, whether it produced an instance or failed to.
	 * <p>
	 * Never updates: an instance already registered is answered with what it says right now, and nothing is touched.
	 *
	 * @param  namespace      the namespace of the errand.
	 * @param  municipalityId the municipality of the errand.
	 * @param  errandId       the errand the process was started for.
	 * @param  report         the start being registered.
	 * @return                the state of the process, and whether the registration created it.
	 */
	public ErrandProcessResult registerProcess(final String namespace, final String municipalityId, final String errandId, final ErrandProcessReport report) {
		final var processInstanceId = report.getProcessInstanceId();

		if (isNull(processInstanceId) && FAILED != toProcessStatus(report)) {
			throw Problem.valueOf(BAD_REQUEST, MISSING_INSTANCE_ID);
		}

		verifySenderOfReport(namespace, municipalityId, report);

		return writeWithCollisionRecovery(errandId, processInstanceId, () -> registerInTransaction(namespace, municipalityId, errandId, processInstanceId, report));
	}

	private ErrandProcessResult registerInTransaction(final String namespace, final String municipalityId, final String errandId, final String processInstanceId, final ErrandProcessReport report) {
		verifyErrandVersion(lockErrandForWriting(namespace, municipalityId, errandId), report);

		return ofNullable(processInstanceId)
			.flatMap(processRepository::findByProcessInstanceId)
			.map(existing -> {
				verifyBelongsToErrand(existing, errandId);
				return new ErrandProcessResult(toErrandProcess(existing, signalRepository.findByErrandProcessIdOrderBySortOrderAsc(existing.getId())), false);
			})
			.orElseGet(() -> createProcess(namespace, municipalityId, errandId, processInstanceId, report));
	}

	/**
	 * Creates the row for an instance this service has not seen, from either write path.
	 * <p>
	 * Both paths are held to the rules of a single process per errand, the first report of a work step as much as the
	 * registration of a start, and a report breaking them is refused with 409.
	 */
	private ErrandProcessResult createProcess(final String namespace, final String municipalityId, final String errandId, final String processInstanceId, final ErrandProcessReport report) {
		final var instances = processRepository.findByErrandIdOrderByCreatedDesc(errandId);

		verifyProcessLifeNotOver(instances, errandId);
		verifySameProcessKeyAsErrand(instances, errandId, report.getProcessKey());
		verifyNoOtherLiveInstance(instances, errandId, processInstanceId, report);

		final var entity = toErrandProcessEntity(namespace, municipalityId, errandId, processInstanceId, report);
		entity.applyStatus(toProcessStatus(report), clock);
		trackOutstandingTask(entity, report);

		final var saved = processRepository.saveAndFlush(entity);
		storeActivities(saved, errandId, report);

		return new ErrandProcessResult(toErrandProcess(saved, replaceAwaitingSignals(saved, emptyList(), report)), true);
	}

	/**
	 * Every process an errand has had, newest first, together with whether a new one may be started right now.
	 * <p>
	 * Whether one may be started is answered by {@link ProcessCommandService#startOptionsOf}, from the process rows read
	 * for the list. The labels of the errand are read only when no process row stands in the way.
	 *
	 * @param  namespace      the namespace of the errand.
	 * @param  municipalityId the municipality of the errand.
	 * @param  errandId       the errand to read.
	 * @return                the processes of the errand, and whether one may be started.
	 */
	@Transactional(readOnly = true)
	public ErrandProcessOverview readProcesses(final String namespace, final String municipalityId, final String errandId) {
		final var errand = accessControlService.getErrand(namespace, municipalityId, errandId, false, PROCESS, R);
		final var instances = processRepository.findByErrandIdOrderByCreatedDesc(errandId);
		final var runsProcesses = namespaceConfigService.getProcessConsumer(namespace, municipalityId).isPresent();

		return ErrandProcessOverview.create()
			.withStartable(toProcessStartable(startOptionsOf(runsProcesses, instances, () -> processKeySelector.select(errand))))
			.withProcesses(toErrandProcesses(instances, signalsOf(instances)));
	}

	/**
	 * The activity log of an errand.
	 * <p>
	 * The log of the errand includes the entries that belong to no instance, such as those explaining why no process ever
	 * started. Narrowing to an instance leaves those entries out, and narrowing to an instance the errand never had
	 * returns an empty page.
	 *
	 * @param  namespace         the namespace of the errand.
	 * @param  municipalityId    the municipality of the errand.
	 * @param  errandId          the errand to read.
	 * @param  processInstanceId the instance to narrow the reading to, or null for the whole log of the errand.
	 * @param  pageable          the page to read.
	 * @return                   the entries of the errand.
	 */
	@Transactional(readOnly = true)
	public Page<ProcessActivity> readProcessActivities(final String namespace, final String municipalityId, final String errandId, final String processInstanceId, final Pageable pageable) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, PROCESS_ACTIVITY, R);

		if (isNull(processInstanceId)) {
			final var page = activityRepository.findByErrandId(errandId, pageable);
			final var processInstanceIds = processInstanceIdsOf(page.getContent());
			return page.map(entity -> toProcessActivity(entity, processInstanceIds));
		}

		return processRepository.findByProcessInstanceIdAndErrandId(processInstanceId, errandId)
			.map(instance -> activityRepository.findByErrandIdAndErrandProcessId(errandId, instance.getId(), pageable)
				.map(entity -> toProcessActivity(entity, Map.of(instance.getId(), processInstanceId))))
			.orElseGet(() -> Page.empty(pageable));
	}

	/**
	 * The latest process of every one of the given errands, which is what the {@code process} field of an errand shows.
	 * The latest process is returned whether it is live or not, a start that failed and a completed process included.
	 * <p>
	 * Reads the processes of all the errands in one query, and the signals of the latest ones in one more; no signals are
	 * read when none of the errands has a process. Only the newest row per errand is kept, before the signals are read.
	 *
	 * @param  namespace      the namespace of the errands.
	 * @param  municipalityId the municipality of the errands.
	 * @param  errandIds      the errands to read the process of.
	 * @return                the latest process per errand id, holding no entry for an errand that has none.
	 */
	@Transactional(readOnly = true)
	public Map<String, ErrandProcess> findLatestProcesses(final String namespace, final String municipalityId, final Collection<String> errandIds) {
		if (isNull(errandIds) || errandIds.isEmpty()) {
			return emptyMap();
		}

		final var latestPerErrand = new LinkedHashMap<String, ErrandProcessEntity>();

		processRepository.findByErrandIdInAndMunicipalityIdAndNamespaceOrderByCreatedDesc(errandIds, municipalityId, namespace)
			.forEach(entity -> latestPerErrand.putIfAbsent(entity.getErrandId(), entity));

		final var signals = signalsOf(latestPerErrand.values());
		final var processes = new LinkedHashMap<String, ErrandProcess>();

		latestPerErrand.forEach((errandId, entity) -> processes.put(errandId, toErrandProcess(entity, signals.getOrDefault(entity.getId(), emptyList()))));

		return processes;
	}

	/**
	 * The signals of every instance sent in, read in one query for all of them and keyed by the id of the process row.
	 * Instances waiting for no one have no entry, and no instances at all ask nothing of the database.
	 */
	private Map<String, List<ErrandProcessSignalEntity>> signalsOf(final Collection<ErrandProcessEntity> instances) {
		if (instances.isEmpty()) {
			return emptyMap();
		}

		return signalRepository.findByErrandProcessIdInOrderBySortOrderAsc(instances.stream().map(ErrandProcessEntity::getId).toList()).stream()
			.collect(Collectors.groupingBy(ErrandProcessSignalEntity::getErrandProcessId));
	}

	/**
	 * Runs a write, and recovers from losing a race for one of the unique keys.
	 * <p>
	 * The write runs in a transaction of its own. When it violates an integrity constraint, it is logged and run once
	 * more in a new transaction, which reads before it writes exactly as the first attempt did: a row that appeared
	 * meanwhile sends the report down the update path, and one that stands in the way is refused with a 409 naming it. A
	 * violation in the second attempt is raised as it is, not as a conflict.
	 */
	private ErrandProcessResult writeWithCollisionRecovery(final String errandId, final String processInstanceId, final Supplier<ErrandProcessResult> attempt) {
		try {
			return transactionTemplate.execute(_ -> attempt.get());
		} catch (final DataIntegrityViolationException lostTheRace) {
			LOG.warn("Retrying the write for process instance '{}' on errand '{}' after an integrity violation", processInstanceId, errandId, lostTheRace);

			return transactionTemplate.execute(_ -> attempt.get());
		}
	}

	/**
	 * Takes the write lock on the errand, which serialises the reports of one errand against each other, and hands the
	 * errand back as read behind the lock. Its version is what a report claiming to have read the errand is held against.
	 */
	private ErrandEntity lockErrandForWriting(final String namespace, final String municipalityId, final String errandId) {
		return accessControlService.getErrand(namespace, municipalityId, errandId, true, PROCESS, RW);
	}

	/**
	 * Checks a report against the process configuration of the namespace before anything is written.
	 * <p>
	 * Answers 400 when the {@code X-Sent-By} header is missing, when the namespace has no process consumer, and when the
	 * report names another process service than the process consumer of the namespace.
	 */
	private void verifySenderOfReport(final String namespace, final String municipalityId, final ErrandProcessReport report) {
		if (isNull(getExecutingUser())) {
			throw Problem.valueOf(BAD_REQUEST, MISSING_IDENTIFIER.formatted(SENT_BY_HEADER));
		}

		final var consumer = namespaceConfigService.getProcessConsumer(namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, NO_PROCESS_CONSUMER.formatted(namespace, municipalityId)));

		if (!consumer.equals(report.getProcessService())) {
			throw Problem.valueOf(BAD_REQUEST, WRONG_PROCESS_CONSUMER.formatted(report.getProcessService(), namespace, consumer));
		}
	}

	/**
	 * Refuses an instance registered on another errand with 409, without naming the other errand.
	 */
	private static void verifyBelongsToErrand(final ErrandProcessEntity entity, final String errandId) {
		if (!errandId.equals(entity.getErrandId())) {
			throw Problem.valueOf(CONFLICT, INSTANCE_ON_OTHER_ERRAND.formatted(entity.getProcessInstanceId(), errandId));
		}
	}

	private static void verifySameProcessKey(final ErrandProcessEntity entity, final ErrandProcessReport report) {
		if (!entity.getProcessKey().equals(report.getProcessKey())) {
			throw Problem.valueOf(CONFLICT, OTHER_PROCESS_KEY.formatted(entity.getErrandId(), report.getProcessKey()));
		}
	}

	private static void verifySameProcessKeyAsErrand(final List<ErrandProcessEntity> instances, final String errandId, final String processKey) {
		if (instances.stream().anyMatch(instance -> !instance.getProcessKey().equals(processKey))) {
			throw Problem.valueOf(CONFLICT, OTHER_PROCESS_KEY.formatted(errandId, processKey));
		}
	}

	/**
	 * Refuses a new instance with 409 on an errand whose process has run to its end. An errand whose process failed may
	 * still be given a new instance.
	 */
	private static void verifyProcessLifeNotOver(final List<ErrandProcessEntity> instances, final String errandId) {
		if (hasCompletedProcess(instances)) {
			throw Problem.valueOf(CONFLICT, PROCESS_LIFE_OVER.formatted(errandId));
		}
	}

	/**
	 * Whether the process life of an errand is over.
	 * <p>
	 * Once it is, a process is never started for the errand again, and the decisions of the errand can no longer be
	 * changed. A failed process leaves the life open.
	 *
	 * @param  instances the process rows of the errand, as {@link ErrandProcessRepository#findByErrandIdOrderByCreatedDesc}
	 *                   reads them.
	 * @return           true when any of them ran to its end.
	 */
	public static boolean hasCompletedProcess(final List<ErrandProcessEntity> instances) {
		return instances.stream().anyMatch(instance -> COMPLETED == instance.getProcessStatus());
	}

	/**
	 * Refuses a second live instance with 409, which is the rule {@code uq_ep_one_active_per_errand} holds. Only a report
	 * that would leave its row live is checked, so a terminal report - a start that failed among them - is taken while
	 * another instance lives on.
	 */
	private static void verifyNoOtherLiveInstance(final List<ErrandProcessEntity> instances, final String errandId, final String processInstanceId, final ErrandProcessReport report) {
		if (toProcessStatus(report).isTerminal()) {
			return;
		}

		instances.stream()
			.filter(instance -> nonNull(instance.getActiveMarker()))
			.filter(instance -> !Objects.equals(instance.getProcessInstanceId(), processInstanceId))
			.findFirst()
			.ifPresent(live -> {
				throw Problem.valueOf(CONFLICT, OTHER_LIVE_INSTANCE.formatted(errandId, live.getProcessInstanceId()));
			});
	}

	/**
	 * Refuses a report whose picture of the errand has gone stale, with 412 when the report carries an errand version
	 * other than the current one.
	 * <p>
	 * This is the If-Match of a work step that reads the errand and then acts outside this service. The version is
	 * optional: a report that carries none is held against nothing.
	 * <p>
	 * Answered before anything is written, so a refused report leaves neither state nor activities behind, only a line
	 * logged at info level.
	 */
	private void verifyErrandVersion(final ErrandEntity errand, final ErrandProcessReport report) {
		final var readVersion = report.getErrandVersion();

		if (isNull(readVersion) || readVersion.equals(errand.getVersion())) {
			return;
		}

		LOG.info("Report on errand '{}' was read at version {}, which the errand has since left behind at {}",
			errand.getId(), readVersion, errand.getVersion());

		throw Problem.valueOf(PRECONDITION_FAILED, ERRAND_CHANGED.formatted(readVersion));
	}

	/**
	 * Notes which external task is working on the instance right now, and answers whether another one already was.
	 * <p>
	 * A work step announces itself with RUNNING and reports again when it is done, so the place is taken between those
	 * two reports and empty the rest of the time. Two tasks that meet here are two branches of the same instance running
	 * at once.
	 * <p>
	 * Only the task standing in the place empties it. A report from another task that does not announce itself, or a
	 * report naming no task at all - the registration of a start among them - leaves the place as it found it.
	 *
	 * @return the task that was already working when this report came in, or null if the place was free.
	 */
	private String trackOutstandingTask(final ErrandProcessEntity entity, final ErrandProcessReport report) {
		final var reporting = report.getExternalTaskId();
		final var outstanding = entity.getOutstandingExternalTaskId();

		if (isNull(reporting)) {
			return null;
		}

		if (RUNNING == toProcessStatus(report) && !reporting.equals(outstanding)) {
			entity.setOutstandingExternalTaskId(reporting);
			return outstanding;
		}

		if (reporting.equals(outstanding)) {
			entity.setOutstandingExternalTaskId(null);
		}

		return null;
	}

	/**
	 * Records that two work steps were running at once, and takes the report anyway.
	 * <p>
	 * Every occurrence is logged as a warning, while the entry in the activity log of the errand is written once per
	 * instance. The entry names both tasks, says what to do about them and carries an error code for an alert to be built
	 * on. It is written without an activity id, so {@code uq_epa_idempotency}, which counts null as distinct, never
	 * refuses it.
	 */
	private void logConcurrentTasks(final ErrandProcessEntity process, final String errandId, final String externalTaskId, final String displacedTaskId) {
		LOG.warn("Concurrent external tasks on process instance '{}' of errand '{}': task '{}' reported RUNNING while task '{}' was still working",
			process.getProcessInstanceId(), errandId, externalTaskId, displacedTaskId);

		if (activityRepository.existsByErrandProcessIdAndActivityType(process.getId(), CONCURRENCY_ACTIVITY_TYPE)) {
			return;
		}

		activityRepository.save(ErrandProcessActivityEntity.create()
			.withErrandProcessId(process.getId())
			.withErrandId(errandId)
			.withExternalTaskId(externalTaskId)
			.withActivityType(CONCURRENCY_ACTIVITY_TYPE)
			.withSeverity(WARN)
			.withMessage(CONCURRENT_TASKS_DETECTED.formatted(externalTaskId, displacedTaskId))
			.withErrorCode(CONCURRENT_TASKS_ERROR_CODE)
			.withOccurredAt(OffsetDateTime.now(clock).truncatedTo(MILLIS)));
	}

	/**
	 * Appends the activities of a report to the log of the errand.
	 * <p>
	 * A replayed report adds nothing: an entry is left out when the log already holds one for the same instance, external
	 * task and activity, which is the key of {@code uq_epa_idempotency}, or when the report repeats it. Entries without an
	 * external task id or an activity id are appended as they come. The log is read for duplicates before anything is
	 * written.
	 */
	private void storeActivities(final ErrandProcessEntity process, final String errandId, final ErrandProcessReport report) {
		final var activities = ofNullable(report.getActivities()).orElse(emptyList());

		if (activities.isEmpty()) {
			return;
		}

		final var externalTaskId = report.getExternalTaskId();
		final var alreadyLogged = new HashSet<String>();

		if (nonNull(externalTaskId)) {
			activityRepository.findByErrandProcessIdAndExternalTaskId(process.getId(), externalTaskId).stream()
				.map(ErrandProcessActivityEntity::getActivityId)
				.filter(Objects::nonNull)
				.forEach(alreadyLogged::add);
		}

		final var toStore = activities.stream()
			.filter(activity -> isNull(externalTaskId) || isNull(activity.getActivityId()) || alreadyLogged.add(activity.getActivityId()))
			.map(activity -> toErrandProcessActivityEntity(process.getId(), errandId, externalTaskId, activity))
			.toList();

		activityRepository.saveAll(toStore);
	}

	/**
	 * Lays what the process now waits for from a handler over what it waited for before.
	 * <p>
	 * The signals of the report replace those stored, and a report carrying none leaves the process waiting for no
	 * person. The same name reported twice is one signal, the first kept. Names are compared exactly.
	 * <p>
	 * A row whose name is still awaited is kept and brought up to date with the label and order of the report; the rows
	 * of names no longer awaited are deleted.
	 *
	 * @param  process the instance the report is about.
	 * @param  stored  the signals the instance waited for before the report.
	 * @param  report  what the process reported.
	 * @return         the signals the instance waits for after the report, in the order reported.
	 */
	private List<ErrandProcessSignalEntity> replaceAwaitingSignals(final ErrandProcessEntity process, final List<ErrandProcessSignalEntity> stored, final ErrandProcessReport report) {
		final var reusable = stored.stream().collect(Collectors.toMap(ErrandProcessSignalEntity::getName, identity()));
		final var awaited = new LinkedHashMap<String, ErrandProcessSignalEntity>();

		for (final var signal : ofNullable(report.getAwaitingSignals()).orElse(emptyList())) {
			if (!awaited.containsKey(signal.getName())) {
				awaited.put(signal.getName(), ofNullable(reusable.remove(signal.getName()))
					.orElseGet(() -> ErrandProcessSignalEntity.create().withErrandProcessId(process.getId()).withName(signal.getName()))
					.withLabel(signal.getLabel())
					.withSortOrder(awaited.size()));
			}
		}

		signalRepository.deleteAll(reusable.values());

		return signalRepository.saveAll(awaited.values());
	}

	/**
	 * The instance id of every process the entries of a page point at, read in one query for the page. Entries belonging
	 * to no instance ask nothing of it.
	 */
	private Map<String, String> processInstanceIdsOf(final List<ErrandProcessActivityEntity> entities) {
		final var processIds = entities.stream()
			.map(ErrandProcessActivityEntity::getErrandProcessId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		if (processIds.isEmpty()) {
			return emptyMap();
		}

		return processRepository.findAllById(processIds).stream()
			.filter(entity -> nonNull(entity.getProcessInstanceId()))
			.collect(Collectors.toMap(ErrandProcessEntity::getId, ErrandProcessEntity::getProcessInstanceId));
	}
}
