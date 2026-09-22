package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import static jakarta.persistence.FetchType.LAZY;

/**
 * A JSON parameter of a decision, held as described in {@link AbstractArtefactJsonParameterEntity}.
 */
@Entity
@Table(name = "decision_json_parameter",
	uniqueConstraints = @UniqueConstraint(name = "uq_decision_json_parameter_decision_id_key", columnNames = {
		"decision_id", "parameter_key"
	}))
public class DecisionJsonParameterEntity extends AbstractArtefactJsonParameterEntity<DecisionJsonParameterEntity> {

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "decision_id", nullable = false, foreignKey = @ForeignKey(name = "fk_decision_json_parameter_decision_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private DecisionEntity decisionEntity;

	public static DecisionJsonParameterEntity create() {
		return new DecisionJsonParameterEntity();
	}

	public DecisionEntity getDecisionEntity() {
		return decisionEntity;
	}

	public void setDecisionEntity(final DecisionEntity decisionEntity) {
		this.decisionEntity = decisionEntity;
	}

	public DecisionJsonParameterEntity withDecisionEntity(final DecisionEntity decisionEntity) {
		this.decisionEntity = decisionEntity;
		return this;
	}

	@Override
	public String toString() {
		return "DecisionJsonParameterEntity{" + super.toString() +
			", decisionEntity=" + (decisionEntity != null ? decisionEntity.getId() : "null") +
			'}';
	}
}
