package se.sundsvall.supportmanagement.api.model.revision;

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

import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RevisionTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(Revision.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var created = OffsetDateTime.now();
		final var entityId = UUID.randomUUID().toString();
		final var entityType = "entityType";
		final var id = UUID.randomUUID().toString();
		final var version = 1;

		final var revisionEntity = Revision.create()
			.withCreated(created)
			.withEntityId(entityId)
			.withEntityType(entityType)
			.withId(id)
			.withVersion(version);

		assertThat(revisionEntity).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(revisionEntity.getCreated()).isEqualTo(created);
		assertThat(revisionEntity.getEntityId()).isEqualTo(entityId);
		assertThat(revisionEntity.getEntityType()).isEqualTo(entityType);
		assertThat(revisionEntity.getId()).isEqualTo(id);
		assertThat(revisionEntity.getVersion()).isEqualTo(version);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Revision.create()).hasAllNullFieldsOrProperties();
		assertThat(new Revision()).hasAllNullFieldsOrProperties();
	}
}
