package se.sundsvall.supportmanagement.integration.db.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class JsonParameterEntityTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(JsonParameterEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("errandEntity"),
			hasValidBeanEqualsExcluding("errandEntity"),
			hasValidBeanToStringExcluding("errandEntity")));
	}

	@Test
	void hasValidBuilderMethods() {

		final var id = "id";
		final var key = "key";
		final var schemaId = "schemaId";
		final var value = "{\"field\": \"value\"}";
		final var errandEntity = ErrandEntity.create().withId("errandId");

		final var jsonParameterEntity = JsonParameterEntity.create()
			.withId(id)
			.withErrandEntity(errandEntity)
			.withKey(key)
			.withSchemaId(schemaId)
			.withValue(value);

		assertThat(jsonParameterEntity).hasNoNullFieldsOrProperties();
		assertThat(jsonParameterEntity.getId()).isEqualTo(id);
		assertThat(jsonParameterEntity.getKey()).isEqualTo(key);
		assertThat(jsonParameterEntity.getSchemaId()).isEqualTo(schemaId);
		assertThat(jsonParameterEntity.getValue()).isEqualTo(value);
		assertThat(jsonParameterEntity.getErrandEntity()).isEqualTo(errandEntity);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(JsonParameterEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new JsonParameterEntity()).hasAllNullFieldsOrProperties();
	}
}
