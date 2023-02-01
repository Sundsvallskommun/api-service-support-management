package se.sundsvall.supportmanagement.integration.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static javax.persistence.EnumType.STRING;
import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "tag", indexes = {
	@Index(name = "idx_tag_name", columnList = "name"),
	@Index(name = "idx_tag_type", columnList = "type")
}, uniqueConstraints = {
	@UniqueConstraint(name = "uq_tag_name", columnNames = { "name" })
})
public class TagEntity implements Serializable {

	private static final long serialVersionUID = -7630789042882416516L;

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private long id;

	@Column(name = "name")
	private String name;

	@Column(name = "type", columnDefinition = "ENUM('CATEGORY', 'STATUS', 'TYPE', 'CLIENT_ID')")
	@Enumerated(STRING)
	private TagType type;

	@Column(name = "created")
	private OffsetDateTime created;

	@Column(name = "updated")
	private OffsetDateTime updated;

	public static TagEntity create() {
		return new TagEntity();
	}

	@PrePersist
	void prePersist() {
		created = now(ZoneId.systemDefault()).truncatedTo(MILLIS);
	}

	@PreUpdate
	void preUpdate() {
		updated = now(ZoneId.systemDefault()).truncatedTo(MILLIS);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public TagEntity withId(long id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TagEntity withName(String name) {
		this.name = name;
		return this;
	}

	public TagType getType() {
		return type;
	}

	public void setType(TagType type) {
		this.type = type;
	}

	public TagEntity withType(TagType type) {
		this.type = type;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public TagEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(OffsetDateTime updated) {
		this.updated = updated;
	}

	public TagEntity withUpdated(OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, id, name, type, updated);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		var other = (TagEntity) obj;
		return Objects.equals(created, other.created) && id == other.id && Objects.equals(name, other.name) && type == other.type && Objects.equals(updated, other.updated);
	}

	@Override
	public String toString() {
		var builder = new StringBuilder();
		builder.append("TagEntity [id=").append(id).append(", name=").append(name).append(", type=").append(type).append(", created=").append(created).append(", updated=").append(updated).append("]");
		return builder.toString();
	}
}
