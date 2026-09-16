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
 * A JSON parameter of a measure. See {@link AbstractArtefactJsonParameterEntity} for why it is not one of the errand.
 */
@Entity
@Table(name = "measure_json_parameter",
	uniqueConstraints = @UniqueConstraint(name = "uq_measure_json_parameter_measure_id_key", columnNames = {
		"measure_id", "parameter_key"
	}))
public class MeasureJsonParameterEntity extends AbstractArtefactJsonParameterEntity<MeasureJsonParameterEntity> {

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "measure_id", nullable = false, foreignKey = @ForeignKey(name = "fk_measure_json_parameter_measure_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private MeasureEntity measureEntity;

	public static MeasureJsonParameterEntity create() {
		return new MeasureJsonParameterEntity();
	}

	public MeasureEntity getMeasureEntity() {
		return measureEntity;
	}

	public void setMeasureEntity(final MeasureEntity measureEntity) {
		this.measureEntity = measureEntity;
	}

	public MeasureJsonParameterEntity withMeasureEntity(final MeasureEntity measureEntity) {
		this.measureEntity = measureEntity;
		return this;
	}

	@Override
	public String toString() {
		return "MeasureJsonParameterEntity{" + super.toString() +
			", measureEntity=" + (measureEntity != null ? measureEntity.getId() : "null") +
			'}';
	}
}
