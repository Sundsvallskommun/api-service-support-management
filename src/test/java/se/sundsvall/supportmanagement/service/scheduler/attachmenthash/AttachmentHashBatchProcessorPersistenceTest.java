package se.sundsvall.supportmanagement.service.scheduler.attachmenthash;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.service.util.ServiceUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Persistence-level test verifying that {@link AttachmentHashBatchProcessor} actually commits the computed hash to the
 * database.
 *
 * <p>
 * This guards a regression: the entity is detached inside the processing loop, so hashing with {@code save()} (which
 * does not flush) discarded the pending update on detach and persisted nothing, while all mocked unit tests still
 * passed. Only a real database round-trip catches it.
 *
 * <p>
 * The test method runs with {@link Propagation#NOT_SUPPORTED} so it is <em>not</em> wrapped in a test-managed
 * transaction. That matters because {@code processBatch} commits in its own {@code REQUIRES_NEW} transaction; verifying
 * from within a surrounding transaction would return a stale REPEATABLE_READ snapshot taken before that commit. Instead
 * each read runs in its own short transaction via {@link TransactionTemplate}, so it observes committed state and has
 * an
 * open session for lazy blob loading.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Import(AttachmentHashBatchProcessor.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class AttachmentHashBatchProcessorPersistenceTest {

	// Attachments seeded by testdata-junit.sql, each with a known blob and no hash.
	private static final List<String> ATTACHMENT_IDS = List.of("ATTACHMENT_ID-1", "ATTACHMENT_ID-2", "ATTACHMENT_ID-3");

	@Autowired
	private AttachmentHashBatchProcessor batchProcessor;

	@Autowired
	private AttachmentRepository attachmentRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void computedHashIsPersisted() {
		final var txTemplate = new TransactionTemplate(transactionManager);

		// Precondition: hashes are null, and capture the expected hash computed from each blob (each in its own tx
		// so the lazily loaded blob has an open session).
		final Map<String, String> expectedHashes = new HashMap<>();
		ATTACHMENT_IDS.forEach(id -> txTemplate.executeWithoutResult(status -> {
			final var attachment = attachmentRepository.findById(id).orElseThrow();
			assertThat(attachment.getHash()).as("precondition: hash should be null for %s", id).isNull();
			try {
				expectedHashes.put(id, ServiceUtil.computeSha256Hex(attachment.getAttachmentData().getFile().getBinaryStream()));
			} catch (final SQLException e) {
				throw new IllegalStateException(e);
			}
		}));

		// Act - runs in its own REQUIRES_NEW transaction that commits.
		final var processed = batchProcessor.processBatch(ATTACHMENT_IDS);

		// Assert - hash is committed, well-formed and matches the content hash.
		assertThat(processed).isEqualTo(ATTACHMENT_IDS.size());
		ATTACHMENT_IDS.forEach(id -> txTemplate.executeWithoutResult(status -> {
			final var attachment = attachmentRepository.findById(id).orElseThrow();
			assertThat(attachment.getHash())
				.as("persisted hash for %s", id)
				.isNotNull()
				.hasSize(64)
				.isEqualTo(expectedHashes.get(id));
		}));
	}
}
