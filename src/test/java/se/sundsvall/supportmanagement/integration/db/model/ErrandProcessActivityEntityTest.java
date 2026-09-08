package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.ERROR;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.INFO;

class ErrandProcessActivityEntityTest {

	/**
	 * No generator is registered for {@link ActivitySeverity}: BeanMatchers generates enum values by itself, and its
	 * registry is static for the whole JVM while surefire reuses the fork. A generator handing out one constant would
	 * therefore leave every later test in the run unable to find two distinct severities - which is what
	 * {@code hasValidBeanEquals} needs, and what {@code ProcessActivityTest} asks for.
	 */
	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(ErrandProcessActivityEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanToString()));
	}

	@Test
	void testHashCodeAndEquals() {
		final var entity1 = ErrandProcessActivityEntity.create().withId("id-1").withErrandId("errand-1");
		final var entity2 = ErrandProcessActivityEntity.create().withId("id-1").withErrandId("errand-1");
		final var entity3 = ErrandProcessActivityEntity.create().withId("id-2").withErrandId("errand-1");

		assertThat(entity1)
			.isEqualTo(entity2)
			.hasSameHashCodeAs(entity2)
			.isNotEqualTo(entity3);
	}

	@Test
	void hasValidBuilderMethods() {
		final var activityId = "granska-ansokan";
		final var activityName = "Granska ansokan";
		final var activityType = "TASK";
		final var created = OffsetDateTime.now();
		final var errandId = "ERRAND_ID-1";
		final var errandProcessId = "6a5b8c9d-1234-5678-abcd-ef0123456789";
		final var errorCode = "AMBIGUOUS_PROCESS_KEY";
		final var externalTaskId = "task-1";
		final var id = "1a2b3c4d-5e6f-7890-abcd-ef0123456789";
		final var message = "Two labels resolve to different process keys";
		final var occurredAt = OffsetDateTime.now().minusMinutes(1);

		final var entity = ErrandProcessActivityEntity.create()
			.withActivityId(activityId)
			.withActivityName(activityName)
			.withActivityType(activityType)
			.withCreated(created)
			.withErrandId(errandId)
			.withErrandProcessId(errandProcessId)
			.withErrorCode(errorCode)
			.withExternalTaskId(externalTaskId)
			.withId(id)
			.withMessage(message)
			.withOccurredAt(occurredAt)
			.withSeverity(ERROR);

		assertThat(entity)
			.hasNoNullFieldsOrProperties()
			.satisfies(e -> {
				assertThat(e.getActivityId()).isEqualTo(activityId);
				assertThat(e.getActivityName()).isEqualTo(activityName);
				assertThat(e.getActivityType()).isEqualTo(activityType);
				assertThat(e.getCreated()).isEqualTo(created);
				assertThat(e.getErrandId()).isEqualTo(errandId);
				assertThat(e.getErrandProcessId()).isEqualTo(errandProcessId);
				assertThat(e.getErrorCode()).isEqualTo(errorCode);
				assertThat(e.getExternalTaskId()).isEqualTo(externalTaskId);
				assertThat(e.getId()).isEqualTo(id);
				assertThat(e.getMessage()).isEqualTo(message);
				assertThat(e.getOccurredAt()).isEqualTo(occurredAt);
				assertThat(e.getSeverity()).isEqualTo(ERROR);
			});
	}

	@Test
	@DisplayName("Verification that an entry written without a severity falls back to INFO, since the column default never fires")
	void testOnCreate() {
		final var entity = ErrandProcessActivityEntity.create();
		entity.onCreate();

		assertThat(entity)
			.hasAllNullFieldsOrPropertiesExcept("created", "severity")
			.satisfies(e -> {
				assertThat(e.getCreated()).isCloseTo(now(), within(1, SECONDS));
				assertThat(e.getSeverity()).isEqualTo(INFO);
			});
	}

	@Test
	void onCreateLeavesAGivenSeverityAlone() {
		final var entity = ErrandProcessActivityEntity.create().withSeverity(ERROR);
		entity.onCreate();

		assertThat(entity.getSeverity()).isEqualTo(ERROR);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandProcessActivityEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandProcessActivityEntity()).hasAllNullFieldsOrProperties();
	}
}
