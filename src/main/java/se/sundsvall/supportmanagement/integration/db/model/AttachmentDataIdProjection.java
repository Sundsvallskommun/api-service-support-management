package se.sundsvall.supportmanagement.integration.db.model;

/**
 * The id of the data row an attachment points at, read without the row itself and so without loading the file it
 * holds. Tells a removal which data row to remove.
 */
public interface AttachmentDataIdProjection {

	Integer getAttachmentDataId();

}
