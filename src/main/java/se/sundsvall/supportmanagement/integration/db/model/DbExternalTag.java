package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DbExternalTag implements Serializable {

	private static final long serialVersionUID = -6197521959635923913L;

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

	public void setKey(String key) {
		this.key = key;
	}

	public DbExternalTag withKey(String key) {
		this.key = key;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public DbExternalTag withValue(String value) {
		this.value = value;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		var that = (DbExternalTag) o;
		return Objects.equals(key, that.key) && Objects.equals(value, that.value);
	}

	@Override
	public String toString() {
		var builder = new StringBuilder();

		builder.append("DbExternalTag [key=").append(key).append(", value=").append(value).append("]");

		return builder.toString();
	}
}
