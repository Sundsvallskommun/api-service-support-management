package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;

import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.GenerationType.IDENTITY;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Objects.isNull;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "namespace_config", indexes = {
	@Index(name = "idx_namespace_municipality_id", columnList = "namespace, municipality_id"),
	@Index(name = "idx_municipality_id", columnList = "municipality_id")
}, uniqueConstraints = {
	@UniqueConstraint(name = "uq_namespace_municipality_id", columnNames = {
		"namespace", "municipality_id"
	})
})
public class NamespaceConfigEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@ElementCollection(fetch = EAGER)
	@CollectionTable(name = "namespace_config_value", indexes = {
		@Index(name = "idx_namespace_config_value_namespace_config_id_key", columnList = "namespace_config_id, `key`")
	}, uniqueConstraints = {
		@UniqueConstraint(name = "uk_namespace_config_id_key_value", columnNames = {
			"namespace_config_id", "`key`", "`value`"
		})
	}, joinColumns = @JoinColumn(name = "namespace_config_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_namespace_config_value_namespace_config")))
	private List<NamespaceConfigValueEmbeddable> values = new ArrayList<>();

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	public static NamespaceConfigEntity create() {
		return new NamespaceConfigEntity();
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public NamespaceConfigEntity withId(final Long id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public NamespaceConfigEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public NamespaceConfigEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public NamespaceConfigEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public NamespaceConfigEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public List<NamespaceConfigValueEmbeddable> getValues() {
		return values;
	}

	public void setValues(List<NamespaceConfigValueEmbeddable> values) {
		this.values = values;
	}

	public NamespaceConfigEntity withValues(List<NamespaceConfigValueEmbeddable> values) {
		this.values = values;
		return this;
	}

	public NamespaceConfigEntity withValue(NamespaceConfigValueEmbeddable value) {
		if (isNull(this.values)) {
			this.values = new ArrayList<>();
		}

		this.values.add(value);
		return this;
	}

	@PrePersist
	void onCreate() {
		created = now(systemDefault()).truncatedTo(MILLIS);
	}

	@PreUpdate
	void onUpdate() {
		modified = now(systemDefault()).truncatedTo(MILLIS);
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, id, modified, municipalityId, namespace, values);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final NamespaceConfigEntity other)) { return false; }
		return Objects.equals(created, other.created) && Objects.equals(id, other.id) && Objects.equals(modified, other.modified) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(namespace, other.namespace) && Objects.equals(
			values, other.values);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("NamespaceConfigEntity [id=").append(id).append(", municipalityId=").append(municipalityId).append(", namespace=").append(namespace).append(", values=").append(values).append(", created=").append(created).append(", modified=")
			.append(modified).append("]");
		return builder.toString();
	}
}
