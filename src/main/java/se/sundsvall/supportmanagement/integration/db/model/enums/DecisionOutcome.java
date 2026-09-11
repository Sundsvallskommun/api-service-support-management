package se.sundsvall.supportmanagement.integration.db.model.enums;

/**
 * Outcome of a decision. The values come from administrative law and hold equally in building permits, income support
 * and supervision, which is why they are shared rather than defined per namespace.
 * <p>
 * Outcomes specific to a line of business do not belong here - they are expressed in legal basis, delegation reference
 * and justification. An enum growing per namespace is a sign that the decision needs a model of its own for that line
 * of business, not one more value here.
 */
public enum DecisionOutcome {

	/** Bifall. */
	APPROVAL,

	/** Delvis bifall. */
	PARTIAL_APPROVAL,

	/** Avslag. */
	REJECTION,

	/** Avvisning. */
	DISMISSAL,

	/** Avskrivning. */
	DISCONTINUATION,

	OTHER
}
