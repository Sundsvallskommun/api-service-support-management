package se.sundsvall.supportmanagement.integration.elasticsearch;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElasticsearchReindexerTest {

	private static final int BATCH_SIZE = 500;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private ElasticsearchIndexService elasticsearchIndexServiceMock;

	@InjectMocks
	private ElasticsearchReindexer reindexer;

	@Test
	void countErrandsToIndex() {
		when(errandsRepositoryMock.countByJsonParametersIsNotEmpty()).thenReturn(4711L);

		assertThat(reindexer.countErrandsToIndex()).isEqualTo(4711L);

		verify(errandsRepositoryMock).countByJsonParametersIsNotEmpty();
	}

	@Test
	void indexBatchIndexesErrands() {
		final var errands = List.of(ErrandEntity.create().withId("id-1"), ErrandEntity.create().withId("id-2"));
		when(errandsRepositoryMock.findByJsonParametersIsNotEmptyAndIdGreaterThanOrderByIdAsc("", Limit.of(BATCH_SIZE))).thenReturn(errands);

		final var result = reindexer.indexBatch("", BATCH_SIZE);

		assertThat(result.lastId()).isEqualTo("id-2");
		assertThat(result.indexedCount()).isEqualTo(2);
		assertThat(result.failed()).isFalse();
		assertThat(result.isEmpty()).isFalse();

		verify(errandsRepositoryMock).findByJsonParametersIsNotEmptyAndIdGreaterThanOrderByIdAsc("", Limit.of(BATCH_SIZE));
		verify(elasticsearchIndexServiceMock).indexAll(errands);
	}

	@Test
	void indexBatchContinuesFromLastId() {
		final var errands = List.of(ErrandEntity.create().withId("id-9"));
		when(errandsRepositoryMock.findByJsonParametersIsNotEmptyAndIdGreaterThanOrderByIdAsc("id-8", Limit.of(BATCH_SIZE))).thenReturn(errands);

		assertThat(reindexer.indexBatch("id-8", BATCH_SIZE).lastId()).isEqualTo("id-9");

		verify(errandsRepositoryMock).findByJsonParametersIsNotEmptyAndIdGreaterThanOrderByIdAsc("id-8", Limit.of(BATCH_SIZE));
		verify(elasticsearchIndexServiceMock).indexAll(errands);
	}

	@Test
	void indexBatchWithoutErrands() {
		when(errandsRepositoryMock.findByJsonParametersIsNotEmptyAndIdGreaterThanOrderByIdAsc("id-1", Limit.of(BATCH_SIZE))).thenReturn(List.of());

		final var result = reindexer.indexBatch("id-1", BATCH_SIZE);

		assertThat(result.isEmpty()).isTrue();
		assertThat(result.lastId()).isNull();
		assertThat(result.indexedCount()).isZero();
		assertThat(result.failed()).isFalse();

		verify(errandsRepositoryMock).findByJsonParametersIsNotEmptyAndIdGreaterThanOrderByIdAsc("id-1", Limit.of(BATCH_SIZE));
		verifyNoInteractions(elasticsearchIndexServiceMock);
	}

	@Test
	void indexBatchReportsLastIdWhenIndexingFails() {
		final var errands = List.of(ErrandEntity.create().withId("id-1"), ErrandEntity.create().withId("id-2"));
		when(errandsRepositoryMock.findByJsonParametersIsNotEmptyAndIdGreaterThanOrderByIdAsc("", Limit.of(BATCH_SIZE))).thenReturn(errands);
		doThrow(new RuntimeException("Elasticsearch down")).when(elasticsearchIndexServiceMock).indexAll(errands);

		final var result = reindexer.indexBatch("", BATCH_SIZE);

		assertThat(result.failed()).isTrue();
		assertThat(result.indexedCount()).isZero();
		assertThat(result.lastId()).isEqualTo("id-2");
		assertThat(result.isEmpty()).isFalse();

		verify(errandsRepositoryMock).findByJsonParametersIsNotEmptyAndIdGreaterThanOrderByIdAsc("", Limit.of(BATCH_SIZE));
		verify(elasticsearchIndexServiceMock).indexAll(errands);
	}

	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(errandsRepositoryMock, elasticsearchIndexServiceMock);
	}
}
