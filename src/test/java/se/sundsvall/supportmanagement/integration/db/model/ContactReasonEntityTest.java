package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import java.time.OffsetDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ContactReasonEntityTest {

	@Test
	void testBean() {
		assertThat(ContactReasonEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {

		final var id = 10L;
		final var reason = "reason";
		final var nameSpace = "nameSpace";
		final var municipalityId = "municipalityId";
		final var created = OffsetDateTime.now();
		final var modified = OffsetDateTime.now();

		final var entity = ContactReasonEntity.create()
			.withId(id)
			.withNamespace(nameSpace)
			.withMunicipalityId(municipalityId)
			.withReason(reason)
			.withCreated(created)
			.withModified(modified);

		Assertions.assertThat(entity).hasNoNullFieldsOrProperties();
		Assertions.assertThat(entity.getReason()).isEqualTo(reason);
		Assertions.assertThat(entity.getId()).isEqualTo(id);
		Assertions.assertThat(entity.getNamespace()).isEqualTo(nameSpace);
		Assertions.assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		Assertions.assertThat(entity.getCreated()).isEqualTo(created);
		Assertions.assertThat(entity.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		Assertions.assertThat(ContactReasonEntity.create()).hasAllNullFieldsOrProperties();
		Assertions.assertThat(new ContactReasonEntity()).hasAllNullFieldsOrProperties();
	}
}
