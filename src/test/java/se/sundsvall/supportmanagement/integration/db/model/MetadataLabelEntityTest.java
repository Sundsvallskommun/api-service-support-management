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
	void bean() {
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
		final var name = "name";
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
			.withName(name)
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
		assertThat(entity.getName()).isEqualTo(name);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getParent()).isEqualTo(parent);
		assertThat(entity.getResourceName()).isEqualTo(resourceName);
		assertThat(entity.getResourcePath()).isEqualTo(resourcePath);
	}

	@Test
	void onCreate() {
		final var entity = MetadataLabelEntity.create();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created", "metadataLabels");
	}

	@Test
	void onCreateShouldBuildHierarchicalResourcePathsOnCreate() {

		// Arrange
		final var parent = MetadataLabelEntity.create()
			.withResourceName("parent");

		final var level1 = MetadataLabelEntity.create()
			.withResourceName("level1")
			.withParent(parent);

		final var level2 = MetadataLabelEntity.create()
			.withResourceName("level2")
			.withParent(level1);

		final var level3 = MetadataLabelEntity.create()
			.withResourceName("level3")
			.withParent(level2);

		// Build relationships
		parent.addChild(level1);
		level1.addChild(level2);
		level2.addChild(level3);

		// Act — simulate JPA lifecycle (PrePersist)
		parent.onCreate();
		level1.onCreate();
		level2.onCreate();
		level3.onCreate();

		// Assert — paths are correct
		assertThat(parent.getResourcePath()).isEqualTo("parent");
		assertThat(level1.getResourcePath()).isEqualTo("parent/level1");
		assertThat(level2.getResourcePath()).isEqualTo("parent/level1/level2");
		assertThat(level3.getResourcePath()).isEqualTo("parent/level1/level2/level3");
	}

	@Test
	void onCreateShouldHandleMissingResourceNameGracefully() {

		// Arrange
		final var entity = MetadataLabelEntity.create()
			.withResourceName(null);

		// Act
		entity.onCreate();

		// Assert
		assertThat(entity.getResourcePath()).isNull();
	}

	@Test
	void onUpdate() {
		final var entity = MetadataLabelEntity.create();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified", "metadataLabels");
	}

	@Test
	void onUpdateShouldUpdateChildPathsRecursivelyWhenParentChanges() {

		// Arrange
		final var parent = MetadataLabelEntity.create()
			.withResourceName("parent");

		final var child = MetadataLabelEntity.create()
			.withResourceName("child")
			.withParent(parent);

		parent.addChild(child);

		// Initial persist
		parent.onCreate();
		child.onCreate();

		assertThat(parent.getResourcePath()).isEqualTo("parent");
		assertThat(child.getResourcePath()).isEqualTo("parent/child");

		// Act — simulate rename + PreUpdate
		parent.setResourceName("renamed");
		parent.onUpdate();

		// Assert — paths are updated recursively
		assertThat(parent.getResourcePath()).isEqualTo("renamed");
		assertThat(child.getResourcePath()).isEqualTo("renamed/child");
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(MetadataLabelEntity.create()).hasAllNullFieldsOrPropertiesExcept("metadataLabels");
		assertThat(new MetadataLabelEntity()).hasAllNullFieldsOrPropertiesExcept("metadataLabels");
	}
}
