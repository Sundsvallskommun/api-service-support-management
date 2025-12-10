package se.sundsvall.supportmanagement.api.model.event;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "Event model", accessMode = READ_ONLY)
public class Event {

	@Schema(implementation = EventType.class)
	private EventType type;

	@Schema(description = "Event description", examples = "Errand has been created")
	private String message;

	@Schema(description = "Service that created event", examples = "SupportManagement")
	private String owner;

	@Schema(description = "Timestamp when the event was created", examples = "2000-10-31T01:30:00.000+02:00")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "Reference to the snapshot of data at the time when the event was created", examples = "fbe2fb67-005c-4f26-990f-1c95b5f6933e")
	private String historyReference;

	@Schema(description = "Source which the event refers to", examples = "errand")
	private String sourceType;

	@ArraySchema(schema = @Schema(implementation = EventMetaData.class))
	private List<EventMetaData> metadata;

	public static Event create() {
		return new Event();
	}

	public EventType getType() {
		return type;
	}

	public void setType(final EventType type) {
		this.type = type;
	}

	public Event withType(final EventType type) {
		this.type = type;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public Event withMessage(final String message) {
		this.message = message;
		return this;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(final String owner) {
		this.owner = owner;
	}

	public Event withOwner(final String owner) {
		this.owner = owner;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Event withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public String getHistoryReference() {
		return historyReference;
	}

	public void setHistoryReference(final String historyReference) {
		this.historyReference = historyReference;
	}

	public Event withHistoryReference(final String historyReference) {
		this.historyReference = historyReference;
		return this;
	}

	public String getSourceType() {
		return sourceType;
	}

	public void setSourceType(final String sourceType) {
		this.sourceType = sourceType;
	}

	public Event withSourceType(final String sourceType) {
		this.sourceType = sourceType;
		return this;
	}

	public List<EventMetaData> getMetadata() {
		return metadata;
	}

	public void setMetadata(final List<EventMetaData> metadata) {
		this.metadata = metadata;
	}

	public Event withMetadata(final List<EventMetaData> metadata) {
		this.metadata = metadata;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, historyReference, message, metadata, owner, sourceType, type);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Event other)) {
			return false;
		}
		return Objects.equals(created, other.created) && Objects.equals(historyReference, other.historyReference) && Objects.equals(message, other.message) && Objects.equals(metadata, other.metadata) && Objects.equals(owner, other.owner) && Objects
			.equals(sourceType, other.sourceType) && type == other.type;
	}

	@Override
	public String toString() {
		return "Event [type=" + type + ", message=" + message + ", owner=" + owner + ", created=" + created + ", historyReference=" + historyReference + ", sourceType="
			+ sourceType + ", metadata=" + metadata + "]";
	}
}
