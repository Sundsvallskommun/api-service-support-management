package se.sundsvall.supportmanagement.integration.db.model.subscriber;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class IdentifierEmbeddable {

	@Column(name = "identifier_type", length = 16)
	private String type;

	@Column(name = "identifier_value")
	private String value;

	public static IdentifierEmbeddable create() {
		return new IdentifierEmbeddable();
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public IdentifierEmbeddable withType(final String type) {
		this.type = type;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public IdentifierEmbeddable withValue(final String value) {
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
		final IdentifierEmbeddable other = (IdentifierEmbeddable) obj;
		return Objects.equals(type, other.type) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "IdentifierEmbeddable{type='" + type + "', value='" + value + "'}";
	}
}
