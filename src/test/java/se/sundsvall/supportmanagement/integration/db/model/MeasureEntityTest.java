package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.enums.Accept;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.MeasureResult;

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

	// What the measure points at rather than what it is: the errand it belongs to, the decision or statement it follows
	// from, and the collections it links. None of them identifies a measure, and comparing them walks back into the
	// errand this measure already hangs on.
	private static final String[] RELATIONS = {
		"errandEntity", "decisionEntity", "statementEntity", "attachments", "jsonParameterLinks"
	};

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> Accept.values()[new Random().nextInt(Accept.values().length)], Accept.class);
		registerValueGenerator(() -> ItemStatus.values()[new Random().nextInt(ItemStatus.values().length)], ItemStatus.class);
		registerValueGenerator(() -> MeasureResult.values()[new Random().nextInt(MeasureResult.values().length)], MeasureResult.class);
	}

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(MeasureEntity.class, allOf(
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
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var status = ItemStatus.ACTIVE;
		final var title = "title";
		final var dueAt = now().plusDays(10);
		final var completedAt = now().plusDays(20);
		final var createdBy = "createdBy";
		final var modifiedBy = "modifiedBy";
		final var version = 1L;
		final var measureResult = MeasureResult.COMPLETED;
		final var resultText = "resultText";
		final var decisionEntity = DecisionEntity.create().withId("decisionId");
		final var statementEntity = StatementEntity.create().withId("statementId");
		final var attachments = List.of(MeasureAttachmentEntity.create());
		final var jsonParameterLinks = List.of(MeasureJsonParameterEntity.create());

		// Act
		final var result = MeasureEntity.create()
			.withId(id)
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
			.withModified(modified)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withStatus(status)
			.withTitle(title)
			.withDueAt(dueAt)
			.withCompletedAt(completedAt)
			.withCreatedBy(createdBy)
			.withModifiedBy(modifiedBy)
			.withVersion(version)
			.withResult(measureResult)
			.withResultText(resultText)
			.withDecisionEntity(decisionEntity)
			.withStatementEntity(statementEntity)
			.withAttachments(attachments)
			.withJsonParameterLinks(jsonParameterLinks);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result)
			.extracting(MeasureEntity::getId, MeasureEntity::getErrandEntity, MeasureEntity::getResponsibleUser, MeasureEntity::getType, MeasureEntity::getPlannedStart, MeasureEntity::getPlannedComplete)
			.containsExactly(id, errandEntity, responsibleUser, type, plannedStart, plannedComplete);
		assertThat(result)
			.extracting(MeasureEntity::getExecuted, MeasureEntity::getAddedByUser, MeasureEntity::getAddedByRole, MeasureEntity::getGoal, MeasureEntity::getDescription, MeasureEntity::getAccept)
			.containsExactly(executed, addedByUser, addedByRole, goal, description, accept);
		assertThat(result)
			.extracting(MeasureEntity::getAcceptMotivation, MeasureEntity::getReworkGoal, MeasureEntity::getReworkDescription, MeasureEntity::getCreated, MeasureEntity::getModified, MeasureEntity::getMunicipalityId)
			.containsExactly(acceptMotivation, reworkGoal, reworkDescription, created, modified, municipalityId);
		assertThat(result)
			.extracting(MeasureEntity::getNamespace, MeasureEntity::getStatus, MeasureEntity::getTitle, MeasureEntity::getDueAt, MeasureEntity::getCompletedAt, MeasureEntity::getCreatedBy)
			.containsExactly(namespace, status, title, dueAt, completedAt, createdBy);
		assertThat(result)
			.extracting(MeasureEntity::getModifiedBy, MeasureEntity::getVersion, MeasureEntity::getResult, MeasureEntity::getResultText, MeasureEntity::getDecisionEntity, MeasureEntity::getStatementEntity)
			.containsExactly(modifiedBy, version, measureResult, resultText, decisionEntity, statementEntity);
		assertThat(result)
			.extracting(MeasureEntity::getAttachments, MeasureEntity::getJsonParameterLinks)
			.containsExactly(attachments, jsonParameterLinks);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(MeasureEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new MeasureEntity()).hasAllNullFieldsOrProperties();
	}

}
