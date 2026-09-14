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

	// The measure holds the parameter in turn, so comparing it would walk back into the measure.
	private static final String OWNER = "measureEntity";

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(MeasureJsonParameterEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding(OWNER),
			hasValidBeanEqualsExcluding(OWNER),
			hasValidBeanToStringExcluding(OWNER)));
	}

	@Test
	void hasValidBuilderMethods() {

		// Arrange
		final var id = "id";
		final var owner = MeasureEntity.create().withId("ownerId");
		final var key = "measureForm";
		final var schemaId = "schemaId";
		final var value = "{\"answer\":\"pending\"}";
		final var version = 3L;

		// Act
		final var result = MeasureJsonParameterEntity.create()
			.withId(id)
			.withMeasureEntity(owner)
			.withKey(key)
			.withSchemaId(schemaId)
			.withValue(value)
			.withVersion(version);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getMeasureEntity()).isEqualTo(owner);
		assertThat(result.getKey()).isEqualTo(key);
		assertThat(result.getSchemaId()).isEqualTo(schemaId);
		assertThat(result.getValue()).isEqualTo(value);
		assertThat(result.getVersion()).isEqualTo(version);
	}

	@Test
	void toStringNamesTheOwnerByItsIdOnly() {
		assertThat(MeasureJsonParameterEntity.create().withMeasureEntity(MeasureEntity.create().withId("ownerId")).toString()).endsWith("measureEntity=ownerId}");
		assertThat(MeasureJsonParameterEntity.create().toString()).endsWith("measureEntity=null}");
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(MeasureJsonParameterEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new MeasureJsonParameterEntity()).hasAllNullFieldsOrProperties();
	}
}
