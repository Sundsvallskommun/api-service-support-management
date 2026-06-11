package se.sundsvall.supportmanagement.service.mapper;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.handover.ClassificationMapping;
import se.sundsvall.supportmanagement.api.model.handover.ClassificationOption;
import se.sundsvall.supportmanagement.api.model.handover.ContactReasonMapping;
import se.sundsvall.supportmanagement.api.model.handover.DirectlyCopyable;
import se.sundsvall.supportmanagement.api.model.handover.LabelCandidate;
import se.sundsvall.supportmanagement.api.model.handover.LabelMapping;
import se.sundsvall.supportmanagement.api.model.handover.LabelMappingGroup;
import se.sundsvall.supportmanagement.api.model.handover.MatchReason;
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
import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static se.sundsvall.supportmanagement.api.model.handover.MatchReason.DISPLAY_NAME_EXACT;
import static se.sundsvall.supportmanagement.api.model.handover.MatchReason.NAME_EXACT;
import static se.sundsvall.supportmanagement.api.model.handover.MatchReason.RESOURCE_PATH_MATCH;

/**
 * Pure (side-effect free) mappers building the parts of a
 * {@link se.sundsvall.supportmanagement.api.model.handover.HandoverPreview}.
 *
 * <p>
 * This mapper projects the source errand values and the candidate lists read from the target namespace.
 * </p>
 */
public class HandoverPreviewMapper {

	// Mirrors MetadataLabelEntity#RESOURCE_PATH_SEPARATOR: resource paths are built as "root/child/grandchild" (no leading
	// separator), so ancestor paths are obtained by dropping the deepest segment.
	private static final String RESOURCE_PATH_SEPARATOR = "/";

	// Orders resource paths hierarchically: segment by segment and case-insensitively, so a parent path always precedes its
	// children and a child never sorts after an unrelated sibling (a plain lexicographic sort would allow that, since the
	// separator '/' interleaves with other characters). Paths that are unset sort last.
	private static final Comparator<String> RESOURCE_PATH_COMPARATOR = nullsLast(HandoverPreviewMapper::compareResourcePathsHierarchically);

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
	//
	// Deprecated target metadata is excluded everywhere: a handover creates a new errand in the target namespace, so a
	// deprecated status/category/type/label/contactReason is not a valid destination and must neither be offered as a
	// candidate nor auto-suggested.

	public static List<MetadataOption> toStatusCandidates(final List<StatusEntity> statuses) {
		return ofNullable(statuses).orElse(emptyList()).stream()
			.filter(not(StatusEntity::isDeprecated))
			.map(status -> MetadataOption.create()
				.withName(status.getName())
				.withDisplayName(status.getDisplayName()))
			.toList();
	}

	/**
	 * Maps each non-deprecated target category name to the names of its non-deprecated types. A {@link LinkedHashMap}
	 * keeps the category order from the repository (sorted by sort order); the merge function is only a defensive no-op
	 * since category names are unique within a namespace.
	 */
	public static Map<String, List<String>> toClassificationCandidates(final List<CategoryEntity> categories) {
		return ofNullable(categories).orElse(emptyList()).stream()
			.filter(not(CategoryEntity::isDeprecated))
			.collect(toMap(
				CategoryEntity::getName,
				category -> ofNullable(category.getTypes()).orElse(emptyList()).stream()
					.filter(not(TypeEntity::isDeprecated))
					.map(TypeEntity::getName)
					.toList(),
				(first, _) -> first,
				LinkedHashMap::new));
	}

	public static List<LabelCandidate> toLabelCandidates(final List<MetadataLabelEntity> labels) {
		return ofNullable(labels).orElse(emptyList()).stream()
			.filter(not(MetadataLabelEntity::isDeprecated))
			.map(label -> LabelCandidate.create()
				.withId(label.getId())
				.withDisplayName(label.getDisplayName())
				.withResourcePath(label.getResourcePath()))
			.toList();
	}

	// =================================================================
	// Mapping suggestions (source value + candidates + auto-suggested target)
	// =================================================================

	/**
	 * Builds the status mapping with an auto-suggested target following the priority order: exact match on the technical
	 * name, then a case-insensitive exact match on the display name. The suggestion (and its {@link MatchReason}) is left
	 * {@code null} when the source errand has no status or no candidate matches.
	 */
	public static StatusMapping toStatusMapping(final ErrandEntity errand, final String sourceDisplayName, final List<MetadataOption> candidates) {
		final var source = ofNullable(errand.getStatus())
			.map(name -> MetadataOption.create().withName(name).withDisplayName(sourceDisplayName))
			.orElse(null);
		final var safeCandidates = ofNullable(candidates).orElse(emptyList());
		final var match = matchStatus(source, safeCandidates);

		return StatusMapping.create()
			.withSource(source)
			.withSuggestedTarget(match.map(m -> m.value().getName()).orElse(null))
			.withMatchReason(match.map(Match::reason).orElse(null))
			.withCandidates(safeCandidates);
	}

