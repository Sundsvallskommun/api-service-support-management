package se.sundsvall.supportmanagement.service;

import java.util.List;

/**
 * A label move on its way to the thread that carries it out.
 * <p>
 * Everything the run needs to know is settled when it is accepted: which job it reports against, which label moves
 * where, who asked for it, and which errands it must restow. The job row carries no payload of its own, so this is
 * the only place these travel.
 *
 * @param jobId          id of the job the run reports its progress against.
 * @param namespace      namespace the moved label belongs to.
 * @param municipalityId id of the municipality the moved label belongs to.
 * @param labelId        id of the label being moved.
 * @param newParentId    id of the label's new parent, or {@code null} to move it to root.
 * @param errandIds      ids of every errand the moved label (or one of its descendants) reached at the moment the
 *                       move was accepted - resolved once, by {@link MetadataService}, from the same query the dry
 *                       run and the job's own {@code total} read from, so the walk restows exactly what was counted.
 *                       Frozen deliberately: an errand created after this point is created against the tree the move
 *                       already left in place, so it needs no restowing.
 * @param startedBy      who asked for the run.
 */
public record LabelMoveRun(String jobId, String namespace, String municipalityId, String labelId, String newParentId, List<String> errandIds, String startedBy) {
}
