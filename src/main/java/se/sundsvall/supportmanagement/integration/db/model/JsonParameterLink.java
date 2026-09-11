package se.sundsvall.supportmanagement.integration.db.model;

/**
 * A link from a handling artefact to a JSON parameter of the errand.
 * <p>
 * Serves the same purpose as {@link AttachmentLink}: one implementation of the reading and the mapping for five link
 * entities that differ only in which column names their owner.
 * <p>
 * Unlike an attachment, which the artefact merely points at, the parameter is the content of the artefact, so it is
 * removed when its artefact is. That removal goes through the collection of the errand rather than through this link -
 * see {@code ErrandEntity.jsonParameters}.
 */
public interface JsonParameterLink {

	JsonParameterEntity getJsonParameterEntity();
}
