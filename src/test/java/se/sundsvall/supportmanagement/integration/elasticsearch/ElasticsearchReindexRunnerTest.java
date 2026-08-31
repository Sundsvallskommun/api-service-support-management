package se.sundsvall.supportmanagement.integration.elasticsearch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import se.sundsvall.supportmanagement.integration.elasticsearch.ElasticsearchReindexer.BatchResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElasticsearchReindexRunnerTest {

	private static final int BATCH_SIZE = 500;

	@Mock
	private ElasticsearchReindexer elasticsearchReindexerMock;

	@Mock
	private ApplicationArguments applicationArgumentsMock;

	@InjectMocks
	private ElasticsearchReindexRunner runner;

	@Test
	void reindexIndexesAllBatches() {
		when(elasticsearchReindexerMock.countErrandsToIndex()).thenReturn(3L);
		when(elasticsearchReindexerMock.indexBatch("", BATCH_SIZE)).thenReturn(new BatchResult("id-2", 2, false));
		when(elasticsearchReindexerMock.indexBatch("id-2", BATCH_SIZE)).thenReturn(new BatchResult("id-3", 1, false));
		when(elasticsearchReindexerMock.indexBatch("id-3", BATCH_SIZE)).thenReturn(BatchResult.empty());

		runner.reindex();

		verify(elasticsearchReindexerMock).countErrandsToIndex();
		verify(elasticsearchReindexerMock).indexBatch("", BATCH_SIZE);
		verify(elasticsearchReindexerMock).indexBatch("id-2", BATCH_SIZE);
		verify(elasticsearchReindexerMock).indexBatch("id-3", BATCH_SIZE);
	}

	@Test
	void reindexWithoutErrands() {
		when(elasticsearchReindexerMock.countErrandsToIndex()).thenReturn(0L);
		when(elasticsearchReindexerMock.indexBatch("", BATCH_SIZE)).thenReturn(BatchResult.empty());

		runner.reindex();

		verify(elasticsearchReindexerMock).countErrandsToIndex();
		verify(elasticsearchReindexerMock).indexBatch("", BATCH_SIZE);
	}

	@Test
	void reindexContinuesPastFailingBatch() {
		when(elasticsearchReindexerMock.countErrandsToIndex()).thenReturn(2L);
		when(elasticsearchReindexerMock.indexBatch("", BATCH_SIZE)).thenReturn(new BatchResult("id-1", 0, true));
		when(elasticsearchReindexerMock.indexBatch("id-1", BATCH_SIZE)).thenReturn(new BatchResult("id-2", 1, false));
		when(elasticsearchReindexerMock.indexBatch("id-2", BATCH_SIZE)).thenReturn(BatchResult.empty());

		assertThatNoException().isThrownBy(() -> runner.reindex());

		verify(elasticsearchReindexerMock).countErrandsToIndex();
		verify(elasticsearchReindexerMock).indexBatch("", BATCH_SIZE);
		verify(elasticsearchReindexerMock).indexBatch("id-1", BATCH_SIZE);
		verify(elasticsearchReindexerMock).indexBatch("id-2", BATCH_SIZE);
	}

	@Test
	void reindexSwallowsExceptions() {
		when(elasticsearchReindexerMock.countErrandsToIndex()).thenThrow(new RuntimeException("Database down"));

		assertThatNoException().isThrownBy(() -> runner.reindex());

		verify(elasticsearchReindexerMock).countErrandsToIndex();
	}

	@Test
	void runReindexesOnBackgroundThreadWithoutBlocking() throws InterruptedException {
		final var latch = new CountDownLatch(1);
		final var reindexThreadName = new String[1];
		when(elasticsearchReindexerMock.countErrandsToIndex()).thenReturn(0L);
		when(elasticsearchReindexerMock.indexBatch("", BATCH_SIZE)).thenAnswer(invocation -> {
			reindexThreadName[0] = Thread.currentThread().getName();
			latch.countDown();
			return BatchResult.empty();
		});

		runner.run(applicationArgumentsMock);

		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(reindexThreadName[0])
			.isEqualTo("elasticsearch-reindex")
			.isNotEqualTo(Thread.currentThread().getName());

		verify(elasticsearchReindexerMock).countErrandsToIndex();
		verify(elasticsearchReindexerMock).indexBatch("", BATCH_SIZE);
		verifyNoInteractions(applicationArgumentsMock);
	}

	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(elasticsearchReindexerMock);
	}
}
