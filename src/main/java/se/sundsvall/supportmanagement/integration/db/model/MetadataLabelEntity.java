package se.sundsvall.supportmanagement.integration.db.model;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "metadata_label",
	indexes = {
		@Index(name = "idx_namespace_municipality_id", columnList = "namespace, municipality_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_namespace_municipality_id_resource_path", columnNames = {
			"namespace", "municipality_id", "resource_path"
		})
	})
public class MetadataLabelEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "classification")
	private String classification;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "resource_name")
	private String resourceName;

	@Column(name = "resource_path")
	private String resourcePath;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_metadata_label_id"))
	private MetadataLabelEntity parent;

	@OneToMany(mappedBy = "parent", cascade = ALL, orphanRemoval = true)
	private List<MetadataLabelEntity> metadataLabels = new ArrayList<>();

	public static MetadataLabelEntity create() {
		return new MetadataLabelEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public MetadataLabelEntity withId(String id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public MetadataLabelEntity withMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public MetadataLabelEntity withNamespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getClassification() {
		return classification;
	}

	public void setClassification(String classification) {
		this.classification = classification;
	}

	public MetadataLabelEntity withClassification(String classification) {
		this.classification = classification;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public MetadataLabelEntity withDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	public MetadataLabelEntity withResourceName(String resourceName) {
		this.resourceName = resourceName;
		return this;
	}

	public String getResourcePath() {
		return resourcePath;
	}

	public void setResourcePath(String resourcePath) {
		this.resourcePath = resourcePath;
	}

	public MetadataLabelEntity withResourcePath(String resourcePath) {
		this.resourcePath = resourcePath;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public MetadataLabelEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public MetadataLabelEntity withModified(OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public MetadataLabelEntity getParent() {
		return parent;
	}

	public void setParent(MetadataLabelEntity parent) {
		this.parent = parent;
	}

	public MetadataLabelEntity withParent(MetadataLabelEntity parent) {
		this.parent = parent;
		return this;
	}

	public List<MetadataLabelEntity> getMetadataLabels() {
		return metadataLabels;
	}

	public void setMetadataLabels(List<MetadataLabelEntity> metadataLabels) {
		this.metadataLabels = metadataLabels;
	}

	public MetadataLabelEntity withMetadataLabels(List<MetadataLabelEntity> metadataLabels) {
		this.metadataLabels = Optional.ofNullable(metadataLabels).orElseGet(ArrayList::new);
		this.metadataLabels.forEach(children -> children.setParent(this));
		return this;
	}

	public void addChild(MetadataLabelEntity child) {
		metadataLabels.add(child);
		child.setParent(this);
	}

	public void removeChild(MetadataLabelEntity child) {
		metadataLabels.remove(child);
		child.setParent(null);
	}

	@PrePersist
	void onCreate() {
		created = now(ZoneId.systemDefault()).truncatedTo(MILLIS);
	}

	@PreUpdate
	void onUpdate() {
		modified = now(ZoneId.systemDefault()).truncatedTo(MILLIS);
	}

	@Override
	public int hashCode() {
		return Objects.hash(classification, created, displayName, id, metadataLabels, modified, municipalityId, namespace, parent, resourceName, resourcePath);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MetadataLabelEntity other = (MetadataLabelEntity) obj;
		return Objects.equals(classification, other.classification) && Objects.equals(created, other.created) && Objects.equals(displayName, other.displayName) && Objects.equals(id, other.id) && Objects.equals(metadataLabels, other.metadataLabels)
			&& Objects.equals(modified, other.modified) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(namespace, other.namespace) && Objects.equals(
				resourceName, other.resourceName)
			&& Objects.equals(resourcePath, other.resourcePath);
	}

	@Override
	public String toString() {
		return "MetadataLabelEntity [id=" + id + ", municipalityId=" + municipalityId + ", namespace=" + namespace + ", classification=" + classification + ", displayName=" + displayName + ", resourceName=" + resourceName + ", resourcePath=" + resourcePath
			+ ", created=" + created + ", modified=" + modified + ", parent.id=" + (parent != null ? parent.id : null) + ", metadataLabels=" + metadataLabels + "]";
	}
}
