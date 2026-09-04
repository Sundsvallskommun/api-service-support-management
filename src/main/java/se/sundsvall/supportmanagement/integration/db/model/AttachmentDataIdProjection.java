package se.sundsvall.supportmanagement.integration.db.model;

/**
 * The id of the data row an attachment points at, read without the row itself.
 * <p>
 * What the row holds is the file, and a removal has no use for its content - only for knowing which row to remove.
 * Reading the attachment instead and reaching its data through the association would load every byte of the file into
 * the heap on the way.
 */
public interface AttachmentDataIdProjection {

	Integer getAttachmentDataId();

}
