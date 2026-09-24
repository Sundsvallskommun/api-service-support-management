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
import org.springframework.transaction.support.TransactionSynchronization;
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
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity.EXECUTED_BY_LENGTH;
import static se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity.PROCESS_KEY_LENGTH;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ErrandLifecycle.DRAFT;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.PROCESS;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.AUTOMATIC;
import static se.sundsvall.supportmanagement.service.ProcessActivityLog.CONFIG_ACTIVITY_TYPE;
import static se.sundsvall.supportmanagement.service.ProcessActivityLog.LOOP_GUARD_ACTIVITY_TYPE;
import static se.sundsvall.supportmanagement.service.ProcessRules.hasCompletedProcess;
import static se.sundsvall.supportmanagement.service.ProcessRules.hasLiveProcess;
import static se.sundsvall.supportmanagement.service.ProcessRules.isOversized;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getAdUser;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getTriggerProcess;

/**
 * Turns an errand event into a row in the outbox, in the same transaction as the change the event is about.
 * <p>
 * Called last in {@link EventService#createErrandEvent}, which every errand event goes through, the email intake and
 * the web messages included. A command comes in through {@link EventService#createProcessCommandEvent}.
 * <p>
 * The rule it applies, step by step:
 *
 * <pre>
 * 1. process consumer for (municipalityId, namespace)?   no   -&gt; return
 *    event type CREATE, UPDATE or DELETE?                no   -&gt; throw, which takes the errand change down
 *    errand a draft?                                     yes  -&gt; return
 * 2. X-Trigger-Process: false, from a non ad identity?   yes  -&gt; return                 (loop guard, layer 1)
 *                    commands (PROCESS, SIGNAL) and deletions skip steps 2, 3 and 4
 * 3. event sub type among the process triggers?          no   -&gt; return                 (layer 2)
 * 4. delivered events for the errand in the window?      over -&gt; error entry, return    (layer 3)
 *                    a decision concluded by an ad account skips this step
 * 5. process key: the command's own first, then the instance's, and the labels last
 *                    none          -&gt; a deletion is published anyway, everything else returns
 *                    more than one -&gt; error entry, return
 *                    too long      -&gt; error entry, return
 * 6. start permission: a start command -&gt; yes
 *                    otherwise no live instance, no completed instance, no start of another process on its way,
 *                    and an AUTOMATIC label naming the same key
 * 7. insert the row, addressed to the process consumer of the namespace, and signal that it is there - once per
 *    errand and transaction
 * </pre>
 *
 * Layer 1 lets a process ask, through the header, not to be woken by its own writes.
 * <p>
 * The three layers guard the machine traffic about an errand. Commands and deletions pass them all.
 * <p>
 * A decision concluded by a handler passes the emergency brake and nothing else; the process triggers still apply. A
 * decision the process concludes itself is held to the brake like any other write of the process.
 * <p>
 * When a publication fails, the transaction is marked rollback only before the exception is handed on, so the errand
 * change is rolled back with the row whatever the caller does with the exception.
 */
@Component
public class ProcessEventPublisher {

	private static final Logger LOG = LoggerFactory.getLogger(ProcessEventPublisher.class);

	private static final String OPT_OUT = "false";

	private static final String LOOP_GUARD_ERROR_CODE = "EVENT_RATE_EXCEEDED";
	private static final String AMBIGUOUS_KEY_ERROR_CODE = "AMBIGUOUS_PROCESS_KEY";
	private static final String OVERSIZED_KEY_ERROR_CODE = "OVERSIZED_PROCESS_KEY";

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
	private final ProcessActivityLog activityLog;
	private final ProcessEngineProperties processEngineProperties;
	private final Clock clock;
	private final ApplicationEventPublisher applicationEventPublisher;

