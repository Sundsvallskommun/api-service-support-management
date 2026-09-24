package se.sundsvall.supportmanagement.service;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessSignalRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.model.ProcessCommand;
import se.sundsvall.supportmanagement.service.model.ProcessStartOptions;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.INFO;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SIGNAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource.PROCESS;
import static se.sundsvall.supportmanagement.service.ProcessActivityLog.SIGNAL_ACTIVITY_TYPE;
import static se.sundsvall.supportmanagement.service.ProcessActivityLog.START_ACTIVITY_TYPE;
import static se.sundsvall.supportmanagement.service.ProcessKeySelector.excerptOf;
import static se.sundsvall.supportmanagement.service.ProcessRules.NO_PROCESS_CONSUMER;
import static se.sundsvall.supportmanagement.service.ProcessRules.requireProcessConsumer;
import static se.sundsvall.supportmanagement.service.ProcessRules.startOptionsOf;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.draftConflict;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.requireAdUser;

/**
 * Requests a handler aims straight at the process of an errand, as opposed to changes made to the errand itself -
 * starting the handling by hand, and stepping a process past a gate it waits at.
 * <p>
 * A command changes nothing on the errand, writes no revision and moves no version. It leaves two things behind: an
 * entry in the activity log naming who sent it and when, and an event that carries it to the process - but for a start
 * the process already has on its way, whose event goes no further. It notifies no one.
 * <p>
 * Only an ad account may send one, and anyone else is refused with 403 before anything is written.
 */
@Service
public class ProcessCommandService {

	private static final String NOT_AN_AD_ACCOUNT = "A signal steps a process on behalf of a person, and has to be sent by an ad account";
	private static final String NO_SUCH_INSTANCE = "The errand '%s' has no process instance '%s'";
	private static final String PROCESS_ENDED = "The process instance '%s' has ended and waits for no signal";
	private static final String SIGNAL_NOT_AWAITED = "The process instance '%s' does not wait for the signal '%s'. Read the errand again to see what it waits for now";

	private static final String START_NOT_BY_AN_AD_ACCOUNT = "Starting the handling of an errand is a decision made by a person, and has to be sent by an ad account";
	private static final String LIVE_PROCESS_IN_THE_WAY = "The errand '%s' already has a live process, and a process is started only for an errand without one";
	private static final String PROCESS_LIFE_OVER = "The errand '%s' has a process that ran to its end, and a completed process is never started again. A new process means a new errand";
	private static final String NO_LABEL_NAMES_A_PROCESS = """
		No label of the errand '%s' carries a process key it can be started with, so there is no process to start. Give \
		the errand the label of the process to start""";
	private static final String NO_LABEL_NAMES_ITS_PROCESS = """
		The errand '%s' has run the process '%s', and runs that one process for the whole of its life, but none of its \
		labels names it. Give the errand back the label carrying that process key""";
	private static final String KEY_NOT_CHOSEN = "The labels of the errand '%s' point at more than one process (%s), and the request has to name the one to start";
	private static final String KEY_NOT_OFFERED = "The process key '%s' is not one the errand '%s' can be started with, which are: %s";
	private static final String OTHER_START_ON_ITS_WAY = """
		A start of the process '%s' is already on its way for the errand '%s', so '%s' is not started. Wait for the \
		process to be registered, and read the errand again""";

	private static final String SIGNAL_SENT = "signal '%s' sent by %s";
	private static final String START_REQUESTED = "start of process '%s' requested by %s";
	private static final String EVENT_LOG_SIGNAL = "En signal har skickats till processen i ärendet: %s.";
	private static final String EVENT_LOG_START = "En start av processen har begärts i ärendet: %s.";

	private final AccessControlService accessControlService;
	private final NamespaceConfigService namespaceConfigService;
	private final ErrandProcessRepository processRepository;
	private final ErrandProcessSignalRepository signalRepository;
	private final ProcessEventOutboxRepository outboxRepository;
	private final ProcessKeySelector processKeySelector;
	private final ProcessActivityLog activityLog;
	private final EventService eventService;

