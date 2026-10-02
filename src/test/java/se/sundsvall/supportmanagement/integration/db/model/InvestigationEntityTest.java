package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.allOf;

class InvestigationEntityTest {

	// What the artefact points at rather than what it is. None of it identifies the artefact, and comparing it would
	// walk back into the errand the artefact already hangs on.
	private static final String[] RELATIONS = {
		"errandEntity", "sections", "attachments", "jsonParameters", "parameters"
	};

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> ItemStatus.values()[new Random().nextInt(ItemStatus.values().length)], ItemStatus.class);
	}

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(InvestigationEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding(RELATIONS),
			hasValidBeanEqualsExcluding(RELATIONS),
			hasValidBeanToStringExcluding(RELATIONS)));
	}

	@Test
	void hasValidBuilderMethods() {

		// Arrange
		final var id = "id";
		final var errandEntity = ErrandEntity.create().withId("errandId");
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var type = "type";
		final var status = ItemStatus.ACTIVE;
		final var title = "title";
		final var description = "description";
		final var dueAt = now().plusDays(10);
		final var completedAt = now().plusDays(20);
		final var createdBy = "createdBy";
		final var modifiedBy = "modifiedBy";
		final var created = now();
		final var modified = now();
		final var version = 1L;
		final var investigatorUserId = "jo12doe";
		final var startedAt = now().plusDays(1);
		final var summary = "summary";
		final var conclusion = "conclusion";
		final var recommendation = "APPROVAL";
		final var recommendationMotivation = "recommendationMotivation";
		final var sections = List.of(InvestigationSectionEntity.create());
		final var attachments = List.of(AttachmentEntity.create());
		final var jsonParameters = List.of(InvestigationJsonParameterEntity.create());
		final var parameters = List.of(InvestigationParameterEntity.create());

		// Act
		final var result = InvestigationEntity.create()
			.withId(id)
			.withErrandEntity(errandEntity)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withType(type)
			.withStatus(status)
			.withTitle(title)
			.withDescription(description)
			.withDueAt(dueAt)
			.withCompletedAt(completedAt)
			.withCreatedBy(createdBy)
			.withModifiedBy(modifiedBy)
			.withCreated(created)
			.withModified(modified)
			.withVersion(version)
			.withInvestigatorUserId(investigatorUserId)
			.withStartedAt(startedAt)
			.withSummary(summary)
			.withConclusion(conclusion)
			.withRecommendation(recommendation)
			.withRecommendationMotivation(recommendationMotivation)
			.withSections(sections)
			.withAttachments(attachments)
			.withJsonParameters(jsonParameters)
			.withParameters(parameters);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result)
			.extracting(InvestigationEntity::getId, InvestigationEntity::getErrandEntity, InvestigationEntity::getMunicipalityId, InvestigationEntity::getNamespace, InvestigationEntity::getType, InvestigationEntity::getStatus)
			.containsExactly(id, errandEntity, municipalityId, namespace, type, status);
		assertThat(result)
			.extracting(InvestigationEntity::getTitle, InvestigationEntity::getDescription, InvestigationEntity::getDueAt, InvestigationEntity::getCompletedAt, InvestigationEntity::getCreatedBy, InvestigationEntity::getModifiedBy)
			.containsExactly(title, description, dueAt, completedAt, createdBy, modifiedBy);
		assertThat(result)
			.extracting(InvestigationEntity::getCreated, InvestigationEntity::getModified, InvestigationEntity::getVersion, InvestigationEntity::getInvestigatorUserId, InvestigationEntity::getStartedAt, InvestigationEntity::getSummary)
			.containsExactly(created, modified, version, investigatorUserId, startedAt, summary);
		assertThat(result)
			.extracting(InvestigationEntity::getConclusion, InvestigationEntity::getRecommendation, InvestigationEntity::getRecommendationMotivation)
			.containsExactly(conclusion, recommendation, recommendationMotivation);
		assertThat(result)
			.extracting(InvestigationEntity::getSections, InvestigationEntity::getAttachments, InvestigationEntity::getJsonParameters, InvestigationEntity::getParameters)
			.containsExactly(sections, attachments, jsonParameters, parameters);
	}

	@Test
	void onCreateSetsCreated() {
		final var entity = InvestigationEntity.create();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created");
	}

	@Test
	void onUpdateSetsModified() {
		final var entity = InvestigationEntity.create();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified");
	}

	@Test
	void markModifiedSetsModified() {
		final var entity = InvestigationEntity.create();
		entity.markModified();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified");
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(InvestigationEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new InvestigationEntity()).hasAllNullFieldsOrProperties();
	}
}
