package se.sundsvall.supportmanagement.service.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import se.sundsvall.supportmanagement.api.model.handover.LabelCandidate;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.service.mapper.LabelSuggestionMatcher.ClassificationPath;
import se.sundsvall.supportmanagement.service.mapper.LabelSuggestionMatcher.MatchableCandidate;

import static java.util.Collections.reverse;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.api.model.handover.MatchReason.BEST_EFFORT;
import static se.sundsvall.supportmanagement.api.model.handover.MatchReason.RESOURCE_PATH_MATCH;
import static se.sundsvall.supportmanagement.service.mapper.LabelSuggestionMatcher.isClassificationLabel;
import static se.sundsvall.supportmanagement.service.mapper.LabelSuggestionMatcher.match;
import static se.sundsvall.supportmanagement.service.mapper.LabelSuggestionMatcher.toClassificationPath;
import static se.sundsvall.supportmanagement.service.mapper.LabelSuggestionMatcher.toDisplayNameChain;
import static se.sundsvall.supportmanagement.service.mapper.LabelSuggestionMatcher.toLabelsByPath;

class LabelSuggestionMatcherTest {

	private static final String CATEGORY = "CATEGORY";
	private static final String TYPE = "TYPE";
	private static final String CLASS = "CLASS";

	// =================================================================
	// Exact match on the classification path
	// =================================================================

	@Test
	void matchExactWithRootOnBothSides() {
		final var source = List.of(
			label("s-root", CLASS, "Rot", "ROOT"),
			label("s-cat", CATEGORY, "Faktura", "ROOT/FAKTURA"),
			label("s-type", TYPE, "Annat", "ROOT/FAKTURA/ANNAT"));
		final var target = List.of(
			label("t-root", CLASS, "Rot", "ROOT"),
			label("t-cat", CATEGORY, "Faktura", "ROOT/FAKTURA"),
			label("t-type", TYPE, "Annat", "ROOT/FAKTURA/ANNAT"));

		final var result = match(sourcePath("s-type", source), candidates(target));

		assertThat(result.suggestedTargetId()).isEqualTo("t-type");
		assertThat(result.reason()).isEqualTo(RESOURCE_PATH_MATCH);
		assertThat(result.candidates()).isEmpty();
	}

	@Test
	void matchExactWhenRootIsMissingInTarget() {
		final var source = List.of(
			label("s-root", CLASS, "Rot", "ROOT"),
			label("s-cat", CATEGORY, "Faktura", "ROOT/FAKTURA"),
			label("s-type", TYPE, "Annat", "ROOT/FAKTURA/ANNAT"));
		final var target = List.of(
			label("t-cat", CATEGORY, "Faktura", "FAKTURA"),
			label("t-type", TYPE, "Annat", "FAKTURA/ANNAT"));

		final var result = match(sourcePath("s-type", source), candidates(target));

		assertThat(result.suggestedTargetId()).isEqualTo("t-type");
		assertThat(result.reason()).isEqualTo(RESOURCE_PATH_MATCH);
		assertThat(result.candidates()).isEmpty();
	}

	@Test
	void matchExactWhenRootIsMissingInSource() {
		final var source = List.of(
			label("s-cat", CATEGORY, "Faktura", "FAKTURA"),
			label("s-type", TYPE, "Annat", "FAKTURA/ANNAT"));
		final var target = List.of(
			label("t-root", CLASS, "Rot", "ROOT"),
			label("t-cat", CATEGORY, "Faktura", "ROOT/FAKTURA"),
			label("t-type", TYPE, "Annat", "ROOT/FAKTURA/ANNAT"));

		final var result = match(sourcePath("s-type", source), candidates(target));

		assertThat(result.suggestedTargetId()).isEqualTo("t-type");
		assertThat(result.reason()).isEqualTo(RESOURCE_PATH_MATCH);
	}

