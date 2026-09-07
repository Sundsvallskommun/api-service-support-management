package se.sundsvall.supportmanagement.integration.db.model.enums;

/**
 * Type of grant a namespace configuration gives to a role. FIELD grants control which parts of an errand are returned,
 * RESOURCE grants control which resources the role may reach.
 */
public enum RoleAccessType {
	FIELD,
	RESOURCE
}
