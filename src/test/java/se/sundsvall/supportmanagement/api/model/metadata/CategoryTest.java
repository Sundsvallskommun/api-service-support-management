package se.sundsvall.supportmanagement.api.model.metadata;

import java.time.OffsetDateTime;
import java.util.List;
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

class CategoryTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(Category.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var created = OffsetDateTime.now();
		final var displayName = "displayName";
		final var modified = OffsetDateTime.now().plusDays(1);
		final var name = "name";
		final var types = List.of(Type.create());

		final var bean = Category.create()
			.withCreated(created)
			.withDisplayName(displayName)
			.withModified(modified)
			.withName(name)
			.withTypes(types);

		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getDisplayName()).isEqualTo(displayName);
		assertThat(bean.getModified()).isEqualTo(modified);
		assertThat(bean.getName()).isEqualTo(name);
		assertThat(bean.getTypes()).isEqualTo(types);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Category.create()).hasAllNullFieldsOrProperties();
		assertThat(new Category()).hasAllNullFieldsOrProperties();
	}
}
