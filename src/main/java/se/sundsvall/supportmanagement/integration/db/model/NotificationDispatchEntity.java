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
	indexes = @Index(name = "idx_dispatch_errand_id", columnList = "errand_id"))
public class NotificationDispatchEntity {

	@Id
	@UuidGenerator
	@Column(name = "id", length = 36)
	private String id;

	@Column(name = "event_id", length = 36)
	private String eventId;

	@Column(name = "request_group_id", length = 36)
	private String requestGroupId;

	@Column(name = "errand_id", nullable = false, length = 36)
	private String errandId;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "event_type", nullable = false, length = 64)
	private String eventType;

	@Column(name = "description")
	private String description;

	@Column(name = "sub_type", length = 64)
	private String subType;

	@Column(name = "executing_user_id")
	private String executingUserId;

	@Column(name = "created", nullable = false, columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

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

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public NotificationDispatchEntity withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getSubType() {
		return subType;
	}

	public void setSubType(final String subType) {
		this.subType = subType;
	}

	public NotificationDispatchEntity withSubType(final String subType) {
		this.subType = subType;
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

	@Override
	public int hashCode() {
		return Objects.hash(id, eventId, requestGroupId, errandId, municipalityId, namespace, eventType, description, subType, executingUserId, created);
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
		return Objects.equals(id, other.id)
			&& Objects.equals(eventId, other.eventId)
			&& Objects.equals(requestGroupId, other.requestGroupId)
			&& Objects.equals(errandId, other.errandId)
			&& Objects.equals(municipalityId, other.municipalityId)
			&& Objects.equals(namespace, other.namespace)
			&& Objects.equals(eventType, other.eventType)
			&& Objects.equals(description, other.description)
			&& Objects.equals(subType, other.subType)
			&& Objects.equals(executingUserId, other.executingUserId)
			&& Objects.equals(created, other.created);
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
			", description='" + description + '\'' +
			", subType='" + subType + '\'' +
			", executingUserId='" + executingUserId + '\'' +
			", created=" + created +
			'}';
	}
}