	/**
	 * Builds the classification mapping with an auto-suggested category/type. The category is matched by exact technical
	 * name, and the type only by exact name <em>within</em> the matched category (a type is meaningless without its
	 * category). The errand stores classification as names only (no source display name is resolved), so display-name
	 * matching does not apply here. {@code suggestedType} stays {@code null} when the category did not match.
	 */
	public static ClassificationMapping toClassificationMapping(final ErrandEntity errand, final Map<String, List<String>> candidates) {
		final var source = (errand.getCategory() == null && errand.getType() == null) ? null
			: ClassificationOption.create().withCategory(errand.getCategory()).withType(errand.getType());
		final var safeCandidates = ofNullable(candidates).orElse(emptyMap());

		final var suggestedCategory = ofNullable(source)
			.map(ClassificationOption::getCategory)
			.filter(safeCandidates::containsKey)
			.orElse(null);
		final var suggestedType = ofNullable(suggestedCategory)
			.map(safeCandidates::get)
			.flatMap(types -> ofNullable(source.getType()).filter(types::contains))
			.orElse(null);

		return ClassificationMapping.create()
			.withSource(source)
			.withSuggestedCategory(suggestedCategory)
			.withSuggestedType(suggestedType)
			.withCandidates(safeCandidates);
	}

	/**
	 * Builds the label mapping section: the shared candidate pool (the selectable target labels, always present) plus one
	 * mapping per source label. The pool lives on the group rather than on each mapping so it is available to the client
	 * even when the source errand has no labels, and is not duplicated per source label.
	 *
	 * <p>
	 * Each mapping's auto-suggested target follows the priority order specific to labels: a match on the hierarchical
	 * resource path (the full path first, then progressively shorter ancestor paths), then a case-insensitive exact match
	 * on the display name. Labels carry no technical name (they are identified by uuid), so name matching does not apply.
	 * </p>
	 *
	 * <p>
	 * Both the candidate pool and the mappings are ordered by resource path, segment by segment and case-insensitively (see
	 * {@link #RESOURCE_PATH_COMPARATOR}), with unset paths last. The group sorts the candidates itself rather than trusting
	 * the caller, so the returned ordering holds regardless of how the candidate list was produced.
	 * </p>
	 */
	public static LabelMappingGroup toLabelMappingGroup(final ErrandEntity errand, final List<LabelCandidate> candidates) {
		final var sortedCandidates = ofNullable(candidates).orElse(emptyList()).stream()
			.sorted(comparing(LabelCandidate::getResourcePath, RESOURCE_PATH_COMPARATOR))
			.toList();

		final var mappings = ofNullable(errand.getLabels()).orElse(emptyList()).stream()
			.map(label -> {
				final var sourceDisplayName = ofNullable(label.getMetadataLabel()).map(MetadataLabelEntity::getDisplayName).orElse(null);
				final var sourceResourcePath = ofNullable(label.getMetadataLabel()).map(MetadataLabelEntity::getResourcePath).orElse(null);
				final var match = matchLabel(sourceResourcePath, sourceDisplayName, sortedCandidates);

				return LabelMapping.create()
					.withSourceId(label.getMetadataLabelId())
					.withSourceDisplayName(sourceDisplayName)
					.withSourceResourcePath(sourceResourcePath)
					.withSuggestedTargetId(match.map(m -> m.value().getId()).orElse(null))
					.withMatchReason(match.map(Match::reason).orElse(null));
			})
			.sorted(comparing(LabelMapping::getSourceResourcePath, RESOURCE_PATH_COMPARATOR))
			.toList();

		return LabelMappingGroup.create()
			.withCandidates(sortedCandidates)
			.withMappings(mappings);
	}

	/**
	 * Builds the contact reason mapping with an auto-suggested target. The {@code reason} is a GUI-visible text string, so
	 * it is matched first (exact, then case-insensitive); only if it does not match any target is the secondary
	 * {@code displayName} tried (case-insensitive). The target {@code reason} is always returned so it can be selected from
	 * the candidate list, and so its casing is preserved. Deprecated target contact reasons are excluded.
	 */
	public static ContactReasonMapping toContactReasonMapping(final ErrandEntity errand, final List<ContactReasonEntity> targetContactReasons) {
		final var activeTargets = activeContactReasons(targetContactReasons);
		final var source = errand.getContactReason();

		return ContactReasonMapping.create()
			.withSource(ofNullable(source).map(ContactReasonEntity::getReason).orElse(null))
			.withSuggested(matchContactReason(source, activeTargets))
			.withCandidates(activeTargets.stream().map(ContactReasonEntity::getReason).toList());
	}