	@Test
	void matchExactWithDifferentRootsAndCasing() {
		final var source = List.of(
			label("s-root", CLASS, "Rot", "ROOT-A"),
			label("s-cat", CATEGORY, "Faktura", "ROOT-A/FAKTURA"),
			label("s-type", TYPE, "Annat", "ROOT-A/FAKTURA/ANNAT"));
		final var target = List.of(
			label("t-root", CLASS, "Rot", "ROOT-B"),
			label("t-cat", CATEGORY, "Faktura", "ROOT-B/faktura"),
			label("t-type", TYPE, "Annat", "ROOT-B/faktura/annat"));

		final var result = match(sourcePath("s-type", source), candidates(target));

		assertThat(result.suggestedTargetId()).isEqualTo("t-type");
		assertThat(result.reason()).isEqualTo(RESOURCE_PATH_MATCH);
	}

	@Test
	void matchExactOnCategory() {
		final var source = List.of(label("s-cat", CATEGORY, "Faktura", "FAKTURA"));
		final var target = List.of(
			label("t-cat", CATEGORY, "Faktura", "FAKTURA"),
			label("t-type", TYPE, "Annat", "FAKTURA/ANNAT"));

		final var result = match(sourcePath("s-cat", source), candidates(target));

		assertThat(result.suggestedTargetId()).isEqualTo("t-cat");
		assertThat(result.reason()).isEqualTo(RESOURCE_PATH_MATCH);
	}

	@Test
	void matchSameClassificationPathUnderTwoRootsIsNotGuessed() {
		// The category and type exist under two roots in the target: both are exact on the classification path, so they are
		// as good as one another and nothing is suggested
		final var source = List.of(
			label("s-cat", CATEGORY, "Faktura", "FAKTURA"),
			label("s-type", TYPE, "Annat", "FAKTURA/ANNAT"));
		final var target = List.of(
			label("t-root-1", CLASS, "Rot 1", "ROOT-1"),
			label("t-cat-1", CATEGORY, "Faktura", "ROOT-1/FAKTURA"),
			label("t-type-1", TYPE, "Annat", "ROOT-1/FAKTURA/ANNAT"),
			label("t-root-2", CLASS, "Rot 2", "ROOT-2"),
			label("t-cat-2", CATEGORY, "Faktura", "ROOT-2/FAKTURA"),
			label("t-type-2", TYPE, "Annat", "ROOT-2/FAKTURA/ANNAT"));

		final var result = match(sourcePath("s-type", source), candidates(target));

		assertThat(result.suggestedTargetId()).isNull();
		assertThat(result.reason()).isEqualTo(BEST_EFFORT);
		assertThat(result.candidates()).extracting(LabelCandidate::getId).startsWith("t-type-1", "t-type-2");
	}

	// =================================================================
	// Best effort
	// =================================================================

	@Test
	void matchBestEffortWithSingleMatchingCandidate() {
		final var source = List.of(
			label("s-cat", CATEGORY, "Faktura", "FAKTURA"),
			label("s-type", TYPE, "Annat", "FAKTURA/ANNAT"));
		final var target = List.of(
			label("t-eko", CATEGORY, "Ekonomi", "EKONOMI"),
			label("t-eko-annat", TYPE, "Övrigt", "EKONOMI/ANNAT"),
			label("t-it", CATEGORY, "IT", "IT"),
			label("t-it-natverk", TYPE, "Nätverk", "IT/NATVERK"));

		final var result = match(sourcePath("s-type", source), candidates(target));

		assertThat(result.suggestedTargetId()).isEqualTo("t-eko-annat");
		assertThat(result.reason()).isEqualTo(BEST_EFFORT);
		assertThat(result.candidates()).extracting(LabelCandidate::getId).containsExactly("t-eko-annat");
	}

