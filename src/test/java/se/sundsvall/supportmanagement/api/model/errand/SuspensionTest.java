package se.sundsvall.supportmanagement.api.model.errand;

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

class SuspensionTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(Suspension.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var suspendFrom = OffsetDateTime.now();
		final var suspendTo = OffsetDateTime.now().plusDays(1);

		final var bean = Suspension.create()
			.withSuspendedTo(suspendTo)
			.withSuspendedFrom(suspendFrom);

		Assertions.assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		Assertions.assertThat(bean.getSuspendedTo()).isEqualTo(suspendTo);
		Assertions.assertThat(bean.getSuspendedFrom()).isEqualTo(suspendFrom);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		Assertions.assertThat(Suspension.create()).hasAllNullFieldsOrProperties();
		Assertions.assertThat(new Suspension()).hasAllNullFieldsOrProperties();
	}
}
