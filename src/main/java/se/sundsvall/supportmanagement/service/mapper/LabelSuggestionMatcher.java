package se.sundsvall.supportmanagement.service.mapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import se.sundsvall.supportmanagement.api.model.handover.LabelCandidate;
import se.sundsvall.supportmanagement.api.model.handover.MatchReason;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.nullsLast;
import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.Strings.CI;
import static se.sundsvall.supportmanagement.api.model.handover.MatchReason.BEST_EFFORT;
import static se.sundsvall.supportmanagement.api.model.handover.MatchReason.RESOURCE_PATH_MATCH;

/**
 * Suggests a target label for a source label when an errand is handed over to another namespace.
 *
 * <p>
 * Only labels that classify an errand, those with classification {@code CATEGORY} or {@code TYPE}, take part. A label
 * is compared on its <em>classification path</em>: the {@code CATEGORY} and {@code TYPE} segments of its resource path,
 * from the root down to the label itself. Roots and other levels with another classification fall away, so two
 * namespaces compare equal whether either of them has a root above its categories, or they have different roots.
 * </p>
 *
 * <p>
 * The match is made in descending priority:
 * </p>
 * <ol>
 * <li>An exact match on the classification path is suggested with {@link MatchReason#RESOURCE_PATH_MATCH}.</li>
 * <li>Otherwise every candidate is scored on how many segments in a row, upwards from its leaf, it shares with the
 * source path; then on how near the leaf of the source that run ends, so a match on the leaf of the source ranks above
 * one higher up its path; and then on the display name chain as on the segments. The candidates that score are
 * returned ranked, the best first, with {@link MatchReason#BEST_EFFORT}. A single best candidate is suggested; when
 * several score the same at the top, nothing is suggested.</li>
 * <li>When no candidate scores, there is no suggestion and no reason.</li>
 * </ol>
 *
 * <p>
 * Comparisons are case-insensitive. The outcome does not depend on the order of the candidates.
 * </p>
 */
public final class LabelSuggestionMatcher {

	static final String CLASSIFICATION_CATEGORY = "CATEGORY";
	static final String CLASSIFICATION_TYPE = "TYPE";

	// Mirrors MetadataLabelEntity#RESOURCE_PATH_SEPARATOR: resource paths are built as "root/child/grandchild"
	private static final String RESOURCE_PATH_SEPARATOR = "/";

	// Orders resource paths hierarchically: segment by segment and case-insensitively, so a parent path always precedes its
	// children and a child never sorts after an unrelated sibling (a plain lexicographic sort would allow that, since the
	// separator '/' interleaves with other characters). Paths that are unset sort last.
	static final Comparator<String> RESOURCE_PATH_COMPARATOR = nullsLast(LabelSuggestionMatcher::compareResourcePathsHierarchically);

	private LabelSuggestionMatcher() {}

	/**
	 * Whether the label classifies an errand, that is has classification {@code CATEGORY} or {@code TYPE}.
	 */
	public static boolean isClassificationLabel(final MetadataLabelEntity label) {
		return ofNullable(label)
			.map(MetadataLabelEntity::getClassification)
			.filter(classification -> CI.equalsAny(classification, CLASSIFICATION_CATEGORY, CLASSIFICATION_TYPE))
			.isPresent();
	}

	/**
	 * Indexes the labels of a namespace on their resource path, lowercased. Labels without a path are left out; should a
	 * path occur more than once, the first label is kept.
	 */
	public static Map<String, MetadataLabelEntity> toLabelsByPath(final List<MetadataLabelEntity> labels) {
		return ofNullable(labels).orElse(emptyList()).stream()
			.filter(label -> isNotBlank(label.getResourcePath()))
			.collect(toMap(label -> label.getResourcePath().toLowerCase(ROOT), label -> label, (first, _) -> first, LinkedHashMap::new));
	}

