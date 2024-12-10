package se.sundsvall.supportmanagement.integration.db.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import se.sundsvall.supportmanagement.integration.db.model.enums.EntityType;

@Entity
@Table(name = "validation",
	indexes = {
		@Index(name = "idx_namespace_municipality_id_type", columnList = "namespace, municipality_id, type")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_namespace_municipality_id_type", columnNames = {
			"namespace", "municipality_id", "type"
		})
	})
public class ValidationEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "`type`", nullable = false, columnDefinition = "varchar(255)")
	@Enumerated(STRING)
	private EntityType type;

	@Column(name = "validated")
	private boolean validated;

	@Column(name = "municipality_id", nullable = false)
	private String municipalityId;

	@Column(name = "namespace", nullable = false)
	private String namespace;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	public static ValidationEntity create() {
		return new ValidationEntity();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public ValidationEntity withId(Long id) {
		this.id = id;
		return this;
	}

	public EntityType getType() {
		return this.type;
	}

	public void setType(EntityType type) {
		this.type = type;
	}

	public ValidationEntity withType(EntityType type) {
		this.type = type;
		return this;
	}

	public boolean isValidated() {
		return this.validated;
	}

	public void setValidated(boolean validated) {
		this.validated = validated;
	}

	public ValidationEntity withValidated(boolean validated) {
		this.validated = validated;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public ValidationEntity withMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public ValidationEntity withNamespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public ValidationEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public ValidationEntity withModified(OffsetDateTime modified) {
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
		final ValidationEntity other = (ValidationEntity) obj;
		return Objects.equals(created, other.created) && Objects.equals(id, other.id) && Objects.equals(modified, other.modified) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(namespace, other.namespace)
			&& (type == other.type) && (validated == other.validated);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ValidationEntity [id=").append(id).append(", type=").append(type).append(", validated=").append(validated).append(", municipalityId=").append(municipalityId).append(", namespace=").append(namespace).append(", created=")
			.append(created).append(", modified=").append(modified).append("]");
		return builder.toString();
	}
}
