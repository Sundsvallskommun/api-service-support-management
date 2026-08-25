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
@Table(name = "messaging_outbox",
	indexes = @Index(name = "idx_outbox_dead_letter_retry", columnList = "dead_letter,next_retry_at"))
public class MessagingOutboxEntity {

	@Id
	@UuidGenerator
	@Column(name = "id", length = 36)
	private String id;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "message_type", nullable = false, length = 16)
	private String messageType;

	@Column(name = "payload", nullable = false, columnDefinition = "text")
	private String payload;

	@Column(name = "created", nullable = false, columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "retry_count", nullable = false)
	private int retryCount;

	@Column(name = "next_retry_at", columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime nextRetryAt;

	@Column(name = "dead_letter", nullable = false)
	private boolean deadLetter;

	public static MessagingOutboxEntity create() {
		return new MessagingOutboxEntity();
	}

	@PrePersist
	void onCreate() {
		created = now(systemDefault()).truncatedTo(MILLIS);
	}

	public String getId() {
		return id;
	}

	public MessagingOutboxEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public MessagingOutboxEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getMessageType() {
		return messageType;
	}

	public MessagingOutboxEntity withMessageType(final String messageType) {
		this.messageType = messageType;
		return this;
	}

	public String getPayload() {
		return payload;
	}

	public MessagingOutboxEntity withPayload(final String payload) {
		this.payload = payload;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public MessagingOutboxEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(final int retryCount) {
		this.retryCount = retryCount;
	}

	public OffsetDateTime getNextRetryAt() {
		return nextRetryAt;
	}

	public void setNextRetryAt(final OffsetDateTime nextRetryAt) {
		this.nextRetryAt = nextRetryAt;
	}

	public boolean isDeadLetter() {
		return deadLetter;
	}

	public void setDeadLetter(final boolean deadLetter) {
		this.deadLetter = deadLetter;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final var that = (MessagingOutboxEntity) o;
		return retryCount == that.retryCount
			&& deadLetter == that.deadLetter
			&& Objects.equals(id, that.id)
			&& Objects.equals(municipalityId, that.municipalityId)
			&& Objects.equals(messageType, that.messageType)
			&& Objects.equals(payload, that.payload)
			&& Objects.equals(created, that.created)
			&& Objects.equals(nextRetryAt, that.nextRetryAt);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, municipalityId, messageType, payload, created, retryCount, nextRetryAt, deadLetter);
	}

	@Override
	public String toString() {
		return "MessagingOutboxEntity{" +
			"id='" + id + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", messageType='" + messageType + '\'' +
			", created=" + created +
			", retryCount=" + retryCount +
			", nextRetryAt=" + nextRetryAt +
			", deadLetter=" + deadLetter +
			'}';
	}
}
