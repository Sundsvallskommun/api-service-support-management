package se.sundsvall.supportmanagement.integration.db;

import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.ContactChannelEntity;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.fail;
import static se.sundsvall.supportmanagement.integration.db.specification.ErrandSpecification.hasMatchingTags;

/**
 * Errands repository tests.
 *
 * @see <a href="file:src/test/resources/db/testdata.sql">src/test/resources/db/testdata.sql</a> for data setup.
 */
@SpringBootTest // Needs to be a SpringBootTest as turkraft components are used in test
@ActiveProfiles("junit")
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
@Transactional
class ErrandsRepositoryTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Autowired
	private ErrandsRepository errandsRepository;

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
	void countByLabelsMetadataLabelId_noMatch() {
		assertThat(errandsRepository.countByLabelsMetadataLabelId("non-existent-label-id")).isZero();
	}

	@Test
	void countByLabelsMetadataLabelId_oneErrand() {
		final var labelId = "a0bb7b61-8d55-4857-b619-547572eed26f";
		errandsRepository.save(errandWithLabel("errand-count-1", labelId));

		assertThat(errandsRepository.countByLabelsMetadataLabelId(labelId)).isOne();
	}

	@Test
	void countByLabelsMetadataLabelId_multipleErrands() {
		final var labelId = "a0bb7b61-8d55-4857-b619-547572eed26f";
		errandsRepository.save(errandWithLabel("errand-count-2", labelId));
		errandsRepository.save(errandWithLabel("errand-count-3", labelId));

		assertThat(errandsRepository.countByLabelsMetadataLabelId(labelId)).isEqualTo(2);
	}

	@Test
	void findAllByLabelsMetadataLabelId_noMatch() {
		assertThat(errandsRepository.findAllByLabelsMetadataLabelId("non-existent-label-id")).isEmpty();
	}

	@Test
	void findAllByLabelsMetadataLabelId_oneErrand() {
		final var labelId = "a0bb7b61-8d55-4857-b619-547572eed26f";
		final var saved = errandsRepository.save(errandWithLabel("errand-find-1", labelId));

		assertThat(errandsRepository.findAllByLabelsMetadataLabelId(labelId))
			.hasSize(1)
			.extracting(ErrandEntity::getId)
			.containsExactly(saved.getId());
	}

	@Test
	void findAllByLabelsMetadataLabelId_multipleErrands() {
		final var labelId = "a0bb7b61-8d55-4857-b619-547572eed26f";
		final var saved1 = errandsRepository.save(errandWithLabel("errand-find-2", labelId));
		final var saved2 = errandsRepository.save(errandWithLabel("errand-find-3", labelId));

		assertThat(errandsRepository.findAllByLabelsMetadataLabelId(labelId))
			.hasSize(2)
			.extracting(ErrandEntity::getId)
			.containsExactlyInAnyOrder(saved1.getId(), saved2.getId());
	}

	private ErrandEntity errandWithLabel(final String errandNumber, final String labelId) {
		return ErrandEntity.create()
			.withNamespace("namespace-1")
			.withMunicipalityId(MUNICIPALITY_ID)
			.withErrandNumber(errandNumber)
			.withLabels(List.of(ErrandLabelEmbeddable.create().withMetadataLabelId(labelId)));
	}
}
