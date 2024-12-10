package se.sundsvall.supportmanagement.api.model.metadata;

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

import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ExternalIdTypeTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(ExternalIdType.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var created = OffsetDateTime.now();
		final var modified = OffsetDateTime.now().plusDays(1);
		final var name = "name";

		final var bean = ExternalIdType.create()
			.withCreated(created)
			.withModified(modified)
			.withName(name);

		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getModified()).isEqualTo(modified);
		assertThat(bean.getName()).isEqualTo(name);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ExternalIdType.create()).hasAllNullFieldsOrProperties();
		assertThat(new ExternalIdType()).hasAllNullFieldsOrProperties();
	}
}
