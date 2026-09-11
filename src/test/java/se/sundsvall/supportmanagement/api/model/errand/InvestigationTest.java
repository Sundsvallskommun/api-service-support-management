package se.sundsvall.supportmanagement.api.model.errand;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
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
import static org.hamcrest.CoreMatchers.allOf;

class InvestigationTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);

	}

	@Test
	void bean() {
		MatcherAssert.assertThat(Investigation.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builder() {

		// Arrange
		final var id = "id";
		final var type = "SUITABILITY";
		final var status = "ACTIVE";
		final var title = "title";
		final var description = "description";
		final var dueAt = now().plusDays(10);
		final var completedAt = now().plusDays(20);
		final var investigatorUserId = "investigatorUserId";
		final var startedAt = now().plusDays(1);
		final var summary = "summary";
		final var conclusion = "conclusion";
		final var recommendation = "APPROVAL";
		final var recommendationMotivation = "recommendationMotivation";
		final var sections = List.of(InvestigationSection.create());
		final var attachments = List.of(ArtefactAttachment.create());
		final var createdBy = "createdBy";
		final var modifiedBy = "modifiedBy";
		final var created = now();
		final var modified = now();
		final var version = 1L;

		// Act
		final var result = Investigation.create()
			.withId(id)
			.withType(type)
			.withStatus(status)
			.withTitle(title)
			.withDescription(description)
			.withDueAt(dueAt)
			.withCompletedAt(completedAt)
			.withInvestigatorUserId(investigatorUserId)
			.withStartedAt(startedAt)
			.withSummary(summary)
			.withConclusion(conclusion)
			.withRecommendation(recommendation)
			.withRecommendationMotivation(recommendationMotivation)
			.withSections(sections)
			.withAttachments(attachments)
			.withCreatedBy(createdBy)
			.withModifiedBy(modifiedBy)
			.withCreated(created)
			.withModified(modified)
			.withVersion(version);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getType()).isEqualTo(type);
		assertThat(result.getStatus()).isEqualTo(status);
		assertThat(result.getTitle()).isEqualTo(title);
		assertThat(result.getDescription()).isEqualTo(description);
		assertThat(result.getDueAt()).isEqualTo(dueAt);
		assertThat(result.getCompletedAt()).isEqualTo(completedAt);
		assertThat(result.getInvestigatorUserId()).isEqualTo(investigatorUserId);
		assertThat(result.getStartedAt()).isEqualTo(startedAt);
		assertThat(result.getSummary()).isEqualTo(summary);
		assertThat(result.getConclusion()).isEqualTo(conclusion);
		assertThat(result.getRecommendation()).isEqualTo(recommendation);
		assertThat(result.getRecommendationMotivation()).isEqualTo(recommendationMotivation);
		assertThat(result.getSections()).isEqualTo(sections);
		assertThat(result.getAttachments()).isEqualTo(attachments);
		assertThat(result.getCreatedBy()).isEqualTo(createdBy);
		assertThat(result.getModifiedBy()).isEqualTo(modifiedBy);
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getModified()).isEqualTo(modified);
		assertThat(result.getVersion()).isEqualTo(version);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(Investigation.create()).hasAllNullFieldsOrProperties();
		assertThat(new Investigation()).hasAllNullFieldsOrProperties();
	}
}
