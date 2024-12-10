package se.sundsvall.supportmanagement.api.model.errand;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class ClassificationTest {
	@Test
	void testBean() {
		assertThat(Classification.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		var category = "category";
		var type = "type";

		var classification = Classification.create()
			.withCategory(category)
			.withType(type);

		assertThat(classification).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(classification.getCategory()).isEqualTo(category);
		assertThat(classification.getType()).isEqualTo(type);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Classification.create()).hasAllNullFieldsOrProperties();
		assertThat(new Classification()).hasAllNullFieldsOrProperties();
	}
}
