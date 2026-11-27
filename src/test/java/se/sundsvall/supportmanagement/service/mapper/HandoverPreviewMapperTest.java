package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.handover.LabelCandidate;
import se.sundsvall.supportmanagement.api.model.handover.LabelMapping;
import se.sundsvall.supportmanagement.api.model.handover.LabelMappingGroup;
import se.sundsvall.supportmanagement.api.model.handover.MatchReason;
import se.sundsvall.supportmanagement.api.model.handover.MetadataOption;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.CategoryEntity;
import se.sundsvall.supportmanagement.integration.db.model.ContactReasonEntity;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatusEntity;
import se.sundsvall.supportmanagement.integration.db.model.TypeEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toClassificationCandidates;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toClassificationMapping;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toContactReasonMapping;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toDirectlyCopyable;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toLabelCandidates;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toLabelMappingGroup;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toNotCopyable;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toSourceHandling;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toStatusCandidates;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toStatusMapping;

class HandoverPreviewMapperTest {

	@Test
	void toDirectlyCopyableMapsValuesAndCounts() {
		final var errand = ErrandEntity.create()
			.withTitle("title")
			.withPriority("HIGH")
			.withStakeholders(List.of(StakeholderEntity.create(), StakeholderEntity.create(), StakeholderEntity.create()))
			.withExternalTags(List.of(DbExternalTag.create(), DbExternalTag.create()))
			.withAttachments(List.of(AttachmentEntity.create()));

		final var result = toDirectlyCopyable(errand);

		assertThat(result.getTitle()).isEqualTo("title");
		assertThat(result.getPriority()).isEqualTo(Priority.HIGH);
		assertThat(result.getStakeholderCount()).isEqualTo(3);
		assertThat(result.getExternalTagCount()).isEqualTo(2);
		assertThat(result.getAttachmentCount()).isEqualTo(1);
	}

	@Test
	void toDirectlyCopyableHandlesNullCollectionsAndPriority() {
		final var result = toDirectlyCopyable(ErrandEntity.create().withTitle("title"));

		assertThat(result.getTitle()).isEqualTo("title");
		assertThat(result.getPriority()).isNull();
		assertThat(result.getStakeholderCount()).isZero();
		assertThat(result.getExternalTagCount()).isZero();
		assertThat(result.getAttachmentCount()).isZero();
	}

	@Test
	void toDirectlyCopyableNullErrand() {
		assertThat(toDirectlyCopyable(null)).isNull();
	}

	@Test
	void toStatusCandidatesMapsNameAndDisplayName() {
		final var candidates = toStatusCandidates(List.of(
			StatusEntity.create().withName("NEW_CASE").withDisplayName("Nytt ärende"),
			StatusEntity.create().withName("IN_PROGRESS").withDisplayName("Pågående")));

		assertThat(candidates).containsExactly(
			MetadataOption.create().withName("NEW_CASE").withDisplayName("Nytt ärende"),
			MetadataOption.create().withName("IN_PROGRESS").withDisplayName("Pågående"));
	}

	@Test
	void toStatusCandidatesNull() {
		assertThat(toStatusCandidates(null)).isEmpty();
	}

	@Test
	void toStatusCandidatesExcludesDeprecated() {
		final var candidates = toStatusCandidates(List.of(
			StatusEntity.create().withName("NEW_CASE").withDisplayName("Nytt ärende"),
			StatusEntity.create().withName("OLD").withDisplayName("Gammal").withDeprecated(true)));

		assertThat(candidates).containsExactly(MetadataOption.create().withName("NEW_CASE").withDisplayName("Nytt ärende"));
	}

	@Test
	void toSourceHandlingMapsStatusCandidatesExcludingDeprecated() {
		final var result = toSourceHandling(List.of(
			StatusEntity.create().withName("SOLVED").withDisplayName("Löst"),
			StatusEntity.create().withName("OLD").withDisplayName("Gammal").withDeprecated(true)));

		assertThat(result.getStatusCandidates()).containsExactly(MetadataOption.create().withName("SOLVED").withDisplayName("Löst"));
	}

	@Test
	void toSourceHandlingNull() {
		final var result = toSourceHandling(null);

		assertThat(result).isNotNull();
		assertThat(result.getStatusCandidates()).isEmpty();
	}

