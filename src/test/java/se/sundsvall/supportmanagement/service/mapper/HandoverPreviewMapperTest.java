package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.handover.LabelCandidate;
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
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toContactReasonCandidates;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toContactReasonMapping;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toDirectlyCopyable;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toLabelCandidates;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toLabelMappings;
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
	void toContactReasonCandidatesMapsReason() {
		final var candidates = toContactReasonCandidates(List.of(
			ContactReasonEntity.create().withReason("Bygglov"),
			ContactReasonEntity.create().withReason("Övrigt")));

		assertThat(candidates).containsExactly("Bygglov", "Övrigt");
	}

	@Test
	void toContactReasonCandidatesNull() {
		assertThat(toContactReasonCandidates(null)).isEmpty();
	}

	@Test
	void toStatusMappingSetsSourceAndCandidatesAndLeavesSuggestionNull() {
		final var candidates = List.of(MetadataOption.create().withName("IN_PROGRESS").withDisplayName("Pågående"));
		final var errand = ErrandEntity.create().withStatus("ONGOING");

		final var mapping = toStatusMapping(errand, "Pågående", candidates);

		assertThat(mapping.getSource()).isEqualTo(MetadataOption.create().withName("ONGOING").withDisplayName("Pågående"));
		assertThat(mapping.getCandidates()).isEqualTo(candidates);
		assertThat(mapping.getSuggestedTarget()).isNull();
		assertThat(mapping.getMatchReason()).isNull();
	}

	@Test
	void toStatusMappingWithNullStatusHasNullSource() {
		final var mapping = toStatusMapping(ErrandEntity.create(), null, null);

		assertThat(mapping.getSource()).isNull();
		assertThat(mapping.getCandidates()).isEmpty();
	}

	@Test
	void toClassificationMappingSetsSourceAndCandidatesAndLeavesSuggestionsNull() {
		final var errand = ErrandEntity.create().withCategory("SUPPORT_CASE").withType("OTHER_ISSUES");

		final var mapping = toClassificationMapping(errand, java.util.Map.of("SUPPORT_CASE", List.of("OTHER_ISSUES")));

		assertThat(mapping.getSource().getCategory()).isEqualTo("SUPPORT_CASE");
		assertThat(mapping.getSource().getType()).isEqualTo("OTHER_ISSUES");
		assertThat(mapping.getCandidates()).containsExactly(entry("SUPPORT_CASE", List.of("OTHER_ISSUES")));
		assertThat(mapping.getSuggestedCategory()).isNull();
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
	void toLabelMappingsBuildsOneEntryPerSourceLabelWithSharedCandidates() {
		final var candidates = List.of(LabelCandidate.create().withId("uuid-b").withDisplayName("Nyckelkort").withResourcePath("/access/keycard"));
		final var errand = ErrandEntity.create().withLabels(List.of(
			ErrandLabelEmbeddable.create().withMetadataLabelId("uuid-a")));

		final var mappings = toLabelMappings(errand, candidates);

		assertThat(mappings).hasSize(1);
		assertThat(mappings.getFirst().getSourceId()).isEqualTo("uuid-a");
		// The lazy metadataLabel association is only populated by Hibernate on load, so it is null for a hand-built entity
		assertThat(mappings.getFirst().getSourceDisplayName()).isNull();
		assertThat(mappings.getFirst().getSourceResourcePath()).isNull();
		assertThat(mappings.getFirst().getSuggestedTargetId()).isNull();
		assertThat(mappings.getFirst().getMatchReason()).isNull();
		assertThat(mappings.getFirst().getCandidates()).isEqualTo(candidates);
	}

	@Test
	void toLabelMappingsResolvesDisplayNameAndResourcePathFromMetadataLabel() {
		final var candidates = List.of(LabelCandidate.create().withId("uuid-b").withDisplayName("Nyckelkort").withResourcePath("/access/keycard"));
		final var label = ErrandLabelEmbeddable.create().withMetadataLabelId("uuid-a");
		// The metadataLabel association has no setter (populated by Hibernate on load); set it reflectively to exercise the
		// display name / resource path resolution branch.
		ReflectionTestUtils.setField(label, "metadataLabel",
			MetadataLabelEntity.create().withId("uuid-a").withDisplayName("Passerkort").withResourcePath("/access/passcard"));
		final var errand = ErrandEntity.create().withLabels(List.of(label));

		final var mappings = toLabelMappings(errand, candidates);

		assertThat(mappings).hasSize(1);
		assertThat(mappings.getFirst().getSourceId()).isEqualTo("uuid-a");
		assertThat(mappings.getFirst().getSourceDisplayName()).isEqualTo("Passerkort");
		assertThat(mappings.getFirst().getSourceResourcePath()).isEqualTo("/access/passcard");
		assertThat(mappings.getFirst().getCandidates()).isEqualTo(candidates);
	}

	@Test
	void toLabelMappingsWithNullLabels() {
		assertThat(toLabelMappings(ErrandEntity.create(), null)).isEmpty();
	}

	@Test
	void toContactReasonMappingSetsSourceAndCandidates() {
		final var errand = ErrandEntity.create().withContactReason(ContactReasonEntity.create().withReason("Bygglov"));

		final var mapping = toContactReasonMapping(errand, List.of("Bygglov", "Övrigt"));

		assertThat(mapping.getSource()).isEqualTo("Bygglov");
		assertThat(mapping.getCandidates()).containsExactly("Bygglov", "Övrigt");
		assertThat(mapping.getSuggested()).isNull();
	}

	@Test
	void toContactReasonMappingWithNullContactReason() {
		final var mapping = toContactReasonMapping(ErrandEntity.create(), null);

		assertThat(mapping.getSource()).isNull();
		assertThat(mapping.getCandidates()).isEmpty();
	}

	@Test
	void toNotCopyableListsStructuralFields() {
		final var notCopyable = toNotCopyable();

		assertThat(notCopyable).extracting("field").containsExactly("phases", "activePhaseId");
		assertThat(notCopyable).extracting("reason").containsExactly("Phase history is source-specific", "Phase IDs differ per namespace");
	}
}
