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
 * A JSON parameter of an investigation, held apart from the JSON parameters of the errand. See
 * {@link AbstractArtefactJsonParameterEntity} for how it is held.
 */
@Entity
@Table(name = "investigation_json_parameter",
	uniqueConstraints = @UniqueConstraint(name = "uq_investigation_json_parameter_investigation_id_key", columnNames = {
		"investigation_id", "parameter_key"
	}))
public class InvestigationJsonParameterEntity extends AbstractArtefactJsonParameterEntity<InvestigationJsonParameterEntity> {

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "investigation_id", nullable = false, foreignKey = @ForeignKey(name = "fk_investigation_json_parameter_investigation_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private InvestigationEntity investigationEntity;

	public static InvestigationJsonParameterEntity create() {
		return new InvestigationJsonParameterEntity();
	}

	public InvestigationEntity getInvestigationEntity() {
		return investigationEntity;
	}

	public void setInvestigationEntity(final InvestigationEntity investigationEntity) {
		this.investigationEntity = investigationEntity;
	}

	public InvestigationJsonParameterEntity withInvestigationEntity(final InvestigationEntity investigationEntity) {
		this.investigationEntity = investigationEntity;
		return this;
	}

	@Override
	public String toString() {
		return "InvestigationJsonParameterEntity{" + super.toString() +
			", investigationEntity=" + (investigationEntity != null ? investigationEntity.getId() : "null") +
			'}';
	}
}
