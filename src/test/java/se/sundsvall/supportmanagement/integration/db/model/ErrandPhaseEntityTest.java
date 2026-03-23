package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class ErrandPhaseEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(ErrandPhaseEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("errandEntity", "phaseEntity"),
			hasValidBeanEqualsExcluding("errandEntity", "phaseEntity"),
			hasValidBeanToStringExcluding("errandEntity", "phaseEntity")));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = "id";
		final var errandEntity = ErrandEntity.create().withId("errand-id");
		final var phaseEntity = PhaseEntity.create().withId("phase-id");
		final var started = OffsetDateTime.now().minusDays(1);
		final var ended = OffsetDateTime.now();

		final var entity = ErrandPhaseEntity.create()
			.withId(id)
			.withErrandEntity(errandEntity)
			.withPhaseEntity(phaseEntity)
			.withStarted(started)
			.withEnded(ended);

		assertThat(entity).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getErrandEntity()).isEqualTo(errandEntity);
		assertThat(entity.getPhaseEntity()).isEqualTo(phaseEntity);
		assertThat(entity.getStarted()).isEqualTo(started);
		assertThat(entity.getEnded()).isEqualTo(ended);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandPhaseEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandPhaseEntity()).hasAllNullFieldsOrProperties();
	}
}
