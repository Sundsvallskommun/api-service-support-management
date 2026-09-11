package se.sundsvall.supportmanagement.api.model.errand;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class ArtefactAttachmentLinkTest {

	@Test
	void testBean() {
		assertThat(ArtefactAttachmentLink.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		// Act
		final var bean = ArtefactAttachmentLink.create().withSortOrder(3);

		// Assert
		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getSortOrder()).isEqualTo(3);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ArtefactAttachmentLink.create()).hasAllNullFieldsOrProperties();
		assertThat(new ArtefactAttachmentLink()).hasAllNullFieldsOrProperties();
	}
}
