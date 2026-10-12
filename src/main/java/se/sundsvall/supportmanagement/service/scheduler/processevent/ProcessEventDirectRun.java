package se.sundsvall.supportmanagement.service.scheduler.processevent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties;
import se.sundsvall.supportmanagement.integration.pwalkt.PwAlktUnavailableException;
import se.sundsvall.supportmanagement.service.ProcessEventWritten;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.service.scheduler.processevent.ProcessEventDirectRunConfig.PROCESS_EVENT_EXECUTOR;

/**
 * Starts a delivery as soon as a transaction that wrote to the outbox is committed, ahead of the next scheduled run.
 * <p>
 * It only brings the delivery forward, and wakes the relay, not the process. A run that is dropped, fails or never
 * starts loses nothing: the scheduled run delivers the row within a minute.
 * <p>
 * Nothing here fails the write that sent the signal. The pool drops a run it has no room for without throwing in the
 * committing thread, and whatever else goes wrong is logged and left to the scheduled run.
 */
@Component
public class ProcessEventDirectRun {

	private static final Logger LOG = LoggerFactory.getLogger(ProcessEventDirectRun.class);

	private final ProcessEventRelay processEventRelay;
	private final TaskExecutor executor;
	private final ProcessEngineProperties processEngineProperties;

	public ProcessEventDirectRun(
		final ProcessEventRelay processEventRelay,
		@Qualifier(PROCESS_EVENT_EXECUTOR) final TaskExecutor executor,
		final ProcessEngineProperties processEngineProperties) {

		this.processEventRelay = processEventRelay;
		this.executor = executor;
		this.processEngineProperties = processEngineProperties;
	}

	/**
	 * Hands the delivery to the pool once the row is committed, or at once when it was written without a transaction.
	 * The pool drops a run it has no room for, so handing it over does not throw in the thread that committed the
	 * errand.
	 *
	 * @param event the signal that a row has been written.
	 */
	@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)
	public void onProcessEventWritten(final ProcessEventWritten event) {
		if (processEngineProperties.directRun().enabled()) {
			executor.execute(() -> run(event.errandId()));
		}
	}

	private void run(final String errandId) {
		RequestId.init();

		try {
			processEventRelay.relayErrand(errandId);
		} catch (final PwAlktUnavailableException e) {
			LOG.warn("Direct run for errand {} did not reach pw-alkt, and the scheduled run tries again: {}", sanitizeForLogging(errandId), e.getMessage());
		} catch (final Exception e) {
			LOG.error("Direct run for errand {} failed, and the scheduled run tries again", sanitizeForLogging(errandId), e);
		} finally {
			RequestId.reset();
		}
	}
}
