package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UuidGenerator;

import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.FetchType.LAZY;

/**
 * A parameter of an investigation section: a key with a list of values, held as unstructured metadata of the section.
 */
@Entity
@Table(name = "investigation_section_parameter",
	indexes = @Index(name = "idx_investigation_section_parameter_section_id", columnList = "investigation_section_id"))
public class InvestigationSectionParameterEntity implements ArtefactParameter {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "investigation_section_id", nullable = false, foreignKey = @ForeignKey(name = "fk_investigation_section_parameter_section_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private InvestigationSectionEntity investigationSectionEntity;

	@Column(name = "parameters_key", nullable = false)
	private String key;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "parameter_group")
	private String parameterGroup;

	@ElementCollection(fetch = EAGER)
	@CollectionTable(
		name = "investigation_section_parameter_values",
		joinColumns = @JoinColumn(name = "investigation_section_parameter_id",
			foreignKey = @ForeignKey(name = "fk_investigation_section_parameter_values_parameter_id")))
	@OnDelete(action = OnDeleteAction.CASCADE)
	@OrderColumn(name = "value_order", nullable = false, columnDefinition = "integer default 0")
	@Column(name = "value", length = 3000)
	private List<String> values;

	public static InvestigationSectionParameterEntity create() {
		return new InvestigationSectionParameterEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public InvestigationSectionParameterEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public InvestigationSectionEntity getInvestigationSectionEntity() {
		return investigationSectionEntity;
	}

	public void setInvestigationSectionEntity(final InvestigationSectionEntity investigationSectionEntity) {
		this.investigationSectionEntity = investigationSectionEntity;
	}

	public InvestigationSectionParameterEntity withInvestigationSectionEntity(final InvestigationSectionEntity investigationSectionEntity) {
		this.investigationSectionEntity = investigationSectionEntity;
		return this;
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public InvestigationSectionParameterEntity withKey(final String key) {
		this.key = key;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public InvestigationSectionParameterEntity withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getParameterGroup() {
		return parameterGroup;
	}

	public void setParameterGroup(final String parameterGroup) {
		this.parameterGroup = parameterGroup;
	}

	public InvestigationSectionParameterEntity withParameterGroup(final String parameterGroup) {
		this.parameterGroup = parameterGroup;
		return this;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(final List<String> values) {
		this.values = values;
	}

	public InvestigationSectionParameterEntity withValues(final List<String> values) {
		this.values = values;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof final InvestigationSectionParameterEntity that)) {
			return false;
		}
		return Objects.equals(id, that.id)
			&& Objects.equals(key, that.key)
			&& Objects.equals(displayName, that.displayName)
			&& Objects.equals(parameterGroup, that.parameterGroup)
			&& Objects.equals(values, that.values);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, key, displayName, parameterGroup, values);
	}

	@Override
	public String toString() {
		return "InvestigationSectionParameterEntity{" +
			"id='" + id + '\'' +
			", investigationSectionEntity=" + (investigationSectionEntity != null ? investigationSectionEntity.getId() : "null") +
			", key='" + key + '\'' +
			", displayName='" + displayName + '\'' +
			", parameterGroup='" + parameterGroup + '\'' +
			", values=" + values +
			'}';
	}
}