	@Test
	void toClassificationCandidatesMapsCategoryToTypeNamesPreservingOrder() {
		final var candidates = toClassificationCandidates(List.of(
			CategoryEntity.create().withName("SUPPORT_CASE").withTypes(List.of(
				TypeEntity.create().withName("OTHER_ISSUES"),
				TypeEntity.create().withName("LOGIN"))),
			CategoryEntity.create().withName("EMPTY_CATEGORY")));

		assertThat(candidates)
			.containsExactly(
				entry("SUPPORT_CASE", List.of("OTHER_ISSUES", "LOGIN")),
				entry("EMPTY_CATEGORY", List.of()));
	}

	@Test
	void toClassificationCandidatesNull() {
		assertThat(toClassificationCandidates(null)).isEmpty();
	}

	@Test
	void toClassificationCandidatesExcludesDeprecatedCategoriesAndTypes() {
		final var candidates = toClassificationCandidates(List.of(
			CategoryEntity.create().withName("ACTIVE").withTypes(List.of(
				TypeEntity.create().withName("KEEP"),
				TypeEntity.create().withName("DROP").withDeprecated(true))),
			CategoryEntity.create().withName("DEPRECATED_CATEGORY").withDeprecated(true).withTypes(List.of(
				TypeEntity.create().withName("HIDDEN")))));

		// The deprecated category is dropped entirely; the deprecated type is removed from the active category
		assertThat(candidates).containsExactly(entry("ACTIVE", List.of("KEEP")));
	}

	@Test
	void toClassificationCandidatesMergesDuplicateCategoryNamesKeepingFirst() {
		// Category names are unique within a namespace; the defensive merge function keeps the first entry should a
		// duplicate ever appear.
		final var candidates = toClassificationCandidates(List.of(
			CategoryEntity.create().withName("DUP").withTypes(List.of(TypeEntity.create().withName("FIRST"))),
			CategoryEntity.create().withName("DUP").withTypes(List.of(TypeEntity.create().withName("SECOND")))));

		assertThat(candidates).containsExactly(entry("DUP", List.of("FIRST")));
	}

	@Test
	void toLabelCandidatesMapsIdDisplayNameResourcePathAndDisplayNameChain() {
		final var candidates = toLabelCandidates(List.of(
			label("uuid-a", "CLASS", "Behörighet", "ACCESS"),
			label("uuid-b", "CATEGORY", "Nyckelkort", "ACCESS/KEYCARD")));

		assertThat(candidates).containsExactly(
			LabelCandidate.create().withId("uuid-a").withDisplayName("Behörighet").withResourcePath("ACCESS").withDisplayNameChain(List.of("Behörighet")),
			LabelCandidate.create().withId("uuid-b").withDisplayName("Nyckelkort").withResourcePath("ACCESS/KEYCARD").withDisplayNameChain(List.of("Behörighet", "Nyckelkort")));
	}

	@Test
	void toLabelCandidatesNull() {
		assertThat(toLabelCandidates(null)).isEmpty();
	}

	@Test
	void toLabelCandidatesExcludesDeprecated() {
		final var candidates = toLabelCandidates(List.of(
			MetadataLabelEntity.create().withId("keep").withDisplayName("Behåll").withResourcePath("CATEGORY-1"),
			MetadataLabelEntity.create().withId("drop").withDisplayName("Släng").withResourcePath("CATEGORY-2").withDeprecated(true)));

		assertThat(candidates).extracting(LabelCandidate::getId).containsExactly("keep");
	}

	@Test
	void toLabelCandidatesPreservesInputOrder() {
		// toLabelCandidates is a pure projection; ordering is the responsibility of toLabelMappingGroup
		final var candidates = toLabelCandidates(List.of(
			MetadataLabelEntity.create().withId("c").withResourcePath("CATEGORY-2"),
			MetadataLabelEntity.create().withId("a").withResourcePath("CATEGORY-1/TYPE-1"),
			MetadataLabelEntity.create().withId("b").withResourcePath("CATEGORY-1")));

		assertThat(candidates).extracting(LabelCandidate::getId).containsExactly("c", "a", "b");
	}

