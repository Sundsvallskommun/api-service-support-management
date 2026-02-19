package se.sundsvall.supportmanagement.integration.db.model;

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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.annotations.TimeZoneStorage;

import static jakarta.persistence.GenerationType.IDENTITY;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "`type`",
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_category_id_name", columnNames = {
			"category_id", "name"
		})
	})
public class TypeEntity {

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
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
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

	public void setId(final Long id) {
		this.id = id;
	}

	public TypeEntity withId(final Long id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public TypeEntity withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public TypeEntity withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getEscalationEmail() {
		return escalationEmail;
	}

	public void setEscalationEmail(final String escalationEmail) {
		this.escalationEmail = escalationEmail;
	}

	public TypeEntity withEscalationEmail(final String escalationEmail) {
		this.escalationEmail = escalationEmail;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public TypeEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public TypeEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public CategoryEntity getCategoryEntity() {
		return categoryEntity;
	}

	public void setCategoryEntity(final CategoryEntity categoryEntity) {
		this.categoryEntity = categoryEntity;
	}

	public TypeEntity withCategoryEntity(final CategoryEntity categoryEntity) {
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
		final var categoryId = Optional.ofNullable(categoryEntity).map(CategoryEntity::getId).orElse(null);
		return Objects.hash(categoryId, created, displayName, escalationEmail, id, modified, name);
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
		final TypeEntity other = (TypeEntity) obj;

		final var thisCategoryId = Optional.ofNullable(categoryEntity).map(CategoryEntity::getId).orElse(null);
		final var otherCategoryId = Optional.ofNullable(other.categoryEntity).map(CategoryEntity::getId).orElse(null);
		return Objects.equals(thisCategoryId, otherCategoryId) && Objects.equals(created, other.created) && Objects.equals(displayName, other.displayName) && Objects.equals(escalationEmail, other.escalationEmail) && Objects.equals(id, other.id)
			&& Objects.equals(modified, other.modified) && Objects.equals(name, other.name);
	}

	@Override
	public String toString() {
		final var categoryId = Optional.ofNullable(categoryEntity).map(CategoryEntity::getId).orElse(null);
		return "TypeEntity [id=" + id + ", name=" + name + ", displayName=" + displayName + ", escalationEmail=" + escalationEmail + ", created=" + created + ", modified="
			+ modified + ", categoryEntity.id=" + categoryId + "]";
	}
}
