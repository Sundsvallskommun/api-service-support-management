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
import se.sundsvall.supportmanagement.api.model.process.ErrandProcesses;
import se.sundsvall.supportmanagement.api.model.process.ProcessActivity;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessActivityRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
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
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toErrandProcess;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toErrandProcessActivityEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toErrandProcessEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toErrandProcesses;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toProcessActivity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toProcessStatus;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.updateErrandProcessEntity;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getExecutingUser;

/**
 * The state a process reports about an errand, and the reading of it.
 * <p>
 * Two write paths lead here, and the difference between them is the whole point: reporting on an instance creates the
 * row or updates it, while registering a start only ever creates one. A process engine can hand out the first work step
 * before the call that started the process has answered, so a report may arrive before the registration of the start it
 * belongs to. With one path creating or updating and the other only creating, neither order is an error - without that
 * rule the service that started a perfectly healthy process would be told its errand already had one, and would abort
 * it.
 */
@Service
public class ErrandProcessService {

	private static final String INSTANCE_ID_MISMATCH = "The process instance id '%s' in the body differs from '%s' in the path";
	private static final String MISSING_INSTANCE_ID = "A process instance id is required unless the report registers a start that failed";
	private static final String INSTANCE_ON_OTHER_ERRAND = "The process instance '%s' belongs to another errand and cannot be reported on errand '%s'";
	private static final String OTHER_LIVE_INSTANCE = "The errand '%s' already has a live process instance '%s' and cannot be given another one";
	private static final String OTHER_PROCESS_KEY = "The errand '%s' already runs a process other than '%s', and every instance of an errand runs the same process";
	private static final String PROCESS_LIFE_OVER = "The errand '%s' has a process that ran to its end, and a completed process is never started again";
	private static final String NO_PROCESS_CONSUMER = "The namespace '%s' in municipality '%s' has no process consumer configured and runs no process";
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
	private final AccessControlService accessControlService;
	private final NamespaceConfigService namespaceConfigService;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;

	public ErrandProcessService(
		final ErrandProcessRepository processRepository,
		final ErrandProcessActivityRepository activityRepository,
		final AccessControlService accessControlService,
		final NamespaceConfigService namespaceConfigService,
		final PlatformTransactionManager transactionManager,
		final Clock clock) {

		this.processRepository = processRepository;
		this.activityRepository = activityRepository;
		this.accessControlService = accessControlService;
		this.namespaceConfigService = namespaceConfigService;
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
	public ErrandProcessResult reportProcess(final String namespace, final String municipalityId, final String errandId, final String processInstanceId, final ErrandProcess report) {
		if (nonNull(report.getProcessInstanceId()) && !processInstanceId.equals(report.getProcessInstanceId())) {
			throw Problem.valueOf(BAD_REQUEST, INSTANCE_ID_MISMATCH.formatted(report.getProcessInstanceId(), processInstanceId));
		}

		verifySenderOfReport(namespace, municipalityId, report);

		return writeWithCollisionRecovery(errandId, processInstanceId, () -> reportInTransaction(namespace, municipalityId, errandId, processInstanceId, report));
	}

	private ErrandProcessResult reportInTransaction(final String namespace, final String municipalityId, final String errandId, final String processInstanceId, final ErrandProcess report) {
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

		return new ErrandProcessResult(toErrandProcess(saved), false);
	}

	/**
	 * Registers a start, whether it produced an instance or failed to.
	 * <p>
	 * Never updates. An instance already registered is answered with what it says right now and nothing is touched,
	 * because the report that registered it came from the process itself and is newer than this registration.
	 *
	 * @param  namespace      the namespace of the errand.
	 * @param  municipalityId the municipality of the errand.
	 * @param  errandId       the errand the process was started for.
	 * @param  report         the start being registered.
	 * @return                the state of the process, and whether the registration created it.
	 */
	public ErrandProcessResult registerProcess(final String namespace, final String municipalityId, final String errandId, final ErrandProcess report) {
		final var processInstanceId = report.getProcessInstanceId();

		if (isNull(processInstanceId) && FAILED != toProcessStatus(report)) {
			throw Problem.valueOf(BAD_REQUEST, MISSING_INSTANCE_ID);
		}

		verifySenderOfReport(namespace, municipalityId, report);

		return writeWithCollisionRecovery(errandId, processInstanceId, () -> registerInTransaction(namespace, municipalityId, errandId, processInstanceId, report));
	}

	private ErrandProcessResult registerInTransaction(final String namespace, final String municipalityId, final String errandId, final String processInstanceId, final ErrandProcess report) {
		verifyErrandVersion(lockErrandForWriting(namespace, municipalityId, errandId), report);

		return ofNullable(processInstanceId)
			.flatMap(processRepository::findByProcessInstanceId)
			.map(existing -> {
				verifyBelongsToErrand(existing, errandId);
				return new ErrandProcessResult(toErrandProcess(existing), false);
			})
			.orElseGet(() -> createProcess(namespace, municipalityId, errandId, processInstanceId, report));
	}

	/**
	 * Creates the row for an instance this service has not seen, from either write path.
	 * <p>
	 * Both paths are held to the rules of a single process per errand, the first report of a work step as much as the
	 * registration of a start. A work step can report before its start is registered, so rules asked only by the
	 * registration would let an instance started on a stale permission in through the report - and the registration would
	 * then find the row and answer 200, instead of the conflict that tells the process engine to abort the instance.
	 */
	private ErrandProcessResult createProcess(final String namespace, final String municipalityId, final String errandId, final String processInstanceId, final ErrandProcess report) {
		final var instances = processRepository.findByErrandIdOrderByCreatedDesc(errandId);

		verifyProcessLifeNotOver(instances, errandId);
		verifySameProcessKeyAsErrand(instances, errandId, report.getProcessKey());
		verifyNoOtherLiveInstance(instances, errandId, processInstanceId, report);

		final var entity = toErrandProcessEntity(namespace, municipalityId, errandId, processInstanceId, report);
		entity.applyStatus(toProcessStatus(report), clock);
		trackOutstandingTask(entity, report);

		final var saved = processRepository.saveAndFlush(entity);
		storeActivities(saved, errandId, report);

		return new ErrandProcessResult(toErrandProcess(saved), true);
	}

	/**
	 * Every process an errand has had, newest first, in the envelope that leaves room for saying whether a new one may be
	 * started.
	 *
	 * @param  namespace      the namespace of the errand.
	 * @param  municipalityId the municipality of the errand.
	 * @param  errandId       the errand to read.
	 * @return                the processes of the errand.
	 */
	@Transactional(readOnly = true)
	public ErrandProcesses readProcesses(final String namespace, final String municipalityId, final String errandId) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, PROCESS, R);

