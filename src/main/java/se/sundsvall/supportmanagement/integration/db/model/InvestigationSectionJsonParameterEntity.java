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
 * A JSON parameter of an investigation section, held apart from those of the errand. See
 * {@link AbstractArtefactJsonParameterEntity}.
 */
@Entity
@Table(name = "investigation_section_json_parameter",
	uniqueConstraints = @UniqueConstraint(name = "uq_investigation_section_json_parameter_section_id_key", columnNames = {
		"investigation_section_id", "parameter_key"
	}))
public class InvestigationSectionJsonParameterEntity extends AbstractArtefactJsonParameterEntity<InvestigationSectionJsonParameterEntity> {

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "investigation_section_id", nullable = false, foreignKey = @ForeignKey(name = "fk_investigation_section_json_parameter_section_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private InvestigationSectionEntity investigationSectionEntity;

	public static InvestigationSectionJsonParameterEntity create() {
		return new InvestigationSectionJsonParameterEntity();
	}

	public InvestigationSectionEntity getInvestigationSectionEntity() {
		return investigationSectionEntity;
	}

	public void setInvestigationSectionEntity(final InvestigationSectionEntity investigationSectionEntity) {
		this.investigationSectionEntity = investigationSectionEntity;
	}

	public InvestigationSectionJsonParameterEntity withInvestigationSectionEntity(final InvestigationSectionEntity investigationSectionEntity) {
		this.investigationSectionEntity = investigationSectionEntity;
		return this;
	}

	@Override
	public String toString() {
		return "InvestigationSectionJsonParameterEntity{" + super.toString() +
			", investigationSectionEntity=" + (investigationSectionEntity != null ? investigationSectionEntity.getId() : "null") +
			'}';
	}
}
