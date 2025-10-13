package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MetadataLabelEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(MetadataLabelEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("parent"),
			hasValidBeanEqualsExcluding("parent"),
			hasValidBeanToStringExcluding("parent")));
	}

	@Test
	void hasValidBuilderMethods() {

		final var classification = "classification";
		final var created = now().minusDays(1);
		final var displayName = "displayName";
		final var id = randomUUID().toString();
		final var modified = now();
		final var metadataLabels = List.of(MetadataLabelEntity.create());
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var parent = MetadataLabelEntity.create();
		final var resourceName = "resourceName";
		final var resourcePath = "resourcePath";

		final var entity = MetadataLabelEntity.create()
			.withClassification(classification)
			.withCreated(created)
			.withDisplayName(displayName)
			.withId(id)
			.withModified(modified)
			.withMetadataLabels(metadataLabels)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withParent(parent)
			.withResourceName(resourceName)
			.withResourcePath(resourcePath);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getClassification()).isEqualTo(classification);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getDisplayName()).isEqualTo(displayName);
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getModified()).isEqualTo(modified);
		assertThat(entity.getMetadataLabels()).isEqualTo(metadataLabels);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getParent()).isEqualTo(parent);
		assertThat(entity.getResourceName()).isEqualTo(resourceName);
		assertThat(entity.getResourcePath()).isEqualTo(resourcePath);
	}

	@Test
	void testOnCreate() {
		final var entity = MetadataLabelEntity.create();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created", "metadataLabels");
	}

	@Test
	void testOnUpdate() {
		final var entity = MetadataLabelEntity.create();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified", "metadataLabels");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(MetadataLabelEntity.create()).hasAllNullFieldsOrPropertiesExcept("metadataLabels");
		assertThat(new MetadataLabelEntity()).hasAllNullFieldsOrPropertiesExcept("metadataLabels");
	}
}
