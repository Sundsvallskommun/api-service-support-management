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

import se.sundsvall.supportmanagement.api.model.errand.CustomerType;

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

		final var id = UUID.randomUUID().toString();
		final var attachmentId = UUID.randomUUID().toString();
		final var externalTags = List.of(DbExternalTag.create().withKey("key").withValue("value"));
		final var customer = EmbeddableCustomer.create().withId(id).withType(CustomerType.PRIVATE.toString());
		final var clientIdTag = "clientIdTag";
		final var title = "title";
		final var categoryTag = "categoryTag";
		final var typeTag = "typeTag";
		final var statusTag = "statusTag";
		final var priority = "priority";
		final var reporterUserId = "reporterUserId";
		final var assignedUserId = "assignedUserId";
		final var assignedGroupId = "assignedGroupId";
		final var attachments = List.of(AttachmentEntity.create().withId(attachmentId).withFileName("fileName").withFile("file".getBytes()).withMimeType("mimeType"));
		final var created = now();
		final var updated = now().plusDays(1);

		final var errandEntity = ErrandEntity.create()
			.withId(id)
			.withExternalTags(externalTags)
			.withCustomer(customer)
			.withClientIdTag(clientIdTag)
			.withTitle(title)
			.withCategoryTag(categoryTag)
			.withTypeTag(typeTag)
			.withStatusTag(statusTag)
			.withPriority(priority)
			.withReporterUserId(reporterUserId)
			.withAssignedUserId(assignedUserId)
			.withAssignedGroupId(assignedGroupId)
			.withAttachments(attachments)
			.withCreated(created)
			.withModified(updated);

		assertThat(errandEntity).hasNoNullFieldsOrProperties();
		assertThat(errandEntity.getId()).isEqualTo(id);
		assertThat(errandEntity.getExternalTags()).isEqualTo(externalTags);
		assertThat(errandEntity.getCustomer()).isEqualTo(customer);
		assertThat(errandEntity.getClientIdTag()).isEqualTo(clientIdTag);
		assertThat(errandEntity.getTitle()).isEqualTo(title);
		assertThat(errandEntity.getCategoryTag()).isEqualTo(categoryTag);
		assertThat(errandEntity.getTypeTag()).isEqualTo(typeTag);
		assertThat(errandEntity.getStatusTag()).isEqualTo(statusTag);
		assertThat(errandEntity.getPriority()).isEqualTo(priority);
		assertThat(errandEntity.getReporterUserId()).isEqualTo(reporterUserId);
		assertThat(errandEntity.getAssignedUserId()).isEqualTo(assignedUserId);
		assertThat(errandEntity.getAssignedGroupId()).isEqualTo(assignedGroupId);
		assertThat(errandEntity.getAttachments()).isEqualTo(attachments);
		assertThat(errandEntity.getCreated()).isEqualTo(created);
		assertThat(errandEntity.getModified()).isEqualTo(updated);
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
