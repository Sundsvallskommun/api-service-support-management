package se.sundsvall.supportmanagement.integration.elasticsearch.model;

import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class JsonParameterDocumentTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(JsonParameterDocument.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = "id";
		final var namespace = "namespace";
		final var municipalityId = "2281";
		final var jsonParameters = Map.<String, Object>of("facility", Map.of("schemaId", "schema-1.0"));

		final var document = JsonParameterDocument.create()
			.withId(id)
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withJsonParameters(jsonParameters);

		assertThat(document).hasNoNullFieldsOrProperties();
		assertThat(document.getId()).isEqualTo(id);
		assertThat(document.getNamespace()).isEqualTo(namespace);
		assertThat(document.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(document.getJsonParameters()).isEqualTo(jsonParameters);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(JsonParameterDocument.create()).hasAllNullFieldsOrProperties();
		assertThat(new JsonParameterDocument()).hasAllNullFieldsOrProperties();
	}
}
