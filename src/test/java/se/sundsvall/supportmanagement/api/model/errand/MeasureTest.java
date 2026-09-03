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

class MeasureTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void bean() {
		MatcherAssert.assertThat(Measure.class, allOf(
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
		final var responsibleUser = "responsibleUser";
		final var type = "type";
		final var plannedStart = now();
		final var plannedComplete = now().plusDays(30);
		final var executed = now().plusDays(15);
		final var addedByUser = "addedByUser";
		final var addedByRole = "MANAGER";
		final var goal = "goal";
		final var description = "description";
		final var accept = "TRUE";
		final var acceptMotivation = "acceptMotivation";
		final var reworkGoal = "reworkGoal";
		final var reworkDescription = "reworkDescription";
		final var created = now();
		final var modified = now();

		// Act
		final var measure = Measure.create()
			.withId(id)
			.withResponsibleUser(responsibleUser)
			.withType(type)
			.withPlannedStart(plannedStart)
			.withPlannedComplete(plannedComplete)
			.withExecuted(executed)
			.withAddedByUser(addedByUser)
			.withAddedByRole(addedByRole)
			.withGoal(goal)
			.withDescription(description)
			.withAccept(accept)
			.withAcceptMotivation(acceptMotivation)
			.withReworkGoal(reworkGoal)
			.withReworkDescription(reworkDescription)
			.withCreated(created)
			.withModified(modified);

		// Assert
		assertThat(measure).hasNoNullFieldsOrProperties();
		assertThat(measure.getId()).isEqualTo(id);
		assertThat(measure.getResponsibleUser()).isEqualTo(responsibleUser);
		assertThat(measure.getType()).isEqualTo(type);
		assertThat(measure.getPlannedStart()).isEqualTo(plannedStart);
		assertThat(measure.getPlannedComplete()).isEqualTo(plannedComplete);
		assertThat(measure.getExecuted()).isEqualTo(executed);
		assertThat(measure.getAddedByUser()).isEqualTo(addedByUser);
		assertThat(measure.getAddedByRole()).isEqualTo(addedByRole);
		assertThat(measure.getGoal()).isEqualTo(goal);
		assertThat(measure.getDescription()).isEqualTo(description);
		assertThat(measure.getAccept()).isEqualTo(accept);
		assertThat(measure.getAcceptMotivation()).isEqualTo(acceptMotivation);
		assertThat(measure.getReworkGoal()).isEqualTo(reworkGoal);
		assertThat(measure.getReworkDescription()).isEqualTo(reworkDescription);
		assertThat(measure.getCreated()).isEqualTo(created);
		assertThat(measure.getModified()).isEqualTo(modified);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(Measure.create()).hasAllNullFieldsOrPropertiesExcept("suppliedFields");
		assertThat(new Measure()).hasAllNullFieldsOrPropertiesExcept("suppliedFields");
	}

}
