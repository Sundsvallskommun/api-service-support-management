package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Contact channel model")
public class ContactChannel {

	@Schema(description = "Type of channel. Defines how value is interpreted", examples = "Email")
	private String type;

	@Schema(description = "Value for Contact channel", examples = "arthur.dent@earth.com")
	private String value;

	public static ContactChannel create() {
		return new ContactChannel();
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public ContactChannel withType(final String type) {
		this.type = type;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public ContactChannel withValue(final String value) {
		this.value = value;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if ((o == null) || (getClass() != o.getClass())) {
			return false;
		}
		final ContactChannel that = (ContactChannel) o;
		return Objects.equals(type, that.type) && Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, value);
	}

	@Override
	public String toString() {
		return "ContactChannel{" +
			"type='" + type + '\'' +
			", value='" + value + '\'' +
			'}';
	}
}
