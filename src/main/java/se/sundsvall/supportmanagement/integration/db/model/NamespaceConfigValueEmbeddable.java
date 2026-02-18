package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import se.sundsvall.supportmanagement.integration.db.model.enums.ValueType;

import static java.sql.Types.LONGVARCHAR;

@Embeddable
public class NamespaceConfigValueEmbeddable {

	@Column(name = "`key`", nullable = false)
	private String key;

	@Column(name = "`value`", nullable = false)
	@JdbcTypeCode(LONGVARCHAR)
	private String value;

	@Column(name = "`type`", nullable = false)
	private ValueType type;

	public static NamespaceConfigValueEmbeddable create() {
		return new NamespaceConfigValueEmbeddable();
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public NamespaceConfigValueEmbeddable withKey(String key) {
		this.key = key;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public NamespaceConfigValueEmbeddable withValue(String value) {
		this.value = value;
		return this;
	}

	public ValueType getType() {
		return type;
	}

	public void setType(ValueType type) {
		this.type = type;
	}

	public NamespaceConfigValueEmbeddable withType(ValueType type) {
		this.type = type;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, type, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final NamespaceConfigValueEmbeddable other)) { return false; }
		return Objects.equals(key, other.key) && type == other.type && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("NamespaceConfigValueEmbeddable [key=").append(key).append(", value=").append(value).append(", type=").append(type).append("]");
		return builder.toString();
	}

}
