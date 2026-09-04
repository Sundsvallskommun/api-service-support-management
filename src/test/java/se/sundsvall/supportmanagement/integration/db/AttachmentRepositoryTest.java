package se.sundsvall.supportmanagement.integration.db;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataIdProjection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

/**
 * What these tests are here for is the one thing about attachments that cannot be read off the code: whether removing
 * them loads the files they hold. A file is a blob of up to fifty megabytes, an errand may carry any number of them,
 * and a retention purge walks errands by the thousand - so a removal that loads what it removes is a removal that ends
 * the service rather than the errand.
 * <p>
 * Statistics are what answers that, since a load leaves no other trace. They are switched on for this test alone.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class AttachmentRepositoryTest {

	// Held by no communication, so it can be removed on its own without taking a blob out from under one.
	private static final String ATTACHMENT_ID = "ATTACHMENT_ID-3";
	private static final int ATTACHMENT_DATA_ID = 3;

	@Autowired
	private AttachmentRepository attachmentRepository;

	@Autowired
	private AttachmentDataRepository attachmentDataRepository;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Test
	@DisplayName("Verification that the id of an attachment's data row can be read without the file in it being loaded")
	void findByIdInReadsTheDataIdsWithoutTheFiles() {
		final var statistics = statistics();
		statistics.clear();

		final var dataIds = attachmentRepository.findByIdIn(List.of("ATTACHMENT_ID-2", ATTACHMENT_ID)).stream()
			.map(AttachmentDataIdProjection::getAttachmentDataId)
			.toList();

		assertThat(dataIds).containsExactlyInAnyOrder(2, ATTACHMENT_DATA_ID);
		assertThat(loadCountOfAttachmentData(statistics)).isZero();
	}

	@Test
	@DisplayName("Verification that an attachment id nothing answers to yields nothing, rather than a row with no data id")
	void findByIdInWithAnIdThatIsNotThere() {
		assertThat(attachmentRepository.findByIdIn(List.of("ATTACHMENT_ID-that-is-not-there"))).isEmpty();
	}

	@Test
	@DisplayName("Verification that an attachment and the file it holds are both removed, and that neither is loaded on the way")
	void deleteAllByIdInBatchRemovesBothRowsWithoutLoadingTheFile() {
		final var statistics = statistics();
		statistics.clear();

		// The attachment first: it is the one holding the foreign key.
		attachmentRepository.deleteAllByIdInBatch(List.of(ATTACHMENT_ID));
		attachmentDataRepository.deleteAllByIdInBatch(List.of(ATTACHMENT_DATA_ID));

		final var loadCount = loadCountOfAttachmentData(statistics);

		assertThat(attachmentRepository.findById(ATTACHMENT_ID)).isEmpty();
		assertThat(attachmentDataRepository.findById(ATTACHMENT_DATA_ID)).isEmpty();
		assertThat(loadCount).isZero();
	}

	private long loadCountOfAttachmentData(final Statistics statistics) {
		return statistics.getEntityStatistics(AttachmentDataEntity.class.getName()).getLoadCount();
	}

	private Statistics statistics() {
		return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
	}
}
