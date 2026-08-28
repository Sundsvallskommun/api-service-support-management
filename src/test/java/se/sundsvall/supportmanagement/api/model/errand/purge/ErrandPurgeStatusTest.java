package se.sundsvall.supportmanagement.api.model.errand.purge;

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
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.COMPLETED;

class ErrandPurgeStatusTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(ErrandPurgeStatus.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var jobId = "b82bd8ac-1507-4d9a-958d-369261eecc15";
		final var namespace = "namespace";
		final var municipalityId = "2281";
		final var olderThan = OffsetDateTime.parse("2024-08-28T00:00:00+02:00");
		final var dryRun = true;
		final var started = OffsetDateTime.parse("2026-08-28T09:00:00+02:00");
		final var finished = OffsetDateTime.parse("2026-08-28T11:00:00+02:00");
		final var processed = 250L;
		final var deleted = 248L;
		final var failed = 2L;
		final var message = "Access control is active for the namespace";

		final var bean = ErrandPurgeStatus.create()
			.withJobId(jobId)
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withOlderThan(olderThan)
			.withDryRun(dryRun)
			.withState(COMPLETED)
			.withStarted(started)
			.withFinished(finished)
			.withProcessed(processed)
			.withDeleted(deleted)
			.withFailed(failed)
			.withMessage(message);

		Assertions.assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		Assertions.assertThat(bean.getJobId()).isEqualTo(jobId);
		Assertions.assertThat(bean.getNamespace()).isEqualTo(namespace);
		Assertions.assertThat(bean.getMunicipalityId()).isEqualTo(municipalityId);
		Assertions.assertThat(bean.getOlderThan()).isEqualTo(olderThan);
		Assertions.assertThat(bean.isDryRun()).isEqualTo(dryRun);
		Assertions.assertThat(bean.getState()).isEqualTo(COMPLETED);
		Assertions.assertThat(bean.getStarted()).isEqualTo(started);
		Assertions.assertThat(bean.getFinished()).isEqualTo(finished);
		Assertions.assertThat(bean.getProcessed()).isEqualTo(processed);
		Assertions.assertThat(bean.getDeleted()).isEqualTo(deleted);
		Assertions.assertThat(bean.getFailed()).isEqualTo(failed);
		Assertions.assertThat(bean.getMessage()).isEqualTo(message);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		Assertions.assertThat(ErrandPurgeStatus.create()).hasAllNullFieldsOrPropertiesExcept("dryRun", "processed", "deleted", "failed");
		Assertions.assertThat(new ErrandPurgeStatus()).hasAllNullFieldsOrPropertiesExcept("dryRun", "processed", "deleted", "failed");
		Assertions.assertThat(ErrandPurgeStatus.create().isDryRun()).isFalse();
		Assertions.assertThat(ErrandPurgeStatus.create().getProcessed()).isZero();
		Assertions.assertThat(ErrandPurgeStatus.create().getDeleted()).isZero();
		Assertions.assertThat(ErrandPurgeStatus.create().getFailed()).isZero();
	}
}
