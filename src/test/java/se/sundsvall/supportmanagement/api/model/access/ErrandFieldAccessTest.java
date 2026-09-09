package se.sundsvall.supportmanagement.api.model.access;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.config.AccessLevel;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class ErrandFieldAccessTest {

	@Test
	void testBean() {
		assertThat(ErrandFieldAccess.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var keys = List.of(ErrandFieldKeyAccess.create().withKey("granted-key").withLevel(AccessLevel.RW));
		final var bean = ErrandFieldAccess.create()
			.withField("parameters")
			.withAllKeys(false)
			.withKeys(keys);

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getField()).isEqualTo("parameters");
		assertThat(bean.getAllKeys()).isFalse();
		assertThat(bean.getKeys()).isEqualTo(keys);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandFieldAccess.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandFieldAccess()).hasAllNullFieldsOrProperties();
	}
}
