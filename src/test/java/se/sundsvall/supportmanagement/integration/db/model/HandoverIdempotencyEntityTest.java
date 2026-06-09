package se.sundsvall.supportmanagement.integration.db.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class HandoverIdempotencyEntityTest {

	@Test
	void testBean() {
		assertThat(HandoverIdempotencyEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding(),
			hasValidBeanEqualsExcluding(),
			hasValidBeanToStringExcluding()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = "test-id";
		final var sourceErrandId = "source-errand-id";
		final var newErrandId = "new-errand-id";
		final var newErrandNumber = "KC-24010001";
		final var targetNamespace = "TARGET_NS";
		final var targetMunicipalityId = "2281";
		final var relationId = "relation-uuid";
		final var warnings = "some warning";

		final var entity = HandoverIdempotencyEntity.create()
			.withId(id)
			.withSourceErrandId(sourceErrandId)
			.withNewErrandId(newErrandId)
			.withNewErrandNumber(newErrandNumber)
			.withTargetNamespace(targetNamespace)
			.withTargetMunicipalityId(targetMunicipalityId)
			.withRelationId(relationId)
			.withWarnings(warnings);

		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getSourceErrandId()).isEqualTo(sourceErrandId);
		assertThat(entity.getNewErrandId()).isEqualTo(newErrandId);
		assertThat(entity.getNewErrandNumber()).isEqualTo(newErrandNumber);
		assertThat(entity.getTargetNamespace()).isEqualTo(targetNamespace);
		assertThat(entity.getTargetMunicipalityId()).isEqualTo(targetMunicipalityId);
		assertThat(entity.getRelationId()).isEqualTo(relationId);
		assertThat(entity.getWarnings()).isEqualTo(warnings);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(HandoverIdempotencyEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new HandoverIdempotencyEntity()).hasAllNullFieldsOrProperties();
	}
}
