package se.sundsvall.supportmanagement.integration.db.model;

import static jakarta.persistence.GenerationType.IDENTITY;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

import org.hibernate.annotations.TimeZoneStorage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "label",
	indexes = {
		@Index(name = "idx_namespace_municipality_id", columnList = "namespace, municipality_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_namespace_municipality_id", columnNames = {
			"namespace", "municipality_id"
		})
	})
public class LabelEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "municipality_id", nullable = false)
	private String municipalityId;

	@Column(name = "namespace", nullable = false)
	private String namespace;

	@Column(name = "json_structure", nullable = false, columnDefinition = "json")
	private String jsonStructure;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	public static LabelEntity create() {
		return new LabelEntity();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LabelEntity withId(Long id) {
		setId(id);
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public LabelEntity withMunicipalityId(String municipalityId) {
		setMunicipalityId(municipalityId);
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public LabelEntity withNamespace(String namespace) {
		setNamespace(namespace);
		return this;
	}

	public String getJsonStructure() {
		return jsonStructure;
	}

	public void setJsonStructure(String jsonStructure) {
		this.jsonStructure = jsonStructure;
	}

	public LabelEntity withJsonStructure(String jsonStructure) {
		setJsonStructure(jsonStructure);
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public LabelEntity withCreated(OffsetDateTime created) {
		setCreated(created);
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public LabelEntity withModified(OffsetDateTime modified) {
		setModified(modified);
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
		return Objects.hash(created, id, jsonStructure, modified, municipalityId, namespace);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof LabelEntity)) {
			return false;
		}
		LabelEntity other = (LabelEntity) obj;
		return Objects.equals(created, other.created) && Objects.equals(id, other.id) && Objects.equals(jsonStructure, other.jsonStructure) && Objects.equals(modified, other.modified) && Objects.equals(municipalityId, other.municipalityId) &&
			Objects.equals(namespace, other.namespace);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LabelEntity [id=").append(id).append(", municipalityId=").append(municipalityId).append(", namespace=").append(namespace).append(", jsonStructure=").append(jsonStructure).append(", created=").append(created).append(
			", modified=")
			.append(modified).append("]");
		return builder.toString();
	}
}
