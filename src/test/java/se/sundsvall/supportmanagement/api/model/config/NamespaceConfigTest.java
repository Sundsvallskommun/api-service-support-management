package se.sundsvall.supportmanagement.api.model.config;

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

class NamespaceConfigTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(NamespaceConfig.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var namespace = "namespace";
		final var displayName = "displayName";
		final var shortCode = "shortCode";
		final var created = OffsetDateTime.now();
		final var modified = OffsetDateTime.now().plusDays(1);

		final var bean = NamespaceConfig.create()
			.withNamespace(namespace)
			.withDisplayName(displayName)
			.withShortCode(shortCode)
			.withCreated(created)
			.withModified(modified);

		assertThat(bean.getNamespace()).isEqualTo(namespace);
		assertThat(bean.getDisplayName()).isEqualTo(displayName);
		assertThat(bean.getShortCode()).isEqualTo(shortCode);
		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NamespaceConfig.create()).hasAllNullFieldsOrProperties();
		assertThat(new NamespaceConfig()).hasAllNullFieldsOrProperties();
	}
}
