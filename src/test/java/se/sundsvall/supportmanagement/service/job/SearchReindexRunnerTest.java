package se.sundsvall.supportmanagement.service.job;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.service.search.index.NamespaceIndexRebuilder;
import se.sundsvall.supportmanagement.service.search.index.SearchIndexHealth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchReindexRunnerTest {

	private static final SearchReindexRun RUN = new SearchReindexRun("job-id", "namespace", "2281");

	@Mock
	private JobService jobServiceMock;

	@Mock
	private NamespaceIndexRebuilder rebuilderMock;

	@Mock
	private SearchIndexHealth indexHealthMock;

	private SearchReindexRunner runner;

	@BeforeEach
	void runner() {
		runner = new SearchReindexRunner(jobServiceMock, rebuilderMock, indexHealthMock);
	}

	@Test
	void rebuildsTheNamespaceAndCompletesTheJob() throws InterruptedException {
		when(rebuilderMock.rebuild(eq("namespace"), eq("2281"), any())).thenAnswer(invocation -> {
			final NamespaceIndexRebuilder.Progress progress = invocation.getArgument(2);
			progress.written(10);
			progress.removed(2);
			return new NamespaceIndexRebuilder.Outcome(10, 2);
		});

		runner.run(RUN);

		final InOrder order = inOrder(jobServiceMock, indexHealthMock);
		order.verify(jobServiceMock).setRunning("job-id");
		// Writing is reported at most so often, the removal every time it is asked about
		order.verify(jobServiceMock).reportProgress("job-id", 10, SearchReindexRunner.REMOVING.formatted(2));
		order.verify(jobServiceMock).complete("job-id", SearchReindexRunner.SUMMARY.formatted(10, 2));
		order.verify(indexHealthMock).inspect();
		verify(jobServiceMock, never()).fail(any(), any());
	}

	@Test
	void aRebuildThatFailsFailsTheJob() throws InterruptedException {
		when(rebuilderMock.rebuild(eq("namespace"), eq("2281"), any())).thenThrow(new IllegalStateException("3 errand(s) could not be written"));

		runner.run(RUN);

		verify(jobServiceMock).fail("job-id", "3 errand(s) could not be written");
		verify(jobServiceMock, never()).complete(any(), any());
		verifyNoInteractions(indexHealthMock);
	}

	@Test
	void aRebuildThatIsInterruptedFailsTheJobAndKeepsTheInterrupt() throws InterruptedException {
		when(rebuilderMock.rebuild(eq("namespace"), eq("2281"), any())).thenThrow(new InterruptedException());

		try {
			runner.run(RUN);

			verify(jobServiceMock).fail("job-id", SearchReindexRunner.INTERRUPTED);
			assertThat(Thread.currentThread().isInterrupted()).isTrue();
		} finally {
			Thread.interrupted();
		}
	}

	@Test
	void writingIsReportedOnceTheIntervalHasPassed() throws InterruptedException {
		final var reportingEveryTime = new SearchReindexRunner(jobServiceMock, rebuilderMock, indexHealthMock, Duration.ZERO);
		when(rebuilderMock.rebuild(eq("namespace"), eq("2281"), any())).thenAnswer(invocation -> {
			invocation.<NamespaceIndexRebuilder.Progress>getArgument(2).written(4);
			return new NamespaceIndexRebuilder.Outcome(4, 0);
		});

		reportingEveryTime.run(RUN);

		verify(jobServiceMock).reportProgress("job-id", 4, SearchReindexRunner.WRITING);
		verify(jobServiceMock).complete("job-id", SearchReindexRunner.SUMMARY.formatted(4, 0));
	}

	@Test
	void anErrandCountBeyondWhatAJobHoldsIsReportedAsTheMost() throws InterruptedException {
		final var reportingEveryTime = new SearchReindexRunner(jobServiceMock, rebuilderMock, indexHealthMock, Duration.ZERO);
		when(rebuilderMock.rebuild(eq("namespace"), eq("2281"), any())).thenAnswer(invocation -> {
			invocation.<NamespaceIndexRebuilder.Progress>getArgument(2).written(Long.MAX_VALUE);
			return new NamespaceIndexRebuilder.Outcome(1, 0);
		});

		reportingEveryTime.run(RUN);

		verify(jobServiceMock).reportProgress("job-id", Integer.MAX_VALUE, SearchReindexRunner.WRITING);
	}

	@Test
	void aThreadTakenDownWithoutAnExceptionFailsTheJob() throws InterruptedException {
		when(rebuilderMock.rebuild(eq("namespace"), eq("2281"), any())).thenThrow(new StackOverflowError());

		try {
			runner.run(RUN);
		} catch (final StackOverflowError e) {
			// Let through by the envelope once the job has been failed
		}

		verify(jobServiceMock).fail("job-id", SearchReindexRunner.ENDED_WITHOUT_RESULT);
	}
}