		return ErrandProcesses.create()
			.withProcesses(toErrandProcesses(processRepository.findByErrandIdOrderByCreatedDesc(errandId)));
	}

	/**
	 * The activity log of an errand.
	 * <p>
	 * Read per errand rather than per instance, since the entries explaining why no process ever started belong to no
	 * instance and could not be reached at all otherwise. Narrowing to an instance therefore leaves them out, which is
	 * the point of narrowing, and narrowing to an instance the errand never had leaves nothing rather than everything.
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
	 * The latest process of every one of sent in errands, which is what the {@code process} field of an errand shows.
	 * <p>
	 * The latest rather than the live one, since a start that failed leaves no live instance behind and a completed
	 * process leaves none either. Showing only the live one would render both as an errand with no process at all, and a
	 * mistyped process key would be invisible.
	 * <p>
	 * One query for the whole page. The rows arrive newest first, so the first one seen per errand is the latest one.
	 * Deduplicated before they are mapped, since an errand whose process start has failed repeatedly carries a row per
	 * attempt and all but the newest are thrown away.
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

		final var latestPerErrand = new LinkedHashMap<String, ErrandProcess>();

		processRepository.findByErrandIdInAndMunicipalityIdAndNamespaceOrderByCreatedDesc(errandIds, municipalityId, namespace)
			.forEach(entity -> latestPerErrand.computeIfAbsent(entity.getErrandId(), _ -> toErrandProcess(entity)));

		return latestPerErrand;
	}

	/**
	 * Runs a write, and recovers from losing a race for one of the unique keys.
	 * <p>
	 * Both keys can be hit by the same insert, so which one gave way says nothing about what to do. The second attempt
	 * settles it instead, by reading before it writes exactly as the first did: a row that appeared meanwhile sends the
	 * report down the update path, and one that stands in the way is refused as the conflict it is, named. That reading
	 * has to happen after the transaction that lost has been rolled back, since a constraint violation leaves it
	 * unusable, which is why the second attempt is a transaction of its own.
	 * <p>
	 * A violation that survives that second attempt is therefore not contention: contention would have been seen and
	 * answered by the checks the attempt begins with. What is left is a row this service cannot write at all - a value
	 * too long for its column, a key it collides with for reasons no concurrent writer explains - and it is raised as
	 * the fault it is rather than dressed up as a conflict, since a report answered with 409 tells a process engine to
	 * abort the process it just started. It is logged once, where it leaves this service.
	 * <p>
	 * Asking the database afterwards which rows exist cannot tell the two apart, and must not be tried: on the update
	 * path the row and the live slot were both already taken by this very report, so every such question answers yes
	 * whatever the true cause was.
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
	 * Takes the write lock on the errand, which is what serialises the reports of one errand against each other. The
	 * checks below read what other reports have written and would otherwise be answering a question that is no longer
	 * true by the time the row is inserted.
	 * <p>
	 * Hands the errand back, since the version it carries is what a report claiming to have read the errand is held
	 * against, and it has to be the version behind the lock rather than one read before it.
	 */
	private ErrandEntity lockErrandForWriting(final String namespace, final String municipalityId, final String errandId) {
		return accessControlService.getErrand(namespace, municipalityId, errandId, true, PROCESS, RW);
	}

	/**
	 * Checks a report against the process configuration of the namespace before anything is written.
	 * <p>
	 * Validation rather than authorization, and answered as such. {@code X-Sent-By} is set by the caller and nothing
	 * behind it is verified - this service authenticates no one, the gateway does - so refusing a report with 403 would
	 * claim a check that was never made and send the caller looking for credentials over a field in the body. What the
	 * rules do buy is that {@code process_service} is a column someone guarantees rather than free text, that a namespace
	 * running no process cannot collect rows for one, and that the log and the notification of the errand name an author.
	 */
	private void verifySenderOfReport(final String namespace, final String municipalityId, final ErrandProcess report) {
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
	 * Refuses an instance registered on another errand. The other errand is not named, since it can belong to another
	 * namespace or municipality than the caller has any business with.
	 */
	private static void verifyBelongsToErrand(final ErrandProcessEntity entity, final String errandId) {
		if (!errandId.equals(entity.getErrandId())) {
			throw Problem.valueOf(CONFLICT, INSTANCE_ON_OTHER_ERRAND.formatted(entity.getProcessInstanceId(), errandId));
		}
	}

	private static void verifySameProcessKey(final ErrandProcessEntity entity, final ErrandProcess report) {
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
	 * Refuses a new instance on an errand whose process has run to its end. A completed instance and a failed one leave
	 * the same empty slot in the unique key behind, so this is the rule asked of the rows instead: a failed start is
	 * recovered from, a completed process is not.
	 */
	private static void verifyProcessLifeNotOver(final List<ErrandProcessEntity> instances, final String errandId) {
		if (instances.stream().anyMatch(instance -> COMPLETED == instance.getProcessStatus())) {
			throw Problem.valueOf(CONFLICT, PROCESS_LIFE_OVER.formatted(errandId));
		}
	}

	/**
	 * Refuses a second live instance, which is the rule {@code uq_ep_one_active_per_errand} holds. Asked only of a row
	 * that would itself be live, exactly as the key is: a terminal row leaves the slot empty and can never take one that
	 * is occupied, which is what lets a start that failed be registered while the instance it failed to replace lives on.
	 */
	private static void verifyNoOtherLiveInstance(final List<ErrandProcessEntity> instances, final String errandId, final String processInstanceId, final ErrandProcess report) {
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
	 * Refuses a report whose picture of the errand has gone stale.
	 * <p>
	 * This is the If-Match of a work step that reads the errand and then acts outside this service - sends a letter,
	 * calls another party - and therefore never writes back and has no header to carry one on. It is optional for that
	 * reason: a step that neither reads nor writes the errand sends no version, and is held against nothing.
	 * <p>
	 * Answered before anything is written, so a refused report leaves neither state nor activities behind. For the step
	 * it is a 412 like any other: report RETRYING, throw, and let the process engine run it again against the errand as
	 * it now is. The log line is the only trace a refused report leaves, and it is logged as routine rather than as a
	 * fault: how often it happens is worth knowing, not any single occurrence.
	 */
	private void verifyErrandVersion(final ErrandEntity errand, final ErrandProcess report) {
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
	 * two reports and empty the rest of the time. Steps that run one after another therefore never meet here: the
	 * process engine completes a task before it hands out the next one, so the report that empties the place has always
	 * arrived before the next task announces itself. Two that do meet are two branches of the same instance running at
	 * once, which the process models are not allowed to have.
	 * <p>
	 * Only the task standing in the place empties it. A report from another task that does not announce itself, or a
	 * report naming no task at all - the registration of a start among them - says nothing about the task standing there
	 * and leaves the place as it found it.
	 *
	 * @return the task that was already working when this report came in, or null if the place was free. The id
	 *         rather than a yes or no, since the warning names both tasks to be worth acting on.
	 */
	private String trackOutstandingTask(final ErrandProcessEntity entity, final ErrandProcess report) {
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
	 * Refusing one of them would silence the very entry that reveals the model is breaking the rule, and would leave
	 * the two steps knocking each other out with nothing on the errand to say why.
	 * <p>
	 * Written once per instance, while every occurrence is logged as a warning. Branches that pass each other keep doing
	 * so for as long as the model has the gateway, and the log a handler reads on the errand would otherwise drown in a
	 * fault it has already been told about.
	 * <p>
	 * That entry is written without an activity id, so that {@code uq_epa_idempotency} - which counts null as
	 * distinct - can never refuse it. A constraint violation here would take the report it was found in down with
	 * it, which is the opposite of the point.
	 * <p>
	 * The entry names both tasks and says what to do about them. Whoever reads it is looking at an errand, not at a
	 * BPMN file, and the fix is in the model rather than anywhere they can reach from here - so a warning that only
	 * announced the problem would be read once and left alone. The error code is what an alert is built on, since the
	 * message carries the two task ids and is therefore different every time.
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
	 * A replayed report must add nothing, and {@code uq_epa_idempotency} says when two entries are the same one: the
	 * instance, the external task and the activity. Entries missing either half of the key are appended as they come,
	 * which is the answer the key gives as well, since null is distinct in a unique index. Asked before writing rather
	 * than recovered from afterwards, since a violation would take the rest of the report down with it.
	 */
	private void storeActivities(final ErrandProcessEntity process, final String errandId, final ErrandProcess report) {
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
