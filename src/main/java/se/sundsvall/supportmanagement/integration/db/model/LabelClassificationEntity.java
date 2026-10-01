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
 * Display name for a label classification. Held apart from the labels, and matched to them on the classification
 * value, so that every label of a classification is renamed by changing one row.
 */
@Entity
@Table(name = "metadata_label_classification",
	indexes = {
		@Index(name = "idx_label_classification_ns_municipality_id", columnList = "namespace, municipality_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_label_classification_ns_municipality_classification", columnNames = {
			"namespace", "municipality_id", "classification"
		})
	})
public class LabelClassificationEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "classification", nullable = false)
	private String classification;

	@Column(name = "display_name")
	private String displayName;

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

	public static LabelClassificationEntity create() {
		return new LabelClassificationEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public LabelClassificationEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getClassification() {
		return classification;
	}

	public void setClassification(final String classification) {
		this.classification = classification;
	}

	public LabelClassificationEntity withClassification(final String classification) {
		this.classification = classification;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public LabelClassificationEntity withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public LabelClassificationEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public LabelClassificationEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public LabelClassificationEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public LabelClassificationEntity withModified(final OffsetDateTime modified) {
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
	public int hashCode() {
		return Objects.hash(classification, created, displayName, id, modified, municipalityId, namespace);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final LabelClassificationEntity other)) {
			return false;
		}
		return Objects.equals(classification, other.classification) && Objects.equals(created, other.created) && Objects.equals(displayName, other.displayName) && Objects.equals(id, other.id)
			&& Objects.equals(modified, other.modified) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(namespace, other.namespace);
	}

	@Override
	public String toString() {
		return "LabelClassificationEntity [id=" + id
			+ ", classification=" + classification
			+ ", displayName=" + displayName
			+ ", municipalityId=" + municipalityId
			+ ", namespace=" + namespace
			+ ", created=" + created
			+ ", modified=" + modified
			+ "]";
	}
}
