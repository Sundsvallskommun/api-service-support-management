package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobType;

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

class JobEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> JobType.MOVE_LABEL, JobType.class);
	}

	@Test
	void testBean() {
		assertThat(JobEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanToString()));
	}

	@Test
	void testHashCodeAndEquals() {
		final var entity1 = JobEntity.create().withId("id-1").withNamespace("ns").withMunicipalityId("2281");
		final var entity2 = JobEntity.create().withId("id-1").withNamespace("ns").withMunicipalityId("2281");
		final var entity3 = JobEntity.create().withId("id-2").withNamespace("ns").withMunicipalityId("2281");

		assertThat(entity1).isEqualTo(entity2);
		assertThat(entity1).hasSameHashCodeAs(entity2);
		assertThat(entity1).isNotEqualTo(entity3);
	}

	@Test
	void hasValidBuilderMethods() {
		final var created = OffsetDateTime.now().minusDays(1);
		final var id = "6a5b8c9d-1234-5678-abcd-ef0123456789";
		final var message = "something went wrong";
		final var modified = OffsetDateTime.now();
		final var municipalityId = "2281";
		final var namespace = "MY_NAMESPACE";
		final var processed = 42;
		final var progress = 50;
		final var status = JobStatus.RUNNING;
		final var total = 84;
		final var type = JobType.MOVE_LABEL;

		final var entity = JobEntity.create()
			.withCreated(created)
			.withId(id)
			.withMessage(message)
			.withModified(modified)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withProcessed(processed)
			.withProgress(progress)
			.withStatus(status)
			.withTotal(total)
			.withType(type);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getMessage()).isEqualTo(message);
		assertThat(entity.getModified()).isEqualTo(modified);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getProcessed()).isEqualTo(processed);
		assertThat(entity.getProgress()).isEqualTo(progress);
		assertThat(entity.getStatus()).isEqualTo(status);
		assertThat(entity.getTotal()).isEqualTo(total);
		assertThat(entity.getType()).isEqualTo(type);
	}

	@Test
	void testOnCreate() {
		final var entity = JobEntity.create();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity.getStatus()).isEqualTo(JobStatus.PENDING);
		assertThat(entity.getProgress()).isZero();
		assertThat(entity.getProcessed()).isZero();
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created", "status", "progress", "processed");
	}

	@Test
	void testOnUpdate() {
		final var entity = JobEntity.create();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(JobEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new JobEntity()).hasAllNullFieldsOrProperties();
	}
}
