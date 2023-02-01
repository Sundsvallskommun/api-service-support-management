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
import se.sundsvall.supportmanagement.api.model.errand.CustomerType;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.EmbeddableCustomer;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

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
		final var customer = EmbeddableCustomer.create().withId("id").withType(CustomerType.EMPLOYEE.toString()	);
		final var clientIdTag = "clientIdTag";
		final var title = "title";
		final var categoryTag = "categoryTag";
		final var typeTag = "typeTag";
		final var statusTag = "statusTag";
		final var priority = "priority";
		final var reporterUserId = "reporterUserId";
		final var assignedUserId = "assignedUserId";
		final var assignedGroupId = "assignedGroupId";

		var errandEntity = ErrandEntity.create()
			.withClientIdTag(clientIdTag)
			.withTitle(title)
			.withCategoryTag(categoryTag)
			.withTypeTag(typeTag)
			.withStatusTag(statusTag)
			.withPriority(priority)
			.withReporterUserId(reporterUserId)
			.withAssignedUserId(assignedUserId)
			.withAssignedGroupId(assignedGroupId)
			.withExternalTags(List.of(externalTag))
			.withCustomer(customer);

		// Execution
		final var persistedEntity = errandsRepository.save(errandEntity);

		assertThat(persistedEntity).isNotNull();
		assertThat(persistedEntity.getId()).isNotNull();
		assertThat(persistedEntity.getClientIdTag()).isEqualTo(clientIdTag);
		assertThat(persistedEntity.getTitle()).isEqualTo(title);
		assertThat(persistedEntity.getCategoryTag()).isEqualTo(categoryTag);
		assertThat(persistedEntity.getTypeTag()).isEqualTo(typeTag);
		assertThat(persistedEntity.getStatusTag()).isEqualTo(statusTag);
		assertThat(persistedEntity.getPriority()).isEqualTo(priority);
		assertThat(persistedEntity.getReporterUserId()).isEqualTo(reporterUserId);
		assertThat(persistedEntity.getAssignedUserId()).isEqualTo(assignedUserId);
		assertThat(persistedEntity.getAssignedGroupId()).isEqualTo(assignedGroupId);
		assertThat(persistedEntity.getExternalTags()).contains(externalTag);
		assertThat(persistedEntity.getCustomer()).isEqualTo(customer);
		assertThat(persistedEntity.getCreated()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		assertThat(persistedEntity.getModified()).isNull();
	}

	@Test
	void findByAssignedGroupAndAssignedUserIdWhereExternalTagsIsNotEmpty() {

		Specification<ErrandEntity> specification = new FilterSpecification<>("(assignedGroupId : 'ASSIGNED_GROUP_ID-1' and assignedUserId : 'ASSIGNED_USER_ID-1' and externalTags is not empty)");
		Pageable pageable = PageRequest.of(0, 20);

		final var errandEntities = errandsRepository.findAll(specification, pageable);

		assertThat(errandEntities).isNotNull();
		assertThat(errandEntities.getTotalElements()).isEqualTo(2);

		assertThat(errandEntities)
			.extracting(ErrandEntity::getId, ErrandEntity::getAssignedGroupId, ErrandEntity::getAssignedUserId).containsExactlyInAnyOrder(
				tuple("ERRAND_ID-1", "ASSIGNED_GROUP_ID-1", "ASSIGNED_USER_ID-1"),
				tuple("ERRAND_ID-2", "ASSIGNED_GROUP_ID-1", "ASSIGNED_USER_ID-1"));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"(externalTags.key : 'KEY-1')",
		"(attachments.id : 'ATTACHMENT_ID-1')",
		"(customer.id : 'CUSTOMER_ID-1' and externalTags is not empty)",
		"(customer.id : 'CUSTOMER_ID-1' and attachments is not empty)"
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
	void findByDateFilter() {
		//Setup date filter
		final var dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		final var from = OffsetDateTime.now().minusMinutes(1).format(dateFormat);
		final var to = OffsetDateTime.now().plusMinutes(1).format(dateFormat);
		final var filter = String.format("(created > '%s' AND created < '%s')", from, to);

		final var specification = new FilterSpecification<ErrandEntity>(filter);
		final var pageable = PageRequest.of(0, 20);

		//Setup ErrandEntity to find
		final var entityToUpdate = errandsRepository.findById("ERRAND_ID-3");
		entityToUpdate.get().setCreated(OffsetDateTime.now());
		errandsRepository.save(entityToUpdate.get());

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
}