	@Test
	void toStatusMappingSuggestsExactNameMatchOverDisplayNameMatch() {
		// NAME_EXACT has priority: the source name "IN_PROGRESS" matches the second candidate by name even though the first
		// candidate also matches the source display name "Pågående".
		final var candidates = List.of(
			MetadataOption.create().withName("ONGOING").withDisplayName("Pågående"),
			MetadataOption.create().withName("IN_PROGRESS").withDisplayName("Påbörjad"));
		final var errand = ErrandEntity.create().withStatus("IN_PROGRESS");

		final var mapping = toStatusMapping(errand, "Pågående", candidates);

		assertThat(mapping.getSource()).isEqualTo(MetadataOption.create().withName("IN_PROGRESS").withDisplayName("Pågående"));
		assertThat(mapping.getSuggestedTarget()).isEqualTo("IN_PROGRESS");
		assertThat(mapping.getMatchReason()).isEqualTo(MatchReason.NAME_EXACT);
		assertThat(mapping.getCandidates()).isEqualTo(candidates);
	}

	@Test
	void toStatusMappingSuggestsCaseInsensitiveDisplayNameMatchWhenNameDiffers() {
		final var candidates = List.of(MetadataOption.create().withName("IN_PROGRESS").withDisplayName("Pågående"));
		final var errand = ErrandEntity.create().withStatus("ONGOING");

		// Source name differs from every candidate name, but the display name matches case-insensitively
		final var mapping = toStatusMapping(errand, "pågående", candidates);

		assertThat(mapping.getSuggestedTarget()).isEqualTo("IN_PROGRESS");
		assertThat(mapping.getMatchReason()).isEqualTo(MatchReason.DISPLAY_NAME_EXACT);
	}

	@Test
	void toStatusMappingLeavesSuggestionNullWhenNoCandidateMatches() {
		final var candidates = List.of(MetadataOption.create().withName("CLOSED").withDisplayName("Avslutad"));
		final var errand = ErrandEntity.create().withStatus("ONGOING");

		final var mapping = toStatusMapping(errand, "Pågående", candidates);

		assertThat(mapping.getSuggestedTarget()).isNull();
		assertThat(mapping.getMatchReason()).isNull();
		assertThat(mapping.getCandidates()).isEqualTo(candidates);
	}

	@Test
	void toStatusMappingWithNullStatusHasNullSourceAndSuggestion() {
		final var mapping = toStatusMapping(ErrandEntity.create(), null, null);

		assertThat(mapping.getSource()).isNull();
		assertThat(mapping.getSuggestedTarget()).isNull();
		assertThat(mapping.getMatchReason()).isNull();
		assertThat(mapping.getCandidates()).isEmpty();
	}

	@Test
	void toClassificationMappingSuggestsExactCategoryAndTypeNameMatch() {
		final var errand = ErrandEntity.create().withCategory("SUPPORT_CASE").withType("OTHER_ISSUES");

		final var mapping = toClassificationMapping(errand, java.util.Map.of("SUPPORT_CASE", List.of("OTHER_ISSUES")));

		assertThat(mapping.getSource().getCategory()).isEqualTo("SUPPORT_CASE");
		assertThat(mapping.getSource().getType()).isEqualTo("OTHER_ISSUES");
		assertThat(mapping.getCandidates()).containsExactly(entry("SUPPORT_CASE", List.of("OTHER_ISSUES")));
		assertThat(mapping.getSuggestedCategory()).isEqualTo("SUPPORT_CASE");
		assertThat(mapping.getSuggestedType()).isEqualTo("OTHER_ISSUES");
	}

	@Test
	void toClassificationMappingSuggestsCategoryButNotTypeWhenTypeMissingFromCategory() {
		// The category matches but the source type is not configured under it, so only the category is suggested
		final var errand = ErrandEntity.create().withCategory("SUPPORT_CASE").withType("LOGIN");

		final var mapping = toClassificationMapping(errand, java.util.Map.of("SUPPORT_CASE", List.of("OTHER_ISSUES")));

		assertThat(mapping.getSuggestedCategory()).isEqualTo("SUPPORT_CASE");
		assertThat(mapping.getSuggestedType()).isNull();
	}

	@Test
	void toClassificationMappingLeavesSuggestionsNullWhenCategoryMissingFromTarget() {
		// The category is absent from the target, so neither category nor type is suggested even if a type with the same name
		// happens to exist under another category
		final var errand = ErrandEntity.create().withCategory("UNKNOWN").withType("OTHER_ISSUES");

		final var mapping = toClassificationMapping(errand, java.util.Map.of("SUPPORT_CASE", List.of("OTHER_ISSUES")));

		assertThat(mapping.getSuggestedCategory()).isNull();
		assertThat(mapping.getSuggestedType()).isNull();
	}

