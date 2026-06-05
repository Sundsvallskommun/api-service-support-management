package se.sundsvall.supportmanagement.service.mapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.handover.ClassificationMapping;
import se.sundsvall.supportmanagement.api.model.handover.ClassificationOption;
import se.sundsvall.supportmanagement.api.model.handover.ContactReasonMapping;
import se.sundsvall.supportmanagement.api.model.handover.DirectlyCopyable;
import se.sundsvall.supportmanagement.api.model.handover.LabelCandidate;
import se.sundsvall.supportmanagement.api.model.handover.LabelMapping;
import se.sundsvall.supportmanagement.api.model.handover.MetadataOption;
import se.sundsvall.supportmanagement.api.model.handover.NotCopyable;
import se.sundsvall.supportmanagement.api.model.handover.StatusMapping;
import se.sundsvall.supportmanagement.integration.db.model.CategoryEntity;
import se.sundsvall.supportmanagement.integration.db.model.ContactReasonEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatusEntity;
import se.sundsvall.supportmanagement.integration.db.model.TypeEntity;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

/**
 * Pure (side-effect free) mappers building the parts of a
 * {@link se.sundsvall.supportmanagement.api.model.handover.HandoverPreview}.
 *
 * <p>
 * This mapper projects the source errand values and the candidate lists read from the target namespace.
 * </p>
 */
public class HandoverPreviewMapper {

	private HandoverPreviewMapper() {}

	// =================================================================
	// Directly copyable fields
	// =================================================================

	public static DirectlyCopyable toDirectlyCopyable(final ErrandEntity errand) {
		return ofNullable(errand)
			.map(e -> DirectlyCopyable.create()
				.withTitle(e.getTitle())
				.withPriority(ofNullable(e.getPriority()).map(Priority::valueOf).orElse(null))
				.withStakeholderCount(size(e.getStakeholders()))
				.withExternalTagCount(size(e.getExternalTags()))
				.withAttachmentCount(size(e.getAttachments())))
			.orElse(null);
	}

	// =================================================================
	// Candidate lists (selectable options in the target namespace)
	// =================================================================

	public static List<MetadataOption> toStatusCandidates(final List<StatusEntity> statuses) {
		return ofNullable(statuses).orElse(emptyList()).stream()
			.map(status -> MetadataOption.create()
				.withName(status.getName())
				.withDisplayName(status.getDisplayName()))
			.toList();
	}

	/**
	 * Maps each target category name to the names of its selectable types. A {@link LinkedHashMap} keeps the category
	 * order from the repository (sorted by sort order); the merge function is only a defensive no-op since category names
	 * are unique within a namespace.
	 */
	public static Map<String, List<String>> toClassificationCandidates(final List<CategoryEntity> categories) {
		return ofNullable(categories).orElse(emptyList()).stream()
			.collect(toMap(
				CategoryEntity::getName,
				category -> ofNullable(category.getTypes()).orElse(emptyList()).stream()
					.map(TypeEntity::getName)
					.toList(),
				(first, _) -> first,
				LinkedHashMap::new));
	}

	public static List<LabelCandidate> toLabelCandidates(final List<MetadataLabelEntity> labels) {
		return ofNullable(labels).orElse(emptyList()).stream()
			.map(label -> LabelCandidate.create()
				.withId(label.getId())
				.withDisplayName(label.getDisplayName())
				.withResourcePath(label.getResourcePath()))
			.toList();
	}

	public static List<String> toContactReasonCandidates(final List<ContactReasonEntity> contactReasons) {
		return ofNullable(contactReasons).orElse(emptyList()).stream()
			.map(ContactReasonEntity::getReason)
			.toList();
	}

	// =================================================================
	// Mapping suggestions (source value + candidates, suggestions left null)
	// =================================================================

	public static StatusMapping toStatusMapping(final ErrandEntity errand, final String sourceDisplayName, final List<MetadataOption> candidates) {
		return StatusMapping.create()
			.withSource(ofNullable(errand.getStatus())
				.map(name -> MetadataOption.create().withName(name).withDisplayName(sourceDisplayName))
				.orElse(null))
			.withCandidates(ofNullable(candidates).orElse(emptyList()));
	}

	public static ClassificationMapping toClassificationMapping(final ErrandEntity errand, final Map<String, List<String>> candidates) {
		final var source = (errand.getCategory() == null && errand.getType() == null) ? null
			: ClassificationOption.create().withCategory(errand.getCategory()).withType(errand.getType());

		return ClassificationMapping.create()
			.withSource(source)
			.withCandidates(ofNullable(candidates).orElse(emptyMap()));
	}

	public static List<LabelMapping> toLabelMappings(final ErrandEntity errand, final List<LabelCandidate> candidates) {
		final var sharedCandidates = ofNullable(candidates).orElse(emptyList());

		return ofNullable(errand.getLabels()).orElse(emptyList()).stream()
			.map(label -> LabelMapping.create()
				.withSourceId(label.getMetadataLabelId())
				.withSourceDisplayName(ofNullable(label.getMetadataLabel()).map(MetadataLabelEntity::getDisplayName).orElse(null))
				.withSourceResourcePath(ofNullable(label.getMetadataLabel()).map(MetadataLabelEntity::getResourcePath).orElse(null))
				.withCandidates(sharedCandidates))
			.toList();
	}

	public static ContactReasonMapping toContactReasonMapping(final ErrandEntity errand, final List<String> candidates) {
		return ContactReasonMapping.create()
			.withSource(ofNullable(errand.getContactReason()).map(ContactReasonEntity::getReason).orElse(null))
			.withCandidates(ofNullable(candidates).orElse(emptyList()));
	}

	// =================================================================
	// Fields that can not be carried over to another namespace
	// =================================================================

	public static List<NotCopyable> toNotCopyable() {
		return List.of(
			NotCopyable.create().withField("phases").withReason("Phase history is source-specific"),
			NotCopyable.create().withField("activePhaseId").withReason("Phase IDs differ per namespace"));
	}

	private static int size(final List<?> list) {
		return ofNullable(list).map(List::size).orElse(0);
	}
}
