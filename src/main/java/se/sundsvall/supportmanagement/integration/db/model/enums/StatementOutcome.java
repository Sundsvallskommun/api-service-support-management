package se.sundsvall.supportmanagement.integration.db.model.enums;

/**
 * Outcome of a statement response. The values of the Swedish consultation form rather than those of any single line of
 * business, so that the responses can be counted without knowing which namespace they came from.
 */
public enum StatementOutcome {

	/** Tillstyrker. */
	SUPPORTS,

	/** Tillstyrker med villkor eller erinran. */
	SUPPORTS_WITH_CONDITIONS,

	/** Ingen erinran. */
	NO_OBJECTION,

	/** Avstyrker. */
	OPPOSES,

	/** Avstår, frågan berör inte instansen. */
	NOT_APPLICABLE,

	/** Fristen gick ut utan svar. */
	NO_RESPONSE
}
