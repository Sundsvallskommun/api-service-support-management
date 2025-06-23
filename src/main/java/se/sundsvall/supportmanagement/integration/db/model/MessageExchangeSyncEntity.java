package se.sundsvall.supportmanagement.integration.db.model;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;

@Entity
@Table(name = "message_exchange_sync")
public class MessageExchangeSyncEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "namespace", length = 32, nullable = false)
	private String namespace;

	@Column(name = "municipality_id", length = 4, nullable = false)
	private String municipalityId;

	@Column(name = "latest_synced_sequence_number", columnDefinition = "bigint default 0")
	private Long latestSyncedSequenceNumber;

	@Column(name = "updated")
	@TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
	private OffsetDateTime modified;

	@Column(name = "active")
	private boolean active;

	public static MessageExchangeSyncEntity create() {
		return new MessageExchangeSyncEntity();
	}

	@PreUpdate
	void onUpdate() {
		modified = now(systemDefault()).truncatedTo(MILLIS);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public MessageExchangeSyncEntity withId(Long id) {
		this.id = id;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public MessageExchangeSyncEntity withNamespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public MessageExchangeSyncEntity withMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public Long getLatestSyncedSequenceNumber() {
		return latestSyncedSequenceNumber;
	}

	public void setLatestSyncedSequenceNumber(Long latestSyncedSequenceNumber) {
		this.latestSyncedSequenceNumber = latestSyncedSequenceNumber;
	}

	public MessageExchangeSyncEntity withLatestSyncedSequenceNumber(Long latestSyncedSequenceNumber) {
		this.latestSyncedSequenceNumber = latestSyncedSequenceNumber;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public MessageExchangeSyncEntity withModified(OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public MessageExchangeSyncEntity withActive(boolean active) {
		this.active = active;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		MessageExchangeSyncEntity that = (MessageExchangeSyncEntity) o;
		return active == that.active && Objects.equals(id, that.id) && Objects.equals(namespace, that.namespace) && Objects.equals(municipalityId, that.municipalityId) && Objects.equals(latestSyncedSequenceNumber,
			that.latestSyncedSequenceNumber) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, namespace, municipalityId, latestSyncedSequenceNumber, modified, active);
	}

	@Override
	public String toString() {
		return "MessageExchangeSyncEntity{" +
			"id=" + id +
			", namespace='" + namespace + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", latestSyncedSequenceNumber=" + latestSyncedSequenceNumber +
			", modified=" + modified +
			", active=" + active +
			'}';
	}
}
