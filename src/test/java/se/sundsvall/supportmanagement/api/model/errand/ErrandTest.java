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

import se.sundsvall.supportmanagement.api.model.parameter.ErrandParameter;
import se.sundsvall.supportmanagement.integration.db.model.ContactReasonEntity;

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
		final var category = "category";
		final var created = OffsetDateTime.now();
		final var stakeholder = Stakeholder.create().withExternalId("id").withExternalIdType("type");
		final var externalTags = List.of(ExternalTag.create().withKey("externalTagkey").withValue("externalTagValue"));
		final var parameters = List.of(ErrandParameter.create().withName("name").withValue("value"));
		final var id = randomUUID().toString();
		final var modified = OffsetDateTime.now().plusDays(1);
		final var priority = Priority.MEDIUM;
		final var reporterUserId = "reporterUserId";
		final var status = "status";
		final var title = "title";
		final var touched = OffsetDateTime.now().plusDays(2);
		final var type = "type";
		final var resolution = "resolution";
		final var description = "description";
		final var escalationEmail = "escalation@email.com";
		final var errandNumber = "errandNumber";
		final var suspension = Suspension.create().withSuspendedFrom(OffsetDateTime.now()).withSuspendedTo(OffsetDateTime.now().plusDays(1));
		final var businessRelated = true;
		final var contactReason = ContactReasonEntity.create().withReason("reason");

		final var bean = Errand.create()
			.withAssignedGroupId(assignedGroupId)
			.withAssignedUserId(assignedUserId)
			.withClassification(Classification.create().withCategory(category).withType(type))
			.withCreated(created)
			.withStakeholders(List.of(stakeholder))
			.withExternalTags(externalTags)
			.withParameters(parameters)
			.withId(id)
			.withModified(modified)
			.withPriority(priority)
			.withReporterUserId(reporterUserId)
			.withStatus(status)
			.withTitle(title)
			.withErrandNumber(errandNumber)
			.withTouched(touched)
			.withResolution(resolution)
			.withDescription(description)
			.withEscalationEmail(escalationEmail)
			.withSuspension(suspension)
			.withBusinessRelated(businessRelated)
			.withContactReason(contactReason.getReason());

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getAssignedGroupId()).isEqualTo(assignedGroupId);
		assertThat(bean.getAssignedUserId()).isEqualTo(assignedUserId);
		assertThat(bean.getClassification().getCategory()).isEqualTo(category);
		assertThat(bean.getClassification().getType()).isEqualTo(type);
		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getStakeholders()).containsExactly(stakeholder);
		assertThat(bean.getExternalTags()).isEqualTo(externalTags);
		assertThat(bean.getParameters()).isEqualTo(parameters);
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getModified()).isEqualTo(modified);
		assertThat(bean.getPriority()).isEqualTo(priority);
		assertThat(bean.getReporterUserId()).isEqualTo(reporterUserId);
		assertThat(bean.getStatus()).isEqualTo(status);
		assertThat(bean.getTitle()).isEqualTo(title);
		assertThat(bean.getTouched()).isEqualTo(touched);
		assertThat(bean.getResolution()).isEqualTo(resolution);
		assertThat(bean.getDescription()).isEqualTo(description);
		assertThat(bean.getEscalationEmail()).isEqualTo(escalationEmail);
		assertThat(bean.getErrandNumber()).isEqualTo(errandNumber);
		assertThat(bean.getSuspension()).isEqualTo(suspension);
		assertThat(bean.getBusinessRelated()).isEqualTo(businessRelated);
		assertThat(bean.getContactReason()).isEqualTo(contactReason.getReason());
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Errand.create()).hasAllNullFieldsOrProperties();
		assertThat(new Errand()).hasAllNullFieldsOrProperties();
	}
}
