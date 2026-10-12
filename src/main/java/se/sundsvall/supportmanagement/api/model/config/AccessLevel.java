package se.sundsvall.supportmanagement.api.model.config;

/**
 * Access levels this API grants, ordered LR before R before RW. The names match those of the access mapper client
 * enum, which the service layer converts them to by name.
 */
public enum AccessLevel {

	/**
	 * Limited read. The errand is reachable, but trimmed to the fields the namespace exposes.
	 */
	LR,

	/**
	 * Read.
	 */
	R,

	/**
	 * Read and write.
	 */
	RW
}
