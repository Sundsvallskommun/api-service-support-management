package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import java.util.List;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

class ActionConfigParameterEntityTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(ActionConfigParameterEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("actionConfigEntity"),
			hasValidBeanEqualsExcluding("actionConfigEntity"),
			hasValidBeanToStringExcluding("actionConfigEntity")));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = "id";
		final var key = "key";
		final var values = List.of("value1", "value2");
		final var actionConfigEntity = ActionConfigEntity.create().withId("id");

		final var entity = ActionConfigParameterEntity.create()
			.withId(id)
			.withKey(key)
			.withValues(values)
			.withActionConfigEntity(actionConfigEntity);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getKey()).isEqualTo(key);
		assertThat(entity.getValues()).isEqualTo(values);
		assertThat(entity.getActionConfigEntity()).isEqualTo(actionConfigEntity);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(ActionConfigParameterEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ActionConfigParameterEntity()).hasAllNullFieldsOrProperties();
	}
}
