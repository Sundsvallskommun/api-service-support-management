package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MeasureEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.Accept;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public final class ErrandMeasureMapper {

	private ErrandMeasureMapper() {}

	public static MeasureEntity toMeasureEntity(final Measure measure, final ErrandEntity errandEntity) {
		return MeasureEntity.create()
			.withErrandEntity(errandEntity)
			.withResponsibleUser(measure.getResponsibleUser())
			.withType(measure.getType())
			.withPlannedStart(measure.getPlannedStart())
			.withPlannedComplete(measure.getPlannedComplete())
			.withExecuted(measure.getExecuted())
			.withAddedByUser(measure.getAddedByUser())
			.withAddedByRole(measure.getAddedByRole())
			.withGoal(measure.getGoal())
			.withDescription(measure.getDescription())
			.withAccept(ofNullable(measure.getAccept()).map(Accept::valueOf).orElse(null))
			.withAcceptMotivation(measure.getAcceptMotivation())
			.withReworkGoal(measure.getReworkGoal())
			.withReworkDescription(measure.getReworkDescription());
	}

	public static MeasureEntity updateMeasureEntity(final MeasureEntity entity, final Measure measure) {
		ofNullable(measure.getResponsibleUser()).ifPresent(entity::setResponsibleUser);
		ofNullable(measure.getType()).ifPresent(entity::setType);
		ofNullable(measure.getPlannedStart()).ifPresent(entity::setPlannedStart);
		ofNullable(measure.getPlannedComplete()).ifPresent(entity::setPlannedComplete);
		ofNullable(measure.getExecuted()).ifPresent(entity::setExecuted);
		ofNullable(measure.getAddedByUser()).ifPresent(entity::setAddedByUser);
		ofNullable(measure.getAddedByRole()).ifPresent(entity::setAddedByRole);
		ofNullable(measure.getGoal()).ifPresent(entity::setGoal);
		ofNullable(measure.getDescription()).ifPresent(entity::setDescription);
		ofNullable(measure.getAccept()).map(Accept::valueOf).ifPresent(entity::setAccept);
		ofNullable(measure.getAcceptMotivation()).ifPresent(entity::setAcceptMotivation);
		ofNullable(measure.getReworkGoal()).ifPresent(entity::setReworkGoal);
		ofNullable(measure.getReworkDescription()).ifPresent(entity::setReworkDescription);
		return entity;
	}

	public static Measure toMeasure(final MeasureEntity entity) {
		return Measure.create()
			.withId(entity.getId())
			.withResponsibleUser(entity.getResponsibleUser())
			.withType(entity.getType())
			.withPlannedStart(entity.getPlannedStart())
			.withPlannedComplete(entity.getPlannedComplete())
			.withExecuted(entity.getExecuted())
			.withAddedByUser(entity.getAddedByUser())
			.withAddedByRole(entity.getAddedByRole())
			.withGoal(entity.getGoal())
			.withDescription(entity.getDescription())
			.withAccept(ofNullable(entity.getAccept()).map(Enum::name).orElse(null))
			.withAcceptMotivation(entity.getAcceptMotivation())
			.withReworkGoal(entity.getReworkGoal())
			.withReworkDescription(entity.getReworkDescription())
			.withCreated(entity.getCreated())
			.withModified(entity.getModified());
	}

	public static List<Measure> toMeasures(final List<MeasureEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(ErrandMeasureMapper::toMeasure)
			.toList();
	}
}
