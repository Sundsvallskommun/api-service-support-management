package se.sundsvall.supportmanagement.api.model.metadata;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class MetadataResponseTest {

	@Test
	void testBean() {
		assertThat(MetadataResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {

		final var categories = List.of(Category.create().withName("CATEGORY").withTypes(List.of(Type.create().withName("TYPE"))));
		final var externalIdTypes = List.of(ExternalIdType.create().withName("EXTERNAL_ID_TYPE"));
		final var statuses = List.of(Status.create().withName("STATUS"));

		final var response = MetadataResponse.create()
			.withCategories(categories)
			.withExternalIdTypes(externalIdTypes)
			.withStatuses(statuses);

		assertThat(response.getCategories()).isEqualTo(categories);
		assertThat(response.getExternalIdTypes()).isEqualTo(externalIdTypes);
		assertThat(response.getStatuses()).isEqualTo(statuses);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(MetadataResponse.create()).hasAllNullFieldsOrProperties();
		assertThat(new MetadataResponse()).hasAllNullFieldsOrProperties();
	}
}
