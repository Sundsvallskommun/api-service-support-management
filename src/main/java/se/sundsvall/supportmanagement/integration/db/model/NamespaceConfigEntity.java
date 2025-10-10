package se.sundsvall.supportmanagement.integration.db.model;

import static jakarta.persistence.GenerationType.IDENTITY;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

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

@Entity
@Table(name = "namespace_config", indexes = {
	@Index(name = "idx_namespace_municipality_id", columnList = "namespace, municipality_id"),
	@Index(name = "idx_municipality_id", columnList = "municipality_id")
}, uniqueConstraints = {
	@UniqueConstraint(name = "uq_namespace_municipality_id", columnNames = {
		"namespace", "municipality_id"
	})
})
public class NamespaceConfigEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "display_name", nullable = false)
	private String displayName;

	@Column(name = "short_code", nullable = false)
	private String shortCode;

	@Column(name = "notification_ttl_in_days", nullable = false)
	private Integer notificationTTLInDays;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	@Column(name = "access_control")
	private boolean accessControl;

	public static NamespaceConfigEntity create() {
		return new NamespaceConfigEntity();
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public NamespaceConfigEntity withId(final Long id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public NamespaceConfigEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public NamespaceConfigEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public NamespaceConfigEntity withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getShortCode() {
		return shortCode;
	}

	public void setShortCode(final String shortCode) {
		this.shortCode = shortCode;
	}

	public NamespaceConfigEntity withShortCode(final String shortCode) {
		this.shortCode = shortCode;
		return this;
	}

	public Integer getNotificationTTLInDays() {
		return notificationTTLInDays;
	}

	public void setNotificationTTLInDays(final Integer notificationTTLInDays) {
		this.notificationTTLInDays = notificationTTLInDays;
	}

	public NamespaceConfigEntity withNotificationTTLInDays(final Integer notificationTTLInDays) {
		this.notificationTTLInDays = notificationTTLInDays;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public NamespaceConfigEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public NamespaceConfigEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public boolean getAccessControl() {
		return accessControl;
	}

	public void setAccessControl(final boolean accessControl) {
		this.accessControl = accessControl;
	}

	public NamespaceConfigEntity withAccessControl(final boolean accessControl) {
		this.accessControl = accessControl;
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
		final NamespaceConfigEntity that = (NamespaceConfigEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(municipalityId, that.municipalityId) && Objects.equals(namespace, that.namespace) && Objects.equals(displayName, that.displayName) && Objects.equals(
			shortCode, that.shortCode) && Objects.equals(notificationTTLInDays, that.notificationTTLInDays) && Objects.equals(created, that.created) && Objects.equals(modified, that.modified) && Objects.equals(
				accessControl, that.accessControl);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, municipalityId, namespace, displayName, shortCode, notificationTTLInDays, created, modified, accessControl);
	}

	@Override
	public String toString() {
		return "NamespaceConfigEntity{" +
			"id=" + id +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", displayName='" + displayName + '\'' +
			", shortCode='" + shortCode + '\'' +
			", notificationTTLInDays=" + notificationTTLInDays +
			", created=" + created +
			", modified=" + modified +
			", accessControl='" + accessControl + '\'' +
			'}';
	}
}
