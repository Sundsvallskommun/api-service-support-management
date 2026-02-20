package se.sundsvall.supportmanagement.integration.db.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class AccessLabelEmbeddableTest {

	@Test
	void testBean() {
		assertThat(AccessLabelEmbeddable.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {

		final var metadataLabelId = "id";

		final var bean = AccessLabelEmbeddable.create()
			.withMetadataLabelId(metadataLabelId);

		assertThat(bean.getMetadataLabelId()).isEqualTo(metadataLabelId);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(AccessLabelEmbeddable.create()).hasAllNullFieldsOrProperties();
		assertThat(new AccessLabelEmbeddable()).hasAllNullFieldsOrProperties();
	}
}
