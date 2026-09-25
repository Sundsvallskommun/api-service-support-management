package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandLifecycle;
import se.sundsvall.supportmanagement.service.model.ProcessKeySelection;
import se.sundsvall.supportmanagement.service.model.ProcessStartOptions;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.supportmanagement.api.model.process.ProcessStartability.AVAILABLE;
import static se.sundsvall.supportmanagement.api.model.process.ProcessStartability.ERRAND_DRAFT;
import static se.sundsvall.supportmanagement.api.model.process.ProcessStartability.LIVE_INSTANCE;
import static se.sundsvall.supportmanagement.api.model.process.ProcessStartability.NO_PROCESS_ENGINE;
import static se.sundsvall.supportmanagement.api.model.process.ProcessStartability.NO_PROCESS_KEY;
import static se.sundsvall.supportmanagement.api.model.process.ProcessStartability.PROCESS_COMPLETED;
import static se.sundsvall.supportmanagement.api.model.process.ProcessStartability.START_PENDING;
import static se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity.PROCESS_KEY_LENGTH;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ErrandLifecycle.DRAFT;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.service.model.ProcessStartOptions.unavailable;

/**
 * The rules of the process life of an errand, read off its process rows: whether a process lives, whether the process
 * life is over, whether a process may be started and with which keys, and whether a namespace runs processes at all.
 * <p>
 * Every instance takes the process rows of an errand as
 * {@link ErrandProcessRepository#findByErrandIdOrderByCreatedDesc} reads them.
 */
public final class ProcessRules {

	static final String NO_PROCESS_CONSUMER = "The namespace '%s' in municipality '%s' has no process consumer configured and runs no process";

	private ProcessRules() {}

	/**
	 * Whether a process instance of the errand is alive.
	 *
	 * @param  instances the process rows of the errand.
	 * @return           true when any of them lives.
	 */
	public static boolean hasLiveProcess(final List<ErrandProcessEntity> instances) {
		return instances.stream().anyMatch(ErrandProcessEntity::isLive);
	}

	/**
	 * Whether the process life of an errand is over.
	 * <p>
	 * Once it is, a process is never started for the errand again, and the decisions of the errand can no longer be
	 * changed. A failed process leaves the life open.
	 *
	 * @param  instances the process rows of the errand.
	 * @return           true when any of them ran to its end.
	 */
	public static boolean hasCompletedProcess(final List<ErrandProcessEntity> instances) {
		return instances.stream().anyMatch(instance -> COMPLETED == instance.getProcessStatus());
	}

	/**
	 * Whether a process may be started for an errand, and with which keys - the rules the start command is held to.
	 * Asked in this order, and the first obstacle found is the answer:
	 *
	 * <pre>
	 * the namespace has no process consumer        -&gt; NO_PROCESS_ENGINE
	 * the errand is a draft                        -&gt; ERRAND_DRAFT
	 * a live instance                              -&gt; LIVE_INSTANCE
	 * a completed instance                         -&gt; PROCESS_COMPLETED
	 * no label naming a process the errand can run -&gt; NO_PROCESS_KEY
	 * otherwise                                    -&gt; AVAILABLE, with every key the labels name
	 * </pre>
	 *
	 * Once the errand has a process row - a failed start included - only the key of that process counts. A key longer
	 * than a process key may be is never offered. The start mode of the labels is not read.
	 *
	 * @param  runsProcesses whether the namespace of the errand has a process consumer.
	 * @param  lifecycle     the life cycle of the errand.
	 * @param  instances     the process rows of the errand.
	 * @param  labels        what the labels of the errand say, asked only when no process row stands in the way.
	 * @return               whether a start is possible, and the keys it may name.
	 */
	public static ProcessStartOptions startOptionsOf(final boolean runsProcesses, final ErrandLifecycle lifecycle, final List<ErrandProcessEntity> instances,
		final Supplier<ProcessKeySelection> labels) {
		if (!runsProcesses) {
			return unavailable(NO_PROCESS_ENGINE);
		}

		if (DRAFT == lifecycle) {
			return unavailable(ERRAND_DRAFT);
		}

		if (hasLiveProcess(instances)) {
			return unavailable(LIVE_INSTANCE);
		}

		if (hasCompletedProcess(instances)) {
			return unavailable(PROCESS_COMPLETED);
		}

		final var keys = labels.get().keys().stream()
			.filter(key -> !isOversized(key))
			.filter(key -> instances.stream().allMatch(instance -> key.equals(instance.getProcessKey())))
			.toList();

		return keys.isEmpty() ? unavailable(NO_PROCESS_KEY) : new ProcessStartOptions(AVAILABLE, keys);
	}

	/**
	 * Whether a process may be started for an errand as the {@code startable} field shows it: the answer of
	 * {@link #startOptionsOf}, except that an errand whose start is already on its way is START_PENDING rather than
	 * AVAILABLE.
	 *
	 * @param  runsProcesses whether the namespace of the errand has a process consumer.
	 * @param  lifecycle     the life cycle of the errand.
	 * @param  instances     the process rows of the errand.
	 * @param  labels        what the labels of the errand say, asked only when no process row stands in the way.
	 * @param  startOnItsWay whether an undelivered event of the errand carries the permission to start a process, asked
	 *                       only when a start would otherwise be available.
	 * @return               whether a start is possible, and the keys it may name.
	 */
	public static ProcessStartOptions startableOf(final boolean runsProcesses, final ErrandLifecycle lifecycle, final List<ErrandProcessEntity> instances,
		final Supplier<ProcessKeySelection> labels, final BooleanSupplier startOnItsWay) {

		final var options = startOptionsOf(runsProcesses, lifecycle, instances, labels);

		return AVAILABLE == options.status() && startOnItsWay.getAsBoolean() ? unavailable(START_PENDING) : options;
	}

	/**
	 * The process consumer of a namespace, which a namespace that runs no process lacks.
	 *
	 * @param  consumer                                     the process consumer of the namespace, as configured.
	 * @param  namespace                                    the namespace.
	 * @param  municipalityId                               the municipality of the namespace.
	 * @return                                              the process consumer.
	 * @throws se.sundsvall.dept44.problem.ThrowableProblem 400 when the namespace has no process consumer.
	 */
	public static String requireProcessConsumer(final Optional<String> consumer, final String namespace, final String municipalityId) {
		return consumer.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, NO_PROCESS_CONSUMER.formatted(namespace, municipalityId)));
	}

	/**
	 * Whether a process key is longer than a process key may be.
	 *
	 * @param  processKey the key to measure.
	 * @return            true when the key does not fit.
	 */
	public static boolean isOversized(final String processKey) {
		return processKey.length() > PROCESS_KEY_LENGTH;
	}
}
