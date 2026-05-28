package se.sundsvall.supportmanagement.api.model.identifier;

/**
 * Canonical string values for {@link Identifier#getType()}.
 *
 * <p>
 * The on-the-wire strings mirror dept44's {@code Identifier.Type} enum after
 * UPPER_UNDERSCORE → LOWER_CAMEL conversion (e.g. {@code AD_ACCOUNT → "adAccount"}).
 * The values are kept as literals because dept44 derives them at runtime via
 * {@code com.google.common.base.CaseFormat}, which is not a compile-time constant
 * expression and therefore cannot be referenced from annotations.
 */
public final class IdentifierTypeValues {

	public static final String AD_ACCOUNT = "adAccount";
	public static final String PARTY_ID = "partyId";

	private IdentifierTypeValues() {
		// Intentionally empty
	}
}
