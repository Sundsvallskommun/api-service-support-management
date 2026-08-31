package se.sundsvall.supportmanagement.service.purge;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import se.sundsvall.supportmanagement.config.ErrandPurgeProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.service.ErrandService;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.COMPLETED;
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.FAILED;
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.STOPPED;

@ExtendWith(MockitoExtension.class)
class ErrandPurgeWorkerTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final OffsetDateTime OLDER_THAN = OffsetDateTime.parse("2020-08-28T00:00:00+02:00");
	private static final String STARTED_BY = "joe01doe";
	private static final OffsetDateTime STARTED = OffsetDateTime.parse("2026-08-28T10:00:00+02:00");
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-28T12:00:00Z"), ZoneId.of("UTC"));
	private static final int BATCH_SIZE = 2;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private ErrandService errandServiceMock;

	@Captor
	private ArgumentCaptor<String> cursorCaptor;

	@Captor
	private ArgumentCaptor<Pageable> pageableCaptor;

	private ErrandPurgeWorker worker;

	private static ErrandPurgeProperties properties() {
		return new ErrandPurgeProperties(Period.ofYears(2), BATCH_SIZE, Duration.ofHours(24), 2);
	}

	private ErrandPurgeWorker worker() {
		if (worker == null) {
			worker = new ErrandPurgeWorker(errandsRepositoryMock, errandServiceMock, CLOCK, properties());
		}
		return worker;
	}

	@Test
	@DisplayName("Verification that a namespace with nothing old enough completes without touching an errand")
	void runWithNothingToPurge() {
		final var job = job(false, null);

		when(errandsRepositoryMock.findIdsToPurge(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(OLDER_THAN), anyString(), any(Pageable.class)))
			.thenReturn(emptyList());

		worker().run(job);

		final var status = job.toStatus();
		assertThat(status.getState()).isEqualTo(COMPLETED);
		assertThat(status.getProcessed()).isZero();
		assertThat(status.getFinished()).isEqualTo(OffsetDateTime.now(CLOCK));
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	@DisplayName("Verification that the walk moves the keyset forward past every batch, including past an errand it could not remove")
	void runWalksEveryBatchAndCarriesTheCursorForward() {
		final var job = job(false, null);

		when(errandsRepositoryMock.findIdsToPurge(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(OLDER_THAN), cursorCaptor.capture(), any(Pageable.class)))
			.thenReturn(List.of("a", "b"), List.of("c"), emptyList());
		when(errandServiceMock.purgeErrand(NAMESPACE, MUNICIPALITY_ID, "a")).thenReturn(true);
		when(errandServiceMock.purgeErrand(NAMESPACE, MUNICIPALITY_ID, "b")).thenThrow(new RuntimeException("Constraint violation"));
		when(errandServiceMock.purgeErrand(NAMESPACE, MUNICIPALITY_ID, "c")).thenReturn(true);

		worker().run(job);

		assertThat(cursorCaptor.getAllValues()).containsExactly("", "b", "c");

		final var status = job.toStatus();
		assertThat(status.getState()).isEqualTo(COMPLETED);
		assertThat(status.getProcessed()).isEqualTo(3);
		assertThat(status.getDeleted()).isEqualTo(2);
		assertThat(status.getFailed()).isEqualTo(1);
		assertThat(status.getMessage()).isNull();
	}

	@Test
	@DisplayName("Verification that a dry run counts every errand it reaches and removes none of them")
	void runAsDryRun() {
		final var job = job(true, null);

		when(errandsRepositoryMock.findIdsToPurge(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(OLDER_THAN), cursorCaptor.capture(), any(Pageable.class)))
			.thenReturn(List.of("a", "b"), emptyList());

		worker().run(job);

		assertThat(cursorCaptor.getAllValues()).containsExactly("", "b");

		final var status = job.toStatus();
		assertThat(status.getState()).isEqualTo(COMPLETED);
		assertThat(status.getProcessed()).isEqualTo(2);
		assertThat(status.getDeleted()).isZero();
		assertThat(status.getFailed()).isZero();
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	@DisplayName("Verification that an errand gone before the run reached it counts as reached but not as removed by this run")
	void runWhenErrandIsAlreadyGone() {
		final var job = job(false, null);

		when(errandsRepositoryMock.findIdsToPurge(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(OLDER_THAN), anyString(), any(Pageable.class)))
			.thenReturn(List.of("a"), emptyList());
		when(errandServiceMock.purgeErrand(NAMESPACE, MUNICIPALITY_ID, "a")).thenReturn(false);

		worker().run(job);

		final var status = job.toStatus();
		assertThat(status.getState()).isEqualTo(COMPLETED);
		assertThat(status.getProcessed()).isEqualTo(1);
		assertThat(status.getDeleted()).isZero();
		assertThat(status.getFailed()).isZero();
	}

	@Test
	@DisplayName("Verification that the limit of a run caps the batch it asks for and stops it once reached")
	void runStopsWhenTheLimitIsReached() {
		final var job = job(false, 3);

		when(errandsRepositoryMock.findIdsToPurge(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(OLDER_THAN), anyString(), pageableCaptor.capture()))
			.thenReturn(List.of("a", "b"), List.of("c"));
		when(errandServiceMock.purgeErrand(eq(NAMESPACE), eq(MUNICIPALITY_ID), anyString())).thenReturn(true);

		worker().run(job);

		// Two of the batch size, then only what is left of the limit.
		assertThat(pageableCaptor.getAllValues()).extracting(Pageable::getPageSize).containsExactly(2, 1);

		final var status = job.toStatus();
		assertThat(status.getState()).isEqualTo(STOPPED);
		assertThat(status.getProcessed()).isEqualTo(3);
		assertThat(status.getDeleted()).isEqualTo(3);
		assertThat(status.getMessage()).isEqualTo("Stopped after reaching the limit of errands set for the run");
	}

	@Test
	@DisplayName("Verification that a run asked to stop finishes the errand it is on and stops there")
	void runStopsWhenAskedTo() {
		final var job = job(false, null);

		when(errandsRepositoryMock.findIdsToPurge(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(OLDER_THAN), anyString(), any(Pageable.class)))
			.thenReturn(List.of("a", "b"));
		when(errandServiceMock.purgeErrand(NAMESPACE, MUNICIPALITY_ID, "a")).thenAnswer(_ -> {
			job.requestStop();
			return true;
		});

		worker().run(job);

		verify(errandServiceMock).purgeErrand(NAMESPACE, MUNICIPALITY_ID, "a");

		final var status = job.toStatus();
		assertThat(status.getState()).isEqualTo(STOPPED);
		assertThat(status.getProcessed()).isEqualTo(1);
		assertThat(status.getDeleted()).isEqualTo(1);
		assertThat(status.getFinished()).isEqualTo(OffsetDateTime.now(CLOCK));
	}

	@Test
	@DisplayName("Verification that a run already asked to stop never reads a batch at all")
	void runThatIsStoppedBeforeItStarts() {
		final var job = job(false, null);
		job.requestStop();

		worker().run(job);

		assertThat(job.toStatus().getState()).isEqualTo(STOPPED);
		verifyNoInteractions(errandsRepositoryMock, errandServiceMock);
	}

	@Test
	@DisplayName("Verification that a database that cannot be read ends the run as failed with the reason, rather than throwing on a thread with nobody to catch it")
	void runWhenReadingErrandsFails() {
		final var job = job(false, null);

		when(errandsRepositoryMock.findIdsToPurge(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(OLDER_THAN), anyString(), any(Pageable.class)))
			.thenThrow(new RuntimeException("Database is unreachable"));

		worker().run(job);

		final var status = job.toStatus();
		assertThat(status.getState()).isEqualTo(FAILED);
		assertThat(status.getMessage()).isEqualTo("Purge aborted: Database is unreachable");
		assertThat(status.getFinished()).isEqualTo(OffsetDateTime.now(CLOCK));
	}

	private static PurgeJob job(final boolean dryRun, final Integer maxErrands) {
		return new PurgeJob(randomUUID().toString(), NAMESPACE, MUNICIPALITY_ID, OLDER_THAN, dryRun, maxErrands, STARTED_BY, STARTED);
	}
}
