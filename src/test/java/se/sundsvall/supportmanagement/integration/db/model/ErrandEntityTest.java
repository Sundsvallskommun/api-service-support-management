package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.google.code.beanmatchers.BeanMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mariadb.jdbc.MariaDbBlob;


class ErrandEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {

		assertThat(ErrandEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var now = OffsetDateTime.now();

		final var assignedUserId = "assignedUserId";
		final var assignedGroupId = "assignedGroupId";
		final var attachmentId = UUID.randomUUID().toString();
		final var attachments = List.of(AttachmentEntity.create().withId(attachmentId).withFileName("fileName").withAttachmentData(AttachmentDataEntity.create().withFile(new MariaDbBlob("file".getBytes()))).withMimeType("mimeType"));
		final var category = "category";
		final var namespace = "namespace";
		final var stakeholder = StakeholderEntity.create().withExternalId(UUID.randomUUID().toString()).withExternalIdType("PRIVATE");
		final var description = "description";
		final var externalTags = List.of(DbExternalTag.create().withKey("key").withValue("value"));
		final var id = UUID.randomUUID().toString();
		final var municipalityId = "municipalityId";
		final var priority = "priority";
		final var reporterUserId = "reporterUserId";
		final var resolution = "resolution";
		final var status = "status";
		final var title = "title";
		final var type = "type";
		final var escalationEmail = "escalation@email.com";
		final var errandNumber = "errandNumber";
		final var parameters = List.of(ParameterEntity.create());
		final var businessRelated = true;
		final var contactReason = ContactReasonEntity.create().withReason("reason");

		final var errandEntity = ErrandEntity.create()
			.withAssignedGroupId(assignedGroupId)
			.withAssignedUserId(assignedUserId)
			.withAttachments(attachments)
			.withCategory(category)
			.withNamespace(namespace)
			.withStakeholders(List.of(stakeholder))
			.withDescription(description)
			.withExternalTags(externalTags)
			.withId(id)
			.withMunicipalityId(municipalityId)
			.withPriority(priority)
			.withReporterUserId(reporterUserId)
			.withResolution(resolution)
			.withStatus(status)
			.withTitle(title)
			.withType(type)
			.withParameters(parameters)
			.withEscalationEmail(escalationEmail)
			.withBusinessRelated(businessRelated)
			.withContactReason(contactReason)
			.withErrandNumber(errandNumber)
			.withTouched(now)
			.withCreated(now)
			.withModified(now)
			.withSuspendedFrom(now)
			.withSuspendedTo(now);

		assertThat(errandEntity).hasNoNullFieldsOrProperties();
		assertThat(errandEntity.getAssignedGroupId()).isEqualTo(assignedGroupId);
		assertThat(errandEntity.getAssignedUserId()).isEqualTo(assignedUserId);
		assertThat(errandEntity.getAttachments()).isEqualTo(attachments);
		assertThat(errandEntity.getCategory()).isEqualTo(category);
		assertThat(errandEntity.getNamespace()).isEqualTo(namespace);
		assertThat(errandEntity.getStakeholders()).containsExactly(stakeholder);
		assertThat(errandEntity.getDescription()).isEqualTo(description);
		assertThat(errandEntity.getExternalTags()).isEqualTo(externalTags);
		assertThat(errandEntity.getId()).isEqualTo(id);
		assertThat(errandEntity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(errandEntity.getPriority()).isEqualTo(priority);
		assertThat(errandEntity.getReporterUserId()).isEqualTo(reporterUserId);
		assertThat(errandEntity.getResolution()).isEqualTo(resolution);
		assertThat(errandEntity.getStatus()).isEqualTo(status);
		assertThat(errandEntity.getTitle()).isEqualTo(title);
		assertThat(errandEntity.getType()).isEqualTo(type);
		assertThat(errandEntity.getParameters()).isEqualTo(parameters);
		assertThat(errandEntity.getEscalationEmail()).isEqualTo(escalationEmail);
		assertThat(errandEntity.getErrandNumber()).isEqualTo(errandNumber);
		assertThat(errandEntity.getBusinessRelated()).isEqualTo(businessRelated);
		assertThat(errandEntity.getContactReason()).isEqualTo(contactReason);
		assertThat(errandEntity).extracting(ErrandEntity::getModified,
			ErrandEntity::getTouched, ErrandEntity::getSuspendedFrom, ErrandEntity::getSuspendedTo,
			ErrandEntity::getCreated).allSatisfy(date -> assertThat(date).isEqualTo(now));
	}

	@Test
	void testOnCreate() {
		final var entity = new ErrandEntity().withStakeholders(List.of(StakeholderEntity.create()));
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity.getStakeholders().getFirst().getErrandEntity()).isSameAs(entity);
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created", "stakeholders");
	}

	@Test
	void testOnUpdate() {
		final var entity = new ErrandEntity().withStakeholders(List.of(StakeholderEntity.create()));
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity.getStakeholders().getFirst().getErrandEntity()).isSameAs(entity);
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified", "stakeholders");
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(ErrandEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandEntity()).hasAllNullFieldsOrProperties();
	}
}
