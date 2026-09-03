package se.sundsvall.supportmanagement.service.mapper;

import java.util.ArrayList;
import java.util.List;
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MeasureEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.Accept;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

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
		if (measure.hasField(Measure.Field.RESPONSIBLE_USER)) {
			entity.setResponsibleUser(measure.getResponsibleUser());
		}
		if (measure.hasField(Measure.Field.TYPE)) {
			entity.setType(measure.getType());
		}
		if (measure.hasField(Measure.Field.PLANNED_START)) {
			entity.setPlannedStart(measure.getPlannedStart());
		}
		if (measure.hasField(Measure.Field.PLANNED_COMPLETE)) {
			entity.setPlannedComplete(measure.getPlannedComplete());
		}
		if (measure.hasField(Measure.Field.EXECUTED)) {
			entity.setExecuted(measure.getExecuted());
		}

		if (measure.hasField(Measure.Field.GOAL)) {
			entity.setGoal(measure.getGoal());
		}
		if (measure.hasField(Measure.Field.DESCRIPTION)) {
			entity.setDescription(measure.getDescription());
		}
		if (measure.hasField(Measure.Field.ACCEPT)) {
			entity.setAccept(ofNullable(measure.getAccept()).map(Accept::valueOf).orElse(null));
		}
		if (measure.hasField(Measure.Field.ACCEPT_MOTIVATION)) {
			entity.setAcceptMotivation(measure.getAcceptMotivation());
		}
		if (measure.hasField(Measure.Field.REWORK_GOAL)) {
			entity.setReworkGoal(measure.getReworkGoal());
		}
		if (measure.hasField(Measure.Field.REWORK_DESCRIPTION)) {
			entity.setReworkDescription(measure.getReworkDescription());
		}
		return entity;
	}

	/**
	 * Merges sent in measures into the errand, matching on the id each measure carries and updating those in place.
	 * <p>
	 * Replacing the list wholesale would work, but the measures of an errand are addressable in their own right - a
	 * created measure hands out a Location of its id, and clients hold on to it. Since measures are part of the errand
	 * payload, the ordinary read, change a field, patch it back round trip carries them along, and a wholesale replace
	 * would delete and reinsert every one of them under a fresh id, breaking every id previously handed out and resetting
	 * created. Matching on the id keeps them stable, the same way parameters are merged on their key.
	 * <p>
	 * A measure without an id is new. So is one carrying an id this errand does not hold, which is what keeps a measure id
	 * belonging to another errand from ever reaching across to it. Anything the request leaves out is removed, which is
	 * what makes a patch able to delete a measure.
	 *
	 * @param entity   errand to merge into
	 * @param measures measures replacing the existing ones
	 */
	public static void mergeMeasures(final ErrandEntity entity, final List<Measure> measures) {
		if (entity.getMeasures() == null) {
			entity.setMeasures(new ArrayList<>());
		}
		final var existing = entity.getMeasures();
		final var existingIds = existing.stream()
			.map(MeasureEntity::getId)
			.collect(toSet());
		final var incomingById = ofNullable(measures).orElse(emptyList()).stream()
			.filter(measure -> nonNull(measure.getId()))
			.collect(toMap(Measure::getId, identity(), (first, _) -> first));

		existing.removeIf(measure -> !incomingById.containsKey(measure.getId()));
		existing.forEach(measure -> updateMeasureEntity(measure, incomingById.get(measure.getId())));

		ofNullable(measures).orElse(emptyList()).stream()
			.filter(measure -> isNull(measure.getId()) || !existingIds.contains(measure.getId()))
			.map(measure -> toMeasureEntity(measure, entity))
			.forEach(existing::add);
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
