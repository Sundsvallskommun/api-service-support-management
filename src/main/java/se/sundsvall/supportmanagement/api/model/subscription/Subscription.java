package se.sundsvall.supportmanagement.api.model.subscription;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import se.sundsvall.supportmanagement.api.model.identifier.Identifier;
import se.sundsvall.supportmanagement.api.model.subscriber.EventFilter;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@Schema(description = "A subscription describes what a subscriber is listening for (an errand or all events in a namespace). " +
	"Subscriptions support create and delete only — to change what is being listened to, delete and create a new one.")
public class Subscription {

	@Null(groups = OnCreate.class)
	@Schema(description = "Unique identifier of the subscription", examples = "123e4567-e89b-12d3-a456-426614174000", accessMode = READ_ONLY)
	private String id;

	@NotNull(groups = OnCreate.class)
	@Valid
	@Schema(description = "What this subscription targets (an errand or the whole namespace).")
	private SubscriptionTarget target;

	@Valid
	@Schema(description = "Optional per-subscription override of the subscriber-level event filters. " +
		"When set, these filters apply to events matched by this subscription instead of the subscriber's global filters. " +
		"When null or empty, the subscriber-level filters are used as-is.")
	private List<EventFilter> eventFilters;

	@DateTimeFormat(iso = DATE_TIME)
	@Schema(description = "Optional expiration timestamp. After this point the subscription is eligible for automatic cleanup.", examples = "2026-12-31T23:59:59+02:00")
	private OffsetDateTime expiresAt;

	@DateTimeFormat(iso = DATE_TIME)
	@Schema(description = "Timestamp when the subscription was created", accessMode = READ_ONLY)
	private OffsetDateTime created;

	@Schema(description = "Identifier of the principal that created the subscription (may differ from the owning subscriber, e.g. when an admin subscribes on behalf of someone else).", accessMode = READ_ONLY)
	private Identifier createdBy;

	public static Subscription create() {
		return new Subscription();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Subscription withId(final String id) {
		this.id = id;
		return this;
	}

	public SubscriptionTarget getTarget() {
		return target;
	}

	public void setTarget(final SubscriptionTarget target) {
		this.target = target;
	}

	public Subscription withTarget(final SubscriptionTarget target) {
		this.target = target;
		return this;
	}

	public List<EventFilter> getEventFilters() {
		return eventFilters;
	}

	public void setEventFilters(final List<EventFilter> eventFilters) {
		this.eventFilters = eventFilters;
	}

	public Subscription withEventFilters(final List<EventFilter> eventFilters) {
		this.eventFilters = eventFilters;
		return this;
	}

	public OffsetDateTime getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(final OffsetDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}

	public Subscription withExpiresAt(final OffsetDateTime expiresAt) {
		this.expiresAt = expiresAt;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Subscription withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public Identifier getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final Identifier createdBy) {
		this.createdBy = createdBy;
	}

	public Subscription withCreatedBy(final Identifier createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, target, eventFilters, expiresAt, created, createdBy);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final Subscription other = (Subscription) obj;
		return Objects.equals(id, other.id) && Objects.equals(target, other.target)
			&& Objects.equals(eventFilters, other.eventFilters) && Objects.equals(expiresAt, other.expiresAt)
			&& Objects.equals(created, other.created) && Objects.equals(createdBy, other.createdBy);
	}

	@Override
	public String toString() {
		return "Subscription{" +
			"id='" + id + '\'' +
			", target=" + target +
			", eventFilters=" + eventFilters +
			", expiresAt=" + expiresAt +
			", created=" + created +
			", createdBy=" + createdBy +
			'}';
	}
}
