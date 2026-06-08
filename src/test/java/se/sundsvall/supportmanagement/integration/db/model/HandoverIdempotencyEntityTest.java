package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class HandoverIdempotencyEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

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
		final var idempotencyKey = "idem-key";
		final var newErrandId = "errand-id";
		final var newErrandNumber = "KC-24010001";
		final var targetNamespace = "TARGET_NS";
		final var targetMunicipalityId = "2281";
		final var relationId = "relation-uuid";
		final var warnings = "some warning";
		final var createdAt = now().minusHours(1);
		final var expiresAt = now().plusHours(23);

		final var entity = HandoverIdempotencyEntity.create()
			.withId(id)
			.withIdempotencyKey(idempotencyKey)
			.withNewErrandId(newErrandId)
			.withNewErrandNumber(newErrandNumber)
			.withTargetNamespace(targetNamespace)
			.withTargetMunicipalityId(targetMunicipalityId)
			.withRelationId(relationId)
			.withWarnings(warnings)
			.withCreatedAt(createdAt)
			.withExpiresAt(expiresAt);

		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getIdempotencyKey()).isEqualTo(idempotencyKey);
		assertThat(entity.getNewErrandId()).isEqualTo(newErrandId);
		assertThat(entity.getNewErrandNumber()).isEqualTo(newErrandNumber);
		assertThat(entity.getTargetNamespace()).isEqualTo(targetNamespace);
		assertThat(entity.getTargetMunicipalityId()).isEqualTo(targetMunicipalityId);
		assertThat(entity.getRelationId()).isEqualTo(relationId);
		assertThat(entity.getWarnings()).isEqualTo(warnings);
		assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
		assertThat(entity.getExpiresAt()).isEqualTo(expiresAt);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(HandoverIdempotencyEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new HandoverIdempotencyEntity()).hasAllNullFieldsOrProperties();
	}
}