	private static List<ContactReasonEntity> activeContactReasons(final List<ContactReasonEntity> contactReasons) {
		return ofNullable(contactReasons).orElse(emptyList()).stream()
			.filter(not(ContactReasonEntity::isDeprecated))
			.toList();
	}

	// =================================================================
	// Matching logic (auto-suggestion rules in descending priority)
	// =================================================================

	private static Optional<Match<MetadataOption>> matchStatus(final MetadataOption source, final List<MetadataOption> candidates) {
		return ofNullable(source).flatMap(s -> firstMatch(candidates,
			candidate -> isNotBlank(s.getName()) && s.getName().equals(candidate.getName()), NAME_EXACT)
			.or(() -> firstMatch(candidates,
				candidate -> isNotBlank(s.getDisplayName()) && s.getDisplayName().equalsIgnoreCase(candidate.getDisplayName()), DISPLAY_NAME_EXACT)));
	}

	private static Optional<Match<LabelCandidate>> matchLabel(final String sourceResourcePath, final String sourceDisplayName, final List<LabelCandidate> candidates) {
		return matchResourcePathHierarchically(sourceResourcePath, candidates)
			.or(() -> firstMatch(candidates,
				candidate -> isNotBlank(sourceDisplayName) && sourceDisplayName.equalsIgnoreCase(candidate.getDisplayName()), DISPLAY_NAME_EXACT));
	}

	/**
	 * Matches the source label against the target candidates on the hierarchical resource path: the full path is tried
	 * first, then progressively shorter ancestor paths (dropping the deepest segment each step), so the deepest target
	 * label on the source's ancestor chain is suggested. Comparison is case-insensitive; a resource path is unique within a
	 * namespace, so at most one candidate matches per level. Every path-based match (exact or ancestor) reports
	 * {@link MatchReason#RESOURCE_PATH_MATCH}.
	 */
	private static Optional<Match<LabelCandidate>> matchResourcePathHierarchically(final String sourceResourcePath, final List<LabelCandidate> candidates) {
		var path = sourceResourcePath;
		while (isNotBlank(path)) {
			final var ancestor = path;
			final var match = firstMatch(candidates, candidate -> ancestor.equalsIgnoreCase(candidate.getResourcePath()), RESOURCE_PATH_MATCH);
			if (match.isPresent()) {
				return match;
			}
			final var separatorIndex = path.lastIndexOf(RESOURCE_PATH_SEPARATOR);
			if (separatorIndex < 0) {
				break;
			}
			path = path.substring(0, separatorIndex);
		}
		return Optional.empty();
	}

	/**
	 * Compares two non-null resource paths hierarchically: each path is split into its slash-separated segments and the
	 * segments are compared pairwise (case-insensitively). When one path is an ancestor prefix of the other, the shorter
	 * (the ancestor) sorts first. This keeps children grouped directly under their parent, unlike a plain lexicographic
	 * comparison where the separator can interleave with other characters.
	 */
	private static int compareResourcePathsHierarchically(final String left, final String right) {
		final var leftSegments = left.split(RESOURCE_PATH_SEPARATOR, -1);
		final var rightSegments = right.split(RESOURCE_PATH_SEPARATOR, -1);
		final var sharedDepth = Math.min(leftSegments.length, rightSegments.length);

		for (var i = 0; i < sharedDepth; i++) {
			final var segmentComparison = leftSegments[i].compareToIgnoreCase(rightSegments[i]);
			if (segmentComparison != 0) {
				return segmentComparison;
			}
		}
		return Integer.compare(leftSegments.length, rightSegments.length);
	}

	private static String matchContactReason(final ContactReasonEntity source, final List<ContactReasonEntity> targets) {
		return ofNullable(source)
			.flatMap(s -> firstReason(targets, t -> isNotBlank(s.getReason()) && s.getReason().equals(t.getReason()))
				.or(() -> firstReason(targets, t -> isNotBlank(s.getReason()) && s.getReason().equalsIgnoreCase(t.getReason())))
				.or(() -> firstReason(targets, t -> isNotBlank(s.getDisplayName()) && s.getDisplayName().equalsIgnoreCase(t.getDisplayName()))))
			.orElse(null);
	}

	private static Optional<String> firstReason(final List<ContactReasonEntity> targets, final Predicate<ContactReasonEntity> predicate) {
		return targets.stream().filter(predicate).findFirst().map(ContactReasonEntity::getReason);
	}

	/**
	 * Returns the first candidate satisfying {@code predicate} paired with the supplied {@link MatchReason}, or
	 * {@link Optional#empty()} if none matches.
	 */
	private static <T> Optional<Match<T>> firstMatch(final List<T> candidates, final Predicate<T> predicate, final MatchReason reason) {
		return candidates.stream().filter(predicate).findFirst().map(candidate -> new Match<>(candidate, reason));
	}

	/**
	 * A matched target candidate together with the reason it was suggested.
	 */
	private record Match<T>(T value, MatchReason reason) {
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
