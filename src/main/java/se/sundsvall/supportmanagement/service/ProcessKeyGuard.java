package se.sundsvall.supportmanagement.service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.service.model.ProcessKeySelection;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toSet;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.service.ProcessActivityLog.CONFIG_ACTIVITY_TYPE;

/**
 * Holds the labels of an errand to the one process it belongs to.
 * <p>
 * The process is read off the labels ({@link ProcessKeySelector}). Two rules are asked, in this order, numbered as in
 * the solution document:
 * <ul>
 * <li>Rule 5: the labels of an errand may name at most one process. This is asked of every errand, whether it has a
 * process or not.</li>
 * <li>Rule 1: from the moment an errand has a process row, or a start of a process on its way, its labels have to go on
 * resolving to the keys they resolved to before, unless the change points them at the process the errand runs or is
 * being started with. This holds for a process that has finished too.</li>
 * </ul>
 *
 * Only the key is held still. The start mode is read off the same labels and may be changed freely.
 * <p>
 * The writers that reach the labels, and what a refusal gives each of them:
 *
 * <pre>
 * POST /errands            -&gt; ErrandService.createErrand       -&gt; 400, and only rule 5: a new errand has no process
 * PATCH /errands/{id}      -&gt; ErrandService.updateErrand       -&gt; 400
 * a scheduled job          -&gt; AddLabelAction.executeAction     -&gt; the labels are left off, and an entry says so
 * a moved label            -&gt; ErrandService.persistLabelUpdate -&gt; the errand is left as it is, and an entry says so
 * </pre>
 */
@Component
public class ProcessKeyGuard {

	static final String TWO_PROCESSES_ERROR_CODE = "LABELS_NAME_TWO_PROCESSES";
	static final String MOVES_KEY_ERROR_CODE = "LABEL_MOVES_PROCESS_KEY";

	private static final Logger LOG = LoggerFactory.getLogger(ProcessKeyGuard.class);

	private static final String NAMED_KEY = "the process key '%s'";
	private static final String NO_KEY = "no single process key";

	private static final String TWO_PROCESSES = """
		The labels would name more than one process (%s), and an errand belongs to one. Labels naming two processes \
		resolve to neither, so the errand would run none of them and be told about nothing. Take the process key off \
		all but one of the labels.""";

	private static final String MOVES_KEY = """
		The labels of errand '%s' resolve to %s, and the change would leave them resolving to %s. The errand runs the \
		process '%s', or has a start of it on its way, and an errand runs the one process for the whole of its life - \
		labels naming another would leave the process it does run with nothing to wake it. Leave the label carrying the \
		process key as it is, and handle the other process on an errand of its own.""";

	private final ErrandProcessRepository processRepository;
	private final ProcessEventOutboxRepository outboxRepository;
	private final ProcessKeySelector processKeySelector;
	private final ProcessActivityLog activityLog;

	public ProcessKeyGuard(
		final ErrandProcessRepository processRepository,
		final ProcessEventOutboxRepository outboxRepository,
		final ProcessKeySelector processKeySelector,
		final ProcessActivityLog activityLog) {

		this.processRepository = processRepository;
		this.outboxRepository = outboxRepository;
		this.processKeySelector = processKeySelector;
		this.activityLog = activityLog;
	}

	/**
	 * Rule 5, asked of the labels an errand is created with. Throws 400 when they name more than one process. Rule 1 is
	 * not asked.
	 *
	 * @param labels the labels the errand would be created with, ancestors included.
	 */
	public void verifyNewLabels(final Collection<ErrandLabelEmbeddable> labels) {
		ambiguityIn(processKeySelector.select(labels))
			.ifPresent(refusal -> {
				throw Problem.valueOf(BAD_REQUEST, refusal.reason());
			});
	}

	/**
	 * Both rules, asked of a label change on an errand that exists. Throws 400 when the change is refused.
	 *
	 * @param errandId     the errand being changed.
	 * @param labelsBefore the labels it wore before the change.
	 * @param labelsAfter  the labels it would wear after it, ancestors included.
	 */
	public void verifyLabelChange(final String errandId, final Collection<ErrandLabelEmbeddable> labelsBefore, final Collection<ErrandLabelEmbeddable> labelsAfter) {
		findRefusal(errandId, labelsBefore, labelsAfter).ifPresent(refusal -> {
			throw Problem.valueOf(BAD_REQUEST, refusal.reason());
		});
	}

