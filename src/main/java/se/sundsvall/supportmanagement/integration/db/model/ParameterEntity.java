package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "parameter")
public class ParameterEntity {

	@Id
	@UuidGenerator
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "errand_id", nullable = false, foreignKey = @ForeignKey(name = "fk_parameter_errand_id"))
	private ErrandEntity errandEntity;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "parameter_group")
	private String parameterGroup;

	@Column(name = "parameters_key")
	private String key;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
		name = "parameter_values",
		joinColumns = @JoinColumn(name = "parameter_id",
			foreignKey = @ForeignKey(name = "fk_parameter_values_parameter_id")))
	@Column(name = "value")
	private List<String> values;

	public static ParameterEntity create() {
		return new ParameterEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ParameterEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public ErrandEntity getErrandEntity() {
		return errandEntity;
	}

	public void setErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
	}

	public ParameterEntity withErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public ParameterEntity withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getParameterGroup() {
		return parameterGroup;
	}

	public void setParameterGroup(final String parameterGroup) {
		this.parameterGroup = parameterGroup;
	}

	public ParameterEntity withParameterGroup(final String parameterGroup) {
		this.parameterGroup = parameterGroup;
		return this;
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public ParameterEntity withKey(final String key) {
		this.key = key;
		return this;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(final List<String> values) {
		this.values = values;
	}

	public ParameterEntity withValues(final List<String> values) {
		this.values = values;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(displayName, parameterGroup, id, key, values);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final ParameterEntity other)) {
			return false;
		}
		return Objects.equals(displayName, other.displayName) && Objects.equals(parameterGroup, other.parameterGroup) && Objects.equals(id, other.id) && Objects.equals(key, other.key) && Objects.equals(
			values,
			other.values);
	}

	@Override
	public String toString() {
		return "ParameterEntity{" +
			"id='" + id + '\'' +
			", displayName='" + displayName + '\'' +
			", parameterGroup='" + parameterGroup + '\'' +
			", key='" + key + '\'' +
			", values=" + values +
			'}';
	}
}
