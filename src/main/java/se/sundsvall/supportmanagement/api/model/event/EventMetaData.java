package se.sundsvall.supportmanagement.api.model.event;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Event Metadata model")
public class EventMetaData {

	@Schema(description = "The key", example = "userId")
	private String key;

	@Schema(description = "The value", example = "john123")
	private String value;

	public static EventMetaData create() {
		return new EventMetaData();
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public EventMetaData withKey(String key) {
		this.key = key;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public EventMetaData withValue(String value) {
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
		EventMetaData metadata = (EventMetaData) o;
		return Objects.equals(key, metadata.key) && Objects.equals(value, metadata.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("EventMetaData{");
		sb.append("key='").append(key).append('\'');
		sb.append(", value='").append(value).append('\'');
		sb.append('}');
		return sb.toString();
	}
}
