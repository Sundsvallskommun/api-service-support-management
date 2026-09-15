package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.eventlog.EventType;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.model.ProcessCommand;
import se.sundsvall.supportmanagement.service.model.ProcessKeySelection;

import static generated.se.sundsvall.eventlog.EventType.DELETE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity.EXECUTED_BY_LENGTH;
import static se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity.PROCESS_KEY_LENGTH;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.PROCESS;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.AUTOMATIC;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getAdUser;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getTriggerProcess;

/**
 * Turns an errand event into a row in the outbox, in the same transaction as the change the event is about.
 * <p>
 * Hung last in {@link EventService#createErrandEvent}, which is the one passage every errand event goes through -
 * the email intake and the web messages included, neither of which writes a revision. Hanging it off the errand service
 * and comparing revisions instead would have left the process blind to exactly those. A command comes in beside it,
 * through {@link EventService#createProcessCommandEvent}.
 * <p>
 * The rule it applies, step by step:
 *
 * <pre>
 * 1. process consumer for (municipalityId, namespace)?   no   -&gt; return
 *    event type CREATE, UPDATE or DELETE?                no   -&gt; throw, which takes the errand change down
 * 2. X-Trigger-Process: false, from a non ad identity?   yes  -&gt; return                 (loop guard, layer 1)
 *                    commands (PROCESS, SIGNAL) and deletions skip steps 2, 3 and 4
 * 3. delivered events for the errand in the window?      over -&gt; error entry, return    (layer 3)
 * 4. event sub type among the process triggers?          no   -&gt; return                 (layer 2)
 * 5. process key: the command's own first, then the instance's, and the labels last
 *                    none          -&gt; a deletion is published anyway, everything else returns
 *                    more than one -&gt; error entry, return
 *                    too long      -&gt; error entry, return
 * 6. start permission: a start command -&gt; yes
 *                    otherwise no live instance, no completed instance, and an AUTOMATIC label naming the same key
 * 7. insert the row, addressed to the process consumer of the namespace, and signal that it is there
 * </pre>
 *
 * Layer 1 goes on intent rather than on identity: a process saying itself that it does not want to be woken is what
 * saves SM from having to recognise every process engine by name.
 * <p>
 * The three layers are there for machine traffic about an errand, and two kinds of event pass them all. A command is
 * a person pressing a button rather than something that happened to the errand. A deletion cannot loop, since the
 * errand is gone, and holding it back would leave the process instance running for an errand that no longer exists.
 * <p>
 * A publication that fails may not be swallowed. Every call site of {@code createErrandEvent} catches Exception and
 * logs a warning, so a publication that merely threw would leave the errand change saved while the process was never
 * told - the very thing the outbox exists to prevent. The transaction is therefore marked rollback only before the
 * exception is handed on, and the errand change goes down with the row whatever the caller does with it.
 */
@Component
public class ProcessEventPublisher {

	static final String LOOP_GUARD_ACTIVITY_TYPE = "LOOP_GUARD";
	static final String CONFIG_ACTIVITY_TYPE = "CONFIG";

	private static final Logger LOG = LoggerFactory.getLogger(ProcessEventPublisher.class);

	private static final String OPT_OUT = "false";

	private static final String LOOP_GUARD_ERROR_CODE = "EVENT_RATE_EXCEEDED";
	private static final String AMBIGUOUS_KEY_ERROR_CODE = "AMBIGUOUS_PROCESS_KEY";
	private static final String OVERSIZED_KEY_ERROR_CODE = "OVERSIZED_PROCESS_KEY";

	/** How much of a key is worth showing in an entry that reports what is wrong with it. */
	private static final int KEY_EXCERPT_LENGTH = 64;

	private static final String LOOP_GUARD_TRIPPED = """
		emergency brake tripped: %d events, the most allowed, have reached the process of this errand within %s, and further \
		events are being dropped. Something is waking the errand in a loop - find what writes to it, and check that \
		the process asks not to be woken by its own writes""";
	private static final String AMBIGUOUS_KEYS = """
		the labels of this errand carry more than one process key (%s), so no process is started for it. Take the \
		process key off all but one of the labels, or start the handling by hand and point out the key to use""";
	private static final String OVERSIZED_KEY = """
		the process key on the labels of this errand is %d characters long, and a process key may hold at most %d. No \
		process is started for it, and no event about it reaches one. Shorten the processKey attribute of the label to \
		the key the process is actually deployed under. The key begins '%s'""";
	private static final String UNKNOWN_EVENT_TYPE = "a process is told of a creation, an update or a deletion, and an event of type %s is none of them";