	public ProcessEventPublisher(
		final NamespaceConfigService namespaceConfigService,
		final ProcessEventOutboxRepository outboxRepository,
		final ErrandProcessRepository processRepository,
		final ProcessKeySelector processKeySelector,
		final ProcessActivityLog activityLog,
		final ProcessEngineProperties processEngineProperties,
		final Clock clock,
		final ApplicationEventPublisher applicationEventPublisher) {

		this.namespaceConfigService = namespaceConfigService;
		this.outboxRepository = outboxRepository;
		this.processRepository = processRepository;
		this.processKeySelector = processKeySelector;
		this.activityLog = activityLog;
		this.processEngineProperties = processEngineProperties;
		this.clock = clock;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	/**
	 * Writes what the process is to be told about an errand event, if anything.
	 *
	 * @param errand            the errand the event is about.
	 * @param eventType         the type of the event.
	 * @param eventSubType      what happened, which is what the process triggers of the namespace are held against.
	 * @param executedBy        the identity behind the write, whatever it calls itself.
	 * @param requestGroupId    the group the write belongs to, so that a double delivery can be traced afterwards.
	 * @param command           the command the event carries, or null for an ordinary errand event.
	 * @param concludesDecision whether the event is a decision of the errand being concluded, which passes the emergency
	 *                          brake when a handler concludes it.
	 */
	@Transactional(propagation = SUPPORTS)
	public void publish(final ErrandEntity errand, final EventType eventType, final EventSubType eventSubType, final String executedBy, final String requestGroupId, final ProcessCommand command,
		final boolean concludesDecision) {
		try {
			write(errand, eventType, eventSubType, executedBy, requestGroupId, command, concludesDecision);
		} catch (final Exception e) {
			failPublication(errand, e);
		}
	}

	private void write(final ErrandEntity errand, final EventType eventType, final EventSubType eventSubType, final String executedBy, final String requestGroupId, final ProcessCommand command,
		final boolean concludesDecision) {
		final var namespace = errand.getNamespace();
		final var municipalityId = errand.getMunicipalityId();
		final var processService = namespaceConfigService.getProcessConsumer(namespace, municipalityId).orElse(null);

		if (isNull(processService)) {
			return;
		}

		final var processEventType = toProcessEventType(eventType);

		if (DRAFT == errand.getLifecycle()) {
			LOG.debug("No process event written for errand {}: the errand is a draft", sanitizeForLogging(errand.getId()));
			return;
		}

		final var guarded = !eventSubType.isCommand() && DELETE != eventType;

		if (guarded && isOptedOut()) {
			LOG.debug("No process event written for errand {}: the write asked not to wake the process", sanitizeForLogging(errand.getId()));
			return;
		}

		if (guarded && !namespaceConfigService.getProcessTriggers(namespace, municipalityId).contains(eventSubType)) {
			return;
		}

		if (guarded && !concludedByPerson(concludesDecision) && isRateExceeded(errand)) {
			return;
		}

		final var instances = processRepository.findByErrandIdOrderByCreatedDesc(errand.getId());
		final var selection = selectFromLabels(errand, eventType);
		final var processKey = resolveProcessKey(command, instances, selection);

		if (isNull(processKey)) {
			if (selection.isAmbiguous()) {
				activityLog.writeOncePerWindow(errand.getId(), null, CONFIG_ACTIVITY_TYPE, AMBIGUOUS_KEY_ERROR_CODE, AMBIGUOUS_KEYS.formatted(ProcessKeySelector.excerptOf(selection.keys())));
				return;
			}

			if (DELETE != eventType) {
				return;
			}
		} else if (isOversized(processKey)) {
			// Left to the insert, one mistyped label would fail every write to every errand wearing it.
			activityLog.writeOncePerWindow(errand.getId(), null, CONFIG_ACTIVITY_TYPE, OVERSIZED_KEY_ERROR_CODE,
				OVERSIZED_KEY.formatted(processKey.length(), PROCESS_KEY_LENGTH, ProcessKeySelector.excerptOf(processKey)));
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
			.withStartAllowed(isStartAllowed(errand.getId(), eventSubType, processKey, instances, selection))
			// Never cut: a shortened name would correlate another gate, so one that does not fit fails the command loudly.
			.withSignalName(ofNullable(command).map(ProcessCommand::signalName).orElse(null))
			.withExecutedBy(StringUtils.truncate(executedBy, EXECUTED_BY_LENGTH))
			.withRequestGroupId(requestGroupId));

		if (isFirstRowOfTransaction(errand.getId())) {
			applicationEventPublisher.publishEvent(new ProcessEventWritten(errand.getId()));
		}
	}

	/**
	 * The event type a process is told, refusing one no process knows while the caller is still there to fail.
	 * <p>
	 * A process is told of a creation, an update or a deletion; any other type throws {@link IllegalArgumentException}. It
	 * is asked before the loop guard and the triggers, so another type fails the call even when the event would have been
	 * dropped.
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
	 * missing header, an empty one, nonsense - wakes the process.
	 * <p>
	 * The header is not honoured for ad accounts: a handler's write wakes the process however the client sets it.
	 */
	private boolean isOptedOut() {
		return isNull(getAdUser()) && Strings.CI.equals(OPT_OUT, StringUtils.trimToNull(getTriggerProcess()));
	}

	/**
	 * Whether the event is a decision concluded by an ad account, the one kind that passes the emergency brake. A
	 * decision concluded without an ad account is held to the brake.
	 */
	private static boolean concludedByPerson(final boolean concludesDecision) {
		return concludesDecision && nonNull(getAdUser());
	}

	/**
	 * Layer 3 of the loop guard: how fast are events reaching the process of this errand?
	 * <p>
	 * Only delivered rows are counted; rows still waiting for delivery do not trip the brake. A tripped brake is logged
	 * and written as an error entry once per window.
	 */
	private boolean isRateExceeded(final ErrandEntity errand) {
		final var guard = processEngineProperties.loopGuard();
		final var delivered = outboxRepository.countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter(errand.getId(), OffsetDateTime.now(clock).minus(guard.window()));

		if (delivered < guard.maxEventsPerErrand()) {
			return false;
		}

		LOG.error("Dropping process event for errand {}: {} events have reached its process within {}", sanitizeForLogging(errand.getId()), delivered, guard.window());
		activityLog.writeOncePerWindow(errand.getId(), null, LOOP_GUARD_ACTIVITY_TYPE, LOOP_GUARD_ERROR_CODE, LOOP_GUARD_TRIPPED.formatted(guard.maxEventsPerErrand(), guard.window()));

		return true;
	}

	/**
	 * What the labels say, asked for every event but a deletion.
	 * <p>
	 * For a deletion the labels are not read and {@link ProcessKeySelection#NONE} is returned, so a deletion can be
	 * published after the errand and its labels are gone.
	 */
	private ProcessKeySelection selectFromLabels(final ErrandEntity errand, final EventType eventType) {
		return DELETE == eventType ? ProcessKeySelection.NONE : processKeySelector.select(errand);
	}

	/**
	 * Which process the row is for.
	 * <p>
	 * The key a command carries goes before everything else, so a start command on an errand with ambiguous labels is
	 * published with the key the handler chose.
	 * <p>
	 * After that the instance answers before the labels do. Once an errand has a process instance the key is fixed, and
	 * a label that is changed or removed no longer changes what is published.
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
	 * The answer is carried along with the event, so the process engine needs neither the start mode of the labels nor
	 * the process history of the errand. It is answered from the process rows already read in step 5. A start command is
	 * allowed to start an instance; a signal is not.
	 * <p>
	 * Any other event is allowed only when the label naming the key the row carries has the start mode AUTOMATIC, the
	 * errand has neither a live nor a completed process instance, and no start of another process is on its way for it.
	 * The outbox is read for the last of these only when the others hold.
	 * <p>
	 * The permission is optimistic and the registration is authoritative: a process can reach its end between
	 * publication and delivery, and registering the start then answers with a conflict.
	 */
	private boolean isStartAllowed(final String errandId, final EventSubType eventSubType, final String processKey, final List<ErrandProcessEntity> instances, final ProcessKeySelection selection) {
		if (eventSubType.isCommand()) {
			return PROCESS == eventSubType;
		}

		return AUTOMATIC == selection.startMode()
			&& selection.processKey().equals(processKey)
			&& !hasLiveProcess(instances)
			&& !hasCompletedProcess(instances)
			&& outboxRepository.findByErrandIdAndStartAllowedIsTrueAndDeliveredAtIsNull(errandId).stream()
				.allMatch(waiting -> processKey.equals(waiting.getProcessKey()));
	}

	/**
	 * Whether this is the first row written for the errand in the current transaction, which is the one that signals the
	 * direct run. Always true when no transaction is synchronised.
	 */
	private static boolean isFirstRowOfTransaction(final String errandId) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return true;
		}

		final var marker = new RowWritten(errandId);

		if (TransactionSynchronizationManager.getSynchronizations().contains(marker)) {
			return false;
		}

		TransactionSynchronizationManager.registerSynchronization(marker);
		return true;
	}

	/**
	 * Takes the errand change down with the row that could not be written.
	 * <p>
	 * The transaction is marked rollback only and the failure is handed on to the caller without being logged here, a
	 * checked one wrapped in an {@link IllegalStateException}. With no transaction there is nothing to roll back and the
	 * errand is already saved, so the failure is logged as an error and not rethrown.
	 * <p>
	 * The transaction status is reached through the aspect, and is bound by the transaction annotation of
	 * {@link #publish}, also for a caller running its own transaction template. The propagation is supports, so a write
	 * without a transaction is reported and not given one.
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

	/**
	 * Marks, in the synchronisations of a transaction, that a row has been written for the errand. Does nothing on its
	 * own.
	 */
	private record RowWritten(String errandId)
		implements
		TransactionSynchronization {}
}
