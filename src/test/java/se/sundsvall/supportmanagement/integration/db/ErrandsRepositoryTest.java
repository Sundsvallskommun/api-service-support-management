package se.sundsvall.supportmanagement.integration.db;

import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.ApplicationTest;
import se.sundsvall.supportmanagement.integration.db.model.ContactChannelEntity;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandLifecycle;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.fail;
import static se.sundsvall.supportmanagement.integration.db.specification.ErrandSpecification.hasMatchingTags;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withDefaultDeleted;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withDefaultLifecycle;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withDeletedBefore;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withMunicipalityId;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withNamespace;

/**
 * Errands repository tests.
 *
 * @see <a href="file:src/test/resources/db/testdata.sql">src/test/resources/db/testdata.sql</a> for data setup.
 */
@ApplicationTest // Needs the application, as turkraft components are used in test
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
@Transactional
class ErrandsRepositoryTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Autowired
	private ErrandsRepository errandsRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private FilterSpecificationConverter filterSpecificationConverter;

	@Test
	void create() {
		final var externalTag = DbExternalTag.create().withKey("key").withValue("value");
		final var stakeholder = StakeholderEntity.create().withExternalId("id").withExternalIdType("EMPLOYEE").withRole("ROLE").withContactChannels(List.of(ContactChannelEntity.create().withType("type").withValue("value")));
		final var namespace = "namespace";
		final var title = "title";
		final var category = "category";
		final var type = "type";
		final var status = "status";
		final var priority = "priority";
		final var reporterUserId = "reporterUserId";
		final var assignedUserId = "assignedUserId";
		final var assignedGroupId = "assignedGroupId";
		final var municipalityId = "2281";
		final var escalationEmail = "escalation@email.com";
		final var errandNumber = "errandNumber";

		final var errandEntity = ErrandEntity.create()
			.withNamespace(namespace)
			.withTitle(title)
			.withCategory(category)
			.withType(type)
			.withStatus(status)
			.withPriority(priority)
			.withReporterUserId(reporterUserId)
			.withAssignedUserId(assignedUserId)
			.withAssignedGroupId(assignedGroupId)
			.withExternalTags(List.of(externalTag))
			.withStakeholders(List.of(stakeholder))
			.withMunicipalityId(municipalityId)
			.withEscalationEmail(escalationEmail)
			.withErrandNumber(errandNumber);

		// Execution
		final var persistedEntity = errandsRepository.save(errandEntity);

		assertThat(persistedEntity).isNotNull();
		assertThat(persistedEntity.getId()).isNotNull();
		assertThat(persistedEntity.getErrandNumber()).isEqualTo(errandNumber);
		assertThat(persistedEntity.getNamespace()).isEqualTo(namespace);
		assertThat(persistedEntity.getTitle()).isEqualTo(title);
		assertThat(persistedEntity.getCategory()).isEqualTo(category);
		assertThat(persistedEntity.getType()).isEqualTo(type);
		assertThat(persistedEntity.getStatus()).isEqualTo(status);
		assertThat(persistedEntity.getPriority()).isEqualTo(priority);
		assertThat(persistedEntity.getReporterUserId()).isEqualTo(reporterUserId);
		assertThat(persistedEntity.getAssignedUserId()).isEqualTo(assignedUserId);
		assertThat(persistedEntity.getAssignedGroupId()).isEqualTo(assignedGroupId);
		assertThat(persistedEntity.getExternalTags()).contains(externalTag);
		assertThat(persistedEntity.getStakeholders()).containsExactly(stakeholder);
		assertThat(persistedEntity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(persistedEntity.getCreated()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		assertThat(persistedEntity.getModified()).isNull();
		assertThat(persistedEntity.getEscalationEmail()).isEqualTo(escalationEmail);
	}

	@Test
	void findByAssignedGroupAndAssignedUserIdWhereExternalTagsIsNotEmpty() {

		final Specification<ErrandEntity> specification = filterSpecificationConverter.convert("(assignedGroupId : 'ASSIGNED_GROUP_ID-1' and assignedUserId : 'ASSIGNED_USER_ID-1' and externalTags is not empty)");

		final Pageable pageable = PageRequest.of(0, 20);

		final var errandEntities = errandsRepository.findAll(specification, pageable);

		assertThat(errandEntities).isNotNull();
		assertThat(errandEntities.getTotalElements()).isEqualTo(2);

		assertThat(errandEntities)
			.extracting(ErrandEntity::getId, ErrandEntity::getAssignedGroupId, ErrandEntity::getAssignedUserId, ErrandEntity::getEscalationEmail).containsExactlyInAnyOrder(
				tuple("ERRAND_ID-1", "ASSIGNED_GROUP_ID-1", "ASSIGNED_USER_ID-1", "ESCALATION_EMAIL_1"),
				tuple("ERRAND_ID-2", "ASSIGNED_GROUP_ID-1", "ASSIGNED_USER_ID-1", "ESCALATION_EMAIL_2"));
	}

	@Test
	void errandWithStakeholderAndContactChannel() {
		final var errandEntity = errandsRepository.findById("ERRAND_ID-1");

		assertThat(errandEntity.get().getStakeholders()).hasSize(1);
		assertThat(errandEntity.get().getStakeholders())
			.extracting(StakeholderEntity::getId, StakeholderEntity::getExternalIdType, StakeholderEntity::getExternalId, StakeholderEntity::getFirstName, StakeholderEntity::getLastName, StakeholderEntity::getAddress, StakeholderEntity::getCareOf,
				StakeholderEntity::getZipCode, StakeholderEntity::getCountry, StakeholderEntity::getRole)
			.containsExactly(tuple(3001L, "EMPLOYEE", "EXTERNAL_ID-1", "FIRST_NAME-1", "LAST_NAME-1", "ADDRESS-1", "CARE_OF-1", "ZIP_CODE-1", "COUNTRY-1", "ROLE-1"));
		assertThat(errandEntity.get().getStakeholders().getFirst().getContactChannels()).hasSize(1);
		assertThat(errandEntity.get().getStakeholders().getFirst().getContactChannels())
			.extracting(ContactChannelEntity::getType, ContactChannelEntity::getValue)
			.containsExactly(tuple("TYPE-1", "VALUE-1"));
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', quoteCharacter = '"', nullValues = "NONE", value = {
		"NONE                                          | ACTIVE-1",
		"errandNumber : 'DRAFT-1'                      | ",
		"lifecycle : 'DRAFT'                           | DRAFT-1",
		"lifecycle : 'DRAFT' or lifecycle : 'ACTIVE'   | ACTIVE-1,DRAFT-1",
		"errandNumber : 'DRAFT-1' and lifecycle is not null | DRAFT-1"
	})
	void defaultLifecycleLeavesDraftsOutUnlessTheFilterNamesTheLifecycle(final String filterString, final String expectedErrandNumbers) {
		errandsRepository.save(ErrandEntity.create().withNamespace("DRAFTS").withMunicipalityId(MUNICIPALITY_ID).withStatus("STATUS").withErrandNumber("DRAFT-1").withLifecycle(ErrandLifecycle.DRAFT));
		errandsRepository.save(ErrandEntity.create().withNamespace("DRAFTS").withMunicipalityId(MUNICIPALITY_ID).withStatus("STATUS").withErrandNumber("ACTIVE-1"));

		final Specification<ErrandEntity> filter = Optional.ofNullable(filterString).<Specification<ErrandEntity>>map(filterSpecificationConverter::convert).orElse(null);
		final var search = withNamespace("DRAFTS").and(withDefaultLifecycle(filter));

		assertThat(errandsRepository.findAll(Optional.ofNullable(filter).map(search::and).orElse(search)))
			.extracting(ErrandEntity::getErrandNumber)
			.containsExactlyInAnyOrderElementsOf(Optional.ofNullable(expectedErrandNumbers).map(numbers -> List.of(numbers.split(","))).orElse(List.of()));
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', quoteCharacter = '"', nullValues = "NONE", value = {
		"NONE                                          | KEPT-1",
		"errandNumber : 'DELETED-1'                    | ",
		"deleted is not null                           | DELETED-1",
		"deleted is null or deleted is not null        | KEPT-1,DELETED-1",
		"deletedBy : 'joe01doe'                        | DELETED-1"
	})
	void defaultDeletedLeavesSoftDeletedErrandsOutUnlessTheFilterNamesTheDeletion(final String filterString, final String expectedErrandNumbers) {
		errandsRepository.save(ErrandEntity.create().withNamespace("SOFT-DELETES").withMunicipalityId(MUNICIPALITY_ID).withStatus("STATUS").withErrandNumber("DELETED-1").withDeleted(OffsetDateTime.now()).withDeletedBy("joe01doe"));
		errandsRepository.save(ErrandEntity.create().withNamespace("SOFT-DELETES").withMunicipalityId(MUNICIPALITY_ID).withStatus("STATUS").withErrandNumber("KEPT-1"));

		final Specification<ErrandEntity> filter = Optional.ofNullable(filterString).<Specification<ErrandEntity>>map(filterSpecificationConverter::convert).orElse(null);
		final var search = withNamespace("SOFT-DELETES").and(withDefaultDeleted(filter));

		assertThat(errandsRepository.findAll(Optional.ofNullable(filter).map(search::and).orElse(search)))
			.extracting(ErrandEntity::getErrandNumber)
			.containsExactlyInAnyOrderElementsOf(Optional.ofNullable(expectedErrandNumbers).map(numbers -> List.of(numbers.split(","))).orElse(List.of()));
	}

	@Test
	void existsLeavingOutSoftDeleted() {
		final var deleted = errandsRepository.save(ErrandEntity.create().withNamespace("SOFT-DELETES").withMunicipalityId(MUNICIPALITY_ID).withErrandNumber("DELETED-2").withDeleted(OffsetDateTime.now()));
		final var kept = errandsRepository.save(ErrandEntity.create().withNamespace("SOFT-DELETES").withMunicipalityId(MUNICIPALITY_ID).withErrandNumber("KEPT-2"));

		assertThat(errandsRepository.existsByIdAndNamespaceAndMunicipalityIdAndDeletedIsNull(kept.getId(), "SOFT-DELETES", MUNICIPALITY_ID)).isTrue();
		assertThat(errandsRepository.existsWithLockingByIdAndNamespaceAndMunicipalityIdAndDeletedIsNull(kept.getId(), "SOFT-DELETES", MUNICIPALITY_ID)).isTrue();
		assertThat(errandsRepository.existsByIdAndNamespaceAndMunicipalityIdAndDeletedIsNull(deleted.getId(), "SOFT-DELETES", MUNICIPALITY_ID)).isFalse();
		assertThat(errandsRepository.existsWithLockingByIdAndNamespaceAndMunicipalityIdAndDeletedIsNull(deleted.getId(), "SOFT-DELETES", MUNICIPALITY_ID)).isFalse();
		assertThat(errandsRepository.existsByIdAndNamespaceAndMunicipalityId(deleted.getId(), "SOFT-DELETES", MUNICIPALITY_ID)).isTrue();
		assertThat(errandsRepository.existsByIdAndDeletedIsNull(kept.getId())).isTrue();
		assertThat(errandsRepository.existsByIdAndDeletedIsNull(deleted.getId())).isFalse();
		assertThat(errandsRepository.findByIdAndNamespaceAndMunicipalityIdAndDeletedIsNull(kept.getId(), "SOFT-DELETES", MUNICIPALITY_ID)).isPresent();
		assertThat(errandsRepository.findByIdAndNamespaceAndMunicipalityIdAndDeletedIsNull(deleted.getId(), "SOFT-DELETES", MUNICIPALITY_ID)).isEmpty();
	}

	@Test
	void deletedBefore() {
		final var cutoff = OffsetDateTime.now().minusDays(30);
		final var ids = List.of(
			errandsRepository.save(ErrandEntity.create().withNamespace("SOFT-DELETES").withMunicipalityId(MUNICIPALITY_ID).withErrandNumber("OLD-1").withDeleted(cutoff.minusDays(1))).getId(),
			errandsRepository.save(ErrandEntity.create().withNamespace("SOFT-DELETES").withMunicipalityId(MUNICIPALITY_ID).withErrandNumber("OLD-2").withDeleted(cutoff.minusDays(10))).getId(),
			errandsRepository.save(ErrandEntity.create().withNamespace("SOFT-DELETES").withMunicipalityId(MUNICIPALITY_ID).withErrandNumber("OLD-3").withDeleted(cutoff.minusMinutes(1))).getId())
			.stream().sorted().toList();
		errandsRepository.save(ErrandEntity.create().withNamespace("SOFT-DELETES").withMunicipalityId(MUNICIPALITY_ID).withErrandNumber("RECENT").withDeleted(cutoff.plusDays(1)));
		errandsRepository.save(ErrandEntity.create().withNamespace("SOFT-DELETES").withMunicipalityId(MUNICIPALITY_ID).withErrandNumber("KEPT"));
		errandsRepository.save(ErrandEntity.create().withNamespace("OTHER-NAMESPACE").withMunicipalityId(MUNICIPALITY_ID).withErrandNumber("OTHER-NAMESPACE").withDeleted(cutoff.minusDays(1)));
		errandsRepository.save(ErrandEntity.create().withNamespace("SOFT-DELETES").withMunicipalityId("2262").withErrandNumber("OTHER-MUNICIPALITY").withDeleted(cutoff.minusDays(1)));

		final var deletedBefore = withNamespace("SOFT-DELETES").and(withMunicipalityId(MUNICIPALITY_ID)).and(withDeletedBefore(cutoff));

		assertThat(errandsRepository.findAll(deletedBefore, Sort.by("id"))).extracting(ErrandEntity::getId).containsExactlyElementsOf(ids);
	}

	@Test
	void findIdsAfter() {
		final var ids = Stream.of("WALK-1", "WALK-2", "WALK-3", "WALK-4")
			.map(errandNumber -> errandsRepository.save(ErrandEntity.create().withNamespace("KEYSET-WALK").withMunicipalityId(MUNICIPALITY_ID).withErrandNumber(errandNumber)).getId())
			.sorted()
			.toList();
		errandsRepository.save(ErrandEntity.create().withNamespace("OTHER-NAMESPACE").withMunicipalityId(MUNICIPALITY_ID).withErrandNumber("WALK-OTHER"));
		final var reached = withNamespace("KEYSET-WALK").and(withMunicipalityId(MUNICIPALITY_ID));

		// The first batch starts from the beginning, each following one after the id the previous one ended on, and the
		// walk ends on an empty batch
		assertThat(errandsRepository.findIdsAfter(reached, null, 3)).containsExactlyElementsOf(ids.subList(0, 3));
		assertThat(errandsRepository.findIdsAfter(reached, ids.get(2), 3)).containsExactly(ids.get(3));
		assertThat(errandsRepository.findIdsAfter(reached, ids.get(3), 3)).isEmpty();
	}

	@Test
	void findIdsWithUnannouncedSuspensionEndAndDeletedIsNull() {
		final var suspendedTo = OffsetDateTime.now().minusDays(1);
		final var due = errandsRepository.save(suspended("DUE", "SUSPENDED", suspendedTo).withAssignedUserId("assignedUser"));
		final var unassigned = errandsRepository.save(suspended("UNASSIGNED", "SUSPENDED", suspendedTo));
		final var announcedEarlierSuspension = errandsRepository.save(suspended("ANNOUNCED-EARLIER", "SUSPENDED", suspendedTo));
		final var announced = errandsRepository.save(suspended("ANNOUNCED", "SUSPENDED", suspendedTo));
		final var softDeleted = errandsRepository.save(suspended("SOFT-DELETED", "SUSPENDED", suspendedTo).withDeleted(OffsetDateTime.now()));
		final var otherStatus = errandsRepository.save(suspended("OTHER-STATUS", "OPEN", suspendedTo));
		final var stillSuspended = errandsRepository.save(suspended("STILL-SUSPENDED", "SUSPENDED", OffsetDateTime.now().plusDays(1)));
		errandsRepository.flush();

		errandsRepository.markSuspensionEndNotified(announcedEarlierSuspension.getId(), suspendedTo.minusDays(7));
		errandsRepository.markSuspensionEndNotified(announced.getId(), announced.getSuspendedTo());

		assertThat(errandsRepository.findIdsWithUnannouncedSuspensionEndAndDeletedIsNull("SUSPENDED", OffsetDateTime.now()))
			.contains(due.getId(), unassigned.getId(), announcedEarlierSuspension.getId())
			.doesNotContain(announced.getId(), softDeleted.getId(), otherStatus.getId(), stillSuspended.getId());
	}

	@Test
	void markSuspensionEndNotifiedLeavesTheErrandUntouchedAndSurvivesALaterSave() {
		final var suspendedTo = OffsetDateTime.now().minusDays(1);
		final var errand = errandsRepository.saveAndFlush(suspended("MARKED", "SUSPENDED", suspendedTo));
		final var modified = errand.getModified();
		final var version = errand.getVersion();

		errandsRepository.markSuspensionEndNotified(errand.getId(), suspendedTo);
		entityManager.clear();

		final var marked = errandsRepository.findById(errand.getId()).orElseThrow();
		assertThat(marked.getSuspensionEndNotifiedFor()).isCloseTo(suspendedTo, within(1, MILLIS));
		assertThat(marked.getModified()).isEqualTo(modified);
		assertThat(marked.getVersion()).isEqualTo(version);

		// An errand read before the mark holds no value for it, and saving such an errand must not put that back
		entityManager.clear();
		final var readBeforeTheMark = errandsRepository.findById(errand.getId()).orElseThrow();
		readBeforeTheMark.setSuspensionEndNotifiedFor(null);
		readBeforeTheMark.setTitle("changed");
		errandsRepository.saveAndFlush(readBeforeTheMark);
		entityManager.clear();

		final var saved = errandsRepository.findById(errand.getId()).orElseThrow();
		assertThat(saved.getTitle()).isEqualTo("changed");
		assertThat(saved.getSuspensionEndNotifiedFor()).isCloseTo(suspendedTo, within(1, MILLIS));
	}

	private static ErrandEntity suspended(final String errandNumber, final String status, final OffsetDateTime suspendedTo) {
		return ErrandEntity.create()
			.withNamespace("SUSPENSIONS")
			.withMunicipalityId(MUNICIPALITY_ID)
			.withErrandNumber(errandNumber)
			.withStatus(status)
			.withSuspendedTo(suspendedTo);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"(externalTags.key : 'KEY-1')",
		"(attachments.id : 'ATTACHMENT_ID-1')",
		"(stakeholders.externalId : 'EXTERNAL_ID-1' and externalTags is not empty)",
		"(stakeholders.externalId : 'EXTERNAL_ID-1' and attachments is not empty)"
	})
	void findByFilter(final String filter) {

		final Specification<ErrandEntity> specification = filterSpecificationConverter.convert(filter);

		final Pageable pageable = PageRequest.of(0, 20);

		final var errandEntities = errandsRepository.findAll(specification, pageable);

		assertThat(errandEntities).isNotNull();
		assertThat(errandEntities.getTotalElements()).isEqualTo(1);

		assertThat(errandEntities)
			.extracting(ErrandEntity::getId, ErrandEntity::getAssignedGroupId, ErrandEntity::getAssignedUserId).containsExactlyInAnyOrder(
				tuple("ERRAND_ID-1", "ASSIGNED_GROUP_ID-1", "ASSIGNED_USER_ID-1"));
	}

	@Test
	void update() {

		// Setup
		final var entityToUpdate = errandsRepository.findById("ERRAND_ID-3");
		final var newAssignedUserId = "ASSIGNED_USER_ID-CHANGED";

		// Execution
		entityToUpdate.get().setAssignedUserId(newAssignedUserId);
		final var updatedEntity = errandsRepository.save(entityToUpdate.get());
		errandsRepository.flush();

		// Assertions
		assertThat(updatedEntity).isNotNull();
		assertThat(updatedEntity.getAssignedUserId()).isEqualTo(newAssignedUserId);
		assertThat(updatedEntity.getModified()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
	}

	@Test
	void delete() {

		final var entityToDelete = errandsRepository.findById("ERRAND_ID-3");
		// Execution
		errandsRepository.delete(entityToDelete.get());

		// Assertions
		assertThat(errandsRepository.findById("ERRAND_ID-3")).isNotPresent();
	}

	@Test
	void findByIdNotFound() {
		assertThat(errandsRepository.findById("THIS_ERRAND_DOES_NOT_EXIST")).isEmpty();
	}

	@Test
	void existsByIdAndNamespaceAndMunicipalityId() {
		assertThat(errandsRepository.existsByIdAndNamespaceAndMunicipalityId("ERRAND_ID-1", "NAMESPACE.1", "2281")).isTrue();
		assertThat(errandsRepository.existsByIdAndNamespaceAndMunicipalityId("ERRAND_ID-1", "NAMESPACE.1", "2305")).isFalse();
		assertThat(errandsRepository.existsByIdAndNamespaceAndMunicipalityId("ERRAND_ID-1", "NAMESPACE.2", "2281")).isFalse();
		assertThat(errandsRepository.existsByIdAndNamespaceAndMunicipalityId("ERRAND_ID-3", "NAMESPACE.1", "2281")).isFalse();
	}

	@Test
	void findByErrandNumberAndNamespace() {
		final var errandEntities = errandsRepository.findByErrandNumberAndNamespaceAndMunicipalityId("KC-23020001", "NAMESPACE.1", MUNICIPALITY_ID);
		assertThat(errandEntities).isNotNull();
		errandEntities.ifPresentOrElse(
			errandEntity -> assertThat(errandEntity.getId()).isEqualTo("ERRAND_ID-1"),
			() -> fail("Expected errandEntity to be present"));
	}

	@Test
	void findByErrandNumberAndNamespaceMissmatchOnNamespace() {
		final var errandEntities = errandsRepository.findByErrandNumberAndNamespaceAndMunicipalityId("KC-23020001", "NAMESPACE.2", MUNICIPALITY_ID);
		assertThat(errandEntities).isEmpty();
	}

	@Test
	void findByErrandNumberAndNamespaceNotFound() {
		final var errandEntities = errandsRepository.findByErrandNumberAndNamespaceAndMunicipalityId("KC-22020002", "NAMESPACE.1", MUNICIPALITY_ID);
		assertThat(errandEntities).isEmpty();
	}

	@Test
	void findByOne() {
		final var specification = hasMatchingTags(List.of(
			DbExternalTag.create().withKey("KEY-1").withValue("VALUE-1"),
			DbExternalTag.create().withKey("KEY-2").withValue("VALUE-2")));

		final var errandEntity = errandsRepository.findOne(specification);

		assertThat(errandEntity).isPresent();
		assertThat(errandEntity.get().getId()).isEqualTo("ERRAND_ID-1");

	}

	@Test
	void findByAllWithEmptyHasMatchingTags() {
		final var specification = hasMatchingTags(emptyList());

		final var errandEntity = errandsRepository.findAll(specification);

		assertThat(errandEntity)
			.hasSize(4)
			.extracting(ErrandEntity::getId)
			.containsExactlyInAnyOrder("ERRAND_ID-1", "ERRAND_ID-2", "ERRAND_ID-3", "ERRAND_ID-4");

	}

	// Label IDs from testdata-junit.sql (namespace-1 / 2281):
	// 'a0bb7b61-8d55-4857-b619-547572eed26f' = parent/child/resource1
	// '86d459cd-4810-4b4a-b365-97aa0c2c0ff5' = parent/child/resource2

	@Test
	void findDistinctIdsByLabelsMetadataLabelIdIn_noMatch() {
		assertThat(errandsRepository.findDistinctIdsByLabelsMetadataLabelIdIn(List.of("non-existent-label-id"))).isEmpty();
	}

	@Test
	@DisplayName("Verification that an errand tagged with more than one of the given label ids is still counted once")
	void findDistinctIdsByLabelsMetadataLabelIdIn_matchesEitherLabelAndDedupes() {
		final var labelIdA = "a0bb7b61-8d55-4857-b619-547572eed26f";
		final var labelIdB = "86d459cd-4810-4b4a-b365-97aa0c2c0ff5";
		final var taggedWithBoth = errandsRepository.save(errandWithLabels("errand-ids-1", List.of(labelIdA, labelIdB)));
		final var taggedWithOne = errandsRepository.save(errandWithLabel("errand-ids-2", labelIdA));

		assertThat(errandsRepository.findDistinctIdsByLabelsMetadataLabelIdIn(List.of(labelIdA, labelIdB)))
			.containsExactlyInAnyOrder(taggedWithBoth.getId(), taggedWithOne.getId());
	}

	private ErrandEntity errandWithLabel(final String errandNumber, final String labelId) {
		return errandWithLabels(errandNumber, List.of(labelId));
	}

	private ErrandEntity errandWithLabels(final String errandNumber, final List<String> labelIds) {
		return ErrandEntity.create()
			.withNamespace("namespace-1")
			.withMunicipalityId(MUNICIPALITY_ID)
			.withErrandNumber(errandNumber)
			.withLabels(labelIds.stream().map(id -> ErrandLabelEmbeddable.create().withMetadataLabelId(id)).toList());
	}
}
