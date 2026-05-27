package se.sundsvall.supportmanagement.api.model.metadata;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
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
import static org.hamcrest.MatcherAssert.assertThat;

class PhaseTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(Phase.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {

		final var id = "id";
		final var name = "INVESTIGATION";
		final var displayName = "Utredning";
		final var deprecated = true;
		final var description = "Fas för utredning";
		final var phaseOrder = 0;
		final var allowedStatuses = List.of("IN_PROGRESS", "WAITING");
		final var transitions = List.of(PhaseTransition.create().withId("transitionId"));
		final var created = OffsetDateTime.now();
		final var modified = OffsetDateTime.now().plusDays(1);

		final var bean = Phase.create()
			.withId(id)
			.withName(name)
			.withDisplayName(displayName)
			.withDeprecated(deprecated)
			.withDescription(description)
			.withPhaseOrder(phaseOrder)
			.withAllowedStatuses(allowedStatuses)
			.withTransitions(transitions)
			.withCreated(created)
			.withModified(modified);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getName()).isEqualTo(name);
		assertThat(bean.getDisplayName()).isEqualTo(displayName);
		assertThat(bean.getDeprecated()).isEqualTo(deprecated);
		assertThat(bean.getDescription()).isEqualTo(description);
		assertThat(bean.getPhaseOrder()).isEqualTo(phaseOrder);
		assertThat(bean.getAllowedStatuses()).isEqualTo(allowedStatuses);
		assertThat(bean.getTransitions()).isEqualTo(transitions);
		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Phase.create()).hasAllNullFieldsOrProperties();
		assertThat(new Phase()).hasAllNullFieldsOrProperties();
	}
}
