package se.sundsvall.supportmanagement.integration.db.model;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static jakarta.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "`type`", uniqueConstraints = {
	@UniqueConstraint(name = "uq_category_id_name", columnNames = { "category_id", "name" })
})
public class TypeEntity implements Serializable {
	private static final long serialVersionUID = -6163643004292601360L;

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "escalation_email")
	private String escalationEmail;

	@Column(name = "created")
	private OffsetDateTime created;

	@Column(name = "modified")
	private OffsetDateTime modified;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_category_id"))
	private CategoryEntity categoryEntity;

	public static TypeEntity create() {
		return new TypeEntity();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public TypeEntity withId(Long id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TypeEntity withName(String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public TypeEntity withDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getEscalationEmail() {
		return escalationEmail;
	}

	public void setEscalationEmail(String escalationEmail) {
		this.escalationEmail = escalationEmail;
	}

	public TypeEntity withEscalationEmail(String escalationEmail) {
		this.escalationEmail = escalationEmail;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public TypeEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public TypeEntity withModified(OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public CategoryEntity getCategoryEntity() {
		return categoryEntity;
	}

	public void setCategoryEntity(CategoryEntity categoryEntity) {
		this.categoryEntity = categoryEntity;
	}

	public TypeEntity withCategoryEntity(CategoryEntity categoryEntity) {
		this.categoryEntity = categoryEntity;
		return this;
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
		var categoryId = Optional.ofNullable(categoryEntity).map(CategoryEntity::getId).orElse(null);
		return Objects.hash(categoryId, created, displayName, escalationEmail, id, modified, name);
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
		TypeEntity other = (TypeEntity) obj;

		var thisCategoryId = Optional.ofNullable(categoryEntity).map(CategoryEntity::getId).orElse(null);
		var otherCategoryId = Optional.ofNullable(other.categoryEntity).map(CategoryEntity::getId).orElse(null);
		return Objects.equals(thisCategoryId, otherCategoryId) && Objects.equals(created, other.created) && Objects.equals(displayName, other.displayName) && Objects.equals(escalationEmail, other.escalationEmail) && Objects.equals(id, other.id)
			&& Objects.equals(modified, other.modified) && Objects.equals(name, other.name);
	}

	@Override
	public String toString() {
		var categoryId = Optional.ofNullable(categoryEntity).map(CategoryEntity::getId).orElse(null);
		StringBuilder builder = new StringBuilder();
		builder.append("TypeEntity [id=").append(id).append(", name=").append(name).append(", displayName=").append(displayName).append(", escalationEmail=").append(escalationEmail).append(", created=").append(created).append(", modified=")
			.append(modified).append(", categoryEntity.id=").append(categoryId).append("]");
		return builder.toString();
	}

}
