package se.sundsvall.supportmanagement.api.model.identifier;

/**
 * Canonical string values for {@link Identifier#getType()}, as compile-time constants that annotations can reference.
 *
 * <p>
 * The on-the-wire strings mirror dept44's {@code Identifier.Type} enum after
 * UPPER_UNDERSCORE → LOWER_CAMEL conversion (e.g. {@code AD_ACCOUNT → "adAccount"}).
 */
public final class IdentifierTypeValues {

	public static final String AD_ACCOUNT = "adAccount";
	public static final String PARTY_ID = "partyId";

	private IdentifierTypeValues() {
		// Intentionally empty
	}
}
