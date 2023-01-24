package se.sundsvall.supportmanagement.api.model.attachment;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class ErrandAttachmentHeaderTest {

	@Test
	void testBean() {
		assertThat(ErrandAttachmentHeader.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "id";
		final var fileName = "fileName";

		final var bean = ErrandAttachmentHeader.create()
			.withId(id)
			.withFileName(fileName);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getFileName()).isEqualTo(fileName);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandAttachmentHeader.create()).hasAllNullFieldsOrProperties();
	}
}
