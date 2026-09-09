package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "measure_type",
	indexes = {
		@Index(name = "idx_measure_type_namespace_municipality_id", columnList = "namespace, municipality_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_measure_type_namespace_municipality_id_name", columnNames = {
			"namespace", "municipality_id", "name"
		})
	})
public class MeasureTypeEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "display_name")
	private String displayName;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
		name = "measure_type_groups",
		joinColumns = @JoinColumn(name = "measure_type_id",
			foreignKey = @ForeignKey(name = "fk_measure_type_groups_measure_type_id")))
	@Column(name = "measure_group")
	private List<String> measureGroups;

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

	public static MeasureTypeEntity create() {
		return new MeasureTypeEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public MeasureTypeEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public MeasureTypeEntity withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public MeasureTypeEntity withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public List<String> getMeasureGroups() {
		return measureGroups;
	}

	public void setMeasureGroups(final List<String> measureGroups) {
		this.measureGroups = measureGroups;
	}

	public MeasureTypeEntity withMeasureGroups(final List<String> measureGroups) {
		this.measureGroups = measureGroups;
		return this;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public MeasureTypeEntity withSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
		return this;
	}

	public boolean isDeprecated() {
		return deprecated;
	}

	public void setDeprecated(final boolean deprecated) {
		this.deprecated = deprecated;
	}

	public MeasureTypeEntity withDeprecated(final boolean deprecated) {
		this.deprecated = deprecated;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public MeasureTypeEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public MeasureTypeEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public MeasureTypeEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public MeasureTypeEntity withModified(final OffsetDateTime modified) {
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
		return Objects.hash(created, deprecated, id, measureGroups, modified, municipalityId, name, namespace, displayName, sortOrder);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final MeasureTypeEntity other)) {
			return false;
		}
		return Objects.equals(created, other.created) && deprecated == other.deprecated && Objects.equals(id, other.id) && Objects.equals(measureGroups, other.measureGroups) && Objects.equals(modified, other.modified)
			&& Objects.equals(municipalityId, other.municipalityId) && Objects.equals(name, other.name) && Objects.equals(namespace, other.namespace) && Objects.equals(displayName, other.displayName) && Objects.equals(sortOrder, other.sortOrder);
	}

	@Override
	public String toString() {
		return "MeasureTypeEntity [id=" + id
			+ ", name=" + name
			+ ", displayName=" + displayName
			+ ", measureGroups=" + measureGroups
			+ ", sortOrder=" + sortOrder
			+ ", deprecated=" + deprecated
			+ ", municipalityId=" + municipalityId
			+ ", namespace=" + namespace
			+ ", created=" + created
			+ ", modified=" + modified
			+ "]";
	}
}
