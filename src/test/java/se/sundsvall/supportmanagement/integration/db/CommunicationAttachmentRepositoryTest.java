package se.sundsvall.supportmanagement.integration.db;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentEntity;

@SpringBootTest
@ActiveProfiles("junit")
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
@Transactional
class CommunicationAttachmentRepositoryTest {

	@Autowired
	private CommunicationAttachmentRepository communicationAttachmentRepository;

	@Test
	void create() {
		// Setup
		final var communicationAttachmentEntity = CommunicationAttachmentEntity.create()
			.withAttachmentID("attachmentID")
			.withName("name")
			.withContentType("contentType");

		// Execution
		final var persistedEntity = communicationAttachmentRepository.save(communicationAttachmentEntity);

		// Assertions
		assertThat(persistedEntity).isNotNull();
		assertThat(persistedEntity.getAttachmentID()).isEqualTo("attachmentID");
		assertThat(persistedEntity.getName()).isEqualTo("name");
		assertThat(persistedEntity.getContentType()).isEqualTo("contentType");
	}

	@Test
	void delete() {
		// Setup
		final var communicationAttachmentEntity = communicationAttachmentRepository.findById("attach1").orElseThrow();

		// Execution
		communicationAttachmentRepository.delete(communicationAttachmentEntity);

		// Assertions
		assertThat(communicationAttachmentRepository.findById("attachmentID")).isNotPresent();
	}

}
