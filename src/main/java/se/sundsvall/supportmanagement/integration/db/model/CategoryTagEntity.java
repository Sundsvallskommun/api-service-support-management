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
@Table(name = "category_tag", indexes = {
	@Index(name = "idx_namespace_municipality_id", columnList = "namespace, municipality_id")
}, uniqueConstraints = {
	@UniqueConstraint(name = "uq_namespace_municipality_id_name", columnNames = { "namespace", "municipality_id", "name" })
})
public class CategoryTagEntity implements Serializable {

	private static final long serialVersionUID = -5979976910282343331L;

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "display_name")
	private String displayName;

	@OneToMany(mappedBy = "categoryTagEntity", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<TypeTagEntity> typeTags;

	@Column(name = "municipality_id", nullable = false)
	private String municipalityId;

	@Column(name = "namespace", nullable = false)
	private String namespace;

	@Column(name = "created")
	private OffsetDateTime created;

	@Column(name = "modified")
	private OffsetDateTime modified;

	public static CategoryTagEntity create() {
		return new CategoryTagEntity();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public CategoryTagEntity withId(Long id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public CategoryTagEntity withName(String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public CategoryTagEntity withDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}

	public List<TypeTagEntity> getTypeTags() {
		return typeTags;
	}

	public void setTypeTags(List<TypeTagEntity> typeTags) {
		Optional.ofNullable(this.typeTags).ifPresentOrElse(
			List::clear,
			() -> this.typeTags = new ArrayList<>());

		Optional.ofNullable(typeTags).orElse(Collections.emptyList()).stream()
			.filter(Objects::nonNull)
			.forEach(tt -> this.typeTags.add(tt.withCategoryTagEntity(this)));
	}

	public CategoryTagEntity withTypeTags(List<TypeTagEntity> typeTags) {
		setTypeTags(typeTags);
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public CategoryTagEntity withMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public CategoryTagEntity withNamespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public CategoryTagEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public CategoryTagEntity withModified(OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@PrePersist
	void onCreate() {
		Optional.ofNullable(typeTags).ifPresent(tt -> tt
			.forEach(t -> t.setCategoryTagEntity(this)));
		created = now(systemDefault()).truncatedTo(MILLIS);
	}

	@PreUpdate
	protected void onUpdate() {
		Optional.ofNullable(typeTags).ifPresent(tt -> tt
			.forEach(t -> t.setCategoryTagEntity(this)));
		modified = now(systemDefault()).truncatedTo(MILLIS);
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, displayName, id, modified, municipalityId, name, namespace, typeTags);
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
		CategoryTagEntity other = (CategoryTagEntity) obj;
		return Objects.equals(created, other.created) && Objects.equals(displayName, other.displayName) && Objects.equals(id, other.id) && Objects.equals(modified, other.modified) && Objects.equals(municipalityId, other.municipalityId) && Objects
			.equals(name, other.name) && Objects.equals(namespace, other.namespace) && Objects.equals(typeTags, other.typeTags);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CategoryTagEntity [id=").append(id).append(", name=").append(name).append(", displayName=").append(displayName).append(", typeTags=").append(typeTags).append(", municipalityId=").append(municipalityId).append(", namespace=")
			.append(namespace).append(", created=").append(created).append(", modified=").append(modified).append("]");
		return builder.toString();
	}
}
