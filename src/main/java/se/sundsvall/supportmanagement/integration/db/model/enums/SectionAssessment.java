package se.sundsvall.supportmanagement.integration.db.model.enums;

/**
 * Assessment of an investigation section. Four values are enough, and they are neutral to the line of business.
 */
public enum SectionAssessment {

	/** Not assessed yet. */
	PENDING,

	/** Nothing to remark on. */
	APPROVED,

	/** A deficiency was established. */
	DEFICIENCY,

	/** The section does not concern this errand. */
	NOT_APPLICABLE
}
