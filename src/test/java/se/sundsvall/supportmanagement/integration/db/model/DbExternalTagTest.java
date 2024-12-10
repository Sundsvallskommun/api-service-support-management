package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import org.junit.jupiter.api.Test;

class DbExternalTagTest {

	@Test
	void testBean() {
		assertThat(DbExternalTag.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToStringExcluding("errandEntity")));
	}

	@Test
	void hasValidBuilderMethods() {

		final var key = "key";
		final var value = "value";

		final var externalDetailEntity = DbExternalTag.create()
			.withKey(key)
			.withValue(value);

		assertThat(externalDetailEntity).hasNoNullFieldsOrProperties();
		assertThat(externalDetailEntity.getKey()).isEqualTo(key);
		assertThat(externalDetailEntity.getValue()).isEqualTo(value);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(DbExternalTag.create()).hasAllNullFieldsOrProperties();
		assertThat(new DbExternalTag()).hasAllNullFieldsOrProperties();
	}
}
