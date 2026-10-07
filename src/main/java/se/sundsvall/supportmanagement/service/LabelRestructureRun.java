package se.sundsvall.supportmanagement.service;

import java.util.List;
import se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStep;

/**
 * A label-tree restructure on its way to the thread that carries it out.
 * <p>
 * Everything the run needs to know is settled when it is accepted: which job it reports against, the ordered steps
 * to apply, and who asked for it. The job row carries no payload of its own, so this is the only place these travel -
 * mirrors {@link LabelMoveRun}/{@link LabelMergeRun}.
 *
 * @param jobId              id of the job the run reports its progress against.
 * @param namespace          namespace the restructured labels belong to.
 * @param municipalityId     id of the municipality the restructured labels belong to.
 * @param steps              the steps to apply, in order.
 * @param startedBy          who asked for the run.
 * @param startedByAdAccount whether the one who asked for the run is an ad account, which holds its label changes to
 *                           the rule that an ad account may not take a label blocking processes off an errand.
 */
public record LabelRestructureRun(String jobId, String namespace, String municipalityId, List<LabelRestructureStep> steps, String startedBy, boolean startedByAdAccount) {
}
