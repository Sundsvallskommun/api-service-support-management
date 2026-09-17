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

class ErrandAccessTest {

	@Test
	void testBean() {
		assertThat(ErrandAccess.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var fields = List.of(ErrandFieldAccess.create().withField("title").withAllKeys(false));
		final var resources = List.of(ErrandResourceAccess.create().withResource("errand/communication").withLevel(AccessLevel.R));
		final var bean = ErrandAccess.create()
			.withLevel(AccessLevel.RW)
			.withFields(fields)
			.withResources(resources);

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getLevel()).isEqualTo(AccessLevel.RW);
		assertThat(bean.getFields()).isEqualTo(fields);
		assertThat(bean.getResources()).isEqualTo(resources);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandAccess.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandAccess()).hasAllNullFieldsOrProperties();
	}
}
