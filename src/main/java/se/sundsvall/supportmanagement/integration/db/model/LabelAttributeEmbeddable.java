package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;

import static java.sql.Types.LONGVARCHAR;

@Embeddable
public class LabelAttributeEmbeddable {

	@Column(name = "`key`", nullable = false)
	private String key;

	@Column(name = "`value`", nullable = false)
	@JdbcTypeCode(LONGVARCHAR)
	private String value;

	public static LabelAttributeEmbeddable create() {
		return new LabelAttributeEmbeddable();
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public LabelAttributeEmbeddable withKey(String key) {
		this.key = key;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public LabelAttributeEmbeddable withValue(String value) {
		this.value = value;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final LabelAttributeEmbeddable other)) { return false; }
		return Objects.equals(key, other.key) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "LabelAttributeEmbeddable [key=" + key + ", value=" + value + "]";
	}
}