	@Test
	void matchBestEffortSuggestsTheCandidateMatchingMostAndRanksItFirst() {
		// Source KUND/FAKTURA/ANNAT: EKONOMI/FAKTURA/ANNAT shares two segments from the leaf, IT/ANNAT one
		final var source = List.of(
			label("s-cat", CATEGORY, "Kund", "KUND"),
			label("s-type", TYPE, "Faktura", "KUND/FAKTURA"),
			label("s-subtype", TYPE, "Annat", "KUND/FAKTURA/ANNAT"));
		final var target = List.of(
			label("t-it", CATEGORY, "IT", "IT"),
			label("t-it-annat", TYPE, "Övrigt", "IT/ANNAT"),
			label("t-eko", CATEGORY, "Ekonomi", "EKONOMI"),
			label("t-eko-faktura", TYPE, "Fakturor", "EKONOMI/FAKTURA"),
			label("t-eko-faktura-annat", TYPE, "Övrigt", "EKONOMI/FAKTURA/ANNAT"));

		final var result = match(sourcePath("s-subtype", source), candidates(target));

		assertThat(result.suggestedTargetId()).isEqualTo("t-eko-faktura-annat");
		assertThat(result.reason()).isEqualTo(BEST_EFFORT);
		// EKONOMI/FAKTURA matches the middle of the source path with one segment, as does IT/ANNAT at the leaf
		assertThat(result.candidates()).extracting(LabelCandidate::getId)
			.containsExactly("t-eko-faktura-annat", "t-eko-faktura", "t-it-annat");
	}

	@Test
	void matchBestEffortOnTheMiddleOfThePath() {
		// The type is missing in the target, but the category of the source is there
		final var source = List.of(
			label("s-cat", CATEGORY, "Faktura", "FAKTURA"),
			label("s-type", TYPE, "Annat", "FAKTURA/ANNAT"));
		final var target = List.of(
			label("t-cat", CATEGORY, "Faktura", "FAKTURA"),
			label("t-type", TYPE, "Påminnelse", "FAKTURA/PAMINNELSE"));

		final var result = match(sourcePath("s-type", source), candidates(target));

		assertThat(result.suggestedTargetId()).isEqualTo("t-cat");
		assertThat(result.reason()).isEqualTo(BEST_EFFORT);
		assertThat(result.candidates()).extracting(LabelCandidate::getId).containsExactly("t-cat");
	}

	@Test
	void matchBestEffortWithSeveralEquallyGoodCandidates() {
		final var source = List.of(
			label("s-cat", CATEGORY, "Faktura", "FAKTURA"),
			label("s-type", TYPE, "Annat", "FAKTURA/ANNAT"));
		final var target = List.of(
			label("t-it", CATEGORY, "IT", "IT"),
			label("t-it-annat", TYPE, "Övrigt", "IT/ANNAT"),
			label("t-eko", CATEGORY, "Ekonomi", "EKONOMI"),
			label("t-eko-annat", TYPE, "Övrigt", "EKONOMI/ANNAT"));

		final var result = match(sourcePath("s-type", source), candidates(target));

		assertThat(result.suggestedTargetId()).isNull();
		assertThat(result.reason()).isEqualTo(BEST_EFFORT);
		// Equal scores are ordered on the resource path
		assertThat(result.candidates()).extracting(LabelCandidate::getId).containsExactly("t-eko-annat", "t-it-annat");
	}

	@Test
	void matchBestEffortDisplayNameDecidesBetweenEqualSegmentScores() {
		final var source = List.of(
			label("s-cat", CATEGORY, "Faktura", "FAKTURA"),
			label("s-type", TYPE, "Annat", "FAKTURA/ANNAT"));
		final var target = List.of(
			label("t-eko", CATEGORY, "Ekonomi", "EKONOMI"),
			label("t-eko-annat", TYPE, "Övrigt", "EKONOMI/ANNAT"),
			label("t-it", CATEGORY, "IT", "IT"),
			label("t-it-annat", TYPE, "annat", "IT/ANNAT"));

		final var result = match(sourcePath("s-type", source), candidates(target));

		assertThat(result.suggestedTargetId()).isEqualTo("t-it-annat");
		assertThat(result.reason()).isEqualTo(BEST_EFFORT);
		assertThat(result.candidates()).extracting(LabelCandidate::getId).containsExactly("t-it-annat", "t-eko-annat");
	}

