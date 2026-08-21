package se.sundsvall.supportmanagement.integration.db.model.enums;

/**
 * Reserved scopes a namespace grant can be keyed by. These are resolved per errand by this service rather than supplied
 * by the access mapper, which is why they are named here. Any other scope value is the name of a role supplied by the
 * access mapper.
 */
public enum AccessGrantScope {

	/**
	 * Applies when the labels of the requesting user do not cover the errand fully, meaning the access mapper granted
	 * them limited read for it.
	 */
	LIMITED,

	/**
	 * Applies to the user whose identifier matches the reporterUserId of the errand.
	 */
	REPORTER
}
