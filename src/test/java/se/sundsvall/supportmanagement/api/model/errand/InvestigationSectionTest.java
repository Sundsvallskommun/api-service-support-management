package se.sundsvall.supportmanagement.api.model.errand;

import java.time.OffsetDateTime;
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

class InvestigationSectionTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);

	}

	@Test
	void bean() {
		MatcherAssert.assertThat(InvestigationSection.class, allOf(
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
		final var sectionKey = "financial";
		final var heading = "heading";
		final var sortOrder = 1;
		final var assessment = "APPROVED";
		final var text = "text";
		final var completedBy = "completedBy";
		final var completedAt = now().plusDays(1);

		// Act
		final var result = InvestigationSection.create()
			.withId(id)
			.withSectionKey(sectionKey)
			.withHeading(heading)
			.withSortOrder(sortOrder)
			.withAssessment(assessment)
			.withText(text)
			.withCompletedBy(completedBy)
			.withCompletedAt(completedAt);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getSectionKey()).isEqualTo(sectionKey);
		assertThat(result.getHeading()).isEqualTo(heading);
		assertThat(result.getSortOrder()).isEqualTo(sortOrder);
		assertThat(result.getAssessment()).isEqualTo(assessment);
		assertThat(result.getText()).isEqualTo(text);
		assertThat(result.getCompletedBy()).isEqualTo(completedBy);
		assertThat(result.getCompletedAt()).isEqualTo(completedAt);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(InvestigationSection.create()).hasAllNullFieldsOrProperties();
		assertThat(new InvestigationSection()).hasAllNullFieldsOrProperties();
	}
}
