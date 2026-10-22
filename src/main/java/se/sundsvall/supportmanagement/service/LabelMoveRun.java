package se.sundsvall.supportmanagement.service;

/**
 * A label move on its way to the thread that carries it out.
 * <p>
 * Everything the run needs to know is settled when it is accepted: which job it reports against, which label moves
 * where, and who asked for it. The job row carries no payload of its own, so this is the only place these travel.
 *
 * @param jobId              id of the job the run reports its progress against.
 * @param namespace          namespace the moved label belongs to.
 * @param municipalityId     id of the municipality the moved label belongs to.
 * @param labelId            id of the label being moved.
 * @param newParentId        id of the label's new parent, or {@code null} to move it to root.
 * @param startedBy          who asked for the run.
 * @param startedByAdAccount whether the one who asked for the run is an ad account, which holds its label changes to
 *                           the rule that an ad account may not take a label blocking processes off an errand.
 */
public record LabelMoveRun(String jobId, String namespace, String municipalityId, String labelId, String newParentId, String startedBy, boolean startedByAdAccount) {
}