	public ProcessCommandService(
		final AccessControlService accessControlService,
		final NamespaceConfigService namespaceConfigService,
		final ErrandProcessRepository processRepository,
		final ErrandProcessSignalRepository signalRepository,
		final ProcessEventOutboxRepository outboxRepository,
		final ProcessKeySelector processKeySelector,
		final ProcessActivityLog activityLog,
		final EventService eventService) {

		this.accessControlService = accessControlService;
		this.namespaceConfigService = namespaceConfigService;
		this.processRepository = processRepository;
		this.signalRepository = signalRepository;
		this.outboxRepository = outboxRepository;
		this.processKeySelector = processKeySelector;
		this.activityLog = activityLog;
		this.eventService = eventService;
	}

	/**
	 * Starts the handling of an errand by hand: writes an activity entry of type START naming the sender, and publishes an
	 * event of sub type PROCESS that carries the chosen key and the permission to start a process.
	 * <p>
	 * The rules are those of {@link ProcessRules#startOptionsOf}, and the start mode of the labels is not read, so the
	 * command works in automatic mode too. When the labels offer more than one key the request has to name one of them. A
	 * blank key is no key named. The chosen key travels with the event and is not resolved from the labels again when the
	 * event is published.
	 * <p>
	 * When a start with the same key is already on its way - a start by hand or an automatic one not yet delivered - the
	 * entry and the event are written all the same, but nothing more is handed on to the process. When one with another
	 * key is on its way, the start is refused with 409 and nothing is written.
	 * <p>
	 * The errand is locked before anything else is read, so that concurrent starts of the same errand are judged one after
	 * the other.
	 *
	 * @param namespace      the namespace of the errand.
	 * @param municipalityId the municipality of the errand.
	 * @param errandId       the errand to start the handling of.
	 * @param processKey     the process to start, or null or blank to start the one process the labels of the errand point
	 *                       at.
	 */
	@Transactional
	public void startProcess(final String namespace, final String municipalityId, final String errandId, final String processKey) {
		final var sender = requireAdUser(START_NOT_BY_AN_AD_ACCOUNT);
		final var errand = accessControlService.getErrand(namespace, municipalityId, errandId, true, PROCESS, RW);
		final var runsProcesses = namespaceConfigService.getProcessConsumer(namespace, municipalityId).isPresent();
		final var instances = processRepository.findByErrandIdOrderByCreatedDesc(errandId);

		final var options = startOptionsOf(runsProcesses, errand.getLifecycle(), instances, () -> processKeySelector.select(errand));
		final var chosenKey = chooseProcessKey(namespace, municipalityId, errandId, options, instances, processKey);
		final var alreadyOnItsWay = isAlreadyOnItsWay(errandId, chosenKey);

		activityLog.write(ErrandProcessActivityEntity.create()
			.withErrandId(errandId)
			.withActivityType(START_ACTIVITY_TYPE)
			.withActivityId(chosenKey)
			.withSeverity(INFO)
			.withMessage(START_REQUESTED.formatted(chosenKey, sender)));

		eventService.createProcessCommandEvent(UPDATE, EVENT_LOG_START.formatted(chosenKey), errand, EventSubType.PROCESS, alreadyOnItsWay ? null : new ProcessCommand(chosenKey, null));
	}

	/**
	 * Steps a process past the gate it waits at, by sending it one of the signals it waits for.
	 * <p>
	 * The signal is a request and forces nothing: the gate decides what it means where the process stands.
	 * <p>
	 * Only a signal the process waits for right now is taken, matched exactly as the process named it. Anything else is
	 * refused with 409 and writes nothing.
	 * <p>
	 * Taking a signal consumes nothing. Until the process reports where it went, the same signal is taken again - a double
	 * click writes two entries and two events.
	 * <p>
	 * The errand is locked before anything else is read, so the reads that follow see a report the lock waited for.
	 *
	 * @param namespace         the namespace of the errand.
	 * @param municipalityId    the municipality of the errand.
	 * @param errandId          the errand whose process is signalled.
	 * @param processInstanceId the instance to signal.
	 * @param signal            the name of the signal, as the process reported it among the ones it waits for.
	 */
	@Transactional
	public void signalProcess(final String namespace, final String municipalityId, final String errandId, final String processInstanceId, final String signal) {
		final var sender = requireAdUser(NOT_AN_AD_ACCOUNT);
		final var errand = accessControlService.getErrand(namespace, municipalityId, errandId, true, PROCESS, RW);

		requireProcessConsumer(namespaceConfigService.getProcessConsumer(namespace, municipalityId), namespace, municipalityId);

		final var process = processRepository.findByProcessInstanceIdAndErrandId(processInstanceId, errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NO_SUCH_INSTANCE.formatted(errandId, processInstanceId)));

