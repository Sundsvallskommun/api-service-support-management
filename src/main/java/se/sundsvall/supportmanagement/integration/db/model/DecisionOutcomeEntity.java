package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * The outcomes a decision may have, registered per namespace.
 * <p>
 * Nothing is seeded. The outcomes of administrative law - approval, partial approval, rejection, dismissal and
 * discontinuation - hold in most lines of business, but which of them a namespace uses, and what else it needs, is for
 * the namespace to say. The code decides nothing on the name of an outcome. The recommendation of an investigation is
 * held to the same outcomes as the decision it proposes.
 */
@Entity
@Table(name = "decision_outcome",
	indexes = {
		@Index(name = "idx_decision_outcome_namespace_municipality_id", columnList = "namespace, municipality_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_decision_outcome_namespace_municipality_id_name", columnNames = {
			"namespace", "municipality_id", "name"
		})
	})
public class DecisionOutcomeEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "sort_order")
	private Integer sortOrder;

	@Column(name = "deprecated", nullable = false)
	private boolean deprecated;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	@PrePersist
	void onCreate() {
		created = now(systemDefault()).truncatedTo(MILLIS);
	}

	@PreUpdate
	void onUpdate() {
		modified = now(systemDefault()).truncatedTo(MILLIS);
	}

	public static DecisionOutcomeEntity create() {
		return new DecisionOutcomeEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public DecisionOutcomeEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public DecisionOutcomeEntity withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public DecisionOutcomeEntity withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public DecisionOutcomeEntity withSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
		return this;
	}

	public boolean isDeprecated() {
		return deprecated;
	}

	public void setDeprecated(final boolean deprecated) {
		this.deprecated = deprecated;
	}

	public DecisionOutcomeEntity withDeprecated(final boolean deprecated) {
		this.deprecated = deprecated;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public DecisionOutcomeEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public DecisionOutcomeEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public DecisionOutcomeEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public DecisionOutcomeEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, displayName, sortOrder, deprecated, municipalityId, namespace, created, modified);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final DecisionOutcomeEntity other)) {
			return false;
		}
		return Objects.equals(id, other.id)
			&& Objects.equals(name, other.name)
			&& Objects.equals(displayName, other.displayName)
			&& Objects.equals(sortOrder, other.sortOrder)
			&& (deprecated == other.deprecated)
			&& Objects.equals(municipalityId, other.municipalityId)
			&& Objects.equals(namespace, other.namespace)
			&& Objects.equals(created, other.created)
			&& Objects.equals(modified, other.modified);
	}

	@Override
	public String toString() {
		return "DecisionOutcomeEntity{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", displayName='" + displayName + '\'' +
			", sortOrder=" + sortOrder +
			", deprecated=" + deprecated +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
