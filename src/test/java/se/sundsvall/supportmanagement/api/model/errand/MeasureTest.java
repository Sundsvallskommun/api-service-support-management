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
		final var status = "ACTIVE";
		final var title = "title";
		final var dueAt = now().plusDays(10);
		final var completedAt = now().plusDays(20);
		final var result = "COMPLETED";
		final var resultText = "resultText";
		final var decisionId = "decisionId";
		final var statementId = "statementId";
		final var attachments = List.of(ArtefactAttachment.create().withAttachmentId("attachmentId"));
		final var createdBy = "createdBy";
		final var modifiedBy = "modifiedBy";
		final var version = 1L;

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
			.withModified(modified)
			.withStatus(status)
			.withTitle(title)
			.withDueAt(dueAt)
			.withCompletedAt(completedAt)
			.withResult(result)
			.withResultText(resultText)
			.withDecisionId(decisionId)
			.withStatementId(statementId)
			.withAttachments(attachments)
			.withCreatedBy(createdBy)
			.withModifiedBy(modifiedBy)
			.withVersion(version);

		// Assert
		assertThat(measure).hasNoNullFieldsOrProperties();
		assertThat(measure)
			.extracting(Measure::getId, Measure::getResponsibleUser, Measure::getType, Measure::getPlannedStart, Measure::getPlannedComplete, Measure::getExecuted)
			.containsExactly(id, responsibleUser, type, plannedStart, plannedComplete, executed);
		assertThat(measure)
			.extracting(Measure::getAddedByUser, Measure::getAddedByRole, Measure::getGoal, Measure::getDescription, Measure::getAccept, Measure::getAcceptMotivation)
			.containsExactly(addedByUser, addedByRole, goal, description, accept, acceptMotivation);
		assertThat(measure)
			.extracting(Measure::getReworkGoal, Measure::getReworkDescription, Measure::getStatus, Measure::getTitle, Measure::getDueAt, Measure::getCompletedAt)
			.containsExactly(reworkGoal, reworkDescription, status, title, dueAt, completedAt);
		assertThat(measure)
			.extracting(Measure::getResult, Measure::getResultText, Measure::getDecisionId, Measure::getStatementId, Measure::getAttachments, Measure::getCreatedBy)
			.containsExactly(result, resultText, decisionId, statementId, attachments, createdBy);
		assertThat(measure)
			.extracting(Measure::getModifiedBy, Measure::getVersion, Measure::getCreated, Measure::getModified)
			.containsExactly(modifiedBy, version, created, modified);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(Measure.create()).hasAllNullFieldsOrProperties();
		assertThat(new Measure()).hasAllNullFieldsOrProperties();
	}

}