		if (process.getProcessStatus().isTerminal()) {
			throw Problem.valueOf(CONFLICT, PROCESS_ENDED.formatted(processInstanceId));
		}

		final var awaited = signalRepository.findByErrandProcessIdOrderBySortOrderAsc(process.getId()).stream()
			.filter(candidate -> candidate.getName().equals(signal))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(CONFLICT, SIGNAL_NOT_AWAITED.formatted(processInstanceId, signal)));

		activityLog.write(ErrandProcessActivityEntity.create()
			.withErrandProcessId(process.getId())
			.withErrandId(errandId)
			.withActivityType(SIGNAL_ACTIVITY_TYPE)
			.withActivityId(awaited.getName())
			.withActivityName(awaited.getLabel())
			.withSeverity(INFO)
			.withMessage(SIGNAL_SENT.formatted(awaited.getName(), sender)));

		eventService.createProcessCommandEvent(UPDATE, EVENT_LOG_SIGNAL.formatted(ofNullable(awaited.getLabel()).orElse(awaited.getName())), errand, SIGNAL,
			new ProcessCommand(null, awaited.getName()));
	}

	/**
	 * The key a start names. Throws 409 for a draft, for a live instance and for a completed one, and 400 when the
	 * namespace runs no process, when no key can be started, and when the request does not choose among several keys or
	 * names one not offered.
	 */
	private static String chooseProcessKey(final String namespace, final String municipalityId, final String errandId, final ProcessStartOptions options,
		final List<ErrandProcessEntity> instances, final String requestedKey) {

		return switch (options.status()) {
			case NO_PROCESS_ENGINE -> throw Problem.valueOf(BAD_REQUEST, NO_PROCESS_CONSUMER.formatted(namespace, municipalityId));
			case ERRAND_DRAFT -> throw draftConflict(errandId);
			case LIVE_INSTANCE -> throw Problem.valueOf(CONFLICT, LIVE_PROCESS_IN_THE_WAY.formatted(errandId));
			case PROCESS_COMPLETED -> throw Problem.valueOf(CONFLICT, PROCESS_LIFE_OVER.formatted(errandId));
			case NO_PROCESS_KEY -> throw Problem.valueOf(BAD_REQUEST, instances.isEmpty()
				? NO_LABEL_NAMES_A_PROCESS.formatted(errandId)
				: NO_LABEL_NAMES_ITS_PROCESS.formatted(errandId, excerptOf(instances.getFirst().getProcessKey())));
			case START_PENDING -> throw new IllegalStateException("The options of a start are never START_PENDING");
			case AVAILABLE -> pick(errandId, options.processKeys(), requestedKey);
		};
	}

	/**
	 * The key of the request when it names one of those offered, and the only one offered when it names none - a blank key
	 * included.
	 */
	private static String pick(final String errandId, final List<String> offered, final String requestedKey) {
		if (StringUtils.isBlank(requestedKey)) {
			if (offered.size() > 1) {
				throw Problem.valueOf(BAD_REQUEST, KEY_NOT_CHOSEN.formatted(errandId, excerptOf(offered)));
			}

			return offered.getFirst();
		}

		if (!offered.contains(requestedKey)) {
			throw Problem.valueOf(BAD_REQUEST, KEY_NOT_OFFERED.formatted(excerptOf(requestedKey), errandId, excerptOf(offered)));
		}

		return requestedKey;
	}

	/**
	 * Whether an undelivered row of the errand already carries the permission to start the process with this key. Throws
	 * 409 when such a row names another key.
	 */
	private boolean isAlreadyOnItsWay(final String errandId, final String processKey) {
		final var waiting = outboxRepository.findByErrandIdAndStartAllowedIsTrueAndDeliveredAtIsNull(errandId);

		waiting.stream()
			.map(ProcessEventOutboxEntity::getProcessKey)
			.filter(key -> !processKey.equals(key))
			.findFirst()
			.ifPresent(other -> {
				throw Problem.valueOf(CONFLICT, OTHER_START_ON_ITS_WAY.formatted(excerptOf(other), errandId, excerptOf(processKey)));
			});

		return !waiting.isEmpty();
	}
}
