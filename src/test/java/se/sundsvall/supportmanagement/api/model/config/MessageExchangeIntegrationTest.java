package se.sundsvall.supportmanagement.api.model.config;

import java.time.OffsetDateTime;
import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class MessageExchangeIntegrationTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(MessageExchangeIntegration.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var triggerStatusChangeOn = "SOLVED";
		final var statusChangeTo = "OPEN";
		final var created = now().minusDays(1);
		final var modified = now();

		final var config = MessageExchangeIntegration.create()
			.withTriggerStatusChangeOn(triggerStatusChangeOn)
			.withStatusChangeTo(statusChangeTo)
			.withCreated(created)
			.withModified(modified);

		Assertions.assertThat(config).hasNoNullFieldsOrProperties();
		Assertions.assertThat(config.getTriggerStatusChangeOn()).isEqualTo(triggerStatusChangeOn);
		Assertions.assertThat(config.getStatusChangeTo()).isEqualTo(statusChangeTo);
		Assertions.assertThat(config.getCreated()).isEqualTo(created);
		Assertions.assertThat(config.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		Assertions.assertThat(MessageExchangeIntegration.create()).hasAllNullFieldsOrProperties();
		Assertions.assertThat(new MessageExchangeIntegration()).hasAllNullFieldsOrProperties();
	}
}
