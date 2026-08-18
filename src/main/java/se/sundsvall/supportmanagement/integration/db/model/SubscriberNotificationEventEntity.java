package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "subscriber_notification_event")
public class SubscriberNotificationEventEntity {

	@Id
	@UuidGenerator
	@Column(name = "id", length = 36)
	private String id;

	@Column(name = "created", nullable = false, columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "event_type", length = 64)
	private String eventType;

	@Column(name = "description")
	private String description;

	@Column(name = "sub_type", length = 64)
	private String subType;

	public static SubscriberNotificationEventEntity create() {
		return new SubscriberNotificationEventEntity();
	}

	@PrePersist
	void onCreate() {
		created = now(systemDefault()).truncatedTo(MILLIS);
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public SubscriberNotificationEventEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public SubscriberNotificationEventEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(final String eventType) {
		this.eventType = eventType;
	}

	public SubscriberNotificationEventEntity withEventType(final String eventType) {
		this.eventType = eventType;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public SubscriberNotificationEventEntity withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getSubType() {
		return subType;
	}

	public void setSubType(final String subType) {
		this.subType = subType;
	}

	public SubscriberNotificationEventEntity withSubType(final String subType) {
		this.subType = subType;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, created, eventType, description, subType);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		final SubscriberNotificationEventEntity other = (SubscriberNotificationEventEntity) obj;
		return Objects.equals(id, other.id)
			&& Objects.equals(created, other.created)
			&& Objects.equals(eventType, other.eventType)
			&& Objects.equals(description, other.description)
			&& Objects.equals(subType, other.subType);
	}

	@Override
	public String toString() {
		return "SubscriberNotificationEventEntity{" +
			"id='" + id + '\'' +
			", created=" + created +
			", eventType='" + eventType + '\'' +
			", description='" + description + '\'' +
			", subType='" + subType + '\'' +
			'}';
	}
}