	@Test
	void matchBestEffortOnDisplayNameOnly() {
		final var source = List.of(
			label("s-cat", CATEGORY, "Faktura", "FAKTURA"),
			label("s-type", TYPE, "Annat ärende", "FAKTURA/ANNAT"));
		final var target = List.of(
			label("t-eko", CATEGORY, "Ekonomi", "EKONOMI"),
			label("t-eko-ovrigt", TYPE, "annat ÄRENDE", "EKONOMI/OVRIGT"));

		final var result = match(sourcePath("s-type", source), candidates(target));

		assertThat(result.suggestedTargetId()).isEqualTo("t-eko-ovrigt");
		assertThat(result.reason()).isEqualTo(BEST_EFFORT);
		assertThat(result.candidates()).extracting(LabelCandidate::getId).containsExactly("t-eko-ovrigt");
	}

	@Test
	void matchIsIndependentOfTheOrderOfTheCandidates() {
		final var source = List.of(
			label("s-cat", CATEGORY, "Faktura", "FAKTURA"),
			label("s-type", TYPE, "Annat", "FAKTURA/ANNAT"));
		final var target = List.of(
			label("t-a", CATEGORY, "A", "A"),
			label("t-a-annat", TYPE, "Annat", "A/ANNAT"),
			label("t-b", CATEGORY, "B", "B"),
			label("t-b-annat", TYPE, "Annat", "B/ANNAT"),
			label("t-faktura", CATEGORY, "Faktura", "FAKTURA"));
		final var reversed = new ArrayList<>(candidates(target));
		reverse(reversed);

		final var result = match(sourcePath("s-type", source), candidates(target));
		final var resultReversed = match(sourcePath("s-type", source), reversed);

		assertThat(resultReversed).isEqualTo(result);
		assertThat(result.suggestedTargetId()).isNull();
		assertThat(result.candidates()).extracting(LabelCandidate::getId).containsExactly("t-a-annat", "t-b-annat", "t-faktura");
	}

	// =================================================================
	// No match
	// =================================================================

	@Test
	void matchWithoutAnyMatchingCandidate() {
		final var source = List.of(
			label("s-cat", CATEGORY, "Faktura", "FAKTURA"),
			label("s-type", TYPE, "Annat", "FAKTURA/ANNAT"));
		final var target = List.of(
			label("t-it", CATEGORY, "IT", "IT"),
			label("t-it-natverk", TYPE, "Nätverk", "IT/NATVERK"));

		final var result = match(sourcePath("s-type", source), candidates(target));

		assertThat(result.suggestedTargetId()).isNull();
		assertThat(result.reason()).isNull();
		assertThat(result.candidates()).isEmpty();
	}

	@Test
	void matchWithEmptyOrNullInput() {
		final var target = candidates(List.of(label("t-cat", CATEGORY, "Faktura", "FAKTURA")));

		assertThat(match(null, target).reason()).isNull();
		assertThat(match(new ClassificationPath(List.of(), List.of()), target).reason()).isNull();
		assertThat(match(new ClassificationPath(List.of("FAKTURA"), List.of("Faktura")), null).candidates()).isEmpty();
	}

	// =================================================================
	// Classification path and display name chain
	// =================================================================

	@ParameterizedTest
	@MethodSource("isClassificationLabelArguments")
	void isClassificationLabelOnlyForCategoryAndType(final MetadataLabelEntity label, final boolean expected) {
		assertThat(isClassificationLabel(label)).isEqualTo(expected);
	}

