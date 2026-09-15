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

/**
 * The purposes an attachment of the errand may be given, registered per namespace.
 * <p>
 * The recommended values - SUPPORTING, RESPONSE, DECISION, APPENDIX, PROTOCOL - are not seeded. A namespace registers
 * the ones it uses, which is what keeps the purpose free per line of business while still rejecting a value nobody
 * meant to write.
 */
@Entity
@Table(name = "attachment_purpose",
	indexes = {
		@Index(name = "idx_attachment_purpose_namespace_municipality_id", columnList = "namespace, municipality_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_attachment_purpose_namespace_municipality_id_name", columnNames = {
			"namespace", "municipality_id", "name"
		})
	})
public class AttachmentPurposeEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "sort_order")
	private Integer sortOrder;

	@Column(name = "deprecated", nullable = false)
	private boolean deprecated;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	@PrePersist
	void onCreate() {
		created = now(systemDefault()).truncatedTo(MILLIS);
	}

	@PreUpdate
	void onUpdate() {
		modified = now(systemDefault()).truncatedTo(MILLIS);
	}

	public static AttachmentPurposeEntity create() {
		return new AttachmentPurposeEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public AttachmentPurposeEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public AttachmentPurposeEntity withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public AttachmentPurposeEntity withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public AttachmentPurposeEntity withSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
		return this;
	}

	public boolean isDeprecated() {
		return deprecated;
	}

	public void setDeprecated(final boolean deprecated) {
		this.deprecated = deprecated;
	}

	public AttachmentPurposeEntity withDeprecated(final boolean deprecated) {
		this.deprecated = deprecated;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public AttachmentPurposeEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public AttachmentPurposeEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public AttachmentPurposeEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public AttachmentPurposeEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, displayName, sortOrder, deprecated, municipalityId, namespace, created, modified);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final AttachmentPurposeEntity other)) {
			return false;
		}
		return Objects.equals(id, other.id)
			&& Objects.equals(name, other.name)
			&& Objects.equals(displayName, other.displayName)
			&& Objects.equals(sortOrder, other.sortOrder)
			&& (deprecated == other.deprecated)
			&& Objects.equals(municipalityId, other.municipalityId)
			&& Objects.equals(namespace, other.namespace)
			&& Objects.equals(created, other.created)
			&& Objects.equals(modified, other.modified);
	}

	@Override
	public String toString() {
		return "AttachmentPurposeEntity{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", displayName='" + displayName + '\'' +
			", sortOrder=" + sortOrder +
			", deprecated=" + deprecated +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
