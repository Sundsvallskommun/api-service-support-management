package se.sundsvall.supportmanagement.api.model.job;

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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class JobResponseTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> JobType.MOVE_LABEL, JobType.class);
	}

	@Test
	void testBean() {
		assertThat(JobResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanToString()));
	}

	@Test
	void testHashCodeAndEquals() {
		final var bean1 = JobResponse.create().withJobId("id-1").withType(JobType.MOVE_LABEL);
		final var bean2 = JobResponse.create().withJobId("id-1").withType(JobType.MOVE_LABEL);
		final var bean3 = JobResponse.create().withJobId("id-2").withType(JobType.MOVE_LABEL);

		assertThat(bean1).isEqualTo(bean2);
		assertThat(bean1).hasSameHashCodeAs(bean2);
		assertThat(bean1).isNotEqualTo(bean3);
	}

	@Test
	void testBuilderMethods() {
		final var jobId = "550e8400-e29b-41d4-a716-446655440000";
		final var type = JobType.MOVE_LABEL;
		final var status = JobStatus.RUNNING;
		final var progress = 42;
		final var total = 100;
		final var processed = 42;
		final var message = "some message";
		final var created = OffsetDateTime.now();
		final var modified = OffsetDateTime.now().plusMinutes(1);

		final var bean = JobResponse.create()
			.withJobId(jobId)
			.withType(type)
			.withStatus(status)
			.withProgress(progress)
			.withTotal(total)
			.withProcessed(processed)
			.withMessage(message)
			.withCreated(created)
			.withModified(modified);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getJobId()).isEqualTo(jobId);
		assertThat(bean.getType()).isEqualTo(type);
		assertThat(bean.getStatus()).isEqualTo(status);
		assertThat(bean.getProgress()).isEqualTo(progress);
		assertThat(bean.getTotal()).isEqualTo(total);
		assertThat(bean.getProcessed()).isEqualTo(processed);
		assertThat(bean.getMessage()).isEqualTo(message);
		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(JobResponse.create()).hasAllNullFieldsOrProperties();
		assertThat(new JobResponse()).hasAllNullFieldsOrProperties();
	}
}
