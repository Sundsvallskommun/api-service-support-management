package se.sundsvall.supportmanagement.service.job;

/**
 * One MOVE step's own payload, as {@link LabelMoveWorker#moveAndRestow} carries it out: which label moves where, and
 * the rename it may combine with the move in the same update.
 *
 * @param labelId         id of the label being moved.
 * @param newParentId     id of the label's new parent, or {@code null} to move it to root.
 * @param newResourceName optional new resourceName to set in the same update, or {@code null} to keep it.
 * @param newDisplayName  optional new displayName to set in the same update, or {@code null} to keep it.
 */
record LabelMoveStep(String labelId, String newParentId, String newResourceName, String newDisplayName) {
}