	private final NamespaceConfigService namespaceConfigService;
	private final ProcessEventOutboxRepository outboxRepository;
	private final ErrandProcessRepository processRepository;
	private final ProcessKeySelector processKeySelector;
	private final ProcessErrorLog errorLog;
	private final ProcessEngineProperties processEngineProperties;
	private final Clock clock;
	private final ApplicationEventPublisher applicationEventPublisher;

	public ProcessEventPublisher(
		final NamespaceConfigService namespaceConfigService,
		final ProcessEventOutboxRepository outboxRepository,
		final ErrandProcessRepository processRepository,
		final ProcessKeySelector processKeySelector,
		final ProcessErrorLog errorLog,
		final ProcessEngineProperties processEngineProperties,
		final Clock clock,
		final ApplicationEventPublisher applicationEventPublisher) {

		this.namespaceConfigService = namespaceConfigService;
		this.outboxRepository = outboxRepository;
		this.processRepository = processRepository;
		this.processKeySelector = processKeySelector;
		this.errorLog = errorLog;
		this.processEngineProperties = processEngineProperties;
		this.clock = clock;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	/**
	 * Writes what the process is to be told about an errand event, if anything.
	 *
	 * @param errand         the errand the event is about.
	 * @param eventType      the type of the event.
	 * @param eventSubType   what happened, which is what the process triggers of the namespace are held against.
	 * @param executedBy     the identity behind the write, whatever it calls itself.
	 * @param requestGroupId the group the write belongs to, so that a double delivery can be traced afterwards.
	 * @param command        the command the event carries, or null for an ordinary errand event.
	 */
	@Transactional(propagation = SUPPORTS)
	public void publish(final ErrandEntity errand, final EventType eventType, final EventSubType eventSubType, final String executedBy, final String requestGroupId, final ProcessCommand command) {
		try {
			write(errand, eventType, eventSubType, executedBy, requestGroupId, command);
		} catch (final Exception e) {
			failPublication(errand, e);
		}
	}

	private void write(final ErrandEntity errand, final EventType eventType, final EventSubType eventSubType, final String executedBy, final String requestGroupId, final ProcessCommand command) {
		final var namespace = errand.getNamespace();
		final var municipalityId = errand.getMunicipalityId();
		final var processService = namespaceConfigService.getProcessConsumer(namespace, municipalityId).orElse(null);

		if (isNull(processService)) {
			return;
		}

		final var processEventType = toProcessEventType(eventType);
		final var guarded = !eventSubType.isCommand() && DELETE != eventType;

		if (guarded && isOptedOut()) {
			LOG.debug("No process event written for errand {}: the write asked not to wake the process", sanitizeForLogging(errand.getId()));
			return;
		}

		if (guarded && isRateExceeded(errand)) {
			return;
		}

		if (guarded && !namespaceConfigService.getProcessTriggers(namespace, municipalityId).contains(eventSubType)) {
			return;
		}

		final var instances = processRepository.findByErrandIdOrderByCreatedDesc(errand.getId());
		final var selection = selectFromLabels(errand, eventType);
		final var processKey = resolveProcessKey(command, instances, selection);

		if (isNull(processKey)) {
			if (selection.isAmbiguous()) {
				errorLog.writeOncePerWindow(errand.getId(), null, CONFIG_ACTIVITY_TYPE, AMBIGUOUS_KEY_ERROR_CODE, AMBIGUOUS_KEYS.formatted(excerptOf(selection.keys())));
				return;
			}

			if (DELETE != eventType) {
				return;
			}
		} else if (processKey.length() > PROCESS_KEY_LENGTH) {
			// Left to the insert, one mistyped label would fail every write to every errand wearing it.
			errorLog.writeOncePerWindow(errand.getId(), null, CONFIG_ACTIVITY_TYPE, OVERSIZED_KEY_ERROR_CODE,
				OVERSIZED_KEY.formatted(processKey.length(), PROCESS_KEY_LENGTH, StringUtils.abbreviate(processKey, KEY_EXCERPT_LENGTH)));
			return;
		}

		outboxRepository.save(ProcessEventOutboxEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withErrandId(errand.getId())
			.withProcessService(processService)
			.withProcessKey(processKey)
			.withEventType(processEventType)
			.withEventSubType(eventSubType.getValue())
			.withStartAllowed(isStartAllowed(eventSubType, processKey, instances, selection))
			// Never cut: a shortened name would correlate another gate, so one that does not fit fails the command loudly.
			.withSignalName(ofNullable(command).map(ProcessCommand::signalName).orElse(null))
			.withExecutedBy(StringUtils.truncate(executedBy, EXECUTED_BY_LENGTH))
			.withRequestGroupId(requestGroupId));

		applicationEventPublisher.publishEvent(new ProcessEventWritten(errand.getId()));
	}

	/**
	 * The event type a process is told, refusing one no process knows while the caller is still there to fail.
	 * <p>
	 * A process engine knows a creation, an update and a deletion, and nothing else. Left to the delivery, any other type
	 * would be a row the relay can never turn into an event - read first again on every run, it would hold back every later
	 * event of its errand until it aged out. Nothing publishes another type today; this is for the next call site that
	 * does. It is asked before the loop guard and the triggers, so that such a call fails in the first test that reaches it
	 * rather than in production on the day a namespace starts triggering on its sub type.
	 * <p>
	 * The switch has no default, so a type added to the event log does not compile until it has been decided here.
	 */
	private static String toProcessEventType(final EventType eventType) {
		return switch (eventType) {
			case CREATE, UPDATE, DELETE -> eventType.getValue();
			case READ, ACCESS, EXECUTE, CANCEL, DROP -> throw new IllegalArgumentException(UNKNOWN_EVENT_TYPE.formatted(eventType.getValue()));
		};
	}

	/**
	 * Layer 1 of the loop guard: did the caller want to wake the process at all?
	 * <p>
	 * Only an exact false, trimmed and whatever its casing, keeps the row from being written. Everything else - a
	 * missing header, an empty one, nonsense - wakes the process, and the direction is chosen on purpose. One row too
	 * many is a needless wake that layers 2 and 3 catch, while one row too few is a process left waiting for ever with
	 * nobody noticing.
	 * <p>
	 * The header is not honoured for ad accounts. A handler's write wakes the process however the client sets it, and
	 * that closes the one hole a freely set header would otherwise open: somebody else's integration quietly silencing
	 * real errand changes. The machine to machine calls are precisely the ones without an ad account.
	 */
	private boolean isOptedOut() {
		return isNull(getAdUser()) && Strings.CI.equals(OPT_OUT, StringUtils.trimToNull(getTriggerProcess()));
	}

	/**
	 * Layer 3 of the loop guard: how fast are events reaching the process of this errand?
	 * <p>
	 * Only delivered rows are counted. Counting the ones still waiting would let a delivery outage trip the brake by
	 * itself - the rows pile up because nothing gets through, the brake reads the pile as a loop, and an outage that
	 * only cost time turns into permanent event loss, at the very moment nothing at all was getting through.
	 */
	private boolean isRateExceeded(final ErrandEntity errand) {
		final var guard = processEngineProperties.loopGuard();
		final var delivered = outboxRepository.countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter(errand.getId(), OffsetDateTime.now(clock).minus(guard.window()));

		if (delivered < guard.maxEventsPerErrand()) {
			return false;
		}

		LOG.error("Dropping process event for errand {}: {} events have reached its process within {}", sanitizeForLogging(errand.getId()), delivered, guard.window());
		errorLog.writeOncePerWindow(errand.getId(), null, LOOP_GUARD_ACTIVITY_TYPE, LOOP_GUARD_ERROR_CODE, LOOP_GUARD_TRIPPED.formatted(guard.maxEventsPerErrand(), guard.window()));

		return true;
	}

	/**
	 * What the labels say, asked for every event but a deletion.
	 * <p>
	 * A deletion starts nothing and needs no key, and by the time one is published the errand it belongs to is normally
	 * already gone - along with the labels, which can no longer be read off the detached entity. Not asking is part of
	 * what lets the deletion be published at all.
	 */
	private ProcessKeySelection selectFromLabels(final ErrandEntity errand, final EventType eventType) {
		return DELETE == eventType ? ProcessKeySelection.NONE : processKeySelector.select(errand);
	}

	/**
	 * Which process the row is for.
	 * <p>
	 * A start command carries its own key and it goes before everything else: the handler has already chosen, and
	 * resolving that choice again here would find two keys on an ambiguous errand and drop the command in exactly the
	 * case it exists for.
	 * <p>
	 * After that the instance answers before the labels do. Once an errand has a process instance the key is nailed
	 * down, and a label that is changed or removed can no longer change what is published. Every instance of an errand
	 * runs the same process, so it does not matter which of them answers.
	 */
	private String resolveProcessKey(final ProcessCommand command, final List<ErrandProcessEntity> instances, final ProcessKeySelection selection) {
		return ofNullable(command)
			.map(ProcessCommand::processKey)
			.filter(StringUtils::isNotBlank)
			.or(() -> instances.stream().map(ErrandProcessEntity::getProcessKey).findFirst())
			.orElseGet(selection::processKey);
	}

	/**
	 * Whether this event may give birth to a process instance.
	 * <p>
	 * Worked out once, here, and carried along with the event, so that the process engine has to know neither the start
	 * mode of the labels nor the process history of the errand. It costs no extra query: step 5 already reads the
	 * process rows of the errand, and both halves of the question are answered from them. Only a start command asks for
	 * an instance to be born; a signal is aimed at one that already runs.
	 * <p>
	 * The mode counts only when it comes from the label naming the key the row carries. An errand whose failed instance
	 * ran one process while its labels now point at another would otherwise take the key from the instance and the mode
	 * from a label that has nothing to do with it.
	 * <p>
	 * The permission is optimistic and the registration is authoritative. A process can reach its end between
	 * publication and delivery, and the conflict answered when a start is registered is the safety net for that.
	 */
	private boolean isStartAllowed(final EventSubType eventSubType, final String processKey, final List<ErrandProcessEntity> instances, final ProcessKeySelection selection) {
		if (eventSubType.isCommand()) {
			return PROCESS == eventSubType;
		}

		return AUTOMATIC == selection.startMode()
			&& selection.processKey().equals(processKey)
			&& instances.stream().noneMatch(instance -> nonNull(instance.getActiveMarker()))
			&& instances.stream().noneMatch(instance -> COMPLETED == instance.getProcessStatus());
	}

	/**
	 * The keys of an ambiguous errand, shortened, for an entry that has to name them without being made of them.
	 */
	private String excerptOf(final List<String> keys) {
		return keys.stream()
			.map(key -> StringUtils.abbreviate(key, KEY_EXCERPT_LENGTH))
			.collect(joining(", "));
	}

	/**
	 * Takes the errand change down with the row that could not be written.
	 * <p>
	 * With no transaction there is nothing to roll back and the errand is already saved, so the failure is logged as the
	 * error it is and the call is left alone. Every way in has a transaction today, so it should never happen.
	 * <p>
	 * The transaction is reached through the aspect, and that is why {@link #publish} carries a transaction annotation of
	 * its own. Without one, the status is bound only when the caller reached here through an annotated method, and a
	 * caller running its own transaction template - as the process service does, for the sake of its collision recovery -
	 * would get a complaint about there being no transaction instead of the rollback, losing the original failure with
	 * it. Supports rather than required, since a write without a transaction is to be reported and not given one.
	 * <p>
	 * The failure is handed on rather than logged here: the caller catches it, and what surfaces from the rollback is
	 * logged where it is turned into a response.
	 */
	private void failPublication(final ErrandEntity errand, final Exception cause) {
		if (!TransactionSynchronizationManager.isActualTransactionActive()) {
			LOG.error("Failed to publish process event for errand {}, and nothing could be rolled back since the write runs without a transaction", sanitizeForLogging(errand.getId()), cause);
			return;
		}

		TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

		if (cause instanceof final RuntimeException runtimeException) {
			throw runtimeException;
		}

		throw new IllegalStateException("Failed to publish process event for errand " + errand.getId(), cause);
	}
}
