package se.sundsvall.supportmanagement.api.model.config;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EmailIntegrationTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(EmailIntegration.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {

		final var enabled = true;
		final var errandClosedEmailSender = "errandClosedEmailSender";
		final var errandClosedEmailTemplate = "errandClosedEmailTemplate";
		final var errandNewEmailSender = "errandNewEmailSender";
		final var errandNewEmailTemplate = "errandNewEmailTemplate";
		final var daysOfInactivityBeforeReject = 5;
		final var statusForNew = "statusForNew";
		final var triggerStatusChangeOn = "triggerStatusChangeOn";
		final var statusChangeTo = "statusChangeTo";
		final var inactiveStatus = "inactiveStatus";
		final var addSenderAsStakeholder = true;
		final var stakeholderRole = "stakeholderRole";
		final var errandChannel = "errandChannel";
		final var created = OffsetDateTime.now();
		final var modified = OffsetDateTime.now().plusDays(1);

		final var bean = EmailIntegration.create()
			.withEnabled(enabled)
			.withErrandClosedEmailSender(errandClosedEmailSender)
			.withErrandClosedEmailTemplate(errandClosedEmailTemplate)
			.withErrandNewEmailSender(errandNewEmailSender)
			.withErrandNewEmailTemplate(errandNewEmailTemplate)
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

		assertThat(bean.getEnabled()).isTrue();
		assertThat(bean.getErrandClosedEmailSender()).isEqualTo(errandClosedEmailSender);
		assertThat(bean.getErrandClosedEmailTemplate()).isEqualTo(errandClosedEmailTemplate);
		assertThat(bean.getErrandNewEmailSender()).isEqualTo(errandNewEmailSender);
		assertThat(bean.getErrandNewEmailTemplate()).isEqualTo(errandNewEmailTemplate);
		assertThat(bean.getDaysOfInactivityBeforeReject()).isEqualTo(daysOfInactivityBeforeReject);
		assertThat(bean.getStatusForNew()).isEqualTo(statusForNew);
		assertThat(bean.getTriggerStatusChangeOn()).isEqualTo(triggerStatusChangeOn);
		assertThat(bean.getStatusChangeTo()).isEqualTo(statusChangeTo);
		assertThat(bean.getInactiveStatus()).isEqualTo(inactiveStatus);
		assertThat(bean.getAddSenderAsStakeholder()).isTrue();
		assertThat(bean.getStakeholderRole()).isEqualTo(stakeholderRole);
		assertThat(bean.getErrandChannel()).isEqualTo(errandChannel);
		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(EmailIntegration.create()).hasAllNullFieldsOrProperties();
		assertThat(new EmailIntegration()).hasAllNullFieldsOrProperties();
	}
}
