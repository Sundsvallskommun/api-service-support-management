package se.sundsvall.supportmanagement.api.model.handover;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class MetadataOptionTest {

	@Test
	void testBean() {
		assertThat(MetadataOption.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var name = "IN_PROGRESS";
		final var displayName = "Pågående";

		final var bean = MetadataOption.create()
			.withName(name)
			.withDisplayName(displayName);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getName()).isEqualTo(name);
		assertThat(bean.getDisplayName()).isEqualTo(displayName);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(MetadataOption.create()).hasAllNullFieldsOrProperties();
		assertThat(new MetadataOption()).hasAllNullFieldsOrProperties();
	}
}
