package se.sundsvall.supportmanagement.api.model.process;

import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.WARN;

class ProcessActivityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void bean() {
		MatcherAssert.assertThat(ProcessActivity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builder() {
		final var occurredAt = now();
		final var created = now().plusSeconds(1);

		final var activity = ProcessActivity.create()
			.withId("id")
			.withProcessInstanceId("processInstanceId")
			.withActivityType("PHASE")
			.withActivityId("review_phase")
			.withActivityName("Granskning")
			.withSeverity(WARN)
			.withMessage("message")
			.withErrorCode("errorCode")
			.withOccurredAt(occurredAt)
			.withCreated(created);

		assertThat(activity.getId()).isEqualTo("id");
		assertThat(activity.getProcessInstanceId()).isEqualTo("processInstanceId");
		assertThat(activity.getActivityType()).isEqualTo("PHASE");
		assertThat(activity.getActivityId()).isEqualTo("review_phase");
		assertThat(activity.getActivityName()).isEqualTo("Granskning");
		assertThat(activity.getSeverity()).isEqualTo(WARN);
		assertThat(activity.getMessage()).isEqualTo("message");
		assertThat(activity.getErrorCode()).isEqualTo("errorCode");
		assertThat(activity.getOccurredAt()).isEqualTo(occurredAt);
		assertThat(activity.getCreated()).isEqualTo(created);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(ProcessActivity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ProcessActivity()).hasAllNullFieldsOrProperties();
	}
}
