package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NamespaceConfigEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(NamespaceConfigEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = 1L;
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var displayName = "displayName";
		final var shortCode = "shortCode";
		final var notificationTTLInDays = 1;
		final var accessControl = true;

		final var entity = NamespaceConfigEntity.create()
			.withId(id)
			.withCreated(created)
			.withDisplayName(displayName)
			.withModified(modified)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withShortCode(shortCode)
			.withNotificationTTLInDays(notificationTTLInDays)
			.withAccessControl(accessControl);

		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getDisplayName()).isEqualTo(displayName);
		assertThat(entity.getModified()).isEqualTo(modified);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getShortCode()).isEqualTo(shortCode);
		assertThat(entity.getNotificationTTLInDays()).isEqualTo(notificationTTLInDays);
		assertThat(entity.getAccessControl()).isEqualTo(accessControl);
	}

	@Test
	void testOnCreate() {
		final var entity = NamespaceConfigEntity.create();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("accessControl", "created")
			.satisfies(namespaceConfigEntity -> assertThat(namespaceConfigEntity.getAccessControl()).isFalse());

	}

	@Test
	void testOnUpdate() {
		final var entity = NamespaceConfigEntity.create();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("accessControl", "modified")
			.satisfies(namespaceConfigEntity -> assertThat(namespaceConfigEntity.getAccessControl()).isFalse());

	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NamespaceConfigEntity.create()).hasAllNullFieldsOrPropertiesExcept("accessControl")
			.satisfies(namespaceConfigEntity -> assertThat(namespaceConfigEntity.getAccessControl()).isFalse());
		assertThat(new NamespaceConfigEntity()).hasAllNullFieldsOrPropertiesExcept("accessControl")
			.satisfies(namespaceConfigEntity -> assertThat(namespaceConfigEntity.getAccessControl()).isFalse());
	}
}
