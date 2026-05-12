package se.sundsvall.supportmanagement.api.model.event;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Event model", accessMode = READ_ONLY)
public class Event {

	@Schema(description = "Unique identifier for the event", examples = "922f95e3-608b-4e3c-ae22-f11fb849799a")
	private String id;

	@Schema(implementation = EventType.class)
	private EventType type;

	@Schema(description = "Subtype describing what kind of entity the event refers to", examples = "ATTACHMENT")
	private String subType;

	@Schema(description = "Groups related events and notifications together within one operation", examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
	private String requestGroupId;

	@Schema(description = "Short event description", examples = "Bilaga har skapats")
	private String message;

	@Schema(description = "Detailed event description", examples = "Filnamn 'abc.pdf'")
	private String details;

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

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Event withId(final String id) {
		this.id = id;
		return this;
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

	public String getSubType() {
		return subType;
	}

	public void setSubType(final String subType) {
		this.subType = subType;
	}

	public Event withSubType(final String subType) {
		this.subType = subType;
		return this;
	}

	public String getRequestGroupId() {
		return requestGroupId;
	}

	public void setRequestGroupId(final String requestGroupId) {
		this.requestGroupId = requestGroupId;
	}

	public Event withRequestGroupId(final String requestGroupId) {
		this.requestGroupId = requestGroupId;
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

	public String getDetails() {
		return details;
	}

	public void setDetails(final String details) {
		this.details = details;
	}

	public Event withDetails(final String details) {
		this.details = details;
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
		return Objects.hash(id, type, subType, requestGroupId, message, details, owner, created, historyReference, sourceType, metadata);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Event other)) {
			return false;
		}
		return Objects.equals(id, other.id) && type == other.type && Objects.equals(subType, other.subType) && Objects.equals(requestGroupId, other.requestGroupId)
			&& Objects.equals(message, other.message) && Objects.equals(details, other.details) && Objects.equals(owner, other.owner)
			&& Objects.equals(created, other.created) && Objects.equals(historyReference, other.historyReference)
			&& Objects.equals(sourceType, other.sourceType) && Objects.equals(metadata, other.metadata);
	}

	@Override
	public String toString() {
		return "Event [id=" + id + ", type=" + type + ", subType=" + subType + ", requestGroupId=" + requestGroupId
			+ ", message=" + message + ", details=" + details + ", owner=" + owner + ", created=" + created
			+ ", historyReference=" + historyReference + ", sourceType=" + sourceType + ", metadata=" + metadata + "]";
	}
}
