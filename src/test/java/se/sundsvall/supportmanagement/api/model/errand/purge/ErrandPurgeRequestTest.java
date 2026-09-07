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

class ErrandPurgeRequestTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(ErrandPurgeRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var olderThan = OffsetDateTime.parse("2024-08-28T00:00:00+02:00");
		final var dryRun = true;
		final var maxErrands = 1000;

		final var bean = ErrandPurgeRequest.create()
			.withOlderThan(olderThan)
			.withDryRun(dryRun)
			.withMaxErrands(maxErrands);

		Assertions.assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		Assertions.assertThat(bean.getOlderThan()).isEqualTo(olderThan);
		Assertions.assertThat(bean.getDryRun()).isEqualTo(dryRun);
		Assertions.assertThat(bean.getMaxErrands()).isEqualTo(maxErrands);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		Assertions.assertThat(ErrandPurgeRequest.create()).hasAllNullFieldsOrProperties();
		Assertions.assertThat(new ErrandPurgeRequest()).hasAllNullFieldsOrProperties();
	}
}
