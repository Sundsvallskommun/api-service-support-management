package se.sundsvall.supportmanagement.integration.db.model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Random;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class EmailWorkerConfigEntityTest {

	@BeforeAll
	static void setup() {
		final var random = new Random();
		registerValueGenerator(() -> now().plusDays(random.nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(EmailWorkerConfigEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}
	@Test
	void hasValidBuilderMethods() {
		final var id = 1L;
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var enabled = true;
		final var errandClosedEmailSender = "noreply@email.se";
		final var errandClosedEmailTemplate = "This is an email";
		final var daysOfInactivityBeforeReject = 3;
		final var statusForNew = "NEW";
		final var triggerStatusChangeOn = "SOLVED";
		final var statusChangeTo = "OPEN";
		final var inactiveStatus = "CLOSED";
		final var addSenderAsStakeholder = true;
		final var stakeholderRole = "stakeholderRole";
		final var errandChannel = "errandChannel";
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();

		final var entity = EmailWorkerConfigEntity.create()
			.withId(id)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withEnabled(enabled)
			.withErrandClosedEmailSender(errandClosedEmailSender)
			.withErrandClosedEmailTemplate(errandClosedEmailTemplate)
			.withDaysOfInactivityBeforeReject(daysOfInactivityBeforeReject)
			.withStatusForNew(statusForNew)
			.withTriggerStatusChangeOn(triggerStatusChangeOn)
			.withStatusChangeTo(statusChangeTo)
			.withInactiveStatus(inactiveStatus)
			.withAddSenderAsStakeholder(addSenderAsStakeholder)
			.withStakeholderRole(stakeholderRole)
			.withErrandChannel(errandChannel)
			.withCreated(created)
			.withModified(modified);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getEnabled()).isEqualTo(enabled);
		assertThat(entity.getErrandClosedEmailSender()).isEqualTo(errandClosedEmailSender);
		assertThat(entity.getErrandClosedEmailTemplate()).isEqualTo(errandClosedEmailTemplate);
		assertThat(entity.getDaysOfInactivityBeforeReject()).isEqualTo(daysOfInactivityBeforeReject);
		assertThat(entity.getStatusForNew()).isEqualTo(statusForNew);
		assertThat(entity.getTriggerStatusChangeOn()).isEqualTo(triggerStatusChangeOn);
		assertThat(entity.getStatusChangeTo()).isEqualTo(statusChangeTo);
		assertThat(entity.getInactiveStatus()).isEqualTo(inactiveStatus);
		assertThat(entity.isAddSenderAsStakeholder()).isTrue();
		assertThat(entity.getStakeholderRole()).isEqualTo(stakeholderRole);
		assertThat(entity.getErrandChannel()).isEqualTo(errandChannel);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getModified()).isEqualTo(modified);
	}

	@Test
	void testOnCreate() {
		final var entity = EmailWorkerConfigEntity.create();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created", "enabled", "addSenderAsStakeholder");
	}

	@Test
	void testOnUpdate() {
		final var entity = EmailWorkerConfigEntity.create();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified", "enabled", "addSenderAsStakeholder");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(EmailWorkerConfigEntity.create()).hasAllNullFieldsOrPropertiesExcept("enabled", "addSenderAsStakeholder");
		assertThat(new EmailWorkerConfigEntity()).hasAllNullFieldsOrPropertiesExcept("enabled", "addSenderAsStakeholder");
	}
}