	@Test
	void toClassificationMappingSuggestsCategoryWhenSourceTypeIsNull() {
		// The category matches and the source has no type, so only the category is suggested
		final var errand = ErrandEntity.create().withCategory("SUPPORT_CASE");

		final var mapping = toClassificationMapping(errand, java.util.Map.of("SUPPORT_CASE", List.of("OTHER_ISSUES")));

		assertThat(mapping.getSuggestedCategory()).isEqualTo("SUPPORT_CASE");
		assertThat(mapping.getSuggestedType()).isNull();
	}

	@Test
	void toClassificationMappingWithNullCategoryAndTypeHasNullSource() {
		final var mapping = toClassificationMapping(ErrandEntity.create(), null);

		assertThat(mapping.getSource()).isNull();
		assertThat(mapping.getCandidates()).isEmpty();
	}

	@Test
	void toClassificationMappingWithOnlyTypeHasSource() {
		// Source is present as soon as either category or type is set, so a type without a category still yields a source.
		final var mapping = toClassificationMapping(ErrandEntity.create().withType("OTHER_ISSUES"), null);

		assertThat(mapping.getSource()).isNotNull();
		assertThat(mapping.getSource().getCategory()).isNull();
		assertThat(mapping.getSource().getType()).isEqualTo("OTHER_ISSUES");
	}

	@Test
	void toLabelMappingGroupKeepsEveryTargetLabelInThePoolButMapsOnlyClassificationLabels() {
		final var sourceLabels = List.of(
			label("s-cat", "CATEGORY", "Faktura", "FAKTURA"),
			label("s-type", "TYPE", "Annat", "FAKTURA/ANNAT"),
			label("s-sub", "SUBTYPE", "Papper", "FAKTURA/ANNAT/PAPPER"),
			label("s-other", "PURPOSE", "Annat syfte", "OTHER"));
		final var targetLabels = List.of(
			label("t-root", "CLASS", "Rot", "ROOT"),
			label("t-cat", "CATEGORY", "Faktura", "ROOT/FAKTURA"),
			label("t-type", "TYPE", "Annat", "ROOT/FAKTURA/ANNAT"),
			label("t-sub", "SUBTYPE", "Papper", "ROOT/FAKTURA/ANNAT/PAPPER"),
			label("t-other", "PURPOSE", "Annat syfte", "OTHER"));
		final var errand = errandWithLabels(sourceLabels);

		final var group = toLabelMappingGroup(errand, targetLabels);

		// The pool holds every target label, the root and the labels for other purposes included
		assertThat(group.getCandidates()).extracting(LabelCandidate::getId)
			.containsExactly("t-other", "t-root", "t-cat", "t-type", "t-sub");
		assertThat(group.getCandidates()).filteredOn(candidate -> "t-type".equals(candidate.getId()))
			.extracting(LabelCandidate::getDisplayNameChain)
			.containsExactly(List.of("Rot", "Faktura", "Annat"));

		// Only the source labels classifying the errand are mapped, ordered by resource path
		assertThat(group.getMappings())
			.extracting(LabelMapping::getSourceId, LabelMapping::getSourceDisplayName, LabelMapping::getSourceResourcePath, LabelMapping::getSuggestedTargetId, LabelMapping::getMatchReason)
			.containsExactly(
				tuple("s-cat", "Faktura", "FAKTURA", "t-cat", MatchReason.RESOURCE_PATH_MATCH),
				tuple("s-type", "Annat", "FAKTURA/ANNAT", "t-type", MatchReason.RESOURCE_PATH_MATCH));
		assertThat(group.getMappings()).allSatisfy(mapping -> assertThat(mapping.getMatchCandidates()).isEmpty());
	}

