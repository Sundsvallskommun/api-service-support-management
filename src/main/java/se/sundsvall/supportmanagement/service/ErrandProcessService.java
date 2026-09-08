package se.sundsvall.supportmanagement.service;

import java.time.Clock;
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
import org.springframework.data.domain.PageImpl;
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
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.service.model.ErrandProcessResult;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource.PROCESS;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource.PROCESS_ACTIVITY;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toErrandProcess;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toErrandProcessActivityEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toErrandProcessEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toErrandProcesses;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toProcessActivities;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.updateErrandProcessEntity;

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
	private static final String INSTANCE_ON_OTHER_ERRAND = "The process instance '%s' is registered on errand '%s' and cannot be reported on errand '%s'";
	private static final String OTHER_LIVE_INSTANCE = "The errand '%s' already has a live process instance '%s' and cannot be given another one";
	private static final String OTHER_PROCESS_KEY = "The errand '%s' already runs a process other than '%s', and every instance of an errand runs the same process";
	private static final String PROCESS_LIFE_OVER = "The errand '%s' has a process that ran to its end, and a completed process is never started again";
	private static final String CONCURRENT_REPORT = "Another report for the process instance '%s' is being written right now";

	private static final Logger LOG = LoggerFactory.getLogger(ErrandProcessService.class);

	private final ErrandProcessRepository processRepository;
	private final ErrandProcessActivityRepository activityRepository;
	private final AccessControlService accessControlService;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;

	public ErrandProcessService(
		final ErrandProcessRepository processRepository,
		final ErrandProcessActivityRepository activityRepository,
		final AccessControlService accessControlService,
		final PlatformTransactionManager transactionManager,
		final Clock clock) {

		this.processRepository = processRepository;
		this.activityRepository = activityRepository;
		this.accessControlService = accessControlService;
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

		return write(errandId, processInstanceId, () -> reportInTransaction(namespace, municipalityId, errandId, processInstanceId, report));
	}

	private ErrandProcessResult reportInTransaction(final String namespace, final String municipalityId, final String errandId, final String processInstanceId, final ErrandProcess report) {
		lockErrandForWriting(namespace, municipalityId, errandId);

		final var existing = processRepository.findByProcessInstanceId(processInstanceId).orElse(null);

		if (isNull(existing)) {
			return createProcess(namespace, municipalityId, errandId, processInstanceId, report);
		}

		verifyBelongsToErrand(existing, errandId);
		verifySameProcessKey(existing, report);

		// Asked of a row that already exists as well, because a terminal one reporting itself alive again - an incident
		// resolved by hand - asks for the place it gave up when it ended, and may find it taken. The unique key would
		// refuse that too, but only the check can say which instance is standing in the way.
		verifyNoOtherLiveInstance(errandId, processInstanceId, report);

		updateErrandProcessEntity(existing, report, clock);
		final var saved = processRepository.saveAndFlush(existing);
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

		if (isNull(processInstanceId) && FAILED != report.getProcessStatus()) {
			throw Problem.valueOf(BAD_REQUEST, MISSING_INSTANCE_ID);
		}

		return write(errandId, processInstanceId, () -> registerInTransaction(namespace, municipalityId, errandId, processInstanceId, report));
	}

	private ErrandProcessResult registerInTransaction(final String namespace, final String municipalityId, final String errandId, final String processInstanceId, final ErrandProcess report) {
		lockErrandForWriting(namespace, municipalityId, errandId);

		// A start that never produced an instance cannot have been reported on: with no instance there is no work step.
		if (nonNull(processInstanceId)) {
			final var existing = processRepository.findByProcessInstanceId(processInstanceId).orElse(null);

			if (nonNull(existing)) {
				verifyBelongsToErrand(existing, errandId);
				return new ErrandProcessResult(toErrandProcess(existing), false);
			}
		}

		// The one rule of a single process per errand that no unique key can hold, since a completed instance and a
		// failed one leave the same empty slot behind. A failed start is recovered from, a completed process is not.
		if (processRepository.existsByErrandIdAndProcessStatus(errandId, COMPLETED)) {
			throw Problem.valueOf(CONFLICT, PROCESS_LIFE_OVER.formatted(errandId));
		}

		return createProcess(namespace, municipalityId, errandId, processInstanceId, report);
	}

	private ErrandProcessResult createProcess(final String namespace, final String municipalityId, final String errandId, final String processInstanceId, final ErrandProcess report) {
		verifySameProcessKeyAsErrand(errandId, report.getProcessKey());
		verifyNoOtherLiveInstance(errandId, processInstanceId, report);

		final var entity = toErrandProcessEntity(namespace, municipalityId, errandId, processInstanceId, report);
		entity.applyStatus(report.getProcessStatus(), clock);

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
			.withProcesses(toErrandProcesses(processRepository.findByErrandIdAndMunicipalityIdAndNamespaceOrderByCreatedDesc(errandId, municipalityId, namespace, Pageable.unpaged())));
	}

	/**
	 * The activity log of an errand.
	 * <p>
	 * Read per errand rather than per instance, since the entries explaining why no process ever started belong to no
	 * instance and could not be reached at all otherwise. Narrowing to an instance therefore leaves them out, which is
	 * the point of narrowing.
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
			return toPage(page, processInstanceIdsOf(page.getContent()), pageable);
		}

		final var instance = processRepository.findByProcessInstanceId(processInstanceId)
			.filter(entity -> errandId.equals(entity.getErrandId()))
			.orElse(null);

		// An instance the errand never had narrows the log to nothing rather than widening it to everything.
		if (isNull(instance)) {
			return new PageImpl<>(emptyList(), pageable, 0);
		}

		final var page = activityRepository.findByErrandIdAndErrandProcessId(errandId, instance.getId(), pageable);
		return toPage(page, Map.of(instance.getId(), processInstanceId), pageable);
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
	 * Both keys can be hit by the same insert, so which one gave way says nothing about what to do; the row is looked up
	 * by its process instance id instead. That lookup has to happen after the transaction that lost has been rolled back,
	 * since a constraint violation leaves it unusable - which is why the second attempt is a transaction of its own
	 * rather than a read inside the failed one. It reaches the same conclusion the first would have: the row now exists,
	 * so the report updates it or the registration returns it, unless another live instance is what stood in the way, in
	 * which case it is refused as the conflict it is.
	 * <p>
	 * Only a violation some other row explains is a race. Every other one - a value too long for its column, a required
	 * one left out - is raised as it is rather than dressed up as contention, since a report answered with 409 tells a
	 * process engine to abort the process it just started.
	 */
	private ErrandProcessResult write(final String errandId, final String processInstanceId, final Supplier<ErrandProcessResult> attempt) {
		try {
			return transactionTemplate.execute(_ -> attempt.get());
		} catch (final DataIntegrityViolationException lostTheRace) {
			LOG.info("Retrying the write for process instance '{}' on errand '{}' after an integrity violation", processInstanceId, errandId, lostTheRace);

			try {
				return transactionTemplate.execute(_ -> attempt.get());
			} catch (final DataIntegrityViolationException stillViolating) {
				// The second attempt reads before it writes, so a row that explains the violation would have sent it down
				// the update path or refused it as the conflict it is. Getting here again with nothing in the way means
				// the violation was never about a race - a value too long for its column, a missing one - and answering
				// that with 409 would tell a process engine its perfectly healthy process had already ended.
				if (!aRowNowStandsInTheWay(errandId, processInstanceId)) {
					LOG.error("The write for process instance '{}' on errand '{}' violates an integrity constraint no concurrent write explains", processInstanceId, errandId, stillViolating);
					throw stillViolating;
				}

				throw Problem.valueOf(CONFLICT, CONCURRENT_REPORT.formatted(processInstanceId));
			}
		}
	}

	/**
	 * Whether a row that was written while this one was being attempted explains the violation.
	 * <p>
	 * Read in a transaction of its own, since a constraint violation leaves the one that hit it unusable. Both unique
	 * keys are asked about: the instance may have been taken by the report of its own work step, and the live slot of
	 * the errand by an instance of another one.
	 */
	private boolean aRowNowStandsInTheWay(final String errandId, final String processInstanceId) {
		return Boolean.TRUE.equals(transactionTemplate.execute(_ -> ofNullable(processInstanceId)
			.flatMap(processRepository::findByProcessInstanceId)
			.isPresent()
			|| processRepository.findByErrandIdAndActiveMarkerIsNotNull(errandId).isPresent()));
	}

	/**
	 * Takes the write lock on the errand, which is what serialises the reports of one errand against each other. The
	 * checks below read what other reports have written and would otherwise be answering a question that is no longer
	 * true by the time the row is inserted.
	 */
	private void lockErrandForWriting(final String namespace, final String municipalityId, final String errandId) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, PROCESS, RW);
	}

	private static void verifyBelongsToErrand(final ErrandProcessEntity entity, final String errandId) {
		if (!errandId.equals(entity.getErrandId())) {
			throw Problem.valueOf(CONFLICT, INSTANCE_ON_OTHER_ERRAND.formatted(entity.getProcessInstanceId(), entity.getErrandId(), errandId));
		}
	}

	private static void verifySameProcessKey(final ErrandProcessEntity entity, final ErrandProcess report) {
		if (!entity.getProcessKey().equals(report.getProcessKey())) {
			throw Problem.valueOf(CONFLICT, OTHER_PROCESS_KEY.formatted(entity.getErrandId(), report.getProcessKey()));
		}
	}

	private void verifySameProcessKeyAsErrand(final String errandId, final String processKey) {
		if (processRepository.existsByErrandIdAndProcessKeyNot(errandId, processKey)) {
			throw Problem.valueOf(CONFLICT, OTHER_PROCESS_KEY.formatted(errandId, processKey));
		}
	}

	/**
	 * Refuses a second live instance, which is the rule {@code uq_ep_one_active_per_errand} holds. Asked only of a row
	 * that would itself be live, exactly as the key is: a terminal row leaves the slot empty and can never take one that
	 * is occupied, which is what lets a start that failed be registered while the instance it failed to replace lives on.
	 */
	private void verifyNoOtherLiveInstance(final String errandId, final String processInstanceId, final ErrandProcess report) {
		if (report.getProcessStatus().isTerminal()) {
			return;
		}

		processRepository.findByErrandIdAndActiveMarkerIsNotNull(errandId)
			.filter(live -> !Objects.equals(live.getProcessInstanceId(), processInstanceId))
			.ifPresent(live -> {
				throw Problem.valueOf(CONFLICT, OTHER_LIVE_INSTANCE.formatted(errandId, live.getProcessInstanceId()));
			});
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

	private static Page<ProcessActivity> toPage(final Page<ErrandProcessActivityEntity> page, final Map<String, String> processInstanceIdByRowId, final Pageable pageable) {
		return new PageImpl<>(toProcessActivities(page.getContent(), processInstanceIdByRowId), pageable, page.getTotalElements());
	}
}
