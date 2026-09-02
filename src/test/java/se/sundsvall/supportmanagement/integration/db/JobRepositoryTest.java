package se.sundsvall.supportmanagement.integration.db;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.JobEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.PENDING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.RUNNING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobType.ERRAND_PURGE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobType.MOVE_LABEL;

/**
 * What is worth pinning down against a real database is the pair of queries that stand in for asking whether a job has
 * gone quiet. A job gets a modified of its own only once the work reports on it, so neither query answers that on its
 * own: one finds what has been reported on and fallen silent, the other what was never reported on at all.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql",
	"/db/scripts/testdata-junit-job.sql"
})
class JobRepositoryTest {

	private static final List<JobStatus> ACTIVE = List.of(PENDING, RUNNING);

	@Autowired
	private JobRepository jobRepository;

	@Test
	@DisplayName("Verification that a job still being written to is left out, however long the run behind it has been going")
	void findByStatusInAndModifiedBefore() {
		final var quietSince = now(systemDefault()).minusDays(1);

		assertThat(jobRepository.findByStatusInAndModifiedBefore(ACTIVE, quietSince))
			.extracting(JobEntity::getId)
			.containsExactly("job-gone-quiet");
	}

	@Test
	@DisplayName("Verification that a job created by an instance which died before it ever reported is found, since it has no modified to be found by")
	void findByStatusInAndModifiedIsNullAndCreatedBefore() {
		final var quietSince = now(systemDefault()).minusDays(1);

		assertThat(jobRepository.findByStatusInAndModifiedIsNullAndCreatedBefore(ACTIVE, quietSince))
			.extracting(JobEntity::getId)
			.containsExactly("job-never-reported-on");
	}

	@Test
	@DisplayName("Verification that a job which has ended is not mistaken for one that went quiet, since it is quiet for the one reason that needs no attention")
	void findByStatusInAndModifiedBeforeLeavesEndedJobsAlone() {
		final var quietSince = now(systemDefault()).minusDays(1);

		assertThat(jobRepository.findByStatusInAndModifiedBefore(ACTIVE, quietSince))
			.extracting(JobEntity::getId)
			.doesNotContain("job-ended-long-ago");
	}

	@Test
	void findByIdAndNamespaceAndMunicipalityIdAndType() {
		assertThat(jobRepository.findByIdAndNamespaceAndMunicipalityIdAndType("job-gone-quiet", "NAMESPACE-1", "2281", ERRAND_PURGE))
			.map(JobEntity::getId)
			.contains("job-gone-quiet");
	}

	@Test
	@DisplayName("Verification that a job of another kind is not answered for, which is what keeps a resource from reaching work that is not its own")
	void findByIdAndNamespaceAndMunicipalityIdAndTypeWithAnotherType() {
		assertThat(jobRepository.findByIdAndNamespaceAndMunicipalityIdAndType("job-gone-quiet", "NAMESPACE-1", "2281", MOVE_LABEL)).isEmpty();
	}
}
