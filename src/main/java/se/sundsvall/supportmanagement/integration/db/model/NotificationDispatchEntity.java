package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(name = "notification_dispatch",
	indexes = {
		@Index(name = "idx_dispatch_errand_id", columnList = "errand_id"),
		@Index(name = "idx_dispatch_dead_letter_retry", columnList = "dead_letter, next_retry_at")
	})
public class NotificationDispatchEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "event_id", nullable = false)
	private String eventId;

	@Column(name = "request_group_id")
	private String requestGroupId;

	@Column(name = "errand_id", nullable = false)
	private String errandId;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "event_type", nullable = false, length = 64)
	private String eventType;

	@Column(name = "executing_user_id")
	private String executingUserId;

	@Column(name = "created", nullable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "retry_count", nullable = false)
	private int retryCount;

	@Column(name = "next_retry_at")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime nextRetryAt;

	@Column(name = "dead_letter", nullable = false)
	private boolean deadLetter;

	public static NotificationDispatchEntity create() {
		return new NotificationDispatchEntity();
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

	public NotificationDispatchEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(final String eventId) {
		this.eventId = eventId;
	}

	public NotificationDispatchEntity withEventId(final String eventId) {
		this.eventId = eventId;
		return this;
	}

	public String getRequestGroupId() {
		return requestGroupId;
	}

	public void setRequestGroupId(final String requestGroupId) {
		this.requestGroupId = requestGroupId;
	}

	public NotificationDispatchEntity withRequestGroupId(final String requestGroupId) {
		this.requestGroupId = requestGroupId;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public NotificationDispatchEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public NotificationDispatchEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public NotificationDispatchEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(final String eventType) {
		this.eventType = eventType;
	}

	public NotificationDispatchEntity withEventType(final String eventType) {
		this.eventType = eventType;
		return this;
	}

	public String getExecutingUserId() {
		return executingUserId;
	}

	public void setExecutingUserId(final String executingUserId) {
		this.executingUserId = executingUserId;
	}

	public NotificationDispatchEntity withExecutingUserId(final String executingUserId) {
		this.executingUserId = executingUserId;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public NotificationDispatchEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(final int retryCount) {
		this.retryCount = retryCount;
	}

	public NotificationDispatchEntity withRetryCount(final int retryCount) {
		this.retryCount = retryCount;
		return this;
	}

	public OffsetDateTime getNextRetryAt() {
		return nextRetryAt;
	}

	public void setNextRetryAt(final OffsetDateTime nextRetryAt) {
		this.nextRetryAt = nextRetryAt;
	}

	public NotificationDispatchEntity withNextRetryAt(final OffsetDateTime nextRetryAt) {
		this.nextRetryAt = nextRetryAt;
		return this;
	}

	public boolean isDeadLetter() {
		return deadLetter;
	}

	public void setDeadLetter(final boolean deadLetter) {
		this.deadLetter = deadLetter;
	}

	public NotificationDispatchEntity withDeadLetter(final boolean deadLetter) {
		this.deadLetter = deadLetter;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, eventId, requestGroupId, errandId, municipalityId, namespace, eventType, executingUserId, created, retryCount, nextRetryAt, deadLetter);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final NotificationDispatchEntity other = (NotificationDispatchEntity) obj;
		return retryCount == other.retryCount
			&& deadLetter == other.deadLetter
			&& Objects.equals(id, other.id)
			&& Objects.equals(eventId, other.eventId)
			&& Objects.equals(requestGroupId, other.requestGroupId)
			&& Objects.equals(errandId, other.errandId)
			&& Objects.equals(municipalityId, other.municipalityId)
			&& Objects.equals(namespace, other.namespace)
			&& Objects.equals(eventType, other.eventType)
			&& Objects.equals(executingUserId, other.executingUserId)
			&& Objects.equals(created, other.created)
			&& Objects.equals(nextRetryAt, other.nextRetryAt);
	}

	@Override
	public String toString() {
		return "NotificationDispatchEntity{" +
			"id='" + id + '\'' +
			", eventId='" + eventId + '\'' +
			", requestGroupId='" + requestGroupId + '\'' +
			", errandId='" + errandId + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", eventType='" + eventType + '\'' +
			", executingUserId='" + executingUserId + '\'' +
			", created=" + created +
			", retryCount=" + retryCount +
			", nextRetryAt=" + nextRetryAt +
			", deadLetter=" + deadLetter +
			'}';
	}
}
