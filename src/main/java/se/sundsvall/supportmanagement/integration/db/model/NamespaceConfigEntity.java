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
import org.hibernate.annotations.TimeZoneStorage;

import java.time.OffsetDateTime;
import java.util.Objects;

import static jakarta.persistence.GenerationType.IDENTITY;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "namespace_config",
	indexes = {
		@Index(name = "idx_namespace_municipality_id", columnList = "namespace, municipality_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_namespace_municipality_id", columnNames = {"namespace", "municipality_id"})
	})
public class NamespaceConfigEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "municipality_id", nullable = false)
	private String municipalityId;

	@Column(name = "namespace", nullable = false)
	private String namespace;

	@Column(name = "short_code", nullable = false)
	private String shortCode;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	public static NamespaceConfigEntity create() {
		return new NamespaceConfigEntity();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public NamespaceConfigEntity withId(Long id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public NamespaceConfigEntity withMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public NamespaceConfigEntity withNamespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getShortCode() {
		return shortCode;
	}

	public void setShortCode(String shortCode) {
		this.shortCode = shortCode;
	}

	public NamespaceConfigEntity withShortCode(String shortCode) {
		this.shortCode = shortCode;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public NamespaceConfigEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public NamespaceConfigEntity withModified(OffsetDateTime modified) {
		this.modified = modified;
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
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		NamespaceConfigEntity that = (NamespaceConfigEntity) object;
		return Objects.equals(id, that.id) && Objects.equals(municipalityId, that.municipalityId) && Objects.equals(namespace, that.namespace) && Objects.equals(shortCode, that.shortCode) && Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, municipalityId, namespace, shortCode, created, modified);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("NamespaceConfigEntity{");
		sb.append("id=").append(id);
		sb.append(", municipalityId='").append(municipalityId).append('\'');
		sb.append(", namespace='").append(namespace).append('\'');
		sb.append(", shortCode='").append(shortCode).append('\'');
		sb.append(", created=").append(created);
		sb.append(", modified=").append(modified);
		sb.append('}');
		return sb.toString();
	}
}
