package se.sundsvall.supportmanagement.integration.db.model.enums;

/**
 * Life cycle of an errand handling item. The same four values in all four entities that extend
 * {@link se.sundsvall.supportmanagement.integration.db.model.AbstractErrandItemEntity}.
 */
public enum ItemStatus {

	/** Started but not carried out: the statement is not sent, the decision is not made. */
	DRAFT,

	/** Ongoing: the statement is out, the investigation is being written, the measure is being carried out. */
	ACTIVE,

	/** Concluded with an outcome. */
	COMPLETED,

	/** Abandoned without an outcome: a withdrawn statement, a discontinued investigation. */
	CANCELLED
}
