package se.sundsvall.supportmanagement.integration.db;

import com.turkraft.springfilter.boot.FilterSpecification;
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
import se.sundsvall.supportmanagement.integration.db.model.ContactChannelEntity;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;

import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tag repository tests.
 *
 * @see /src/test/resources/db/testdata.sql for data setup.
 */
@SpringBootTest
@ActiveProfiles("junit")
@Sql(scripts ={
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class ErrandsRepositoryTest {

	@Autowired
	private ErrandsRepository errandsRepository;

	@Test
	void create() {
		final var externalTag = DbExternalTag.create().withKey("key").withValue("value");
		final var stakeholder = StakeholderEntity.create().withExternalId("id").withExternalIdTypeTag("EMPLOYEE").withRole("ROLE").withContactChannels(List.of(ContactChannelEntity.create().withType("type").withValue("value")));
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

		var errandEntity = ErrandEntity.create()
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
			.withEscalationEmail(escalationEmail);

		// Execution
		final var persistedEntity = errandsRepository.save(errandEntity);

		assertThat(persistedEntity).isNotNull();
		assertThat(persistedEntity.getId()).isNotNull();
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

		Specification<ErrandEntity> specification = new FilterSpecification<>("(assignedGroupId : 'ASSIGNED_GROUP_ID-1' and assignedUserId : 'ASSIGNED_USER_ID-1' and externalTags is not empty)");
		Pageable pageable = PageRequest.of(0, 20);

		final var errandEntities = errandsRepository.findAll(specification, pageable);

		assertThat(errandEntities).isNotNull();
		assertThat(errandEntities.getTotalElements()).isEqualTo(2);

		assertThat(errandEntities)
			.extracting(ErrandEntity::getId, ErrandEntity::getAssignedGroupId, ErrandEntity::getAssignedUserId, ErrandEntity::getEscalationEmail).containsExactlyInAnyOrder(
				tuple("ERRAND_ID-1", "ASSIGNED_GROUP_ID-1", "ASSIGNED_USER_ID-1", "ESCALATION_EMAIL_1"),
				tuple("ERRAND_ID-2", "ASSIGNED_GROUP_ID-1", "ASSIGNED_USER_ID-1", "ESCALATION_EMAIL_2"));
	}

	@Test
	@Transactional
	void errandWithStakeholderAndContactChannel() {
		var errandEntity =  errandsRepository.findById("ERRAND_ID-1");

		assertThat(errandEntity.get().getStakeholders()).hasSize(1);
		assertThat(errandEntity.get().getStakeholders())
				.extracting(StakeholderEntity::getId, StakeholderEntity::getExternalIdTypeTag, StakeholderEntity::getExternalId, StakeholderEntity::getFirstName, StakeholderEntity::getLastName, StakeholderEntity::getAddress, StakeholderEntity::getCareOf, StakeholderEntity::getZipCode, StakeholderEntity::getCountry, StakeholderEntity::getRole)
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

		Specification<ErrandEntity> specification = new FilterSpecification<>(filter);
		Pageable pageable = PageRequest.of(0, 20);

		final var errandEntities = errandsRepository.findAll(specification, pageable);

		assertThat(errandEntities).isNotNull();
		assertThat(errandEntities.getTotalElements()).isEqualTo(1);

		assertThat(errandEntities)
			.extracting(ErrandEntity::getId, ErrandEntity::getAssignedGroupId, ErrandEntity::getAssignedUserId).containsExactlyInAnyOrder(
				tuple("ERRAND_ID-1", "ASSIGNED_GROUP_ID-1", "ASSIGNED_USER_ID-1"));
	}

	@Test
	void findByDateFilter() throws Exception {
		//Setup date filter
		final var dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		final var from = OffsetDateTime.now().minusMinutes(1).format(dateFormat);
		final var to = OffsetDateTime.now().plusMinutes(1).format(dateFormat);
		final var filter = String.format("(created > '%s' AND created < '%s')", from, to);

		final var specification = new FilterSpecification<ErrandEntity>(filter);
		final var pageable = PageRequest.of(0, 20);

		//Setup ErrandEntity to find
		final var entityToUpdate = errandsRepository.findById("ERRAND_ID-3").orElseThrow(() -> new Exception("There's something wrong with the test-data!"));
		entityToUpdate.setCreated(OffsetDateTime.now());
		errandsRepository.save(entityToUpdate);

		final var errandEntities = errandsRepository.findAll(specification, pageable);

		assertThat(errandEntities).isNotNull();
		assertThat(errandEntities.getTotalElements()).isEqualTo(1);

		assertThat(errandEntities)
			.extracting(ErrandEntity::getId, ErrandEntity::getAssignedGroupId, ErrandEntity::getAssignedUserId).containsExactlyInAnyOrder(
				tuple("ERRAND_ID-3", "ASSIGNED_GROUP_ID-3", "ASSIGNED_USER_ID-3"));
	}

	@Test
	void shouldThrowExceptionWhenDateTimeIsOfWrongFormat() {
		//Setup date filter;
		final var invalidDateFormat = "31-01-2023T16:00:20.954+01:00";
		final var filter = String.format("(created > '%s')", invalidDateFormat);

		Specification<ErrandEntity> specification = new FilterSpecification<>(filter);
		Pageable pageable = PageRequest.of(0, 20);

		final var exception = assertThrows(Throwable.class, () -> errandsRepository.findAll(specification, pageable));

		assertThat(exception).isNotNull().isInstanceOf(ClassCastException.class);
		assertThat(exception.getMessage()).isEqualTo("The input '31-01-2023T16:00:20.954+01:00' could not be parsed to OffsetDateTime");
	}

	@Test
	void update() {

		// Setup
		final var entityToUpdate = errandsRepository.findById("ERRAND_ID-3");
		final var newAssignedUserId = "ASSIGNED_USER_ID-CHANGED";

		// Execution
		entityToUpdate.get().setAssignedUserId(newAssignedUserId);
		final var updatedEntity = errandsRepository.save(entityToUpdate.get());

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
}