	/**
	 * The display names of the label and its ancestors, from the root down to the label itself, looked up in the labels of
	 * its namespace.
	 */
	public static List<String> toDisplayNameChain(final MetadataLabelEntity label, final Map<String, MetadataLabelEntity> labelsByPath) {
		return ancestorsAndSelf(label, labelsByPath).stream()
			.map(MetadataLabelEntity::getDisplayName)
			.toList();
	}

	/**
	 * The classification path of the label: its {@code CATEGORY} and {@code TYPE} segments and their display names, from
	 * the root down to the label itself, looked up in the labels of its namespace.
	 */
	public static ClassificationPath toClassificationPath(final MetadataLabelEntity label, final Map<String, MetadataLabelEntity> labelsByPath) {
		final var classificationLabels = ancestorsAndSelf(label, labelsByPath).stream()
			.filter(LabelSuggestionMatcher::isClassificationLabel)
			.toList();

		return new ClassificationPath(
			classificationLabels.stream().map(MetadataLabelEntity::getResourceName).toList(),
			classificationLabels.stream().map(MetadataLabelEntity::getDisplayName).toList());
	}

	/**
	 * Matches the source classification path against the target candidates.
	 *
	 * @param  source     the classification path of the source label
	 * @param  candidates the target labels that classify an errand, with their classification paths
	 * @return            the suggestion, the reason for it and, for a best effort match, the ranked candidates
	 */
	public static LabelMatch match(final ClassificationPath source, final List<MatchableCandidate> candidates) {
		final var safeCandidates = ofNullable(candidates).orElse(emptyList());
		if (ofNullable(source).map(ClassificationPath::segments).filter(not(List::isEmpty)).isEmpty()) {
			return LabelMatch.none();
		}

		final var exactMatches = safeCandidates.stream()
			.filter(candidate -> equalsIgnoreCase(source.segments(), candidate.path().segments()))
			.toList();
		if (exactMatches.size() == 1) {
			return new LabelMatch(exactMatches.getFirst().candidate().getId(), RESOURCE_PATH_MATCH, emptyList());
		}

		// Several candidates with an identical classification path (the same category and type under different roots) are
		// as good as one another, so they are left to the ranking below, where they tie at the top
		final var ranked = safeCandidates.stream()
			.map(candidate -> {
				final var segmentAlignment = align(source.segments(), candidate.path().segments());
				return new ScoredCandidate(candidate.candidate(),
					segmentAlignment.length(),
					segmentAlignment.distanceFromLeaf(),
					align(source.displayNameChain(), candidate.path().displayNameChain()).length());
			})
			.filter(ScoredCandidate::isHit)
			.sorted(comparingInt(ScoredCandidate::segmentScore).reversed()
				.thenComparingInt(ScoredCandidate::distanceFromLeaf)
				.thenComparing(comparingInt(ScoredCandidate::displayNameScore).reversed())
				.thenComparing(scored -> scored.candidate().getResourcePath(), RESOURCE_PATH_COMPARATOR))
			.toList();

		if (ranked.isEmpty()) {
			return LabelMatch.none();
		}

		final var best = ranked.getFirst();
		final var isSingleBest = ranked.size() == 1 || !best.scoresAsHighAs(ranked.get(1));

		final var suggestedTargetId = ofNullable(best.candidate().getId()).filter(_ -> isSingleBest).orElse(null);

		return new LabelMatch(suggestedTargetId, BEST_EFFORT, ranked.stream().map(ScoredCandidate::candidate).toList());
	}

	/**
	 * Places the leaf of the candidate at each position of the source, from the leaf of the source upwards, and counts the
	 * elements in a row they share upwards from there. The leaf of the candidate may thereby match the leaf of the source
	 * or any of its ancestors. The longest run wins; of equally long runs, the one nearest the leaf of the source.
	 */
	private static Alignment align(final List<String> source, final List<String> candidate) {
		var best = new Alignment(0, source.size());
		for (var position = source.size() - 1; position >= 0; position--) {
			var length = 0;
			while (length < candidate.size() && position - length >= 0
				&& isEqualIgnoreCase(source.get(position - length), candidate.get(candidate.size() - 1 - length))) {
				length++;
			}
			if (length > best.length()) {
				best = new Alignment(length, source.size() - 1 - position);
			}
		}
		return best;
	}

