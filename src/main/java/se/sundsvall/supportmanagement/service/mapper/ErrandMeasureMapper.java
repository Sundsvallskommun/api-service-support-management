package se.sundsvall.supportmanagement.service.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import se.sundsvall.supportmanagement.integration.db.model.DecisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MeasureEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.Accept;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.MeasureResult;
import se.sundsvall.supportmanagement.service.util.ArtefactJsonParameters;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus.ACTIVE;
import static se.sundsvall.supportmanagement.service.mapper.ArtefactAttachmentMapper.toArtefactAttachments;
import static se.sundsvall.supportmanagement.service.util.ArtefactJsonParameters.removeParameters;

public final class ErrandMeasureMapper {

	private ErrandMeasureMapper() {}

	/**
	 * The namespace and the municipality are taken from the errand rather than passed in, since a measure of an errand
	 * cannot belong anywhere else and a second source for them is a second thing that can disagree.
	 * <p>
	 * A measure with no status given is ACTIVE. The column has been not null since the shared shape arrived, and
	 * demanding the field would have broken every client creating a measure the way they always have.
	 */
	public static MeasureEntity toMeasureEntity(final Measure measure, final ErrandEntity errandEntity) {
		return MeasureEntity.create()
			.withErrandEntity(errandEntity)
			.withNamespace(errandEntity.getNamespace())
			.withMunicipalityId(errandEntity.getMunicipalityId())
			.withResponsibleUser(measure.getResponsibleUser())
			.withType(measure.getType())
			.withStatus(ofNullable(measure.getStatus()).map(ItemStatus::valueOf).orElse(ACTIVE))
			.withTitle(measure.getTitle())
			.withDueAt(measure.getDueAt())
			.withCompletedAt(measure.getCompletedAt())
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
			.withReworkDescription(measure.getReworkDescription())
			.withResult(ofNullable(measure.getResult()).map(MeasureResult::valueOf).orElse(null))
			.withResultText(measure.getResultText());
	}

	/**
	 * Applies the fields the patch carries. The provenance - which decision or statement the measure follows from - is
	 * not among them: it names another artefact of the errand, and is written on the measures resource where that
	 * artefact is looked up through the errand and cannot be one belonging to a different errand.
	 */
	public static MeasureEntity updateMeasureEntity(final MeasureEntity entity, final Measure measure) {
		ofNullable(measure.getResponsibleUser()).ifPresent(entity::setResponsibleUser);
		ofNullable(measure.getType()).ifPresent(entity::setType);
		ofNullable(measure.getStatus()).map(ItemStatus::valueOf).ifPresent(entity::setStatus);
		ofNullable(measure.getTitle()).ifPresent(entity::setTitle);
		ofNullable(measure.getDueAt()).ifPresent(entity::setDueAt);
		ofNullable(measure.getCompletedAt()).ifPresent(entity::setCompletedAt);
		ofNullable(measure.getResult()).map(MeasureResult::valueOf).ifPresent(entity::setResult);
		ofNullable(measure.getResultText()).ifPresent(entity::setResultText);
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

		// The measures this request drops take their own business content with them, exactly as deleting one through the
		// measure resource does. Their parameter rows belong to the errand, so nothing else would remove them - and a key
		// left behind by a measure that is gone still blocks the next measure asking for it.
		final var dropped = existing.stream()
			.filter(measure -> !incomingById.containsKey(measure.getId()))
			.map(MeasureEntity::getJsonParameterLinks)
			.map(ArtefactJsonParameters::ownedParameterIds)
			.flatMap(Set::stream)
			.collect(toSet());

		existing.removeIf(measure -> !incomingById.containsKey(measure.getId()));
		removeParameters(entity, dropped);

		existing.forEach(measure -> updateMeasureEntity(measure, incomingById.get(measure.getId())));

		ofNullable(measures).orElse(emptyList()).stream()
			.filter(measure -> isNull(measure.getId()) || !existingIds.contains(measure.getId()))
			.map(measure -> toMeasureEntity(measure, entity))
			.forEach(existing::add);
	}

	/**
	 * The measure as the measure resource serves it, attachments included.
	 */
	public static Measure toMeasure(final MeasureEntity entity) {
		return toMeasureWithoutAttachments(entity)
			.withAttachments(toArtefactAttachments(entity.getAttachments()));
	}

	/**
	 * The measure as the errand carries it.
	 * <p>
	 * The attachments are left out. They are read through the measure resource, under the access the measure grants, like
	 * the attachments of the other handling artefacts - and leaving them out spares a listing of errands a query per
	 * measure for a collection it would not show.
	 */
	public static Measure toMeasureWithoutAttachments(final MeasureEntity entity) {
		return Measure.create()
			.withId(entity.getId())
			.withResponsibleUser(entity.getResponsibleUser())
			.withType(entity.getType())
			.withStatus(ofNullable(entity.getStatus()).map(Enum::name).orElse(null))
			.withTitle(entity.getTitle())
			.withDueAt(entity.getDueAt())
			.withCompletedAt(entity.getCompletedAt())
			.withResult(ofNullable(entity.getResult()).map(Enum::name).orElse(null))
			.withResultText(entity.getResultText())
			.withDecisionId(ofNullable(entity.getDecisionEntity()).map(DecisionEntity::getId).orElse(null))
			.withStatementId(ofNullable(entity.getStatementEntity()).map(StatementEntity::getId).orElse(null))
			.withCreatedBy(entity.getCreatedBy())
			.withModifiedBy(entity.getModifiedBy())
			.withVersion(entity.getVersion())
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

	public static List<Measure> toMeasuresWithoutAttachments(final List<MeasureEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(ErrandMeasureMapper::toMeasureWithoutAttachments)
			.toList();
	}
}
