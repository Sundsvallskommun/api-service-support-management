package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import java.util.List;
import org.junit.jupiter.api.Test;

class WebMessageCollectEntityTest {

	@Test
	void testBean() {
		assertThat(WebMessageCollectEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = 123L;
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var instance = "instance";
		final var familyIds = List.of("familyId");

		var result = WebMessageCollectEntity.create()
			.withId(id)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withInstance(instance)
			.withFamilyIds(familyIds);

		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(result.getNamespace()).isEqualTo(namespace);
		assertThat(result.getInstance()).isEqualTo(instance);
		assertThat(result.getFamilyIds()).isEqualTo(familyIds);
	}
}
