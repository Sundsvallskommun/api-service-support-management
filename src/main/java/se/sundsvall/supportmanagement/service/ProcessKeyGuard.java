package se.sundsvall.supportmanagement.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.service.model.ProcessKeySelection;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.service.ProcessErrorLog.CONFIG_ACTIVITY_TYPE;

/**
 * Holds the labels of an errand to the one process it belongs to.
 * <p>
 * The process is read off the labels ({@link ProcessKeySelector}). Two rules are asked, in this order:
 * <ol>
 * <li>The labels of an errand may name at most one process. This is asked of every errand, whether it has a process or
 * not.</li>
 * <li>From the moment an errand has a process row, its labels have to go on resolving to the key they resolved to
 * before, unless the change points them at the process the errand actually runs. This holds for a process that has
 * finished too.</li>
 * </ol>
 *
 * Only the key is held still. The start mode is read off the same labels and may be changed freely.
 * <p>
 * The three writers that reach the labels, and what a refusal gives each of them:
 *
 * <pre>
 * POST /errands            -&gt; ErrandService.createErrand      -&gt; 400, and only rule 1: a new errand has no process
 * PATCH /errands/{id}      -&gt; ErrandService.updateErrand      -&gt; 400
 * a scheduled job          -&gt; AddLabelAction.executeAction    -&gt; the labels are left off, and an entry says so
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
		process '%s', and an errand runs the one process for the whole of its life - labels naming another would leave \
		the process it does run with nothing to wake it. Leave the label carrying the process key as it is, and handle \
		the other process on an errand of its own.""";

	private static final String LEFT_OFF = """
		%s The labels a scheduled action was to add were therefore left off the errand. Take the label off the action, \
		or give the action a condition that keeps it away from the errands it must not relabel.""";

	private final ErrandProcessRepository processRepository;
	private final MetadataLabelRepository metadataLabelRepository;
	private final ProcessKeySelector processKeySelector;
	private final ProcessErrorLog errorLog;

	public ProcessKeyGuard(
		final ErrandProcessRepository processRepository,
		final MetadataLabelRepository metadataLabelRepository,
		final ProcessKeySelector processKeySelector,
		final ProcessErrorLog errorLog) {

		this.processRepository = processRepository;
		this.metadataLabelRepository = metadataLabelRepository;
		this.processKeySelector = processKeySelector;
		this.errorLog = errorLog;
	}

	/**
	 * Rule 1, asked of the labels an errand is created with. Throws 400 when they name more than one process. Rule 2 is
	 * not asked.
	 *
	 * @param labels the labels the errand would be created with, ancestors included.
	 */
	public void verifyNewLabels(final Collection<ErrandLabelEmbeddable> labels) {
		final var ids = idsOf(labels);

		if (ids.isEmpty()) {
			return;
		}

		ambiguityIn(processKeySelector.selectFrom(labelsOf(ids, lookUp(ids))))
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
	 * @param  labelsAfter  the labels it would wear once the new ones had been added.
	 * @return              true when the change is refused, in which case an error entry has been written on the errand.
	 */
	public boolean refusesLabelChange(final String errandId, final Collection<ErrandLabelEmbeddable> labelsBefore, final Collection<ErrandLabelEmbeddable> labelsAfter) {
		final var refusal = findRefusal(errandId, labelsBefore, labelsAfter);

		refusal.ifPresent(refused -> {
			LOG.warn("Labels were not added to errand {}: {}", sanitizeForLogging(errandId), refused.errorCode());
			errorLog.writeOncePerWindow(errandId, null, CONFIG_ACTIVITY_TYPE, refused.errorCode(), LEFT_OFF.formatted(refused.reason()));
		});

		return refusal.isPresent();
	}

	/**
	 * Why the labels may not be settled on the errand, and empty when they may.
	 * <p>
	 * Rule 2 compares the key the labels resolve to before with the key they would resolve to after, so a key taken away
	 * is refused as well as a key exchanged. A change that leaves the labels naming the process the errand actually runs
	 * is let through whatever they named before.
	 * <p>
	 * A change that does not touch the labels is answered before anything is read, and the process rows are read only
	 * once rule 1 has passed.
	 */
	private Optional<Refusal> findRefusal(final String errandId, final Collection<ErrandLabelEmbeddable> labelsBefore, final Collection<ErrandLabelEmbeddable> labelsAfter) {
		final var idsBefore = idsOf(labelsBefore);
		final var idsAfter = idsOf(labelsAfter);

		if (idsBefore.equals(idsAfter)) {
			return Optional.empty();
		}

		final var ids = new HashSet<>(idsBefore);
		ids.addAll(idsAfter);

		// Both sides are read out of the one lookup, and by id rather than off the errand: a label the mapper has just
		// put together carries no metadata label of its own, and neither side would answer for the other's.
		final var labelsById = lookUp(ids);
		final var selectionAfter = processKeySelector.selectFrom(labelsOf(idsAfter, labelsById));

		final var ambiguity = ambiguityIn(selectionAfter);

		if (ambiguity.isPresent()) {
			return ambiguity;
		}

		final var instances = processRepository.findByErrandIdOrderByCreatedDesc(errandId);

		if (instances.isEmpty()) {
			return Optional.empty();
		}

		// Every instance of an errand runs the same process, so it does not matter which of the rows answers.
		final var running = instances.getFirst().getProcessKey();
		final var keyBefore = processKeySelector.selectFrom(labelsOf(idsBefore, labelsById)).processKey();
		final var keyAfter = selectionAfter.processKey();

		if (Objects.equals(keyBefore, keyAfter) || Objects.equals(running, keyAfter)) {
			return Optional.empty();
		}

		return Optional.of(new Refusal(MOVES_KEY_ERROR_CODE, MOVES_KEY.formatted(errandId, describe(keyBefore), describe(keyAfter), ProcessKeySelector.excerptOf(running))));
	}

	/** Rule 1: a refusal when the labels name more than one process, and empty when they do not. */
	private Optional<Refusal> ambiguityIn(final ProcessKeySelection selection) {
		if (!selection.isAmbiguous()) {
			return Optional.empty();
		}

		return Optional.of(new Refusal(TWO_PROCESSES_ERROR_CODE, TWO_PROCESSES.formatted(ProcessKeySelector.excerptOf(selection.keys()))));
	}

	private Map<String, MetadataLabelEntity> lookUp(final Set<String> ids) {
		return metadataLabelRepository.findAllById(ids).stream()
			.collect(toMap(MetadataLabelEntity::getId, identity(), (first, _) -> first));
	}

	private Set<String> idsOf(final Collection<ErrandLabelEmbeddable> labels) {
		return ofNullable(labels).orElse(emptyList()).stream()
			.filter(Objects::nonNull)
			.map(ErrandLabelEmbeddable::getMetadataLabelId)
			.filter(Objects::nonNull)
			.collect(toSet());
	}

	/**
	 * The metadata labels of the sent in ids. An id whose metadata label is gone is passed over without an error.
	 */
	private List<MetadataLabelEntity> labelsOf(final Set<String> ids, final Map<String, MetadataLabelEntity> labelsById) {
		return ids.stream()
			.map(labelsById::get)
			.filter(Objects::nonNull)
			.toList();
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
