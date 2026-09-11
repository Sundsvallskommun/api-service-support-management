package se.sundsvall.supportmanagement.service.mapper;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.errand.Statement;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentPurposeEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.StatementOutcome;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.service.mapper.ErrandStatementMapper.toStatement;
import static se.sundsvall.supportmanagement.service.mapper.ErrandStatementMapper.toStatementEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandStatementMapper.toStatements;
import static se.sundsvall.supportmanagement.service.mapper.ErrandStatementMapper.updateStatementEntity;

class ErrandStatementMapperTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";

	@Test
	void testToStatementEntity() {

		// Arrange
		final var errandEntity = ErrandEntity.create().withId("errandId");
		final var dueAt = now().plusDays(10);
		final var sentAt = now();
		final var statement = Statement.create()
			.withType("REFERRAL")
			.withStatus("ACTIVE")
			.withTitle("title")
			.withDescription("description")
			.withDueAt(dueAt)
			.withCounterpartyName("Miljokontoret")
			.withCounterpartyExternalId("2120002411")
			.withCounterpartyExternalIdType("ORGANIZATION_NUMBER")
			.withCounterpartyReference("MK-2024-0042")
			.withQuestion("question")
			.withSentAt(sentAt)
			.withOutcome("SUPPORTS")
			.withResponseText("responseText")
			.withCommunicationId("communicationId");

		// Act
		final var result = toStatementEntity(statement, errandEntity, NAMESPACE, MUNICIPALITY_ID);

		// Assert
		assertThat(result.getErrandEntity()).isSameAs(errandEntity);
		assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(result.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(result.getStatus()).isEqualTo(ItemStatus.ACTIVE);
		assertThat(result.getOutcome()).isEqualTo(StatementOutcome.SUPPORTS);
		assertThat(result.getDueAt()).isEqualTo(dueAt);
		assertThat(result.getSentAt()).isEqualTo(sentAt);
		assertThat(result.getCounterpartyName()).isEqualTo("Miljokontoret");
		assertThat(result.getCommunicationId()).isEqualTo("communicationId");
	}

	@Test
	void testToStatementEntityWithoutEnums() {

		// Act
		final var result = toStatementEntity(Statement.create().withCounterpartyName("name"), ErrandEntity.create(), NAMESPACE, MUNICIPALITY_ID);

		// Assert
		assertThat(result.getStatus()).isNull();
		assertThat(result.getOutcome()).isNull();
	}

	@Test
	void testUpdateStatementEntity() {

		// Arrange
		final var entity = StatementEntity.create()
			.withStatus(ItemStatus.DRAFT)
			.withTitle("old title")
			.withCounterpartyName("old name");

		// Act
		final var result = updateStatementEntity(entity, Statement.create()
			.withStatus("ACTIVE")
			.withOutcome("NO_RESPONSE")
			.withCounterpartyName("new name"));

		// Assert
		assertThat(result).isSameAs(entity);
		assertThat(result.getStatus()).isEqualTo(ItemStatus.ACTIVE);
		assertThat(result.getOutcome()).isEqualTo(StatementOutcome.NO_RESPONSE);
		assertThat(result.getCounterpartyName()).isEqualTo("new name");
	}

	@Test
	void testUpdateStatementEntityLeavesOmittedFieldsAlone() {

		// Arrange
		final var entity = StatementEntity.create()
			.withStatus(ItemStatus.ACTIVE)
			.withTitle("title")
			.withCounterpartyName("name")
			.withOutcome(StatementOutcome.SUPPORTS);

		// Act - a patch that says nothing about anything
		final var result = updateStatementEntity(entity, Statement.create());

		// Assert
		assertThat(result.getStatus()).isEqualTo(ItemStatus.ACTIVE);
		assertThat(result.getTitle()).isEqualTo("title");
		assertThat(result.getCounterpartyName()).isEqualTo("name");
		assertThat(result.getOutcome()).isEqualTo(StatementOutcome.SUPPORTS);
	}

	@Test
	void testToStatement() {

		// Arrange
		final var created = now();
		final var entity = StatementEntity.create()
			.withId("id")
			.withType("REFERRAL")
			.withStatus(ItemStatus.COMPLETED)
			.withTitle("title")
			.withCounterpartyName("name")
			.withOutcome(StatementOutcome.OPPOSES)
			.withCreatedBy("jo12doe")
			.withCreated(created)
			.withVersion(3L)
			.withAttachments(List.of(StatementAttachmentEntity.create()
				.withAttachmentEntity(AttachmentEntity.create().withId("attachmentId").withFileName("file.pdf").withPurpose(AttachmentPurposeEntity.create().withId("purposeId").withName("RESPONSE")))));

		// Act
		final var result = toStatement(entity);

		// Assert
		assertThat(result.getId()).isEqualTo("id");
		assertThat(result.getStatus()).isEqualTo("COMPLETED");
		assertThat(result.getOutcome()).isEqualTo("OPPOSES");
		assertThat(result.getCreatedBy()).isEqualTo("jo12doe");
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getVersion()).isEqualTo(3L);
		assertThat(result.getAttachments()).singleElement().satisfies(attachment -> {
			assertThat(attachment.getAttachmentId()).isEqualTo("attachmentId");
			assertThat(attachment.getFileName()).isEqualTo("file.pdf");
			assertThat(attachment.getPurpose().getName()).isEqualTo("RESPONSE");
		});
	}

	@Test
	void testToStatementWithNull() {
		assertThat(toStatement(null)).isNull();
	}

	@Test
	void testToStatementWithoutEnums() {

		// Act
		final var result = toStatement(StatementEntity.create().withId("id"));

		// Assert
		assertThat(result.getStatus()).isNull();
		assertThat(result.getOutcome()).isNull();
		assertThat(result.getAttachments()).isEmpty();
	}

	@Test
	void testToStatements() {
		assertThat(toStatements(List.of(StatementEntity.create().withId("id")))).hasSize(1);
	}

	@Test
	void testToStatementsWithNull() {
		assertThat(toStatements(null)).isEmpty();
	}

	@Test
	void testTimestampsSurviveTheRoundTrip() {

		// Arrange
		final OffsetDateTime respondedAt = now().plusDays(5);

		// Act
		final var result = toStatement(toStatementEntity(Statement.create()
			.withCounterpartyName("name")
			.withRespondedAt(respondedAt), ErrandEntity.create(), NAMESPACE, MUNICIPALITY_ID));

		// Assert
		assertThat(result.getRespondedAt()).isEqualTo(respondedAt);
	}
}
