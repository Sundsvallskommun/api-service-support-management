package se.sundsvall.supportmanagement.api.model.metadata;

import java.util.List;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

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
		final var labels = Labels.create().withLabelStructure(List.of(Label.create().withClassification("CLASSIFICATION")));
		final var roles = List.of(Role.create().withName("ROLE"));
		final var statuses = List.of(Status.create().withName("STATUS"));

		final var response = MetadataResponse.create()
			.withCategories(categories)
			.withExternalIdTypes(externalIdTypes)
			.withLabels(labels)
			.withRoles(roles)
			.withStatuses(statuses);

		assertThat(response.getCategories()).isEqualTo(categories);
		assertThat(response.getExternalIdTypes()).isEqualTo(externalIdTypes);
		assertThat(response.getLabels()).isEqualTo(labels);
		assertThat(response.getRoles()).isEqualTo(roles);
		assertThat(response.getStatuses()).isEqualTo(statuses);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(MetadataResponse.create()).hasAllNullFieldsOrProperties();
		assertThat(new MetadataResponse()).hasAllNullFieldsOrProperties();
	}
}
