package se.sundsvall.supportmanagement.integration.elasticsearch;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElasticsearchReindexRunnerTest {

	private static final Pageable FIRST_PAGE = PageRequest.of(0, 500, Sort.by("id"));

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private ElasticsearchIndexService elasticsearchIndexServiceMock;

	@Mock
	private EntityManager entityManagerMock;

	@Mock
	private ApplicationArguments applicationArgumentsMock;

	@InjectMocks
	private ElasticsearchReindexRunner runner;

	@Test
	void runIndexesAllBatches() {
		final var firstBatch = List.of(ErrandEntity.create().withId("id-1"), ErrandEntity.create().withId("id-2"));
		final var secondBatch = List.of(ErrandEntity.create().withId("id-3"));

		when(errandsRepositoryMock.findAllByJsonParametersIsNotEmpty(FIRST_PAGE)).thenReturn(new PageImpl<>(firstBatch, FIRST_PAGE, 501));
		when(errandsRepositoryMock.findAllByJsonParametersIsNotEmpty(FIRST_PAGE.next())).thenReturn(new PageImpl<>(secondBatch, FIRST_PAGE.next(), 501));

		runner.run(applicationArgumentsMock);

		verify(errandsRepositoryMock).findAllByJsonParametersIsNotEmpty(FIRST_PAGE);
		verify(errandsRepositoryMock).findAllByJsonParametersIsNotEmpty(FIRST_PAGE.next());
		verify(elasticsearchIndexServiceMock).indexAll(firstBatch);
		verify(elasticsearchIndexServiceMock).indexAll(secondBatch);
		verify(entityManagerMock, times(2)).clear();
	}

	@Test
	void runWithoutErrands() {
		when(errandsRepositoryMock.findAllByJsonParametersIsNotEmpty(FIRST_PAGE)).thenReturn(new PageImpl<>(List.of(), FIRST_PAGE, 0));

		runner.run(applicationArgumentsMock);

		verify(errandsRepositoryMock).findAllByJsonParametersIsNotEmpty(FIRST_PAGE);
		verifyNoInteractions(elasticsearchIndexServiceMock, entityManagerMock);
	}

	@Test
	void runContinuesWhenBatchFails() {
		final var firstBatch = List.of(ErrandEntity.create().withId("id-1"));
		final var secondBatch = List.of(ErrandEntity.create().withId("id-2"));

		when(errandsRepositoryMock.findAllByJsonParametersIsNotEmpty(FIRST_PAGE)).thenReturn(new PageImpl<>(firstBatch, FIRST_PAGE, 501));
		when(errandsRepositoryMock.findAllByJsonParametersIsNotEmpty(FIRST_PAGE.next())).thenReturn(new PageImpl<>(secondBatch, FIRST_PAGE.next(), 501));
		doThrow(new RuntimeException("Elasticsearch down")).when(elasticsearchIndexServiceMock).indexAll(firstBatch);

		assertThatNoException().isThrownBy(() -> runner.run(applicationArgumentsMock));

		verify(elasticsearchIndexServiceMock).indexAll(firstBatch);
		verify(elasticsearchIndexServiceMock).indexAll(secondBatch);
		verify(entityManagerMock, times(2)).clear();
	}

	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(errandsRepositoryMock, elasticsearchIndexServiceMock, entityManagerMock);
	}
}
