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
		final var customer = Customer.create().withId("id").withType(CustomerType.PRIVATE);
		final var externalTags = List.of(ExternalTag.create().withKey("externalTagkey").withValue("externalTagValue"));
		final var id = randomUUID().toString();
		final var modified = OffsetDateTime.now();
		final var clientIdTag = "clientIdTag";
		final var priority = Priority.MEDIUM;
		final var reporterUserId = "reporterUserId";
		final var statusTag = "statusTag";
		final var title = "title";
		final var typeTag = "typeTag";

		final var bean = Errand.create()
			.withAssignedGroupId(assignedGroupId)
			.withAssignedUserId(assignedUserId)
			.withCategoryTag(categoryTag)
			.withCreated(created)
			.withCustomer(customer)
			.withExternalTags(externalTags)
			.withId(id)
			.withModified(modified)
			.withClientIdTag(clientIdTag)
			.withPriority(priority)
			.withReporterUserId(reporterUserId)
			.withStatusTag(statusTag)
			.withTitle(title)
			.withTypeTag(typeTag);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getAssignedGroupId()).isEqualTo(assignedGroupId);
		assertThat(bean.getAssignedUserId()).isEqualTo(assignedUserId);
		assertThat(bean.getCategoryTag()).isEqualTo(categoryTag);
		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getCustomer()).isEqualTo(customer);
		assertThat(bean.getExternalTags()).isEqualTo(externalTags);
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getModified()).isEqualTo(modified);
		assertThat(bean.getClientIdTag()).isEqualTo(clientIdTag);
		assertThat(bean.getPriority()).isEqualTo(priority);
		assertThat(bean.getReporterUserId()).isEqualTo(reporterUserId);
		assertThat(bean.getStatusTag()).isEqualTo(statusTag);
		assertThat(bean.getTitle()).isEqualTo(title);
		assertThat(bean.getTypeTag()).isEqualTo(typeTag);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Errand.create()).hasAllNullFieldsOrProperties();
	}
}
