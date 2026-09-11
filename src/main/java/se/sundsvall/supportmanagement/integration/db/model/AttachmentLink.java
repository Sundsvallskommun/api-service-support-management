package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;

/**
 * A link from a handling artefact to an attachment of the errand.
 * <p>
 * The four link entities are identical apart from the column naming their owner, and this is what lets the reading
 * and the mapping be written once instead of four times. The writing stays typed: each service adds to and removes
 * from its own collection, so no link can end up on the wrong owner.
 * <p>
 * The link is an entity rather than a plain many-to-many because it carries content of its own: the order the
 * attachment is shown in under its artefact. What the attachment is for belongs to the attachment itself, since the
 * errand shows it in its own list and a file linked to nothing can still have one.
 */
public interface AttachmentLink {

	AttachmentEntity getAttachmentEntity();

	Integer getSortOrder();

	void setSortOrder(Integer sortOrder);

	OffsetDateTime getCreated();

	String getCreatedBy();
}
