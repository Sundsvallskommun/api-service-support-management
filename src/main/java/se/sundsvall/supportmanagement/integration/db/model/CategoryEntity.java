package se.sundsvall.supportmanagement.integration.db.model;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "category", indexes = {
	@Index(name = "idx_namespace_municipality_id", columnList = "namespace, municipality_id")
}, uniqueConstraints = {
	@UniqueConstraint(name = "uq_namespace_municipality_id_name", columnNames = { "namespace", "municipality_id", "name" })
})
public class CategoryEntity implements Serializable {

	private static final long serialVersionUID = -5979976910282343331L;

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

	@Column(name = "municipality_id", nullable = false)
	private String municipalityId;

	@Column(name = "namespace", nullable = false)
	private String namespace;

	@Column(name = "created")
	private OffsetDateTime created;

	@Column(name = "modified")
	private OffsetDateTime modified;

	public static CategoryEntity create() {
		return new CategoryEntity();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public CategoryEntity withId(Long id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public CategoryEntity withName(String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public CategoryEntity withDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}

	public List<TypeEntity> getTypes() {
		return types;
	}

	public void setTypes(List<TypeEntity> types) {
		Optional.ofNullable(this.types).ifPresentOrElse(
			List::clear,
			() -> this.types = new ArrayList<>());

		Optional.ofNullable(types).orElse(Collections.emptyList()).stream()
			.filter(Objects::nonNull)
			.forEach(type -> this.types.add(type.withCategoryEntity(this)));
	}

	public CategoryEntity withTypes(List<TypeEntity> types) {
		setTypes(types);
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public CategoryEntity withMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public CategoryEntity withNamespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public CategoryEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public CategoryEntity withModified(OffsetDateTime modified) {
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
		CategoryEntity other = (CategoryEntity) obj;
		return Objects.equals(created, other.created) && Objects.equals(displayName, other.displayName) && Objects.equals(id, other.id) && Objects.equals(modified, other.modified) && Objects.equals(municipalityId, other.municipalityId) && Objects
			.equals(name, other.name) && Objects.equals(namespace, other.namespace) && Objects.equals(types, other.types);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CategoryEntity [id=").append(id).append(", name=").append(name).append(", displayName=").append(displayName).append(", types=").append(types).append(", municipalityId=").append(municipalityId).append(", namespace=")
			.append(namespace).append(", created=").append(created).append(", modified=").append(modified).append("]");
		return builder.toString();
	}
}
