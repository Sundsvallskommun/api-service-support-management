package se.sundsvall.supportmanagement.integration.db.model.subscriber;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class EventFilterEmbeddable {

	@Column(name = "type", nullable = false, length = 64)
	private String type;

	@Column(name = "subtype", length = 64)
	private String subtype;

	@Column(name = "label_id", length = 36)
	private String labelId;

	public static EventFilterEmbeddable create() {
		return new EventFilterEmbeddable();
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public EventFilterEmbeddable withType(final String type) {
		this.type = type;
		return this;
	}

	public String getSubtype() {
		return subtype;
	}

	public void setSubtype(final String subtype) {
		this.subtype = subtype;
	}

	public EventFilterEmbeddable withSubtype(final String subtype) {
		this.subtype = subtype;
		return this;
	}

	public String getLabelId() {
		return labelId;
	}

	public void setLabelId(final String labelId) {
		this.labelId = labelId;
	}

	public EventFilterEmbeddable withLabelId(final String labelId) {
		this.labelId = labelId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, subtype, labelId);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final EventFilterEmbeddable other = (EventFilterEmbeddable) obj;
		return Objects.equals(type, other.type) && Objects.equals(subtype, other.subtype) && Objects.equals(labelId, other.labelId);
	}

	@Override
	public String toString() {
		return "EventFilterEmbeddable{type='" + type + "', subtype='" + subtype + "', labelId='" + labelId + "'}";
	}
}
