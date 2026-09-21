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
@Table(name = "email_dispatch_outbox")
public class EmailDispatchOutboxEntity {

	@Id
	@UuidGenerator
	@Column(name = "id", length = 36)
	private String id;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "errand_id", nullable = false, length = 36)
	private String errandId;

	@Column(name = "errand_number")
	private String errandNumber;

	@Column(name = "subscriber_id", nullable = false, length = 36)
	private String subscriberId;

	@Column(name = "recipient_email")
	private String recipientEmail;

	@Column(name = "identifier_type", length = 16)
	private String identifierType;

	@Column(name = "identifier_value")
	private String identifierValue;

	@Column(name = "event_summary", columnDefinition = "text")
	private String eventSummary;

	@Column(name = "created", nullable = false, columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "attempts", nullable = false)
	private int attempts;

	@Column(name = "last_attempted", columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime lastAttempted;

	public static EmailDispatchOutboxEntity create() {
		return new EmailDispatchOutboxEntity();
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

	public EmailDispatchOutboxEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public EmailDispatchOutboxEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public EmailDispatchOutboxEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public EmailDispatchOutboxEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getErrandNumber() {
		return errandNumber;
	}

	public void setErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
	}

	public EmailDispatchOutboxEntity withErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
		return this;
	}

	public String getSubscriberId() {
		return subscriberId;
	}

	public void setSubscriberId(final String subscriberId) {
		this.subscriberId = subscriberId;
	}

	public EmailDispatchOutboxEntity withSubscriberId(final String subscriberId) {
		this.subscriberId = subscriberId;
		return this;
	}

	public String getRecipientEmail() {
		return recipientEmail;
	}

	public void setRecipientEmail(final String recipientEmail) {
		this.recipientEmail = recipientEmail;
	}

	public EmailDispatchOutboxEntity withRecipientEmail(final String recipientEmail) {
		this.recipientEmail = recipientEmail;
		return this;
	}

	public String getIdentifierType() {
		return identifierType;
	}

	public void setIdentifierType(final String identifierType) {
		this.identifierType = identifierType;
	}

	public EmailDispatchOutboxEntity withIdentifierType(final String identifierType) {
		this.identifierType = identifierType;
		return this;
	}

	public String getIdentifierValue() {
		return identifierValue;
	}

	public void setIdentifierValue(final String identifierValue) {
		this.identifierValue = identifierValue;
	}

	public EmailDispatchOutboxEntity withIdentifierValue(final String identifierValue) {
		this.identifierValue = identifierValue;
		return this;
	}

	public String getEventSummary() {
		return eventSummary;
	}

	public void setEventSummary(final String eventSummary) {
		this.eventSummary = eventSummary;
	}

	public EmailDispatchOutboxEntity withEventSummary(final String eventSummary) {
		this.eventSummary = eventSummary;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public EmailDispatchOutboxEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public int getAttempts() {
		return attempts;
	}

	public void setAttempts(final int attempts) {
		this.attempts = attempts;
	}

	public EmailDispatchOutboxEntity withAttempts(final int attempts) {
		this.attempts = attempts;
		return this;
	}

	public OffsetDateTime getLastAttempted() {
		return lastAttempted;
	}

	public void setLastAttempted(final OffsetDateTime lastAttempted) {
		this.lastAttempted = lastAttempted;
	}

	public EmailDispatchOutboxEntity withLastAttempted(final OffsetDateTime lastAttempted) {
		this.lastAttempted = lastAttempted;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, municipalityId, namespace, errandId, errandNumber, subscriberId, recipientEmail, identifierType, identifierValue, eventSummary, created, attempts, lastAttempted);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final EmailDispatchOutboxEntity other = (EmailDispatchOutboxEntity) obj;
		return attempts == other.attempts
			&& Objects.equals(id, other.id)
			&& Objects.equals(municipalityId, other.municipalityId)
			&& Objects.equals(namespace, other.namespace)
			&& Objects.equals(errandId, other.errandId)
			&& Objects.equals(errandNumber, other.errandNumber)
			&& Objects.equals(subscriberId, other.subscriberId)
			&& Objects.equals(recipientEmail, other.recipientEmail)
			&& Objects.equals(identifierType, other.identifierType)
			&& Objects.equals(identifierValue, other.identifierValue)
			&& Objects.equals(eventSummary, other.eventSummary)
			&& Objects.equals(created, other.created)
			&& Objects.equals(lastAttempted, other.lastAttempted);
	}

	@Override
	public String toString() {
		return "EmailDispatchOutboxEntity{" +
			"id='" + id + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", errandId='" + errandId + '\'' +
			", errandNumber='" + errandNumber + '\'' +
			", subscriberId='" + subscriberId + '\'' +
			", recipientEmail='" + recipientEmail + '\'' +
			", identifierType='" + identifierType + '\'' +
			", identifierValue='" + identifierValue + '\'' +
			", eventSummary='" + eventSummary + '\'' +
			", created=" + created +
			", attempts=" + attempts +
			", lastAttempted=" + lastAttempted +
			'}';
	}
}
