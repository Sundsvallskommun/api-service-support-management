package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class ContactReasonEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

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
		final var displayName = "displayName";
		final var nameSpace = "nameSpace";
		final var municipalityId = "municipalityId";
		final var created = OffsetDateTime.now();
		final var modified = OffsetDateTime.now();

		final var entity = ContactReasonEntity.create()
			.withId(id)
			.withNamespace(nameSpace)
			.withMunicipalityId(municipalityId)
			.withReason(reason)
			.withDisplayName(displayName)
			.withCreated(created)
			.withModified(modified);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getReason()).isEqualTo(reason);
		assertThat(entity.getDisplayName()).isEqualTo(displayName);
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getNamespace()).isEqualTo(nameSpace);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ContactReasonEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ContactReasonEntity()).hasAllNullFieldsOrProperties();
	}
}
