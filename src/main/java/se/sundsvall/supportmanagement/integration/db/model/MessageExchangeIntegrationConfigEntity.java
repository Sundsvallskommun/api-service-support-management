package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;

import static jakarta.persistence.GenerationType.IDENTITY;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "message_exchange_integration_config",
	indexes = {
		@Index(name = "idx_mex_integration_config_namespace_municipality_id", columnList = "namespace, municipality_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_mex_integration_config_namespace_municipality_id", columnNames = {
			"namespace", "municipality_id"
		})
	})
public class MessageExchangeIntegrationConfigEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "trigger_status_change_on")
	private String triggerStatusChangeOn;

	@Column(name = "status_change_to")
	private String statusChangeTo;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	public static MessageExchangeIntegrationConfigEntity create() {
		return new MessageExchangeIntegrationConfigEntity();
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public MessageExchangeIntegrationConfigEntity withId(final Long id) {
		setId(id);
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public MessageExchangeIntegrationConfigEntity withMunicipalityId(final String municipalityId) {
		setMunicipalityId(municipalityId);
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public MessageExchangeIntegrationConfigEntity withNamespace(final String namespace) {
		setNamespace(namespace);
		return this;
	}

	public String getTriggerStatusChangeOn() {
		return triggerStatusChangeOn;
	}

	public void setTriggerStatusChangeOn(final String triggerStatusChangeOn) {
		this.triggerStatusChangeOn = triggerStatusChangeOn;
	}

	public MessageExchangeIntegrationConfigEntity withTriggerStatusChangeOn(final String triggerStatusChangeOn) {
		setTriggerStatusChangeOn(triggerStatusChangeOn);
		return this;
	}

	public String getStatusChangeTo() {
		return statusChangeTo;
	}

	public void setStatusChangeTo(final String statusChangeTo) {
		this.statusChangeTo = statusChangeTo;
	}

	public MessageExchangeIntegrationConfigEntity withStatusChangeTo(final String statusChangeTo) {
		setStatusChangeTo(statusChangeTo);
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public MessageExchangeIntegrationConfigEntity withCreated(final OffsetDateTime created) {
		setCreated(created);
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public MessageExchangeIntegrationConfigEntity withModified(final OffsetDateTime modified) {
		setModified(modified);
		return this;
	}

	@PrePersist
	void onCreate() {
		created = now(systemDefault()).truncatedTo(MILLIS);
	}

	@PreUpdate
	void onUpdate() {
		modified = now(systemDefault()).truncatedTo(MILLIS);
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final MessageExchangeIntegrationConfigEntity that = (MessageExchangeIntegrationConfigEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(municipalityId, that.municipalityId) && Objects.equals(namespace, that.namespace)
			&& Objects.equals(triggerStatusChangeOn, that.triggerStatusChangeOn) && Objects.equals(statusChangeTo, that.statusChangeTo)
			&& Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, municipalityId, namespace, triggerStatusChangeOn, statusChangeTo, created, modified);
	}

	@Override
	public String toString() {
		return "MessageExchangeIntegrationConfigEntity{" +
			"id=" + id +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", triggerStatusChangeOn='" + triggerStatusChangeOn + '\'' +
			", statusChangeTo='" + statusChangeTo + '\'' +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
