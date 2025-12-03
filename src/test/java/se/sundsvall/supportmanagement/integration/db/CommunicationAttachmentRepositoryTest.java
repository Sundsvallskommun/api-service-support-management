package se.sundsvall.supportmanagement.integration.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationAttachmentEntity;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class CommunicationAttachmentRepositoryTest {

	@Autowired
	private CommunicationAttachmentRepository communicationAttachmentRepository;

	@Test
	void create() {
		// Setup
		final var communicationAttachmentEntity = CommunicationAttachmentEntity.create()
			.withFileName("name")
			.withContentType("contentType");

		// Execution
		final var persistedEntity = communicationAttachmentRepository.save(communicationAttachmentEntity);

		// Assertions
		assertThat(persistedEntity).isNotNull().hasAllNullFieldsOrPropertiesExcept("id", "fileName", "mimeType");
		assertThat(persistedEntity.getFileName()).isEqualTo("name");
		assertThat(persistedEntity.getMimeType()).isEqualTo("contentType");
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
