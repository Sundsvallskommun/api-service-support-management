package se.sundsvall.supportmanagement.integration.db;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.OffsetDateTime;
import java.util.List;

import com.turkraft.springfilter.converter.FilterSpecificationConverter;
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
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;

/**
 * Tag repository tests.
 *
 * @see /src/test/resources/db/testdata.sql for data setup.
 */
@SpringBootTest
@ActiveProfiles("junit")
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
@Transactional
class ErrandsRepositoryTest {

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
		final var municipalityId = "municipalityId";
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
		assertThat(errandEntity.get().getStakeholders().get(0).getContactChannels()).hasSize(1);
		assertThat(errandEntity.get().getStakeholders().get(0).getContactChannels())
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
	void findByFilter(String filter) {

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
	void findByErrandNumber() {
		final var errandEntities = errandsRepository.findByErrandNumber("KC-23020001");
		assertThat(errandEntities).isNotNull();
		errandEntities.ifPresentOrElse(
			errandEntity -> assertThat(errandEntity.getId()).isEqualTo("ERRAND_ID-1"),
			() -> fail("Expected errandEntity to be present")
		);
	}

	@Test
	void findByErrandNumberNotFound() {
		final var errandEntities = errandsRepository.findByErrandNumber("KC-22020002");
		assertThat(errandEntities).isEmpty();
	}

	@Test
	void findByExternalTagValue() {
		final var errandEntity = errandsRepository.findByExternalTagsValue("VALUE-1").orElse(null);

		assertThat(errandEntity).isNotNull();
		assertThat(errandEntity.getId()).isNotNull();
		assertThat(errandEntity.getExternalTags()).anyMatch(val -> val.getValue().equals("VALUE-1"));
	}
}
