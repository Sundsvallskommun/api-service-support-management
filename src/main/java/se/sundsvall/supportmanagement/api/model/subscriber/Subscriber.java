package se.sundsvall.supportmanagement.api.model.subscriber;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.supportmanagement.api.model.identifier.Identifier;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@Schema(description = "A subscriber describes who receives notifications, which channels they prefer, and which event types they are interested in.")
public class Subscriber {

	@Null(groups = OnCreate.class)
	@ValidUuid(groups = OnUpdate.class)
	@Schema(description = "Unique identifier of the subscriber", examples = "123e4567-e89b-12d3-a456-426614174000", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "Optional human-readable label. Useful when a person has several subscribers (e.g. one per role or purpose).", examples = "Servicedesk-bevakning")
	private String name;

	@NotNull(groups = OnCreate.class)
	@Valid
	@Schema(description = "Identifier of the principal that ultimately receives notifications (AD-account or partyId).")
	private Identifier identifier;

	@Valid
	@Schema(description = "Channels the subscriber wants to receive notifications on. If empty, defaults to INTERNAL.")
	private List<NotificationChannel> channels;

	@Valid
	@Schema(description = "Event filters that restrict which eventlog events trigger notifications. If empty, all events match.")
	private List<EventFilter> eventFilters;

	@DateTimeFormat(iso = DATE_TIME)
	@Schema(description = "When the subscriber's notifications are paused from (inclusive). Null means not paused.", examples = "2026-06-01T00:00:00+02:00")
	private OffsetDateTime pausedFrom;

	@DateTimeFormat(iso = DATE_TIME)
	@Schema(description = "When the subscriber's notifications resume (exclusive). Null means paused indefinitely (only meaningful if pausedFrom is set).", examples = "2026-06-30T00:00:00+02:00")
	private OffsetDateTime pausedUntil;

	@DateTimeFormat(iso = DATE_TIME)
	@Schema(description = "Timestamp when the subscriber was created", accessMode = READ_ONLY)
	private OffsetDateTime created;

	@DateTimeFormat(iso = DATE_TIME)
	@Schema(description = "Timestamp when the subscriber was last modified", accessMode = READ_ONLY)
	private OffsetDateTime modified;

	@Schema(description = "Identifier of the principal that created the subscriber", accessMode = READ_ONLY)
	private Identifier createdBy;

	@Schema(description = "Number of subscriptions currently owned by this subscriber", examples = "3", accessMode = READ_ONLY)
	private Integer subscriptionCount;

	public static Subscriber create() {
		return new Subscriber();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Subscriber withId(final String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Subscriber withName(final String name) {
		this.name = name;
		return this;
	}

	public Identifier getIdentifier() {
		return identifier;
	}

	public void setIdentifier(final Identifier identifier) {
		this.identifier = identifier;
	}

	public Subscriber withIdentifier(final Identifier identifier) {
		this.identifier = identifier;
		return this;
	}

	public List<NotificationChannel> getChannels() {
		return channels;
	}

	public void setChannels(final List<NotificationChannel> channels) {
		this.channels = channels;
	}

	public Subscriber withChannels(final List<NotificationChannel> channels) {
		this.channels = channels;
		return this;
	}

	public List<EventFilter> getEventFilters() {
		return eventFilters;
	}

	public void setEventFilters(final List<EventFilter> eventFilters) {
		this.eventFilters = eventFilters;
	}

	public Subscriber withEventFilters(final List<EventFilter> eventFilters) {
		this.eventFilters = eventFilters;
		return this;
	}

	public OffsetDateTime getPausedFrom() {
		return pausedFrom;
	}

	public void setPausedFrom(final OffsetDateTime pausedFrom) {
		this.pausedFrom = pausedFrom;
	}

	public Subscriber withPausedFrom(final OffsetDateTime pausedFrom) {
		this.pausedFrom = pausedFrom;
		return this;
	}

	public OffsetDateTime getPausedUntil() {
		return pausedUntil;
	}

	public void setPausedUntil(final OffsetDateTime pausedUntil) {
		this.pausedUntil = pausedUntil;
	}

	public Subscriber withPausedUntil(final OffsetDateTime pausedUntil) {
		this.pausedUntil = pausedUntil;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Subscriber withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Subscriber withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public Identifier getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final Identifier createdBy) {
		this.createdBy = createdBy;
	}

	public Subscriber withCreatedBy(final Identifier createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public Integer getSubscriptionCount() {
		return subscriptionCount;
	}

	public void setSubscriptionCount(final Integer subscriptionCount) {
		this.subscriptionCount = subscriptionCount;
	}

	public Subscriber withSubscriptionCount(final Integer subscriptionCount) {
		this.subscriptionCount = subscriptionCount;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, identifier, channels, eventFilters, pausedFrom, pausedUntil, created, modified, createdBy, subscriptionCount);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final Subscriber other = (Subscriber) obj;
		return Objects.equals(id, other.id) && Objects.equals(name, other.name) && Objects.equals(identifier, other.identifier)
			&& Objects.equals(channels, other.channels) && Objects.equals(eventFilters, other.eventFilters)
			&& Objects.equals(pausedFrom, other.pausedFrom) && Objects.equals(pausedUntil, other.pausedUntil)
			&& Objects.equals(created, other.created) && Objects.equals(modified, other.modified)
			&& Objects.equals(createdBy, other.createdBy) && Objects.equals(subscriptionCount, other.subscriptionCount);
	}

	@Override
	public String toString() {
		return "Subscriber{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", identifier=" + identifier +
			", channels=" + channels +
			", eventFilters=" + eventFilters +
			", pausedFrom=" + pausedFrom +
			", pausedUntil=" + pausedUntil +
			", created=" + created +
			", modified=" + modified +
			", createdBy=" + createdBy +
			", subscriptionCount=" + subscriptionCount +
			'}';
	}
}
