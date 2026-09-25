package se.sundsvall.supportmanagement.service;

import java.util.Set;

/**
 * A label merge on its way to the thread that carries it out.
 * <p>
 * Everything the run needs to know is settled when it is accepted: which job it reports against, which source labels
 * fold into which destination, and who asked for it. The job row carries no payload of its own, so this is the only
 * place these travel - mirrors {@link LabelMoveRun}.
 *
 * @param jobId          id of the job the run reports its progress against.
 * @param namespace      namespace the merged labels belong to.
 * @param municipalityId id of the municipality the merged labels belong to.
 * @param targetLabelId  id of the destination label the sources are merged into.
 * @param sourceLabelIds ids of the source labels being merged into the destination.
 * @param startedBy      who asked for the run.
 */
public record LabelMergeRun(String jobId, String namespace, String municipalityId, String targetLabelId, Set<String> sourceLabelIds, String startedBy) {
}
