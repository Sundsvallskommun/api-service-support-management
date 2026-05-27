package se.sundsvall.supportmanagement.api.model.identifier;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;

@Schema(description = "Identifier describing a user or subject (AD-account or party-id)")
public class Identifier {

	// Pattern/Schema literals must mirror IdentifierTypeValues — annotation values require
	// compile-time constant expressions, and Type.AD_ACCOUNT.getTypeString() is a method call.
	@NotBlank
	@Pattern(regexp = "^(adAccount|partyId)$", message = "type must be 'adAccount' or 'partyId'")
	@Schema(description = "Identifier type", examples = {
		"adAccount", "partyId"
	}, allowableValues = {
		"adAccount", "partyId"
	})
	private String type;

	@NotBlank
	@Size(max = 255)
	@Schema(description = "Identifier value (AD-account name or partyId UUID)", examples = "joe01doe")
	private String value;

	public static Identifier create() {
		return new Identifier();
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public Identifier withType(final String type) {
		this.type = type;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public Identifier withValue(final String value) {
		this.value = value;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, value);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final Identifier other = (Identifier) obj;
		return Objects.equals(type, other.type) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "Identifier{type='" + type + "', value='" + value + "'}";
	}
}
