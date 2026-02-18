package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.annotations.TimeZoneStorage;

import static jakarta.persistence.GenerationType.IDENTITY;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "category",
	indexes = {
		@Index(name = "idx_namespace_municipality_id", columnList = "namespace, municipality_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_namespace_municipality_id_name", columnNames = {
			"namespace", "municipality_id", "name"
		})
	})
public class CategoryEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "display_name")
	private String displayName;

	@OneToMany(mappedBy = "categoryEntity", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<TypeEntity> types;

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

	public static CategoryEntity create() {
		return new CategoryEntity();
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public CategoryEntity withId(final Long id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public CategoryEntity withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public CategoryEntity withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public List<TypeEntity> getTypes() {
		return types;
	}

	public void setTypes(final List<TypeEntity> types) {
		Optional.ofNullable(this.types).ifPresentOrElse(
			List::clear,
			() -> this.types = new ArrayList<>());

		Optional.ofNullable(types).orElse(Collections.emptyList()).stream()
			.filter(Objects::nonNull)
			.forEach(type -> this.types.add(type.withCategoryEntity(this)));
	}

	public CategoryEntity withTypes(final List<TypeEntity> types) {
		setTypes(types);
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public CategoryEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public CategoryEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public CategoryEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public CategoryEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@PrePersist
	void onCreate() {
		Optional.ofNullable(types).ifPresent(tt -> tt
			.forEach(t -> t.setCategoryEntity(this)));
		created = now(systemDefault()).truncatedTo(MILLIS);
	}

	@PreUpdate
	void onUpdate() {
		Optional.ofNullable(types).ifPresent(tt -> tt
			.forEach(t -> t.setCategoryEntity(this)));
		modified = now(systemDefault()).truncatedTo(MILLIS);
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, displayName, id, modified, municipalityId, name, namespace, types);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final CategoryEntity other = (CategoryEntity) obj;
		return Objects.equals(created, other.created) && Objects.equals(displayName, other.displayName) && Objects.equals(id, other.id) && Objects.equals(modified, other.modified) && Objects.equals(municipalityId, other.municipalityId) && Objects
			.equals(name, other.name) && Objects.equals(namespace, other.namespace) && Objects.equals(types, other.types);
	}

	@Override
	public String toString() {
		return "CategoryEntity{" +
			"id=" + id +
			", name='" + name + '\'' +
			", displayName='" + displayName + '\'' +
			", types=" + types +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
