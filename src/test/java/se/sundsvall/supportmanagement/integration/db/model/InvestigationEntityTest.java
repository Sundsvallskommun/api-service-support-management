package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionOutcome;
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
		"errandEntity", "sections", "attachments", "jsonParameterLinks"
	};

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> ItemStatus.values()[new Random().nextInt(ItemStatus.values().length)], ItemStatus.class);
		registerValueGenerator(() -> DecisionOutcome.values()[new Random().nextInt(DecisionOutcome.values().length)], DecisionOutcome.class);
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
		final var recommendation = DecisionOutcome.APPROVAL;
		final var recommendationMotivation = "recommendationMotivation";
		final var sections = List.of(InvestigationSectionEntity.create());
		final var attachments = List.of(InvestigationAttachmentEntity.create());
		final var jsonParameterLinks = List.of(InvestigationJsonParameterEntity.create());

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
			.withJsonParameterLinks(jsonParameterLinks);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getErrandEntity()).isEqualTo(errandEntity);
		assertThat(result.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(result.getNamespace()).isEqualTo(namespace);
		assertThat(result.getType()).isEqualTo(type);
		assertThat(result.getStatus()).isEqualTo(status);
		assertThat(result.getTitle()).isEqualTo(title);
		assertThat(result.getDescription()).isEqualTo(description);
		assertThat(result.getDueAt()).isEqualTo(dueAt);
		assertThat(result.getCompletedAt()).isEqualTo(completedAt);
		assertThat(result.getCreatedBy()).isEqualTo(createdBy);
		assertThat(result.getModifiedBy()).isEqualTo(modifiedBy);
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getModified()).isEqualTo(modified);
		assertThat(result.getVersion()).isEqualTo(version);
		assertThat(result.getInvestigatorUserId()).isEqualTo(investigatorUserId);
		assertThat(result.getStartedAt()).isEqualTo(startedAt);
		assertThat(result.getSummary()).isEqualTo(summary);
		assertThat(result.getConclusion()).isEqualTo(conclusion);
		assertThat(result.getRecommendation()).isEqualTo(recommendation);
		assertThat(result.getRecommendationMotivation()).isEqualTo(recommendationMotivation);
		assertThat(result.getSections()).isEqualTo(sections);
		assertThat(result.getAttachments()).isEqualTo(attachments);
		assertThat(result.getJsonParameterLinks()).isEqualTo(jsonParameterLinks);
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
	void noDirtOnCreatedBean() {
		assertThat(InvestigationEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new InvestigationEntity()).hasAllNullFieldsOrProperties();
	}
}
