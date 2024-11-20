package se.sundsvall.supportmanagement.integration.db.model;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "stakeholder_parameter")
public class StakeholderParameterEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "stakeholder_id", nullable = false, foreignKey = @ForeignKey(name = "fk_stakeholder_parameter_stakeholder_id"))
	private StakeholderEntity stakeholderEntity;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "parameters_key")
	private String key;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
		name = "stakeholder_parameter_values",
		joinColumns = @JoinColumn(name = "stakeholder_parameter_id",
			foreignKey = @ForeignKey(name = "fk_stakeholder_parameter_values_stakeholder_parameter_id")))
	@Column(name = "value")
	private List<String> values;

	public static StakeholderParameterEntity create() {
		return new StakeholderParameterEntity();
	}

	public long getId() {
		return id;
	}

	public void setId(final long id) {
		this.id = id;
	}

	public StakeholderParameterEntity withId(long id) {
		this.id = id;
		return this;
	}

	public StakeholderEntity getStakeholderEntity() {
		return stakeholderEntity;
	}

	public void setStakeholderEntity(StakeholderEntity stakeholderEntity) {
		this.stakeholderEntity = stakeholderEntity;
	}

	public StakeholderParameterEntity withStakeholderEntity(StakeholderEntity stakeholderEntity) {
		this.stakeholderEntity = stakeholderEntity;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public StakeholderParameterEntity withDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public StakeholderParameterEntity withKey(String key) {
		this.key = key;
		return this;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(final List<String> values) {
		this.values = values;
	}

	public StakeholderParameterEntity withValues(final List<String> values) {
		this.values = values;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(displayName, id, key, stakeholderEntity, values);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final StakeholderParameterEntity other)) { return false; }
		return Objects.equals(displayName, other.displayName) && (id == other.id) && Objects.equals(key, other.key) && Objects.equals(stakeholderEntity, other.stakeholderEntity) && Objects.equals(values, other.values);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("StakeholderParameterEntity [id=").append(id).append(", stakeholderEntity.id=").append((stakeholderEntity != null ? stakeholderEntity.getId() : "null")).append(", displayName=").append(displayName).append(", key=").append(key)
			.append(", values=").append(values).append("]");
		return builder.toString();
	}
}
