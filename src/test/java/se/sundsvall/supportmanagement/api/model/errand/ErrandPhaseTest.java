package se.sundsvall.supportmanagement.api.model.errand;

import java.time.OffsetDateTime;
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

class ErrandPhaseTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(ErrandPhase.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {

		final var phaseId = "phaseId";
		final var name = "INVESTIGATION";
		final var displayName = "Utredning";
		final var started = OffsetDateTime.now();
		final var ended = OffsetDateTime.now().plusDays(1);

		final var bean = ErrandPhase.create()
			.withPhaseId(phaseId)
			.withName(name)
			.withDisplayName(displayName)
			.withStarted(started)
			.withEnded(ended);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getPhaseId()).isEqualTo(phaseId);
		assertThat(bean.getName()).isEqualTo(name);
		assertThat(bean.getDisplayName()).isEqualTo(displayName);
		assertThat(bean.getStarted()).isEqualTo(started);
		assertThat(bean.getEnded()).isEqualTo(ended);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandPhase.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandPhase()).hasAllNullFieldsOrProperties();
	}
}
