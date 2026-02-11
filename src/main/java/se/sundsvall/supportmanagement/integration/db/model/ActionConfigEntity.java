package se.sundsvall.supportmanagement.integration.db.model;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "action_config", indexes = {
	@Index(name = "idx_action_config_municipality_id_namespace", columnList = "municipality_id, namespace")
})
public class ActionConfigEntity {

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

	@Column(name = "active", nullable = false)
	private boolean active;

	@Column(name = "display_value")
	private String displayValue;

	@OneToMany(mappedBy = "actionConfigEntity", cascade = ALL, orphanRemoval = true, fetch = EAGER)
	private List<ActionConfigConditionEntity> conditions = new ArrayList<>();

	@OneToMany(mappedBy = "actionConfigEntity", cascade = ALL, orphanRemoval = true, fetch = EAGER)
	private List<ActionConfigParameterEntity> parameters = new ArrayList<>();

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	public static ActionConfigEntity create() {
		return new ActionConfigEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ActionConfigEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public ActionConfigEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public ActionConfigEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public ActionConfigEntity withName(final String name) {
		this.name = name;
		return this;
	}

	public boolean getActive() {
		return active;
	}

	public void setActive(final boolean active) {
		this.active = active;
	}

	public ActionConfigEntity withActive(final boolean active) {
		this.active = active;
		return this;
	}

	public String getDisplayValue() {
		return displayValue;
	}

	public void setDisplayValue(final String displayValue) {
		this.displayValue = displayValue;
	}

	public ActionConfigEntity withDisplayValue(final String displayValue) {
		this.displayValue = displayValue;
		return this;
	}

	public List<ActionConfigConditionEntity> getConditions() {
		return conditions;
	}

	public void setConditions(final List<ActionConfigConditionEntity> conditions) {
		this.conditions = conditions;
	}

	public ActionConfigEntity withConditions(final List<ActionConfigConditionEntity> conditions) {
		this.conditions = conditions;
		return this;
	}

	public List<ActionConfigParameterEntity> getParameters() {
		return parameters;
	}

	public void setParameters(final List<ActionConfigParameterEntity> parameters) {
		this.parameters = parameters;
	}

	public ActionConfigEntity withParameters(final List<ActionConfigParameterEntity> parameters) {
		this.parameters = parameters;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public ActionConfigEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public ActionConfigEntity withModified(final OffsetDateTime modified) {
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
		return Objects.hash(id, municipalityId, namespace, name, active, displayValue, conditions, parameters, created, modified);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final ActionConfigEntity other)) {
			return false;
		}
		return active == other.active && Objects.equals(id, other.id) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(namespace, other.namespace) && Objects.equals(name, other.name)
			&& Objects.equals(displayValue, other.displayValue) && Objects.equals(conditions, other.conditions) && Objects.equals(parameters, other.parameters) && Objects.equals(created, other.created)
			&& Objects.equals(modified, other.modified);
	}

	@Override
	public String toString() {
		return "ActionConfigEntity{" +
			"id='" + id + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", name='" + name + '\'' +
			", active=" + active +
			", displayValue='" + displayValue + '\'' +
			", conditions=" + conditions +
			", parameters=" + parameters +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
