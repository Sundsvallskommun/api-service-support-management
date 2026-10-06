package se.sundsvall.supportmanagement.service;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.springframework.http.HttpStatus.CONFLICT;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.service.ProcessActivityLog.CONFIG_ACTIVITY_TYPE;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getAdUser;

/**
 * Keeps a label that blocks processes on an errand when an ad account changes its labels.
 * <p>
 * A label carrying {@code processBlocked=true} keeps everything about the errand away from its process
 * ({@link ProcessEventPublisher}). An ad account may put such a label on an errand but not take it off; a service
 * identity may do both. A label counts as taken off when the errand no longer wears it once its labels have been
 * expanded to their ancestors.
 * <p>
 * The writers that can take a label off an errand, and what a refusal gives each of them:
 *
 * <pre>
 * PATCH /errands/{id}      -&gt; ErrandService.updateErrand       -&gt; 409 when the caller is an ad account
 * a moved or merged label  -&gt; ErrandService.persistLabelUpdate -&gt; when an ad account started the job, the errand is
 *                                                                 left as it is, and an entry says so
 * </pre>
 *
 * Creating an errand and the {@code ADD_LABEL} action only add labels, and are not asked.
 */
@Component
public class ProcessBlockGuard {

	static final String REMOVES_BLOCK_ERROR_CODE = "LABEL_REMOVES_PROCESS_BLOCK";

	private static final Logger LOG = LoggerFactory.getLogger(ProcessBlockGuard.class);

	private static final String REMOVES_BLOCK = """
		The change would take the label %s off errand '%s', and it carries processBlocked=true, which keeps every \
		process away from the errand. An ad account cannot take that label off; a service identity can.""";

	private final ProcessKeySelector processKeySelector;
	private final ProcessActivityLog activityLog;

	public ProcessBlockGuard(final ProcessKeySelector processKeySelector, final ProcessActivityLog activityLog) {
		this.processKeySelector = processKeySelector;
		this.activityLog = activityLog;
	}

	/**
	 * Refuses with 409 a label change that an ad account makes and that takes a label blocking processes off the errand.
	 * A change made by any other caller passes.
	 *
	 * @param errandId     the errand being changed.
	 * @param labelsBefore the labels it wore before the change.
	 * @param labelsAfter  the labels it would wear after it, ancestors included.
	 */
	public void verifyLabelChange(final String errandId, final Collection<ErrandLabelEmbeddable> labelsBefore, final Collection<ErrandLabelEmbeddable> labelsAfter) {
		if (isNull(getAdUser())) {
			return;
		}

		findRefusal(errandId, labelsBefore, labelsAfter).ifPresent(reason -> {
			throw Problem.valueOf(CONFLICT, reason);
		});
	}

	/**
	 * The same rule, asked of a label change made by a job with no caller to answer, such as a label move. The change is
	 * held to the rule when the job was started by an ad account. A refusal is not thrown but logged and written as an
	 * error entry on the errand, once per window.
	 *
	 * @param  errandId           the errand being changed.
	 * @param  labelsBefore       the labels it wears.
	 * @param  labelsAfter        the labels it would wear after the change, ancestors included.
	 * @param  startedByAdAccount whether the job making the change was started by an ad account.
	 * @param  consequence        what the job does about a refusal, which the error entry tells after the reason.
	 * @return                    true when the change is refused, in which case an error entry has been written on the
	 *                            errand.
	 */
	public boolean refusesLabelChange(final String errandId, final Collection<ErrandLabelEmbeddable> labelsBefore, final Collection<ErrandLabelEmbeddable> labelsAfter,
		final boolean startedByAdAccount, final String consequence) {

		if (!startedByAdAccount) {
			return false;
		}

		final var refusal = findRefusal(errandId, labelsBefore, labelsAfter);

		refusal.ifPresent(reason -> {
			LOG.warn("Labels of errand {} were not changed: {}", sanitizeForLogging(errandId), REMOVES_BLOCK_ERROR_CODE);
			activityLog.writeOncePerWindow(errandId, null, CONFIG_ACTIVITY_TYPE, REMOVES_BLOCK_ERROR_CODE, reason + " " + consequence);
		});

		return refusal.isPresent();
	}

	/**
	 * Why the change may not be made, and empty when it may. The labels taken off are read for the attribute only when
	 * the change takes any off.
	 */
	private Optional<String> findRefusal(final String errandId, final Collection<ErrandLabelEmbeddable> labelsBefore, final Collection<ErrandLabelEmbeddable> labelsAfter) {
		final var idsAfter = labelsAfter.stream()
			.map(ErrandLabelEmbeddable::getMetadataLabelId)
			.filter(Objects::nonNull)
			.collect(toSet());

		final var takenOff = labelsBefore.stream()
			.filter(label -> !idsAfter.contains(label.getMetadataLabelId()))
			.toList();

		if (takenOff.isEmpty()) {
			return Optional.empty();
		}

		final var blocksTakenOff = processKeySelector.blockingLabelIdsOf(takenOff);

		if (blocksTakenOff.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(REMOVES_BLOCK.formatted(blocksTakenOff.stream().sorted().map("'%s'"::formatted).collect(joining(", ")), errandId));
	}
}
