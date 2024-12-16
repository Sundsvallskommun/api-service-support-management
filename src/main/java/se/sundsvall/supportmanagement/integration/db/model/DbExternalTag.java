package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class DbExternalTag {

	@Column(name = "\"key\"")
	private String key;

	@Column(name = "\"value\"")
	private String value;

	public static DbExternalTag create() {
		return new DbExternalTag();
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public DbExternalTag withKey(final String key) {
		this.key = key;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public DbExternalTag withValue(final String value) {
		this.value = value;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final var that = (DbExternalTag) o;
		return Objects.equals(key, that.key) && Objects.equals(value, that.value);
	}

	@Override
	public String toString() {
		return "DbExternalTag{" +
			"key='" + key + '\'' +
			", value='" + value + '\'' +
			'}';
	}
}
