package se.sundsvall.supportmanagement.api.model.config;

/**
 * Access levels this API grants, ordered LR before R before RW.
 * <p>
 * Deliberately declared here rather than reusing the access mapper client enum. The levels a namespace configures for
 * its reporters are a SupportManagement concept - the access mapper knows nothing about reporters - and reusing its
 * generated type would publish an integration detail in this API, letting a change to the access mapper contract alter
 * ours. The names match on purpose, so the two convert by name where the service layer compares them.
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
