package se.sundsvall.supportmanagement.integration.db.model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Random;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class TagEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(TagEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {

		final var id = 1L;
		final var name = "name";
		final var created = OffsetDateTime.now();
		final var updated = OffsetDateTime.now();
		final var tagType = TagType.CATEGORY;

		final var entity = TagEntity.create()
			.withCreated(created)
			.withId(id)
			.withName(name)
			.withType(tagType)
			.withUpdated(updated);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getName()).isEqualTo(name);
		assertThat(entity.getType()).isEqualTo(tagType);
		assertThat(entity.getUpdated()).isEqualTo(updated);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(TagEntity.create()).hasAllNullFieldsOrPropertiesExcept("id");
		assertThat(new TagEntity()).hasAllNullFieldsOrPropertiesExcept("id");
	}
}
