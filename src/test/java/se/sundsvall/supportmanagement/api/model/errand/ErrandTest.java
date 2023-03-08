package se.sundsvall.supportmanagement.api.model.errand;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ErrandTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(Errand.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var assignedGroupId = "assignedGroupId";
		final var assignedUserId = "assignedUserId";
		final var categoryTag = "categoryTag";
		final var created = OffsetDateTime.now();
		final var stakeholder = Stakeholder.create().withExternalId("id").withexternalIdTypeTag("type");
		final var externalTags = List.of(ExternalTag.create().withKey("externalTagkey").withValue("externalTagValue"));
		final var id = randomUUID().toString();
		final var modified = OffsetDateTime.now().plusDays(1);
		final var priority = Priority.MEDIUM;
		final var reporterUserId = "reporterUserId";
		final var statusTag = "statusTag";
		final var title = "title";
		final var touched = OffsetDateTime.now().plusDays(2);
		final var typeTag = "typeTag";
		final var resolution = "resolution";
		final var description = "description";

		final var bean = Errand.create()
			.withAssignedGroupId(assignedGroupId)
			.withAssignedUserId(assignedUserId)
			.withCategoryTag(categoryTag)
			.withCreated(created)
			.withStakeholders(List.of(stakeholder))
			.withExternalTags(externalTags)
			.withId(id)
			.withModified(modified)
			.withPriority(priority)
			.withReporterUserId(reporterUserId)
			.withStatusTag(statusTag)
			.withTitle(title)
			.withTouched(touched)
			.withTypeTag(typeTag)
			.withResolution(resolution)
			.withDescription(description);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getAssignedGroupId()).isEqualTo(assignedGroupId);
		assertThat(bean.getAssignedUserId()).isEqualTo(assignedUserId);
		assertThat(bean.getCategoryTag()).isEqualTo(categoryTag);
		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getStakeholders()).containsExactly(stakeholder);
		assertThat(bean.getExternalTags()).isEqualTo(externalTags);
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getModified()).isEqualTo(modified);
		assertThat(bean.getPriority()).isEqualTo(priority);
		assertThat(bean.getReporterUserId()).isEqualTo(reporterUserId);
		assertThat(bean.getStatusTag()).isEqualTo(statusTag);
		assertThat(bean.getTitle()).isEqualTo(title);
		assertThat(bean.getTouched()).isEqualTo(touched);
		assertThat(bean.getTypeTag()).isEqualTo(typeTag);
		assertThat(bean.getResolution()).isEqualTo(resolution);
		assertThat(bean.getDescription()).isEqualTo(description);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Errand.create()).hasAllNullFieldsOrProperties();
	}
}
