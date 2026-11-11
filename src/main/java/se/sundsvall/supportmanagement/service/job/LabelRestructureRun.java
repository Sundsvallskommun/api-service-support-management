package se.sundsvall.supportmanagement.service.job;

import java.util.List;
import se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStep;

/**
 * A label-tree restructure on its way to the thread that carries it out.
 * <p>
 * Everything the run needs to know is settled when it is accepted: which job it reports against, the ordered steps
 * to apply, which errands they affect, and who asked for it. The job row carries no payload of its own, so this is
 * the only place these travel.
 *
 * @param jobId              id of the job the run reports its progress against.
 * @param namespace          namespace the restructured labels belong to.
 * @param municipalityId     id of the municipality the restructured labels belong to.
 * @param steps              the steps to apply, in order.
 * @param errandIds          ids of every errand any MOVE/MERGE step's label (or one of its descendants) reached at the
 *                           moment the restructure was accepted - resolved once, by {@link MetadataService}, as one
 *                           deduplicated union across every such step, from the same query the job's own {@code total}
 *                           reads from, so the walk restows exactly what was counted and an errand touched by more than
 *                           one step is restowed once, not once per step. Frozen deliberately: an errand created after
 *                           this point is created against the tree the restructure already left in place, so it needs
 *                           no restowing.
 * @param startedBy          who asked for the run.
 * @param startedByAdAccount whether the one who asked for the run is an ad account, which holds its label changes to
 *                           the rule that an ad account may not take a label blocking processes off an errand.
 */
public record LabelRestructureRun(String jobId, String namespace, String municipalityId, List<LabelRestructureStep> steps, List<String> errandIds, String startedBy,
	boolean startedByAdAccount) {
}
