package se.sundsvall.supportmanagement.integration.db.model;

import java.util.List;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class StakeholderParameterEntityTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(StakeholderParameterEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals()));
	}

	@Test
	void hasValidBuilderMethods() {

		final var id = 1L;
		final var displayName = "displayName";
		final var key = "key";
		final var values = List.of("value");
		final var stakeholderEntity = StakeholderEntity.create().withId(666L);

		final var parameterEntity = StakeholderParameterEntity.create()
			.withId(id)
			.withDisplayName(displayName)
			.withStakeholderEntity(stakeholderEntity)
			.withKey(key)
			.withValues(values);

		assertThat(parameterEntity).hasNoNullFieldsOrProperties();
		assertThat(parameterEntity.getKey()).isEqualTo(key);
		assertThat(parameterEntity.getDisplayName()).isEqualTo(displayName);
		assertThat(parameterEntity.getValues()).isEqualTo(values);
		assertThat(parameterEntity.getId()).isEqualTo(id);
		assertThat(parameterEntity.getStakeholderEntity()).isEqualTo(stakeholderEntity);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(ParameterEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ParameterEntity()).hasAllNullFieldsOrProperties();
	}
}