	private static boolean equalsIgnoreCase(final List<String> left, final List<String> right) {
		if (left.size() != right.size()) {
			return false;
		}
		for (var i = 0; i < left.size(); i++) {
			if (!isEqualIgnoreCase(left.get(i), right.get(i))) {
				return false;
			}
		}
		return true;
	}

	private static boolean isEqualIgnoreCase(final String left, final String right) {
		return isNotBlank(left) && left.equalsIgnoreCase(right);
	}

	/**
	 * The label and its ancestors, from the root down to the label itself. Each ancestor is looked up on its resource path
	 * among the labels of the namespace, so the parent association is never loaded. An ancestor that is not found is left
	 * out.
	 */
	private static List<MetadataLabelEntity> ancestorsAndSelf(final MetadataLabelEntity label, final Map<String, MetadataLabelEntity> labelsByPath) {
		final var result = new ArrayList<MetadataLabelEntity>();
		if (label == null) {
			return result;
		}

		final var safeLabelsByPath = ofNullable(labelsByPath).orElse(Map.of());
		final var segments = ofNullable(label.getResourcePath()).map(path -> path.split(RESOURCE_PATH_SEPARATOR)).orElse(new String[0]);
		final var prefix = new StringBuilder();
		for (var i = 0; i < segments.length - 1; i++) {
			if (i > 0) {
				prefix.append(RESOURCE_PATH_SEPARATOR);
			}
			prefix.append(segments[i]);
			ofNullable(safeLabelsByPath.get(prefix.toString().toLowerCase(ROOT))).ifPresent(result::add);
		}
		result.add(label);
		return result;
	}

	/**
	 * Compares two non-null resource paths hierarchically: each path is split into its slash-separated segments and the
	 * segments are compared pairwise (case-insensitively). When one path is an ancestor prefix of the other, the shorter
	 * (the ancestor) sorts first. This keeps children grouped directly under their parent.
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

	/**
	 * The classification path of a label: the resource names of its {@code CATEGORY} and {@code TYPE} segments and their
	 * display names, from the root down to the label itself.
	 */
	public record ClassificationPath(List<String> segments, List<String> displayNameChain) {
	}

	/**
	 * A target label offered as a candidate, together with its classification path.
	 */
	public record MatchableCandidate(LabelCandidate candidate, ClassificationPath path) {
	}

	/**
	 * The outcome of a match: the suggested target label id (null when there is none, or when several candidates are as
	 * good), the reason (null when no candidate matched) and the ranked best effort candidates (empty unless the reason is
	 * {@link MatchReason#BEST_EFFORT}).
	 */
	public record LabelMatch(String suggestedTargetId, MatchReason reason, List<LabelCandidate> candidates) {

		static LabelMatch none() {
			return new LabelMatch(null, null, emptyList());
		}
	}

	/**
	 * The longest run of shared elements, and how many levels above the leaf of the source it ends (0 for the leaf
	 * itself, the length of the source when nothing is shared).
	 */
	private record Alignment(int length, int distanceFromLeaf) {
	}

	private record ScoredCandidate(LabelCandidate candidate, int segmentScore, int distanceFromLeaf, int displayNameScore) {

		boolean isHit() {
			return segmentScore > 0 || displayNameScore > 0;
		}

		boolean scoresAsHighAs(final ScoredCandidate other) {
			return segmentScore == other.segmentScore && distanceFromLeaf == other.distanceFromLeaf && displayNameScore == other.displayNameScore;
		}
	}
}
