package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.handover.LabelCandidate;
import se.sundsvall.supportmanagement.api.model.handover.LabelMapping;
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
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toClassificationCandidates;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toClassificationMapping;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toContactReasonMapping;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toDirectlyCopyable;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toLabelCandidates;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toLabelMappingGroup;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toNotCopyable;
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
	void toLabelCandidatesMapsIdDisplayNameAndResourcePath() {
		final var candidates = toLabelCandidates(List.of(
			MetadataLabelEntity.create().withId("uuid-b").withDisplayName("Nyckelkort").withResourcePath("/access/keycard")));

		assertThat(candidates).containsExactly(
			LabelCandidate.create().withId("uuid-b").withDisplayName("Nyckelkort").withResourcePath("/access/keycard"));
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
	void toLabelMappingGroupBuildsOneEntryPerSourceLabelWithSharedCandidates() {
		final var candidates = List.of(LabelCandidate.create().withId("uuid-b").withDisplayName("Nyckelkort").withResourcePath("/access/keycard"));
		final var errand = ErrandEntity.create().withLabels(List.of(
			ErrandLabelEmbeddable.create().withMetadataLabelId("uuid-a")));

		final var group = toLabelMappingGroup(errand, candidates);

		assertThat(group.getCandidates()).isEqualTo(candidates);
		assertThat(group.getMappings()).hasSize(1);
		assertThat(group.getMappings().getFirst().getSourceId()).isEqualTo("uuid-a");
		// The lazy metadataLabel association is only populated by Hibernate on load, so it is null for a hand-built entity
		assertThat(group.getMappings().getFirst().getSourceDisplayName()).isNull();
		assertThat(group.getMappings().getFirst().getSourceResourcePath()).isNull();
		assertThat(group.getMappings().getFirst().getSuggestedTargetId()).isNull();
		assertThat(group.getMappings().getFirst().getMatchReason()).isNull();
	}

	@Test
	void toLabelMappingGroupResolvesDisplayNameAndResourcePathFromMetadataLabel() {
		final var candidates = List.of(LabelCandidate.create().withId("uuid-b").withDisplayName("Nyckelkort").withResourcePath("/access/keycard"));
		final var label = ErrandLabelEmbeddable.create().withMetadataLabelId("uuid-a");
		// The metadataLabel association has no setter (populated by Hibernate on load); set it reflectively to exercise the
		// display name / resource path resolution branch.
		ReflectionTestUtils.setField(label, "metadataLabel",
			MetadataLabelEntity.create().withId("uuid-a").withDisplayName("Passerkort").withResourcePath("/access/passcard"));
		final var errand = ErrandEntity.create().withLabels(List.of(label));

		final var group = toLabelMappingGroup(errand, candidates);

		assertThat(group.getCandidates()).isEqualTo(candidates);
		assertThat(group.getMappings()).hasSize(1);
		assertThat(group.getMappings().getFirst().getSourceId()).isEqualTo("uuid-a");
		assertThat(group.getMappings().getFirst().getSourceDisplayName()).isEqualTo("Passerkort");
		assertThat(group.getMappings().getFirst().getSourceResourcePath()).isEqualTo("/access/passcard");
	}

	@Test
	void toLabelMappingGroupWithNullLabels() {
		// The group is always present; with no source labels and no candidates both inner lists are empty
		final var group = toLabelMappingGroup(ErrandEntity.create(), null);

		assertThat(group.getCandidates()).isEmpty();
		assertThat(group.getMappings()).isEmpty();
	}

	@Test
	void toLabelMappingGroupSuggestsResourcePathMatchOverDisplayNameMatch() {
		// RESOURCE_PATH_MATCH has priority: the source resource path matches the second candidate even though the first
		// candidate shares the source display name
		final var candidates = List.of(
			LabelCandidate.create().withId("uuid-display").withDisplayName("Nyckelkort").withResourcePath("/access/other"),
			LabelCandidate.create().withId("uuid-path").withDisplayName("Passerkort").withResourcePath("/access/keycard"));
		final var label = ErrandLabelEmbeddable.create().withMetadataLabelId("uuid-a");
		ReflectionTestUtils.setField(label, "metadataLabel",
			MetadataLabelEntity.create().withId("uuid-a").withDisplayName("Nyckelkort").withResourcePath("/access/keycard"));
		final var errand = ErrandEntity.create().withLabels(List.of(label));

		final var mapping = toLabelMappingGroup(errand, candidates).getMappings().getFirst();

		assertThat(mapping.getSuggestedTargetId()).isEqualTo("uuid-path");
		assertThat(mapping.getMatchReason()).isEqualTo(MatchReason.RESOURCE_PATH_MATCH);
	}

	@Test
	void toLabelMappingGroupSuggestsCaseInsensitiveDisplayNameMatchWhenResourcePathDiffers() {
		final var candidates = List.of(LabelCandidate.create().withId("uuid-b").withDisplayName("Nyckelkort").withResourcePath("/access/keycard"));
		final var label = ErrandLabelEmbeddable.create().withMetadataLabelId("uuid-a");
		ReflectionTestUtils.setField(label, "metadataLabel",
			MetadataLabelEntity.create().withId("uuid-a").withDisplayName("nyckelkort").withResourcePath("/access/passcard"));
		final var errand = ErrandEntity.create().withLabels(List.of(label));

		final var mapping = toLabelMappingGroup(errand, candidates).getMappings().getFirst();

		assertThat(mapping.getSuggestedTargetId()).isEqualTo("uuid-b");
		assertThat(mapping.getMatchReason()).isEqualTo(MatchReason.DISPLAY_NAME_EXACT);
	}

	@Test
	void toLabelMappingGroupLeavesSuggestionNullWhenNeitherPathNorDisplayNameMatches() {
		final var candidates = List.of(LabelCandidate.create().withId("uuid-b").withDisplayName("Nyckelkort").withResourcePath("/access/keycard"));
		final var label = ErrandLabelEmbeddable.create().withMetadataLabelId("uuid-a");
		ReflectionTestUtils.setField(label, "metadataLabel",
			MetadataLabelEntity.create().withId("uuid-a").withDisplayName("Passerkort").withResourcePath("/access/passcard"));
		final var errand = ErrandEntity.create().withLabels(List.of(label));

		final var mapping = toLabelMappingGroup(errand, candidates).getMappings().getFirst();

		assertThat(mapping.getSuggestedTargetId()).isNull();
		assertThat(mapping.getMatchReason()).isNull();
	}

	@Test
	void toLabelMappingGroupSuggestsAncestorResourcePathWhenExactPathMissing() {
		// The exact source path is absent from the target, but its parent path exists and is suggested
		final var candidates = List.of(LabelCandidate.create().withId("uuid-parent").withDisplayName("Typ").withResourcePath("CATEGORY-1/TYPE-2"));
		final var errand = errandWithSourceLabel("Undertyp", "CATEGORY-1/TYPE-2/SUBTYPE-3");

		final var mapping = toLabelMappingGroup(errand, candidates).getMappings().getFirst();

		assertThat(mapping.getSuggestedTargetId()).isEqualTo("uuid-parent");
		assertThat(mapping.getMatchReason()).isEqualTo(MatchReason.RESOURCE_PATH_MATCH);
	}

	@Test
	void toLabelMappingGroupSuggestsDeepestAncestorResourcePath() {
		// Both an ancestor and a grandparent exist in the target; the deepest (closest) ancestor wins
		final var candidates = List.of(
			LabelCandidate.create().withId("uuid-root").withDisplayName("Kategori").withResourcePath("CATEGORY-1"),
			LabelCandidate.create().withId("uuid-parent").withDisplayName("Typ").withResourcePath("CATEGORY-1/TYPE-2"));
		final var errand = errandWithSourceLabel("Undertyp", "CATEGORY-1/TYPE-2/SUBTYPE-3");

		final var mapping = toLabelMappingGroup(errand, candidates).getMappings().getFirst();

		assertThat(mapping.getSuggestedTargetId()).isEqualTo("uuid-parent");
		assertThat(mapping.getMatchReason()).isEqualTo(MatchReason.RESOURCE_PATH_MATCH);
	}

	@Test
	void toLabelMappingGroupPrefersExactResourcePathOverAncestor() {
		// An exact full-path match takes priority over the ancestor fallback
		final var candidates = List.of(
			LabelCandidate.create().withId("uuid-parent").withDisplayName("Typ").withResourcePath("CATEGORY-1/TYPE-2"),
			LabelCandidate.create().withId("uuid-exact").withDisplayName("Undertyp").withResourcePath("CATEGORY-1/TYPE-2/SUBTYPE-3"));
		final var errand = errandWithSourceLabel("Undertyp", "CATEGORY-1/TYPE-2/SUBTYPE-3");

		final var mapping = toLabelMappingGroup(errand, candidates).getMappings().getFirst();

		assertThat(mapping.getSuggestedTargetId()).isEqualTo("uuid-exact");
		assertThat(mapping.getMatchReason()).isEqualTo(MatchReason.RESOURCE_PATH_MATCH);
	}

	@Test
	void toLabelMappingGroupPrefersAncestorResourcePathMatchOverDisplayNameMatch() {
		// An ancestor resource-path match outranks an exact display-name match on an unrelated candidate
		final var candidates = List.of(
			LabelCandidate.create().withId("uuid-displayname").withDisplayName("Undertyp").withResourcePath("OTHER/PATH"),
			LabelCandidate.create().withId("uuid-parent").withDisplayName("Typ").withResourcePath("CATEGORY-1/TYPE-2"));
		final var errand = errandWithSourceLabel("Undertyp", "CATEGORY-1/TYPE-2/SUBTYPE-3");

		final var mapping = toLabelMappingGroup(errand, candidates).getMappings().getFirst();

		assertThat(mapping.getSuggestedTargetId()).isEqualTo("uuid-parent");
		assertThat(mapping.getMatchReason()).isEqualTo(MatchReason.RESOURCE_PATH_MATCH);
	}

	@Test
	void toLabelMappingGroupDoesNotMatchSourceAncestorAgainstUnrelatedDeeperCandidate() {
		// A candidate deeper than (a descendant of) the source path must NOT match; only ancestors-or-self of the source are
		// considered
		final var candidates = List.of(LabelCandidate.create().withId("uuid-child").withDisplayName("Undertyp").withResourcePath("CATEGORY-1/TYPE-2/SUBTYPE-3"));
		final var errand = errandWithSourceLabel("Typ", "CATEGORY-1/TYPE-2");

		final var mapping = toLabelMappingGroup(errand, candidates).getMappings().getFirst();

		assertThat(mapping.getSuggestedTargetId()).isNull();
		assertThat(mapping.getMatchReason()).isNull();
	}

	@Test
	void toLabelMappingGroupSuggestsDisplayNameMatchWhenSourceResourcePathIsNull() {
		// With no source resource path the hierarchical match is skipped entirely and the display-name fallback is used
		final var candidates = List.of(LabelCandidate.create().withId("uuid-b").withDisplayName("Nyckelkort").withResourcePath("CATEGORY-1"));
		final var label = ErrandLabelEmbeddable.create().withMetadataLabelId("uuid-a");
		ReflectionTestUtils.setField(label, "metadataLabel",
			MetadataLabelEntity.create().withId("uuid-a").withDisplayName("Nyckelkort"));
		final var errand = ErrandEntity.create().withLabels(List.of(label));

		final var mapping = toLabelMappingGroup(errand, candidates).getMappings().getFirst();

		assertThat(mapping.getSourceResourcePath()).isNull();
		assertThat(mapping.getSuggestedTargetId()).isEqualTo("uuid-b");
		assertThat(mapping.getMatchReason()).isEqualTo(MatchReason.DISPLAY_NAME_EXACT);
	}

	@Test
	void toLabelMappingGroupMapsEachSourceLabelIndependentlyWithSharedCandidates() {
		// An errand with several labels: one matches by resource path, one by display name, one not at all - each is mapped
		// independently while sharing the same candidate list
		final var candidates = List.of(
			LabelCandidate.create().withId("uuid-path").withDisplayName("Typ").withResourcePath("CATEGORY-1/TYPE-2"),
			LabelCandidate.create().withId("uuid-displayname").withDisplayName("Nyckelkort").withResourcePath("OTHER/PATH"));
		final var errand = ErrandEntity.create().withLabels(List.of(
			sourceLabel("src-path", "Typ", "CATEGORY-1/TYPE-2"),
			sourceLabel("src-displayname", "nyckelkort", "NO/MATCH"),
			sourceLabel("src-none", "Okänd", "NONE")));

		final var group = toLabelMappingGroup(errand, candidates);
		final var mappings = group.getMappings();

		// The candidate pool lives once on the group, shared by every mapping
		assertThat(group.getCandidates()).isEqualTo(candidates);
		assertThat(mappings).hasSize(3);

		assertThat(mappings.get(0).getSourceId()).isEqualTo("src-path");
		assertThat(mappings.get(0).getSuggestedTargetId()).isEqualTo("uuid-path");
		assertThat(mappings.get(0).getMatchReason()).isEqualTo(MatchReason.RESOURCE_PATH_MATCH);

		assertThat(mappings.get(1).getSourceId()).isEqualTo("src-displayname");
		assertThat(mappings.get(1).getSuggestedTargetId()).isEqualTo("uuid-displayname");
		assertThat(mappings.get(1).getMatchReason()).isEqualTo(MatchReason.DISPLAY_NAME_EXACT);

		assertThat(mappings.get(2).getSourceId()).isEqualTo("src-none");
		assertThat(mappings.get(2).getSuggestedTargetId()).isNull();
		assertThat(mappings.get(2).getMatchReason()).isNull();
	}

	@Test
	void toLabelMappingGroupSortsCandidatesHierarchicallyAndCaseInsensitively() {
		// "Fruit" (a parent, matched case-insensitively) sorts before its child "fruit/apple", which in turn precedes the
		// unrelated sibling "fruit-basket" (a plain lexicographic sort would place "fruit-basket" before "fruit/apple" since
		// '-' < '/'); the unset path sorts last
		final var candidates = List.of(
			LabelCandidate.create().withId("basket").withResourcePath("fruit-basket"),
			LabelCandidate.create().withId("apple").withResourcePath("fruit/apple"),
			LabelCandidate.create().withId("fruit").withResourcePath("Fruit"),
			LabelCandidate.create().withId("none").withResourcePath(null));

		final var sorted = toLabelMappingGroup(ErrandEntity.create(), candidates).getCandidates();

		assertThat(sorted).extracting(LabelCandidate::getResourcePath)
			.containsExactly("Fruit", "fruit/apple", "fruit-basket", null);
	}

	@Test
	void toLabelMappingGroupSortsMappingsByResourcePathNullsLast() {
		// Mappings are ordered by source resource path; the entry whose lazy metadataLabel is unset (null path) sorts last
		final var errand = ErrandEntity.create().withLabels(List.of(
			sourceLabel("b", "Beta", "CATEGORY-2"),
			ErrandLabelEmbeddable.create().withMetadataLabelId("no-path"),
			sourceLabel("a", "Alpha", "CATEGORY-1")));

		final var mappings = toLabelMappingGroup(errand, List.of()).getMappings();

		assertThat(mappings).extracting(LabelMapping::getSourceId).containsExactly("a", "b", "no-path");
	}

	private static ErrandEntity errandWithSourceLabel(final String sourceDisplayName, final String sourceResourcePath) {
		return ErrandEntity.create().withLabels(List.of(sourceLabel("uuid-a", sourceDisplayName, sourceResourcePath)));
	}

	private static ErrandLabelEmbeddable sourceLabel(final String id, final String displayName, final String resourcePath) {
		final var label = ErrandLabelEmbeddable.create().withMetadataLabelId(id);
		// The metadataLabel association has no setter (populated by Hibernate on load); set it reflectively
		ReflectionTestUtils.setField(label, "metadataLabel",
			MetadataLabelEntity.create().withId(id).withDisplayName(displayName).withResourcePath(resourcePath));
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
		// The source reason matches no target reason, but its display name matches a target display name case-insensitively;
		// the target reason is returned
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
