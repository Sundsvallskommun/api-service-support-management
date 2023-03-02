package se.sundsvall.supportmanagement.integration.db.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ContactChannelEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = -2018618163698478459L;

	@Column(name = "type")
	private String type;

	@Column(name = "value")
	private String value;

	public static ContactChannelEntity create() {
		return new ContactChannelEntity();
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public ContactChannelEntity withType(String type) {
		this.type = type;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public ContactChannelEntity withValue(String value) {
		this.value = value;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ContactChannelEntity that = (ContactChannelEntity) o;
		return Objects.equals(type, that.type) && Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, value);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ContactChannelEntity{");
		sb.append("type='").append(type).append('\'');
		sb.append(", value='").append(value).append('\'');
		sb.append('}');
		return sb.toString();
	}
}
