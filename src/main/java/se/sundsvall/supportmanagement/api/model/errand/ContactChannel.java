package se.sundsvall.supportmanagement.api.model.errand;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Contact channel model")
public class ContactChannel {

	@Schema(description = "Type of channel. Defines how value is interpreted", example = "Email")
	private String type;

	@Schema(description = "Value for Contact channel", example = "arthur.dent@earth.com")
	private String value;

	public static ContactChannel create() {
		return new ContactChannel();
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public ContactChannel withType(String type) {
		this.type = type;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public ContactChannel withValue(String value) {
		this.value = value;
		return this;
	}

	@Override
	public boolean equals(Object o) {
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
		final StringBuilder sb = new StringBuilder("ContactChannel{");
		sb.append("type='").append(type).append('\'');
		sb.append(", value='").append(value).append('\'');
		sb.append('}');
		return sb.toString();
	}
}
