package se.sundsvall.supportmanagement.integration.db.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSettersExcluding;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class ErrandLabelEmbeddableTest {

	@Test
	void testBean() {
		assertThat(ErrandLabelEmbeddable.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSettersExcluding("metadataLabel"),
			hasValidBeanHashCodeExcluding("metadataLabel"),
			hasValidBeanEqualsExcluding("metadataLabel"),
			hasValidBeanToStringExcluding("metadataLabel")));
	}

	@Test
	void hasValidBuilderMethods() {

		final var metadataLabelId = "id";

		final var bean = ErrandLabelEmbeddable.create()
			.withMetadataLabelId(metadataLabelId);

		assertThat(bean.getMetadataLabelId()).isEqualTo(metadataLabelId);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(ErrandLabelEmbeddable.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandLabelEmbeddable()).hasAllNullFieldsOrProperties();
	}
}
