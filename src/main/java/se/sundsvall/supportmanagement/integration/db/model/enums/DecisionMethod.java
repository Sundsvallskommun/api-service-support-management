package se.sundsvall.supportmanagement.integration.db.model.enums;

/**
 * How the decision was made: manually by a person or automatically by a process. Records which decisions were made
 * automatically, as 28 § of the Swedish administrative law and article 22 of the GDPR require.
 */
public enum DecisionMethod {

	MANUAL,

	AUTOMATIC
}
