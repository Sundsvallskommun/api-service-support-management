package se.sundsvall.supportmanagement.integration.elasticsearch;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;
import se.sundsvall.supportmanagement.integration.elasticsearch.model.JsonParameterDocument;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ElasticsearchIndexServiceTest {

	private static final String ERRAND_ID = "errandId";

	@Mock
	private JsonParameterDocumentRepository repositoryMock;

	@InjectMocks
	private ElasticsearchIndexService service;

	@Captor
	private ArgumentCaptor<JsonParameterDocument> documentCaptor;

	@Captor
	private ArgumentCaptor<List<JsonParameterDocument>> documentsCaptor;

	@Test
	void indexErrandWithJsonParameters() {
		service.index(buildErrandEntityWithJsonParameters());

		verify(repositoryMock).save(documentCaptor.capture());
		assertThat(documentCaptor.getValue().getId()).isEqualTo(ERRAND_ID);
		assertThat(documentCaptor.getValue().getJsonParameters()).containsOnlyKeys("facility");
	}

	@Test
	void indexErrandWithoutJsonParametersRemovesDocument() {
		service.index(ErrandEntity.create().withId(ERRAND_ID));

		verify(repositoryMock).deleteById(ERRAND_ID);
		verify(repositoryMock, never()).save(any());
	}

	@Test
	void indexSwallowsFailure() {
		doThrow(new RuntimeException("Elasticsearch down")).when(repositoryMock).save(any());

		assertThatNoException().isThrownBy(() -> service.index(buildErrandEntityWithJsonParameters()));

		verify(repositoryMock).save(any());
	}

	@Test
	void delete() {
		service.delete(ERRAND_ID);

		verify(repositoryMock).deleteById(ERRAND_ID);
	}

	@Test
	void deleteSwallowsFailure() {
		doThrow(new RuntimeException("Elasticsearch down")).when(repositoryMock).deleteById(ERRAND_ID);

		assertThatNoException().isThrownBy(() -> service.delete(ERRAND_ID));

		verify(repositoryMock).deleteById(ERRAND_ID);
	}

	@Test
	void indexAllFiltersOutErrandsWithoutJsonParameters() {
		service.indexAll(List.of(buildErrandEntityWithJsonParameters(), ErrandEntity.create().withId("other")));

		verify(repositoryMock).saveAll(documentsCaptor.capture());
		assertThat(documentsCaptor.getValue()).hasSize(1);
		assertThat(documentsCaptor.getValue().getFirst().getId()).isEqualTo(ERRAND_ID);
	}

	@Test
	void indexAllWithoutIndexableErrandsSkipsSave() {
		service.indexAll(List.of(ErrandEntity.create().withId("other")));

		verify(repositoryMock, never()).saveAll(any());
	}

	@Test
	void indexAllPropagatesFailure() {
		doThrow(new RuntimeException("Elasticsearch down")).when(repositoryMock).saveAll(any());
		final var entities = List.of(buildErrandEntityWithJsonParameters());

		assertThatThrownBy(() -> service.indexAll(entities))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("Elasticsearch down");
	}

	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(repositoryMock);
	}

	private static ErrandEntity buildErrandEntityWithJsonParameters() {
		return ErrandEntity.create()
			.withId(ERRAND_ID)
			.withNamespace("namespace")
			.withMunicipalityId("2281")
			.withJsonParameters(List.of(JsonParameterEntity.create()
				.withKey("facility")
				.withSchemaId("schema-1.0")
				.withValue("{\"facilityId\":\"FAC-0001\"}")));
	}
}