	@Test
	void toLabelMappingGroupReturnsRankedBestEffortCandidatesWithoutSuggestionWhenTied() {
		final var sourceLabels = List.of(
			label("s-cat", "CATEGORY", "Faktura", "FAKTURA"),
			label("s-type", "TYPE", "Annat", "FAKTURA/ANNAT"));
		final var targetLabels = List.of(
			label("t-it", "CATEGORY", "IT", "IT"),
			label("t-it-annat", "TYPE", "Övrigt", "IT/ANNAT"),
			label("t-eko", "CATEGORY", "Ekonomi", "EKONOMI"),
			label("t-eko-annat", "TYPE", "Övrigt", "EKONOMI/ANNAT"));
		final var errand = errandWithLabels(sourceLabels);

		final var mapping = mappingOf(toLabelMappingGroup(errand, targetLabels), "s-type");

		assertThat(mapping.getSuggestedTargetId()).isNull();
		assertThat(mapping.getMatchReason()).isEqualTo(MatchReason.BEST_EFFORT);
		assertThat(mapping.getMatchCandidates()).containsExactly(
			LabelCandidate.create().withId("t-eko-annat").withDisplayName("Övrigt").withResourcePath("EKONOMI/ANNAT").withDisplayNameChain(List.of("Ekonomi", "Övrigt")),
			LabelCandidate.create().withId("t-it-annat").withDisplayName("Övrigt").withResourcePath("IT/ANNAT").withDisplayNameChain(List.of("IT", "Övrigt")));
	}

	@Test
	void toLabelMappingGroupSuggestsSingleBestEffortCandidate() {
		final var sourceLabels = List.of(
			label("s-cat", "CATEGORY", "Faktura", "FAKTURA"),
			label("s-type", "TYPE", "Annat", "FAKTURA/ANNAT"));
		final var targetLabels = List.of(
			label("t-cat", "CATEGORY", "Faktura", "FAKTURA"),
			label("t-type", "TYPE", "Påminnelse", "FAKTURA/PAMINNELSE"));
		final var errand = errandWithLabels(sourceLabels);

		final var mapping = mappingOf(toLabelMappingGroup(errand, targetLabels), "s-type");

		assertThat(mapping.getSuggestedTargetId()).isEqualTo("t-cat");
		assertThat(mapping.getMatchReason()).isEqualTo(MatchReason.BEST_EFFORT);
		assertThat(mapping.getMatchCandidates()).extracting(LabelCandidate::getId).containsExactly("t-cat");
	}

	@Test
	void toLabelMappingGroupLeavesSuggestionNullWhenNothingMatches() {
		final var sourceLabels = List.of(label("s-cat", "CATEGORY", "Faktura", "FAKTURA"));
		final var targetLabels = List.of(label("t-cat", "CATEGORY", "IT", "IT"));

		final var mapping = toLabelMappingGroup(errandWithLabels(sourceLabels), targetLabels).getMappings().getFirst();

		assertThat(mapping.getSuggestedTargetId()).isNull();
		assertThat(mapping.getMatchReason()).isNull();
		assertThat(mapping.getMatchCandidates()).isEmpty();
	}

	@Test
	void toLabelMappingGroupDoesNotMatchDeprecatedOrOtherTargetLabels() {
		// The deprecated label is left out everywhere; the label for another purpose stays in the pool but is never matched
		final var sourceLabels = List.of(label("s-cat", "CATEGORY", "Faktura", "FAKTURA"));
		final var targetLabels = List.of(
			label("t-deprecated", "CATEGORY", "Faktura", "FAKTURA").withDeprecated(true),
			label("t-other", "PURPOSE", "Faktura", "OTHER/FAKTURA"));

		final var group = toLabelMappingGroup(errandWithLabels(sourceLabels), targetLabels);

		assertThat(group.getCandidates()).extracting(LabelCandidate::getId).containsExactly("t-other");
		assertThat(group.getMappings().getFirst().getSuggestedTargetId()).isNull();
		assertThat(group.getMappings().getFirst().getMatchReason()).isNull();
	}

	@Test
	void toLabelMappingGroupLeavesOutSourceLabelsWithoutMetadataLabel() {
		// The lazy metadataLabel association is only populated by Hibernate on load, so its classification is unknown here
		final var errand = ErrandEntity.create().withLabels(List.of(ErrandLabelEmbeddable.create().withMetadataLabelId("uuid-a")));

		final var group = toLabelMappingGroup(errand, List.of(label("t-cat", "CATEGORY", "Faktura", "FAKTURA")));

		assertThat(group.getCandidates()).hasSize(1);
		assertThat(group.getMappings()).isEmpty();
	}

