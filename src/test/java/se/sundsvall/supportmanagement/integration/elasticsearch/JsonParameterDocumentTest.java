package se.sundsvall.supportmanagement.integration.elasticsearch;

import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class JsonParameterDocumentTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(JsonParameterDocument.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding(),
			hasValidBeanEqualsExcluding(),
			hasValidBeanToStringExcluding()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = "id";
		final var errandId = "errandId";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var parameterKey = "parameterKey";
		final var value = Map.<String, Object>of("key", "value");

		final var document = JsonParameterDocument.create()
			.withId(id)
			.withErrandId(errandId)
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withParameterKey(parameterKey)
			.withValue(value);

		assertThat(document).hasNoNullFieldsOrProperties();
		assertThat(document.getId()).isEqualTo(id);
		assertThat(document.getErrandId()).isEqualTo(errandId);
		assertThat(document.getNamespace()).isEqualTo(namespace);
		assertThat(document.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(document.getParameterKey()).isEqualTo(parameterKey);
		assertThat(document.getValue()).isEqualTo(value);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(JsonParameterDocument.create()).hasAllNullFieldsOrProperties();
		assertThat(new JsonParameterDocument()).hasAllNullFieldsOrProperties();
	}
}
