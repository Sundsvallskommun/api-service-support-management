package se.sundsvall.supportmanagement.api.model.subscription;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import se.sundsvall.supportmanagement.api.model.subscriber.EventFilter;
import se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannelType;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@Schema(description = "A subscription profile is a named set of event filters and the channels the events they match are delivered on. " +
	"A subscription pointing at a profile is governed by the profile alone - the subscriber's own event filters and channels do not apply to it.")
public class SubscriptionProfile {

	@Null(groups = {
		OnCreate.class, OnUpdate.class
	})
	@Schema(description = "Unique identifier of the subscription profile", examples = "123e4567-e89b-12d3-a456-426614174000", accessMode = READ_ONLY)
	private String id;

	@NotBlank(groups = OnCreate.class)
	@Size(max = 255)
	@Schema(description = "Name of the profile, unique within the namespace", examples = "Mejl om nya ärenden och meddelanden")
	private String name;

	@Size(max = 255)
	@Schema(description = "Optional description of what the profile is for", examples = "Används av enhetschefer och verksamhetschefer")
	private String description;

	@NotEmpty(groups = OnCreate.class)
	@Valid
	@Schema(description = "Event filters selecting which events the profile delivers. An event matching any of them is delivered.")
	private List<@NotNull EventFilter> eventFilters;

	@NotEmpty(groups = OnCreate.class)
	@Schema(description = "Channels the events matched by the profile are delivered on")
	private List<@NotNull NotificationChannelType> channels;

	@Null(groups = {
		OnCreate.class, OnUpdate.class
	})
	@DateTimeFormat(iso = DATE_TIME)
	@Schema(description = "Timestamp when the profile was created", accessMode = READ_ONLY)
	private OffsetDateTime created;

	@Null(groups = {
		OnCreate.class, OnUpdate.class
	})
	@DateTimeFormat(iso = DATE_TIME)
	@Schema(description = "Timestamp when the profile was last modified", accessMode = READ_ONLY)
	private OffsetDateTime modified;

	public static SubscriptionProfile create() {
		return new SubscriptionProfile();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public SubscriptionProfile withId(final String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public SubscriptionProfile withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public SubscriptionProfile withDescription(final String description) {
		this.description = description;
		return this;
	}

	public List<EventFilter> getEventFilters() {
		return eventFilters;
	}

	public void setEventFilters(final List<EventFilter> eventFilters) {
		this.eventFilters = eventFilters;
	}

	public SubscriptionProfile withEventFilters(final List<EventFilter> eventFilters) {
		this.eventFilters = eventFilters;
		return this;
	}

	public List<NotificationChannelType> getChannels() {
		return channels;
	}

	public void setChannels(final List<NotificationChannelType> channels) {
		this.channels = channels;
	}

	public SubscriptionProfile withChannels(final List<NotificationChannelType> channels) {
		this.channels = channels;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public SubscriptionProfile withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public SubscriptionProfile withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, description, eventFilters, channels, created, modified);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final SubscriptionProfile other = (SubscriptionProfile) obj;
		return Objects.equals(id, other.id) && Objects.equals(name, other.name) && Objects.equals(description, other.description)
			&& Objects.equals(eventFilters, other.eventFilters) && Objects.equals(channels, other.channels)
			&& Objects.equals(created, other.created) && Objects.equals(modified, other.modified);
	}

	@Override
	public String toString() {
		return "SubscriptionProfile{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", description='" + description + '\'' +
			", eventFilters=" + eventFilters +
			", channels=" + channels +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