	private static Stream<Arguments> isClassificationLabelArguments() {
		return Stream.of(
			Arguments.of(null, false),
			Arguments.of(MetadataLabelEntity.create(), false),
			Arguments.of(MetadataLabelEntity.create().withClassification(CATEGORY), true),
			Arguments.of(MetadataLabelEntity.create().withClassification("type"), true),
			Arguments.of(MetadataLabelEntity.create().withClassification("SUBTYPE"), false),
			Arguments.of(MetadataLabelEntity.create().withClassification(CLASS), false));
	}

	@Test
	void toClassificationPathKeepsOnlyCategoryAndTypeSegments() {
		final var labels = List.of(
			label("root", CLASS, "Rot", "ROOT"),
			label("cat", CATEGORY, "Faktura", "ROOT/FAKTURA"),
			label("type", TYPE, "Annat", "ROOT/FAKTURA/ANNAT"),
			label("sub", "SUBTYPE", "Papper", "ROOT/FAKTURA/ANNAT/PAPPER"));

		final var result = toClassificationPath(labels.getLast(), toLabelsByPath(labels));

		assertThat(result.segments()).containsExactly("FAKTURA", "ANNAT");
		assertThat(result.displayNameChain()).containsExactly("Faktura", "Annat");
	}

	@Test
	void toDisplayNameChainIncludesEveryAncestor() {
		final var labels = List.of(
			label("root", CLASS, "Rot", "ROOT"),
			label("cat", CATEGORY, "Faktura", "ROOT/FAKTURA"),
			label("type", TYPE, "Annat", "ROOT/FAKTURA/ANNAT"));

		assertThat(toDisplayNameChain(labels.getLast(), toLabelsByPath(labels))).containsExactly("Rot", "Faktura", "Annat");
	}

	@Test
	void toDisplayNameChainSkipsAncestorsThatAreNotFound() {
		final var leaf = label("type", TYPE, "Annat", "ROOT/FAKTURA/ANNAT");

		assertThat(toDisplayNameChain(leaf, null)).containsExactly("Annat");
		assertThat(toDisplayNameChain(MetadataLabelEntity.create().withDisplayName("Utan sökväg"), null)).containsExactly("Utan sökväg");
		assertThat(toDisplayNameChain(null, null)).isEmpty();
	}

	@Test
	void toLabelsByPathSkipsLabelsWithoutPathAndKeepsTheFirstDuplicate() {
		final var first = label("first", CATEGORY, "Först", "FAKTURA");
		final var duplicate = label("duplicate", CATEGORY, "Dubblett", "faktura");

		final var result = toLabelsByPath(List.of(first, duplicate, MetadataLabelEntity.create().withId("no-path")));

		assertThat(result).containsOnlyKeys("faktura").containsEntry("faktura", first);
		assertThat(toLabelsByPath(null)).isEmpty();
	}

	// =================================================================
	// Helpers
	// =================================================================

	private static MetadataLabelEntity label(final String id, final String classification, final String displayName, final String resourcePath) {
		return MetadataLabelEntity.create()
			.withId(id)
			.withClassification(classification)
			.withDisplayName(displayName)
			.withResourceName(resourcePath.substring(resourcePath.lastIndexOf('/') + 1))
			.withResourcePath(resourcePath);
	}

	private static ClassificationPath sourcePath(final String id, final List<MetadataLabelEntity> sourceLabels) {
		final var label = sourceLabels.stream().filter(sourceLabel -> id.equals(sourceLabel.getId())).findFirst().orElseThrow();
		return toClassificationPath(label, toLabelsByPath(sourceLabels));
	}

	private static List<MatchableCandidate> candidates(final List<MetadataLabelEntity> targetLabels) {
		final var labelsByPath = toLabelsByPath(targetLabels);
		return targetLabels.stream()
			.filter(LabelSuggestionMatcher::isClassificationLabel)
			.map(label -> new MatchableCandidate(
				LabelCandidate.create().withId(label.getId()).withResourcePath(label.getResourcePath()),
				toClassificationPath(label, labelsByPath)))
			.toList();
	}
}
