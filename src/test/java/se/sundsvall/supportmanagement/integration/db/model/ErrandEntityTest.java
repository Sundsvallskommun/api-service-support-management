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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.code.beanmatchers.BeanMatchers;

import se.sundsvall.supportmanagement.api.model.errand.StakeholderType;

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

		final var assignedUserId = "assignedUserId";
		final var assignedGroupId = "assignedGroupId";
		final var attachmentId = UUID.randomUUID().toString();
		final var attachments = List.of(AttachmentEntity.create().withId(attachmentId).withFileName("fileName").withFile("file".getBytes()).withMimeType("mimeType"));
		final var categoryTag = "categoryTag";
		final var namespace = "namespace";
		final var created = now();
		final var stakeholder = StakeholderEntity.create().withStakeholderId(UUID.randomUUID().toString()).withType(StakeholderType.PRIVATE.toString());
		final var description = "description";
		final var externalTags = List.of(DbExternalTag.create().withKey("key").withValue("value"));
		final var id = UUID.randomUUID().toString();
		final var modified = now().plusDays(1);
		final var municipalityId = "municipalityId";
		final var priority = "priority";
		final var reporterUserId = "reporterUserId";
		final var resolution = "resolution";
		final var statusTag = "statusTag";
		final var title = "title";
		final var touched = now().plusDays(2);
		final var typeTag = "typeTag";

		final var errandEntity = ErrandEntity.create()
			.withAssignedGroupId(assignedGroupId)
			.withAssignedUserId(assignedUserId)
			.withAttachments(attachments)
			.withCategoryTag(categoryTag)
			.withNamespace(namespace)
			.withCreated(created)
			.withStakeholders(List.of(stakeholder))
			.withDescription(description)
			.withExternalTags(externalTags)
			.withId(id)
			.withModified(modified)
			.withMunicipalityId(municipalityId)
			.withPriority(priority)
			.withReporterUserId(reporterUserId)
			.withResolution(resolution)
			.withStatusTag(statusTag)
			.withTitle(title)
			.withTouched(touched)
			.withTypeTag(typeTag);

		assertThat(errandEntity).hasNoNullFieldsOrProperties();
		assertThat(errandEntity.getAssignedGroupId()).isEqualTo(assignedGroupId);
		assertThat(errandEntity.getAssignedUserId()).isEqualTo(assignedUserId);
		assertThat(errandEntity.getAttachments()).isEqualTo(attachments);
		assertThat(errandEntity.getCategoryTag()).isEqualTo(categoryTag);
		assertThat(errandEntity.getNamespace()).isEqualTo(namespace);
		assertThat(errandEntity.getCreated()).isEqualTo(created);
		assertThat(errandEntity.getStakeholders()).containsExactly(stakeholder);
		assertThat(errandEntity.getDescription()).isEqualTo(description);
		assertThat(errandEntity.getExternalTags()).isEqualTo(externalTags);
		assertThat(errandEntity.getId()).isEqualTo(id);
		assertThat(errandEntity.getModified()).isEqualTo(modified);
		assertThat(errandEntity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(errandEntity.getPriority()).isEqualTo(priority);
		assertThat(errandEntity.getReporterUserId()).isEqualTo(reporterUserId);
		assertThat(errandEntity.getResolution()).isEqualTo(resolution);
		assertThat(errandEntity.getStatusTag()).isEqualTo(statusTag);
		assertThat(errandEntity.getTitle()).isEqualTo(title);
		assertThat(errandEntity.getTouched()).isEqualTo(touched);
		assertThat(errandEntity.getTypeTag()).isEqualTo(typeTag);
	}

	@Test
	void testOnCreate() {
		final var entity = new ErrandEntity();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created");
	}

	@Test
	void testOnUpdate() {
		final var entity = new ErrandEntity();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified");
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(ErrandEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandEntity()).hasAllNullFieldsOrProperties();
	}
}
