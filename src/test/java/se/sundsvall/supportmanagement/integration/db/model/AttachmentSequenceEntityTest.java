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

class AttachmentSequenceEntityTest {

	@Test
	void testBean() {
		assertThat(AttachmentSequenceEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("errandEntity"),
			hasValidBeanEqualsExcluding("errandEntity"),
			hasValidBeanToStringExcluding("errandEntity")));
	}

	@Test
	void hasValidBuilderMethods() {
		final var errandId = "errandId";
		final var errandEntity = ErrandEntity.create().withId(errandId);

		final var entity = AttachmentSequenceEntity.create()
			.withErrandId(errandId)
			.withErrandEntity(errandEntity)
			.withLastSequenceNumber(3);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getErrandId()).isEqualTo(errandId);
		assertThat(entity.getErrandEntity()).isSameAs(errandEntity);
		assertThat(entity.getLastSequenceNumber()).isEqualTo(3);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(AttachmentSequenceEntity.create()).hasAllNullFieldsOrPropertiesExcept("lastSequenceNumber");
		assertThat(AttachmentSequenceEntity.create().getLastSequenceNumber()).isZero();
	}
}
