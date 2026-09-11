package se.sundsvall.supportmanagement.integration.db.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;

class MeasureJsonParameterEntityTest {

	// A link carries nothing of its own beyond which two rows it joins, so its id is what identifies it. Comparing the
	// two sides would walk back into the errand they both belong to.
	private static final String[] RELATIONS = {
		"measureEntity", "jsonParameterEntity"
	};

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(MeasureJsonParameterEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding(RELATIONS),
			hasValidBeanEqualsExcluding(RELATIONS),
			hasValidBeanToStringExcluding(RELATIONS)));
	}

	@Test
	void hasValidBuilderMethods() {

		// Arrange
		final var id = "id";
		final var owner = MeasureEntity.create().withId("ownerId");
		final var jsonParameterEntity = JsonParameterEntity.create().withId("parameterId");

		// Act
		final var result = MeasureJsonParameterEntity.create()
			.withId(id)
			.withMeasureEntity(owner)
			.withJsonParameterEntity(jsonParameterEntity);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getMeasureEntity()).isEqualTo(owner);
		assertThat(result.getJsonParameterEntity()).isEqualTo(jsonParameterEntity);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(MeasureJsonParameterEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new MeasureJsonParameterEntity()).hasAllNullFieldsOrProperties();
	}
}
