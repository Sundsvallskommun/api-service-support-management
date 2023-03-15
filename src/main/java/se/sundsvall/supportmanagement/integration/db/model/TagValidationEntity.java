package se.sundsvall.supportmanagement.integration.db.model;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static javax.persistence.EnumType.STRING;
import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

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

import se.sundsvall.supportmanagement.integration.db.model.enums.TagType;

@Entity
@Table(name = "tag_validation", indexes = {
	@Index(name = "idx_namespace_municipality_id_type", columnList = "namespace, municipality_id, type")
}, uniqueConstraints = {
	@UniqueConstraint(name = "uq_namespace_municipality_id_type", columnNames = { "namespace", "municipality_id", "type" })
})
public class TagValidationEntity implements Serializable {
	private static final long serialVersionUID = -6163643004292601360L;

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "`type`", nullable = false)
	@Enumerated(STRING)
	private TagType type;

	@Column(name = "validated")
	private boolean validated;

	@Column(name = "municipality_id", nullable = false)
	private String municipalityId;

	@Column(name = "namespace", nullable = false)
	private String namespace;

	@Column(name = "created")
	private OffsetDateTime created;

	@Column(name = "modified")
	private OffsetDateTime modified;

	public static TagValidationEntity create() {
		return new TagValidationEntity();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public TagValidationEntity withId(Long id) {
		this.id = id;
		return this;
	}

	public TagType getType() {
		return this.type;
	}

	public void setType(TagType type) {
		this.type = type;
	}

	public TagValidationEntity withType(TagType type) {
		this.type = type;
		return this;
	}

	public boolean isValidated() {
		return this.validated;
	}

	public void setValidated(boolean validated) {
		this.validated = validated;
	}

	public TagValidationEntity withValidated(boolean validated) {
		this.validated = validated;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public TagValidationEntity withMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public TagValidationEntity withNamespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public TagValidationEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public TagValidationEntity withModified(OffsetDateTime modified) {
		this.modified = modified;
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
		return Objects.hash(created, id, modified, municipalityId, namespace, type, validated);
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
		TagValidationEntity other = (TagValidationEntity) obj;
		return Objects.equals(created, other.created) && Objects.equals(id, other.id) && Objects.equals(modified, other.modified) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(namespace, other.namespace)
			&& type == other.type && validated == other.validated;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TagValidationEntity [id=").append(id).append(", type=").append(type).append(", validated=").append(validated).append(", municipalityId=").append(municipalityId).append(", namespace=").append(namespace).append(", created=")
			.append(created).append(", modified=").append(modified).append("]");
		return builder.toString();
	}
}
