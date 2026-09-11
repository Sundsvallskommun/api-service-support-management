package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.StatementOutcome;

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

class StatementEntityTest {

	// What the artefact points at rather than what it is. None of it identifies the artefact, and comparing it would
	// walk back into the errand the artefact already hangs on.
	private static final String[] RELATIONS = {
		"errandEntity", "attachments", "jsonParameterLinks"
	};

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> ItemStatus.values()[new Random().nextInt(ItemStatus.values().length)], ItemStatus.class);
		registerValueGenerator(() -> StatementOutcome.values()[new Random().nextInt(StatementOutcome.values().length)], StatementOutcome.class);
	}

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(StatementEntity.class, allOf(
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
		final var counterpartyName = "Miljökontoret";
		final var counterpartyExternalId = "2120002411";
		final var counterpartyExternalIdType = "ORGANIZATION_NUMBER";
		final var counterpartyReference = "MK-2024-0042";
		final var question = "question";
		final var sentAt = now().plusDays(1);
		final var remindedAt = now().plusDays(2);
		final var respondedAt = now().plusDays(3);
		final var outcome = StatementOutcome.SUPPORTS;
		final var responseText = "responseText";
		final var communicationId = "communicationId";
		final var attachments = List.of(StatementAttachmentEntity.create());
		final var jsonParameterLinks = List.of(StatementJsonParameterEntity.create());

		// Act
		final var result = StatementEntity.create()
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
			.withCounterpartyName(counterpartyName)
			.withCounterpartyExternalId(counterpartyExternalId)
			.withCounterpartyExternalIdType(counterpartyExternalIdType)
			.withCounterpartyReference(counterpartyReference)
			.withQuestion(question)
			.withSentAt(sentAt)
			.withRemindedAt(remindedAt)
			.withRespondedAt(respondedAt)
			.withOutcome(outcome)
			.withResponseText(responseText)
			.withCommunicationId(communicationId)
			.withAttachments(attachments)
			.withJsonParameterLinks(jsonParameterLinks);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result)
			.extracting(StatementEntity::getId, StatementEntity::getErrandEntity, StatementEntity::getMunicipalityId, StatementEntity::getNamespace, StatementEntity::getType, StatementEntity::getStatus)
			.containsExactly(id, errandEntity, municipalityId, namespace, type, status);
		assertThat(result)
			.extracting(StatementEntity::getTitle, StatementEntity::getDescription, StatementEntity::getDueAt, StatementEntity::getCompletedAt, StatementEntity::getCreatedBy, StatementEntity::getModifiedBy)
			.containsExactly(title, description, dueAt, completedAt, createdBy, modifiedBy);
		assertThat(result)
			.extracting(StatementEntity::getCreated, StatementEntity::getModified, StatementEntity::getVersion, StatementEntity::getCounterpartyName, StatementEntity::getCounterpartyExternalId, StatementEntity::getCounterpartyExternalIdType)
			.containsExactly(created, modified, version, counterpartyName, counterpartyExternalId, counterpartyExternalIdType);
		assertThat(result)
			.extracting(StatementEntity::getCounterpartyReference, StatementEntity::getQuestion, StatementEntity::getSentAt, StatementEntity::getRemindedAt, StatementEntity::getRespondedAt, StatementEntity::getOutcome)
			.containsExactly(counterpartyReference, question, sentAt, remindedAt, respondedAt, outcome);
		assertThat(result)
			.extracting(StatementEntity::getResponseText, StatementEntity::getCommunicationId, StatementEntity::getAttachments, StatementEntity::getJsonParameterLinks)
			.containsExactly(responseText, communicationId, attachments, jsonParameterLinks);
	}

	@Test
	void onCreateSetsCreated() {
		final var entity = StatementEntity.create();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created");
	}

	@Test
	void onUpdateSetsModified() {
		final var entity = StatementEntity.create();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified");
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(StatementEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new StatementEntity()).hasAllNullFieldsOrProperties();
	}
}
