package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.enums.SectionAssessment;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;

class InvestigationSectionEntityTest {

	// What the section points at rather than what it is: the investigation it belongs to and the parameters it links.
	private static final String[] RELATIONS = {
		"investigationEntity", "jsonParameterLinks"
	};

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> SectionAssessment.values()[new Random().nextInt(SectionAssessment.values().length)], SectionAssessment.class);
	}

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(InvestigationSectionEntity.class, allOf(
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
		final var investigationEntity = InvestigationEntity.create().withId("investigationId");
		final var sectionKey = "financial";
		final var heading = "heading";
		final var sortOrder = 1;
		final var assessment = SectionAssessment.APPROVED;
		final var text = "text";
		final var completedBy = "jo12doe";
		final var completedAt = now().plusDays(1);
		final var jsonParameterLinks = List.of(InvestigationSectionJsonParameterEntity.create());

		// Act
		final var result = InvestigationSectionEntity.create()
			.withId(id)
			.withInvestigationEntity(investigationEntity)
			.withSectionKey(sectionKey)
			.withHeading(heading)
			.withSortOrder(sortOrder)
			.withAssessment(assessment)
			.withText(text)
			.withCompletedBy(completedBy)
			.withCompletedAt(completedAt)
			.withJsonParameterLinks(jsonParameterLinks);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getInvestigationEntity()).isEqualTo(investigationEntity);
		assertThat(result.getSectionKey()).isEqualTo(sectionKey);
		assertThat(result.getHeading()).isEqualTo(heading);
		assertThat(result.getSortOrder()).isEqualTo(sortOrder);
		assertThat(result.getAssessment()).isEqualTo(assessment);
		assertThat(result.getText()).isEqualTo(text);
		assertThat(result.getCompletedBy()).isEqualTo(completedBy);
		assertThat(result.getCompletedAt()).isEqualTo(completedAt);
		assertThat(result.getJsonParameterLinks()).isEqualTo(jsonParameterLinks);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(InvestigationSectionEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new InvestigationSectionEntity()).hasAllNullFieldsOrProperties();
	}
}
