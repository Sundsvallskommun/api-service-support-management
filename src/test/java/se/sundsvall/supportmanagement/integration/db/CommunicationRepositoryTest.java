package se.sundsvall.supportmanagement.integration.db;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;

@SpringBootTest
@ActiveProfiles("junit")
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
@Transactional
class CommunicationRepositoryTest {

	@Autowired
	private CommunicationRepository communicationRepository;

	@Test
	void create() {
		// Setup
		final var communicationEntity = CommunicationEntity.create()
			.withId("id")
			.withSender("sender")
			.withErrandNumber("errandNumber")
			.withDirection(Direction.INBOUND)
			.withExternalCaseID("externalCaseID")
			.withSubject("subject")
			.withMessageBody("messageBody")
			.withSent(OffsetDateTime.now())
			.withType(CommunicationType.EMAIL)
			.withTarget("target")
			.withViewed(true)
			.withAttachments(List.of(CommunicationAttachmentEntity.create().withId(UUID.randomUUID().toString())));

		// Execution
		final var persistedEntity = communicationRepository.save(communicationEntity);

		// Assertions
		assertThat(persistedEntity).isNotNull();
		assertThat(persistedEntity.getId()).isEqualTo("id");
		assertThat(persistedEntity.getSender()).isEqualTo("sender");
		assertThat(persistedEntity.getErrandNumber()).isEqualTo("errandNumber");
		assertThat(persistedEntity.getDirection()).isEqualTo(Direction.INBOUND);
		assertThat(persistedEntity.getExternalCaseID()).isEqualTo("externalCaseID");
		assertThat(persistedEntity.getSubject()).isEqualTo("subject");
		assertThat(persistedEntity.getMessageBody()).isEqualTo("messageBody");
		assertThat(persistedEntity.getSent()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		assertThat(persistedEntity.getType()).isEqualTo(CommunicationType.EMAIL);
		assertThat(persistedEntity.getTarget()).isEqualTo("target");
		assertThat(persistedEntity.isViewed()).isTrue();
		assertThat(persistedEntity.getAttachments()).hasSize(1);
	}

	@Test
	void findByErrandNumber() {
		// Setup
		final var errandNumber = "errand1";

		// Execution
		final var communications = communicationRepository.findByErrandNumber(errandNumber);

		// Assertions
		assertThat(communications).isNotEmpty();
		assertThat(communications.getFirst().getErrandNumber()).isEqualTo(errandNumber);
	}

	@Test
	void delete() {
		// Setup
		final var communicationEntity = communicationRepository.findById("comm1").orElseThrow();

		// Execution
		communicationRepository.delete(communicationEntity);

		// Assertions
		assertThat(communicationRepository.findById("communicationID")).isNotPresent();
	}

}
