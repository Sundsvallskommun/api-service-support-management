package se.sundsvall.supportmanagement.integration.db.model;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "type_tag", uniqueConstraints = {
	@UniqueConstraint(name = "uq_category_tag_id_name", columnNames = { "category_tag_id", "name" })
})
public class TypeTagEntity implements Serializable {
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
	@JoinColumn(name = "category_tag_id", nullable = false, foreignKey = @ForeignKey(name = "fk_category_tag_id"))
	private CategoryTagEntity categoryTagEntity;

	public static TypeTagEntity create() {
		return new TypeTagEntity();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public TypeTagEntity withId(Long id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TypeTagEntity withName(String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public TypeTagEntity withDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getEscalationEmail() {
		return escalationEmail;
	}

	public void setEscalationEmail(String escalationEmail) {
		this.escalationEmail = escalationEmail;
	}

	public TypeTagEntity withEscalationEmail(String escalationEmail) {
		this.escalationEmail = escalationEmail;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public TypeTagEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public TypeTagEntity withModified(OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public CategoryTagEntity getCategoryTagEntity() {
		return categoryTagEntity;
	}

	public void setCategoryTagEntity(CategoryTagEntity categoryTagEntity) {
		this.categoryTagEntity = categoryTagEntity;
	}

	public TypeTagEntity withCategoryTagEntity(CategoryTagEntity categoryTagEntity) {
		this.categoryTagEntity = categoryTagEntity;
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
		var categoryTagId = Optional.ofNullable(categoryTagEntity).map(CategoryTagEntity::getId).orElse(null);
		return Objects.hash(categoryTagId, created, displayName, escalationEmail, id, modified, name);
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
		TypeTagEntity other = (TypeTagEntity) obj;

		var thisCategoryTagId = Optional.ofNullable(categoryTagEntity).map(CategoryTagEntity::getId).orElse(null);
		var otherCategoryTagId = Optional.ofNullable(other.categoryTagEntity).map(CategoryTagEntity::getId).orElse(null);
		return Objects.equals(thisCategoryTagId, otherCategoryTagId) && Objects.equals(created, other.created) && Objects.equals(displayName, other.displayName) && Objects.equals(escalationEmail, other.escalationEmail) && id == other.id
			&& Objects.equals(modified, other.modified) && Objects.equals(name, other.name);
	}

	@Override
	public String toString() {
		var categoryTagId = Optional.ofNullable(categoryTagEntity).map(CategoryTagEntity::getId).orElse(null);
		StringBuilder builder = new StringBuilder();
		builder.append("TypeTagEntity [id=").append(id).append(", name=").append(name).append(", displayName=").append(displayName).append(", escalationEmail=").append(escalationEmail).append(", created=").append(created).append(", modified=")
			.append(modified).append(", categoryTagEntity.id=").append(categoryTagId).append("]");
		return builder.toString();
	}

}
