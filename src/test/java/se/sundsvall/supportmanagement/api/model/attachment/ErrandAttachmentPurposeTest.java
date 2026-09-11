package se.sundsvall.supportmanagement.api.model.attachment;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class ErrandAttachmentPurposeTest {

	@Test
	void testBean() {
		assertThat(ErrandAttachmentPurpose.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		// Arrange
		final var id = "5f79a808-0ef3-4985-99b9-b12f23e202a7";
		final var name = "RESPONSE";
		final var displayName = "Inkommen handling";

		// Act
		final var bean = ErrandAttachmentPurpose.create()
			.withId(id)
			.withName(name)
			.withDisplayName(displayName);

		// Assert
		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getName()).isEqualTo(name);
		assertThat(bean.getDisplayName()).isEqualTo(displayName);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandAttachmentPurpose.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandAttachmentPurpose()).hasAllNullFieldsOrProperties();
	}
}
