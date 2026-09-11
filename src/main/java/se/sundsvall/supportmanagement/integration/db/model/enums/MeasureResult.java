package se.sundsvall.supportmanagement.integration.db.model.enums;

/**
 * Outcome of a measure that has been carried out.
 * <p>
 * Deliberately not {@link Accept}, which belongs to the action plan flow of a single line of business and means
 * something else: whether a proposed measure was accepted, not how the carried out measure turned out.
 */
public enum MeasureResult {

	COMPLETED,

	PARTIALLY_COMPLETED,

	NOT_COMPLETED,

	NOT_APPLICABLE
}
