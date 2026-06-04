package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "subscriber_notification",
	indexes = {
		@Index(name = "idx_sub_notif_identifier", columnList = "municipality_id, namespace, identifier_type, identifier_value"),
		@Index(name = "idx_sub_notif_errand", columnList = "errand_id")
	},
	uniqueConstraints = @UniqueConstraint(name = "uq_sub_notif_errand_identifier", columnNames = {
		"municipality_id", "namespace", "errand_id", "identifier_type", "identifier_value"
	}))
public class SubscriberNotificationEntity {

	@Id
	@UuidGenerator
	@Column(name = "id", length = 36)
	private String id;

	@Column(name = "created", nullable = false, columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified", columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	@Column(name = "identifier_type", nullable = false, length = 16)
	private String identifierType;

	@Column(name = "identifier_value", nullable = false)
	private String identifierValue;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "errand_id", nullable = false, length = 36)
	private String errandId;

	@Column(name = "errand_number")
	private String errandNumber;

	@Column(name = "expires", columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime expires;

	@Column(name = "acknowledged", columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime acknowledged;

	public static SubscriberNotificationEntity create() {
		return new SubscriberNotificationEntity();
	}

	@PrePersist
	void onCreate() {
		created = now(systemDefault()).truncatedTo(MILLIS);
	}

	@PreUpdate
	void onUpdate() {
		modified = now(systemDefault()).truncatedTo(MILLIS);
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public SubscriberNotificationEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public SubscriberNotificationEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public SubscriberNotificationEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public String getIdentifierType() {
		return identifierType;
	}

	public void setIdentifierType(final String identifierType) {
		this.identifierType = identifierType;
	}

	public SubscriberNotificationEntity withIdentifierType(final String identifierType) {
		this.identifierType = identifierType;
		return this;
	}

	public String getIdentifierValue() {
		return identifierValue;
	}

	public void setIdentifierValue(final String identifierValue) {
		this.identifierValue = identifierValue;
	}

	public SubscriberNotificationEntity withIdentifierValue(final String identifierValue) {
		this.identifierValue = identifierValue;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public SubscriberNotificationEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public SubscriberNotificationEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public SubscriberNotificationEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getErrandNumber() {
		return errandNumber;
	}

	public void setErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
	}

	public SubscriberNotificationEntity withErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
		return this;
	}

	public OffsetDateTime getExpires() {
		return expires;
	}

	public void setExpires(final OffsetDateTime expires) {
		this.expires = expires;
	}

	public SubscriberNotificationEntity withExpires(final OffsetDateTime expires) {
		this.expires = expires;
		return this;
	}

	public OffsetDateTime getAcknowledged() {
		return acknowledged;
	}

	public void setAcknowledged(final OffsetDateTime acknowledged) {
		this.acknowledged = acknowledged;
	}

	public SubscriberNotificationEntity withAcknowledged(final OffsetDateTime acknowledged) {
		this.acknowledged = acknowledged;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, created, modified, identifierType, identifierValue, municipalityId, namespace, errandId, errandNumber, expires, acknowledged);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		final SubscriberNotificationEntity other = (SubscriberNotificationEntity) obj;
		return Objects.equals(id, other.id)
			&& Objects.equals(created, other.created)
			&& Objects.equals(modified, other.modified)
			&& Objects.equals(identifierType, other.identifierType)
			&& Objects.equals(identifierValue, other.identifierValue)
			&& Objects.equals(municipalityId, other.municipalityId)
			&& Objects.equals(namespace, other.namespace)
			&& Objects.equals(errandId, other.errandId)
			&& Objects.equals(errandNumber, other.errandNumber)
			&& Objects.equals(expires, other.expires)
			&& Objects.equals(acknowledged, other.acknowledged);
	}

	@Override
	public String toString() {
		return "SubscriberNotificationEntity{" +
			"id='" + id + '\'' +
			", created=" + created +
			", modified=" + modified +
			", identifierType='" + identifierType + '\'' +
			", identifierValue='" + identifierValue + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", errandId='" + errandId + '\'' +
			", errandNumber='" + errandNumber + '\'' +
			", expires=" + expires +
			", acknowledged=" + acknowledged +
			'}';
	}
}
