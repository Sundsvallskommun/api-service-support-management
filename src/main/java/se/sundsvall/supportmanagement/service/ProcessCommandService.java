package se.sundsvall.supportmanagement.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessActivityRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessSignalRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.model.ProcessCommand;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity.MESSAGE_LENGTH;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.INFO;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SIGNAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource.PROCESS;
import static se.sundsvall.supportmanagement.service.ErrandProcessService.NO_PROCESS_CONSUMER;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getAdUser;

/**
 * Requests a handler aims straight at the process of an errand, as opposed to changes made to the errand itself.
 * <p>
 * A command changes nothing on the errand, writes no revision and moves no version. It leaves two things behind: an
 * entry in the activity log naming who sent it, which is what answers afterwards who stepped the process on and when,
 * and an event that carries it to the process. It notifies no one - the process is its only audience.
 * <p>
 * Only an ad account may send one, and anyone else is refused with 403 before anything is written. A command is a
 * person's decision to step the process on, and the entry it leaves has to say which person - a service name answers
 * nothing. Publication does not lean on the check: it waives layer 1 of the loop guard for commands of its own accord,
 * so a command that got past it would still reach the process rather than be silenced.
 */
@Service
public class ProcessCommandService {

	private static final String SIGNAL_ACTIVITY_TYPE = "SIGNAL";

	private static final String NOT_AN_AD_ACCOUNT = "A signal steps a process on behalf of a person, and has to be sent by an ad account";
	private static final String NO_SUCH_INSTANCE = "The errand '%s' has no process instance '%s'";
	private static final String PROCESS_ENDED = "The process instance '%s' has ended and waits for no signal";
	private static final String SIGNAL_NOT_AWAITED = "The process instance '%s' does not wait for the signal '%s'. Read the errand again to see what it waits for now";

	private static final String SIGNAL_SENT = "signal '%s' sent by %s";
	private static final String EVENT_LOG_SIGNAL = "En signal har skickats till processen i ärendet: %s.";

	private final AccessControlService accessControlService;
	private final NamespaceConfigService namespaceConfigService;
	private final ErrandProcessRepository processRepository;
	private final ErrandProcessSignalRepository signalRepository;
	private final ErrandProcessActivityRepository activityRepository;
	private final EventService eventService;
	private final Clock clock;

	public ProcessCommandService(
		final AccessControlService accessControlService,
		final NamespaceConfigService namespaceConfigService,
		final ErrandProcessRepository processRepository,
		final ErrandProcessSignalRepository signalRepository,
		final ErrandProcessActivityRepository activityRepository,
		final EventService eventService,
		final Clock clock) {

		this.accessControlService = accessControlService;
		this.namespaceConfigService = namespaceConfigService;
		this.processRepository = processRepository;
		this.signalRepository = signalRepository;
		this.activityRepository = activityRepository;
		this.eventService = eventService;
		this.clock = clock;
	}

	/**
	 * Steps a process past the gate it waits at, by sending it one of the signals it waits for.
	 * <p>
	 * The signal is a request and forces nothing. The gate decides what it means where the process stands, which is why a
	 * handler may send a signal but never set the state of a process: a step the law requires cannot be skipped by
	 * posting the right string.
	 * <p>
	 * Only a signal the process waits for right now is taken, matched exactly as the process named it. Anything else is
	 * refused with 409 and writes nothing: a process that has reported that it moved on waits for something else, and the
	 * old button stops working.
	 * <p>
	 * Taking a signal consumes nothing. Until the process reports where it went, the same signal is taken again - a double
	 * click writes two entries and two events, and the process engine correlates the first and passes the second over as
	 * a signal no gate waits for any more. That is a decision rather than an oversight: the next report of the process is
	 * what closes the gate, and SM does not second-guess the model about what one signal answers.
	 * <p>
	 * The errand is locked before anything else is read. The reads that follow then see the report the lock waited for
	 * rather than the one before it - under repeatable read the snapshot is taken at the first plain read of the
	 * transaction, and a read ahead of the lock would pin the transaction to what stood before the report committed.
	 *
	 * @param namespace         the namespace of the errand.
	 * @param municipalityId    the municipality of the errand.
	 * @param errandId          the errand whose process is signalled.
	 * @param processInstanceId the instance to signal.
	 * @param signal            the name of the signal, as the process reported it among the ones it waits for.
	 */
	@Transactional
	public void signalProcess(final String namespace, final String municipalityId, final String errandId, final String processInstanceId, final String signal) {
		final var sender = ofNullable(getAdUser()).orElseThrow(() -> Problem.valueOf(FORBIDDEN, NOT_AN_AD_ACCOUNT));
		final var errand = accessControlService.getErrand(namespace, municipalityId, errandId, true, PROCESS, RW);

		if (namespaceConfigService.getProcessConsumer(namespace, municipalityId).isEmpty()) {
			throw Problem.valueOf(BAD_REQUEST, NO_PROCESS_CONSUMER.formatted(namespace, municipalityId));
		}

		final var process = processRepository.findByProcessInstanceIdAndErrandId(processInstanceId, errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NO_SUCH_INSTANCE.formatted(errandId, processInstanceId)));

		if (process.getProcessStatus().isTerminal()) {
			throw Problem.valueOf(CONFLICT, PROCESS_ENDED.formatted(processInstanceId));
		}

		final var awaited = signalRepository.findByErrandProcessIdOrderBySortOrderAsc(process.getId()).stream()
			.filter(candidate -> candidate.getName().equals(signal))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(CONFLICT, SIGNAL_NOT_AWAITED.formatted(processInstanceId, signal)));

		activityRepository.save(ErrandProcessActivityEntity.create()
			.withErrandProcessId(process.getId())
			.withErrandId(errandId)
			.withActivityType(SIGNAL_ACTIVITY_TYPE)
			.withActivityId(awaited.getName())
			.withActivityName(awaited.getLabel())
			.withSeverity(INFO)
			.withMessage(StringUtils.truncate(SIGNAL_SENT.formatted(awaited.getName(), sender), MESSAGE_LENGTH))
			.withOccurredAt(OffsetDateTime.now(clock).truncatedTo(MILLIS)));

		eventService.createProcessCommandEvent(UPDATE, EVENT_LOG_SIGNAL.formatted(ofNullable(awaited.getLabel()).orElse(awaited.getName())), errand, false, SIGNAL,
			new ProcessCommand(null, awaited.getName()));
	}
}
