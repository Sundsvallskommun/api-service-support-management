package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "phase",
	indexes = {
		@Index(name = "idx_phase_municipality_id_namespace", columnList = "municipality_id, namespace")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_phase_namespace_municipality_id_name", columnNames = {
			"namespace", "municipality_id", "name"
		})
	})
public class PhaseEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "description")
	private String description;

	@Column(name = "phase_order")
	private Integer phaseOrder;

	@Column(name = "deprecated", nullable = false)
	private boolean deprecated;

	@ElementCollection(fetch = EAGER)
	@CollectionTable(
		name = "phase_allowed_status",
		joinColumns = @JoinColumn(name = "phase_id", foreignKey = @ForeignKey(name = "fk_phase_allowed_status_phase_id")),
		indexes = @Index(name = "idx_phase_allowed_status_phase_id", columnList = "phase_id"))
	@OrderColumn(name = "status_order", nullable = false, columnDefinition = "integer default 0")
	@Column(name = "status", length = 64)
	private List<String> allowedStatuses = new ArrayList<>();

	@OneToMany(mappedBy = "phaseEntity", cascade = ALL, orphanRemoval = true, fetch = EAGER)
	private List<PhaseTransitionEntity> transitions = new ArrayList<>();

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	public static PhaseEntity create() {
		return new PhaseEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public PhaseEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public PhaseEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public PhaseEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public PhaseEntity withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public PhaseEntity withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public PhaseEntity withDescription(final String description) {
		this.description = description;
		return this;
	}

	public Integer getPhaseOrder() {
		return phaseOrder;
	}

	public void setPhaseOrder(final Integer phaseOrder) {
		this.phaseOrder = phaseOrder;
	}

	public PhaseEntity withPhaseOrder(final Integer phaseOrder) {
		this.phaseOrder = phaseOrder;
		return this;
	}

	public boolean isDeprecated() {
		return deprecated;
	}

	public void setDeprecated(final boolean deprecated) {
		this.deprecated = deprecated;
	}

	public PhaseEntity withDeprecated(final boolean deprecated) {
		this.deprecated = deprecated;
		return this;
	}

	public List<String> getAllowedStatuses() {
		return allowedStatuses;
	}

	public void setAllowedStatuses(final List<String> allowedStatuses) {
		this.allowedStatuses = allowedStatuses;
	}

	public PhaseEntity withAllowedStatuses(final List<String> allowedStatuses) {
		this.allowedStatuses = allowedStatuses;
		return this;
	}

	public List<PhaseTransitionEntity> getTransitions() {
		return transitions;
	}

	public void setTransitions(final List<PhaseTransitionEntity> transitions) {
		this.transitions = transitions;
	}

	public PhaseEntity withTransitions(final List<PhaseTransitionEntity> transitions) {
		this.transitions = transitions;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public PhaseEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public PhaseEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
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
		return Objects.hash(id, municipalityId, namespace, name, displayName, description, phaseOrder, deprecated, allowedStatuses, created, modified);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final PhaseEntity other)) {
			return false;
		}
		return Objects.equals(id, other.id) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(namespace, other.namespace) && Objects.equals(name, other.name)
			&& Objects.equals(displayName, other.displayName) && Objects.equals(description, other.description) && Objects.equals(phaseOrder, other.phaseOrder) && deprecated == other.deprecated
			&& Objects.equals(allowedStatuses, other.allowedStatuses) && Objects.equals(created, other.created) && Objects.equals(modified, other.modified);
	}

	@Override
	public String toString() {
		return "PhaseEntity{" +
			"id='" + id + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", name='" + name + '\'' +
			", displayName='" + displayName + '\'' +
			", description='" + description + '\'' +
			", phaseOrder=" + phaseOrder +
			", deprecated=" + deprecated +
			", allowedStatuses=" + allowedStatuses +
			", transitions=" + transitions +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
