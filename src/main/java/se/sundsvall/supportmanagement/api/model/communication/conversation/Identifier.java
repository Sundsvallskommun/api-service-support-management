package se.sundsvall.supportmanagement.api.model.communication.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Objects;

@Schema(description = "Identifier model")
public class Identifier {

	@Pattern(regexp = "^(adAccount|partyId)$", message = "Type must be 'adAccount' or 'partyId'")
	@Schema(description = "The conversation identifier type", examples = "adAccount")
	private String type;

	@NotBlank
	@Schema(description = "The conversation identifier value", examples = "joe01doe")
	private String value;

	public static Identifier create() {
		return new Identifier();
	}

	public String getType() {
		return type;
	}

	public Identifier withType(String type) {
		this.type = type;
		return this;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Identifier withValue(String value) {
		this.value = value;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Identifier other = (Identifier) obj;
		return Objects.equals(type, other.type) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "ConversationIdentifier [type=" + type + ", value=" + value + "]";
	}
}
