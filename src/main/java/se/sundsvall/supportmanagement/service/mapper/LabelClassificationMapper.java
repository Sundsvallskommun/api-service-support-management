package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import java.util.Map;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.api.model.metadata.LabelClassification;
import se.sundsvall.supportmanagement.integration.db.model.LabelClassificationEntity;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.anyNull;

public final class LabelClassificationMapper {

	private LabelClassificationMapper() {}

	public static LabelClassification toLabelClassification(final LabelClassificationEntity entity) {
		return ofNullable(entity)
			.map(e -> LabelClassification.create()
				.withId(e.getId())
				.withClassification(e.getClassification())
				.withDisplayName(e.getDisplayName())
				.withCreated(e.getCreated())
				.withModified(e.getModified()))
			.orElse(null);
	}

	public static LabelClassificationEntity toLabelClassificationEntity(final String namespace, final String municipalityId, final LabelClassification labelClassification) {
		if (anyNull(namespace, municipalityId, labelClassification)) {
			return null;
		}

		return LabelClassificationEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withClassification(labelClassification.getClassification())
			.withDisplayName(labelClassification.getDisplayName());
	}

	/**
	 * Updates the display name only. The classification is the key the labels are matched on, and is therefore never
	 * changed by an update. A display name left out of the patch is left untouched.
	 */
	public static LabelClassificationEntity updateLabelClassificationEntity(final LabelClassificationEntity entity, final LabelClassification labelClassification) {
		if (isNull(labelClassification)) {
			return entity;
		}

		ofNullable(labelClassification.getDisplayName()).ifPresent(entity::setDisplayName);
		return entity;
	}

	/**
	 * Sets the display name of the classification on every label in the tree. A label whose classification has no display
	 * name registered is left with none.
	 */
	public static void applyClassificationDisplayNames(final List<Label> labels, final Map<String, String> displayNames) {
		ofNullable(labels).ifPresent(list -> list.forEach(label -> {
			label.setClassificationDisplayName(lookup(displayNames, label.getClassification()));
			applyClassificationDisplayNames(label.getLabels(), displayNames);
		}));
	}

	/**
	 * Sets the display name of the classification on the labels of the errand. An errand the user may not see the labels
	 * of carries none, and is left as is.
	 */
	public static Errand applyClassificationDisplayNames(final Errand errand, final Map<String, String> displayNames) {
		ofNullable(errand)
			.map(Errand::getLabels)
			.ifPresent(labels -> labels.forEach(label -> label.setClassificationDisplayName(lookup(displayNames, label.getClassification()))));
		return errand;
	}

	private static String lookup(final Map<String, String> displayNames, final String classification) {
		return isNull(displayNames) || isNull(classification) ? null : displayNames.get(classification);
	}
}