	/**
	 * Both rules, asked of a label change made by a writer with no caller to answer, such as a scheduled job. A refusal is
	 * not thrown but logged and written as an error entry on the errand, once per window.
	 *
	 * @param  errandId     the errand being changed.
	 * @param  labelsBefore the labels it wears.
	 * @param  labelsAfter  the labels it would wear after the change.
	 * @param  consequence  what the writer does about a refusal, which the error entry tells after the reason.
	 * @return              true when the change is refused, in which case an error entry has been written on the errand.
	 */
	public boolean refusesLabelChange(final String errandId, final Collection<ErrandLabelEmbeddable> labelsBefore, final Collection<ErrandLabelEmbeddable> labelsAfter, final String consequence) {
		final var refusal = findRefusal(errandId, labelsBefore, labelsAfter);

		refusal.ifPresent(refused -> {
			LOG.warn("Labels of errand {} were not changed: {}", sanitizeForLogging(errandId), refused.errorCode());
			activityLog.writeOncePerWindow(errandId, null, CONFIG_ACTIVITY_TYPE, refused.errorCode(), refused.reason() + " " + consequence);
		});

		return refusal.isPresent();
	}

	/**
	 * Why the labels may not be settled on the errand, and empty when they may.
	 * <p>
	 * Rule 1 compares the keys the labels resolve to before with the keys they would resolve to after, so a key taken
	 * away is refused as well as a key exchanged, and so is taking away the keys of labels that named two processes. A
	 * change that leaves the labels naming the process the errand runs is let through whatever they named before.
	 * <p>
	 * A change that does not touch the labels is answered before anything is read, and the process rows are read only
	 * once rule 5 has passed.
	 */
	private Optional<Refusal> findRefusal(final String errandId, final Collection<ErrandLabelEmbeddable> labelsBefore, final Collection<ErrandLabelEmbeddable> labelsAfter) {
		if (idsOf(labelsBefore).equals(idsOf(labelsAfter))) {
			return Optional.empty();
		}

		final var selectionAfter = processKeySelector.select(labelsAfter);
		final var ambiguity = ambiguityIn(selectionAfter);

		if (ambiguity.isPresent()) {
			return ambiguity;
		}

		final var running = runningKeysOf(errandId);

		if (running.isEmpty()) {
			return Optional.empty();
		}

		final var selectionBefore = processKeySelector.select(labelsBefore);

		if (selectionBefore.keys().equals(selectionAfter.keys()) || nonNull(selectionAfter.processKey()) && running.contains(selectionAfter.processKey())) {
			return Optional.empty();
		}

		return Optional.of(new Refusal(MOVES_KEY_ERROR_CODE, MOVES_KEY.formatted(errandId, describe(selectionBefore.processKey()), describe(selectionAfter.processKey()),
			ProcessKeySelector.excerptOf(running.getFirst()))));
	}

	/**
	 * The process the errand runs: the key of its process rows, which every instance of an errand shares, or when it has
	 * none the keys of the starts on their way to the process engine.
	 */
	private List<String> runningKeysOf(final String errandId) {
		final var instances = processRepository.findByErrandIdOrderByCreatedDesc(errandId);

		if (!instances.isEmpty()) {
			return List.of(instances.getFirst().getProcessKey());
		}

		return outboxRepository.findByErrandIdAndStartAllowedIsTrueAndDeliveredAtIsNull(errandId).stream()
			.map(ProcessEventOutboxEntity::getProcessKey)
			.filter(Objects::nonNull)
			.distinct()
			.toList();
	}

	/** Rule 5: a refusal when the labels name more than one process, and empty when they do not. */
	private Optional<Refusal> ambiguityIn(final ProcessKeySelection selection) {
		if (!selection.isAmbiguous()) {
			return Optional.empty();
		}

		return Optional.of(new Refusal(TWO_PROCESSES_ERROR_CODE, TWO_PROCESSES.formatted(ProcessKeySelector.excerptOf(selection.keys()))));
	}

	private Set<String> idsOf(final Collection<ErrandLabelEmbeddable> labels) {
		return labels.stream()
			.map(ErrandLabelEmbeddable::getMetadataLabelId)
			.filter(Objects::nonNull)
			.collect(toSet());
	}

	private String describe(final String key) {
		return isNull(key) ? NO_KEY : NAMED_KEY.formatted(ProcessKeySelector.excerptOf(key));
	}

	/**
	 * Why a label change is refused: the code a repetition of the same fault is recognised by, and what is wrong in
	 * words the caller and the activity log can both be given.
	 */
	private record Refusal(String errorCode, String reason) {}
}