	@Test
	void toLabelMappingGroupWithNullLabels() {
		// The group is always present; with no source labels and no target labels both inner lists are empty
		final var group = toLabelMappingGroup(ErrandEntity.create(), null);

		assertThat(group.getCandidates()).isEmpty();
		assertThat(group.getMappings()).isEmpty();
	}

	@Test
	void toLabelMappingGroupSortsCandidatesHierarchicallyAndCaseInsensitively() {
		// "Fruit" (a parent, matched case-insensitively) sorts before its child "fruit/apple", which in turn precedes the
		// unrelated sibling "fruit-basket" (a plain lexicographic sort would place "fruit-basket" before "fruit/apple" since
		// '-' < '/'); the unset path sorts last
		final var targetLabels = List.of(
			MetadataLabelEntity.create().withId("basket").withResourcePath("fruit-basket"),
			MetadataLabelEntity.create().withId("apple").withResourcePath("fruit/apple"),
			MetadataLabelEntity.create().withId("fruit").withResourcePath("Fruit"),
			MetadataLabelEntity.create().withId("none"));

		final var sorted = toLabelMappingGroup(ErrandEntity.create(), targetLabels).getCandidates();

		assertThat(sorted).extracting(LabelCandidate::getResourcePath)
			.containsExactly("Fruit", "fruit/apple", "fruit-basket", null);
	}

	@Test
	void toLabelMappingGroupSortsMappingsByResourcePath() {
		final var sourceLabels = List.of(
			label("b", "CATEGORY", "Beta", "CATEGORY-2"),
			label("a", "CATEGORY", "Alpha", "CATEGORY-1"));

		final var mappings = toLabelMappingGroup(errandWithLabels(sourceLabels), List.of()).getMappings();

		assertThat(mappings).extracting(LabelMapping::getSourceId).containsExactly("a", "b");
	}

	private static MetadataLabelEntity label(final String id, final String classification, final String displayName, final String resourcePath) {
		return MetadataLabelEntity.create()
			.withId(id)
			.withClassification(classification)
			.withDisplayName(displayName)
			.withResourceName(resourcePath.substring(resourcePath.lastIndexOf('/') + 1))
			.withResourcePath(resourcePath);
	}

	@Test
	void toLabelMappingGroupResolvesTheClassificationPathAmongTheLabelsOfTheErrand() {
		// The errand carries only its type, not the category above it: the classification path of the type is then the type
		// alone, which is no exact match on FAKTURA/ANNAT but a best effort one
		final var targetLabels = List.of(
			label("t-cat", "CATEGORY", "Faktura", "FAKTURA"),
			label("t-type", "TYPE", "Annat", "FAKTURA/ANNAT"));
		final var errand = errandWithLabels(List.of(label("s-type", "TYPE", "Annat", "FAKTURA/ANNAT")));

		final var mapping = mappingOf(toLabelMappingGroup(errand, targetLabels), "s-type");

		assertThat(mapping.getSuggestedTargetId()).isEqualTo("t-type");
		assertThat(mapping.getMatchReason()).isEqualTo(MatchReason.BEST_EFFORT);
	}

	private static LabelMapping mappingOf(final LabelMappingGroup group, final String sourceId) {
		return group.getMappings().stream().filter(mapping -> sourceId.equals(mapping.getSourceId())).findFirst().orElseThrow();
	}

	private static ErrandEntity errandWithLabels(final List<MetadataLabelEntity> labels) {
		return ErrandEntity.create().withLabels(labels.stream().map(HandoverPreviewMapperTest::sourceLabel).toList());
	}

	private static ErrandLabelEmbeddable sourceLabel(final MetadataLabelEntity metadataLabel) {
		final var label = ErrandLabelEmbeddable.create().withMetadataLabelId(metadataLabel.getId());
		// The metadataLabel association has no setter (populated by Hibernate on load); set it reflectively
		ReflectionTestUtils.setField(label, "metadataLabel", metadataLabel);
		return label;
	}

	@Test
	void toContactReasonMappingSuggestsExactReasonMatch() {
		final var errand = ErrandEntity.create().withContactReason(ContactReasonEntity.create().withReason("Bygglov"));

		final var mapping = toContactReasonMapping(errand, List.of(
			ContactReasonEntity.create().withReason("Bygglov"),
			ContactReasonEntity.create().withReason("Övrigt")));

		assertThat(mapping.getSource()).isEqualTo("Bygglov");
		assertThat(mapping.getCandidates()).containsExactly("Bygglov", "Övrigt");
		assertThat(mapping.getSuggested()).isEqualTo("Bygglov");
	}

