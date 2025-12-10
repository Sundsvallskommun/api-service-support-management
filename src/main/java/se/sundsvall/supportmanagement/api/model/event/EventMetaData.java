package se.sundsvall.supportmanagement.api.model.event;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Event Metadata model")
public class EventMetaData {

	@Schema(description = "The key", examples = "userId")
	private String key;

	@Schema(description = "The value", examples = "john123")
	private String value;

	public static EventMetaData create() {
		return new EventMetaData();
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public EventMetaData withKey(final String key) {
		this.key = key;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public EventMetaData withValue(final String value) {
		this.value = value;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final EventMetaData metadata = (EventMetaData) o;
		return Objects.equals(key, metadata.key) && Objects.equals(value, metadata.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value);
	}

	@Override
	public String toString() {
		return "EventMetaData{" +
			"key='" + key + '\'' +
			", value='" + value + '\'' +
			'}';
	}
}
