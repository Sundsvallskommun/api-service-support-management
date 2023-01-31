package se.sundsvall.supportmanagement.integration.db;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

import java.time.OffsetDateTime;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;

/**
 * Attachment repository tests.
 *
 * @see src/test/resources/db/testdata.sql for data setup.
 */
@SpringBootTest
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class AttachmentRepositoryTest {

	@Autowired
	private AttachmentRepository attachmentRepository;

	@Autowired
	private ErrandsRepository errandsRepository;

	@Test
	@Transactional
	void create() {

		final var errandId = "ERRAND_ID-3";
		final var fileName = "TestAttachment.txt";
		final var file = "TestAttachment".getBytes();
		final var mimeType = "text/plain";
		final var attachmentEntity = AttachmentEntity.create().withFile(file).withFileName(fileName).withMimeType(mimeType);


		assertThat(attachmentRepository.findByErrandEntityId(errandId)).isNotNull().isEmpty();

		final var errandEntity = errandsRepository.findById(errandId).get();
		attachmentEntity.setErrandEntity(errandEntity);

		// Execution
		attachmentRepository.save(attachmentEntity);

		// Assertions
		final var persistedEntities = attachmentRepository.findByErrandEntityId(errandId);

		assertThat(persistedEntities).isNotNull().hasSize(1);
		assertThat(persistedEntities.get(0).getFileName()).isEqualTo(fileName);
		assertThat(persistedEntities.get(0).getFile()).isEqualTo(file);
		assertThat(persistedEntities.get(0).getMimeType()).isEqualTo(mimeType);
		assertThat(persistedEntities.get(0).getErrandEntity()).isEqualTo(errandEntity);
		assertThat(persistedEntities.get(0).getCreated()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		assertThat(persistedEntities.get(0).getModified()).isNull();
	}

	@Test
	void delete() {

		final var id = "ATTACHMENT_ID-3";
		// Setup
		final var existingAttachment = attachmentRepository.findById(id).orElseThrow();

		// Execution
		attachmentRepository.delete(existingAttachment);

		// Assertions
		assertThat(attachmentRepository.findById(id)).isNotPresent();
	}

	@Test
	void findByErrandIdNull() {
		assertThat(attachmentRepository.findByErrandEntityId(null)).isEmpty();
	}

	@Test
	void findByErrandIdNotFound() {
		assertThat(attachmentRepository.findByErrandEntityId("NotExistingId")).isEmpty();
	}

	@Test
	void findByErrandId() {

		final var errandId = "ERRAND_ID-2";
		final var attachmentEntities = attachmentRepository.findByErrandEntityId(errandId);

		assertThat(attachmentEntities).isNotNull().hasSize(2);

		assertThat(attachmentEntities)
			.extracting(AttachmentEntity::getId, AttachmentEntity::getFileName).containsExactlyInAnyOrder(
				tuple("ATTACHMENT_ID-2", "Test.txt"),
				tuple("ATTACHMENT_ID-3", "Test2.txt"));
	}

}