	@Test
	void toContactReasonMappingSuggestsCaseInsensitiveReasonMatchPreservingTargetCasing() {
		final var errand = ErrandEntity.create().withContactReason(ContactReasonEntity.create().withReason("bygglov"));

		final var mapping = toContactReasonMapping(errand, List.of(
			ContactReasonEntity.create().withReason("Övrigt"),
			ContactReasonEntity.create().withReason("Bygglov")));

		// The case-insensitive reason fallback returns the target value so its casing is preserved
		assertThat(mapping.getSuggested()).isEqualTo("Bygglov");
	}

	@Test
	void toContactReasonMappingPrefersExactReasonOverCaseInsensitiveReason() {
		final var errand = ErrandEntity.create().withContactReason(ContactReasonEntity.create().withReason("Bygglov"));

		final var mapping = toContactReasonMapping(errand, List.of(
			ContactReasonEntity.create().withReason("bygglov"),
			ContactReasonEntity.create().withReason("Bygglov")));

		// The exact-case target wins over the earlier case-insensitive-only one
		assertThat(mapping.getSuggested()).isEqualTo("Bygglov");
	}

	@Test
	void toContactReasonMappingSuggestsDisplayNameMatchWhenReasonDiffers() {
		// The source reason matches no target reason, but its display name matches a target display name case-insensitively,
		// hence the target reason is returned.
		final var errand = ErrandEntity.create().withContactReason(
			ContactReasonEntity.create().withReason("Felanmälan").withDisplayName("Fault report"));

		final var mapping = toContactReasonMapping(errand, List.of(
			ContactReasonEntity.create().withReason("Annat").withDisplayName("Other"),
			ContactReasonEntity.create().withReason("Trasigt").withDisplayName("fault report")));

		assertThat(mapping.getSuggested()).isEqualTo("Trasigt");
	}

	@Test
	void toContactReasonMappingPrefersReasonMatchOverDisplayNameMatch() {
		// A reason match (here case-insensitive) outranks a display-name match on another candidate
		final var errand = ErrandEntity.create().withContactReason(
			ContactReasonEntity.create().withReason("Bygglov").withDisplayName("Delad"));

		final var mapping = toContactReasonMapping(errand, List.of(
			ContactReasonEntity.create().withReason("Annat").withDisplayName("Delad"),
			ContactReasonEntity.create().withReason("bygglov").withDisplayName("Annan")));

		assertThat(mapping.getSuggested()).isEqualTo("bygglov");
	}

	@Test
	void toContactReasonMappingExcludesDeprecatedFromCandidatesAndSuggestion() {
		// A deprecated target that would otherwise match exactly is excluded from both the candidate list and the suggestion
		final var errand = ErrandEntity.create().withContactReason(ContactReasonEntity.create().withReason("Bygglov"));

		final var mapping = toContactReasonMapping(errand, List.of(
			ContactReasonEntity.create().withReason("Bygglov").withDeprecated(true),
			ContactReasonEntity.create().withReason("Övrigt")));

		assertThat(mapping.getCandidates()).containsExactly("Övrigt");
		assertThat(mapping.getSuggested()).isNull();
	}

	@Test
	void toContactReasonMappingLeavesSuggestionNullWhenNoCandidateMatches() {
		final var errand = ErrandEntity.create().withContactReason(ContactReasonEntity.create().withReason("Bygglov"));

		final var mapping = toContactReasonMapping(errand, List.of(ContactReasonEntity.create().withReason("Övrigt")));

		assertThat(mapping.getSuggested()).isNull();
	}

	@Test
	void toContactReasonMappingWithNullContactReason() {
		final var mapping = toContactReasonMapping(ErrandEntity.create(), null);

		assertThat(mapping.getSource()).isNull();
		assertThat(mapping.getSuggested()).isNull();
		assertThat(mapping.getCandidates()).isEmpty();
	}

	@Test
	void toNotCopyableListsStructuralFields() {
		final var notCopyable = toNotCopyable();

		assertThat(notCopyable).extracting("field").containsExactly("phases", "activePhaseId");
		assertThat(notCopyable).extracting("reason").containsExactly("Phase history is source-specific", "Phase IDs differ per namespace");
	}
}
