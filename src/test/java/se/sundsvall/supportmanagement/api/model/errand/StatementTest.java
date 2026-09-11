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

class StatementTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);

	}

	@Test
	void bean() {
		MatcherAssert.assertThat(Statement.class, allOf(
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
		final var type = "REFERRAL";
		final var status = "ACTIVE";
		final var title = "title";
		final var description = "description";
		final var dueAt = now().plusDays(10);
		final var completedAt = now().plusDays(20);
		final var counterpartyName = "counterpartyName";
		final var counterpartyExternalId = "2120002411";
		final var counterpartyExternalIdType = "ORGANIZATION_NUMBER";
		final var counterpartyReference = "counterpartyReference";
		final var question = "question";
		final var sentAt = now().plusDays(1);
		final var remindedAt = now().plusDays(2);
		final var respondedAt = now().plusDays(3);
		final var outcome = "SUPPORTS";
		final var responseText = "responseText";
		final var communicationId = "communicationId";
		final var attachments = List.of(ArtefactAttachment.create());
		final var createdBy = "createdBy";
		final var modifiedBy = "modifiedBy";
		final var created = now();
		final var modified = now();
		final var version = 1L;

		// Act
		final var result = Statement.create()
			.withId(id)
			.withType(type)
			.withStatus(status)
			.withTitle(title)
			.withDescription(description)
			.withDueAt(dueAt)
			.withCompletedAt(completedAt)
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
		assertThat(result.getCounterpartyName()).isEqualTo(counterpartyName);
		assertThat(result.getCounterpartyExternalId()).isEqualTo(counterpartyExternalId);
		assertThat(result.getCounterpartyExternalIdType()).isEqualTo(counterpartyExternalIdType);
		assertThat(result.getCounterpartyReference()).isEqualTo(counterpartyReference);
		assertThat(result.getQuestion()).isEqualTo(question);
		assertThat(result.getSentAt()).isEqualTo(sentAt);
		assertThat(result.getRemindedAt()).isEqualTo(remindedAt);
		assertThat(result.getRespondedAt()).isEqualTo(respondedAt);
		assertThat(result.getOutcome()).isEqualTo(outcome);
		assertThat(result.getResponseText()).isEqualTo(responseText);
		assertThat(result.getCommunicationId()).isEqualTo(communicationId);
		assertThat(result.getAttachments()).isEqualTo(attachments);
		assertThat(result.getCreatedBy()).isEqualTo(createdBy);
		assertThat(result.getModifiedBy()).isEqualTo(modifiedBy);
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getModified()).isEqualTo(modified);
		assertThat(result.getVersion()).isEqualTo(version);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(Statement.create()).hasAllNullFieldsOrProperties();
		assertThat(new Statement()).hasAllNullFieldsOrProperties();
	}
}
