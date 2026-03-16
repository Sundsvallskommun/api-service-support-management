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
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "action_config_parameter")
public class ActionConfigParameterEntity implements ActionConfigKeyValues {

	@Id
	@UuidGenerator
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "action_config_id", nullable = false, foreignKey = @ForeignKey(name = "fk_action_config_parameter_action_config_id"))
	private ActionConfigEntity actionConfigEntity;

	@Column(name = "parameter_key")
	private String key;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
		name = "action_config_parameter_values",
		joinColumns = @JoinColumn(name = "action_config_parameter_id",
			foreignKey = @ForeignKey(name = "fk_action_config_parameter_values_parameter_id")))
	@OrderColumn(name = "value_order", nullable = false, columnDefinition = "integer default 0")
	@Column(name = "value", length = 2000)
	private List<String> values;

	public static ActionConfigParameterEntity create() {
		return new ActionConfigParameterEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ActionConfigParameterEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public ActionConfigEntity getActionConfigEntity() {
		return actionConfigEntity;
	}

	public void setActionConfigEntity(final ActionConfigEntity actionConfigEntity) {
		this.actionConfigEntity = actionConfigEntity;
	}

	public ActionConfigParameterEntity withActionConfigEntity(final ActionConfigEntity actionConfigEntity) {
		this.actionConfigEntity = actionConfigEntity;
		return this;
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public ActionConfigParameterEntity withKey(final String key) {
		this.key = key;
		return this;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(final List<String> values) {
		this.values = values;
	}

	public ActionConfigParameterEntity withValues(final List<String> values) {
		this.values = values;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, key, values);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final ActionConfigParameterEntity other)) {
			return false;
		}
		return Objects.equals(id, other.id) && Objects.equals(key, other.key) && Objects.equals(values, other.values);
	}

	@Override
	public String toString() {
		return "ActionConfigParameterEntity{" +
			"id='" + id + '\'' +
			", key='" + key + '\'' +
			", values=" + values +
			'}';
	}
}
