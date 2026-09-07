package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.enums.Accept;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;

class MeasureEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> Accept.values()[new Random().nextInt(Accept.values().length)], Accept.class);
	}

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(MeasureEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("errandEntity", "version"),
			hasValidBeanEqualsExcluding("errandEntity", "version"),
			hasValidBeanToStringExcluding("errandEntity")));
	}

	@Test
	void hasValidBuilderMethods() {

		// Arrange
		final var id = "id";
		final var errandEntity = ErrandEntity.create().withId("errandId");
		final var responsibleUser = "responsibleUser";
		final var type = "type";
		final var plannedStart = now();
		final var plannedComplete = now().plusDays(30);
		final var executed = now().plusDays(15);
		final var addedByUser = "addedByUser";
		final var addedByRole = "MANAGER";
		final var goal = "goal";
		final var description = "description";
		final var accept = Accept.TRUE;
		final var acceptMotivation = "acceptMotivation";
		final var reworkGoal = "reworkGoal";
		final var reworkDescription = "reworkDescription";
		final var created = now();
		final var modified = now();

		// Act
		final var result = MeasureEntity.create()
			.withId(id)
			.withVersion(1L)
			.withErrandEntity(errandEntity)
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
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getVersion()).isEqualTo(1L);
		assertThat(result.getErrandEntity()).isEqualTo(errandEntity);
		assertThat(result.getResponsibleUser()).isEqualTo(responsibleUser);
		assertThat(result.getType()).isEqualTo(type);
		assertThat(result.getPlannedStart()).isEqualTo(plannedStart);
		assertThat(result.getPlannedComplete()).isEqualTo(plannedComplete);
		assertThat(result.getExecuted()).isEqualTo(executed);
		assertThat(result.getAddedByUser()).isEqualTo(addedByUser);
		assertThat(result.getAddedByRole()).isEqualTo(addedByRole);
		assertThat(result.getGoal()).isEqualTo(goal);
		assertThat(result.getDescription()).isEqualTo(description);
		assertThat(result.getAccept()).isEqualTo(accept);
		assertThat(result.getAcceptMotivation()).isEqualTo(acceptMotivation);
		assertThat(result.getReworkGoal()).isEqualTo(reworkGoal);
		assertThat(result.getReworkDescription()).isEqualTo(reworkDescription);
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getModified()).isEqualTo(modified);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(MeasureEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new MeasureEntity()).hasAllNullFieldsOrProperties();
	}

}
