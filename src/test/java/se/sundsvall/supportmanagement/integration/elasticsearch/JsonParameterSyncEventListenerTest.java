package se.sundsvall.supportmanagement.integration.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsonParameterSyncEventListenerTest {

	private static final String ERRAND_ID = "errandId";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private JsonParameterElasticRepository elasticRepositoryMock;

	@Spy
	private ObjectMapper objectMapper;

	@InjectMocks
	private JsonParameterSyncEventListener listener;

	@Captor
	private ArgumentCaptor<List<JsonParameterDocument>> documentsCaptor;

	@Test
	void onUpsertSyncsDocuments() {
		// Setup
		final var jsonParam = JsonParameterEntity.create()
			.withId("paramId")
			.withKey("address")
			.withValue("{\"city\":\"Sundsvall\",\"zip\":\"85230\"}");
		final var errand = ErrandEntity.create()
			.withId(ERRAND_ID)
			.withJsonParameters(List.of(jsonParam));
		final var event = JsonParameterSyncEvent.upsert(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);

		// Mock
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(errand));

		// Call
		listener.onJsonParameterSync(event);

		// Verify
		verify(errandsRepositoryMock).findById(ERRAND_ID);
		verify(elasticRepositoryMock).deleteByErrandId(ERRAND_ID);
		verify(elasticRepositoryMock).saveAll(documentsCaptor.capture());

		final var savedDocuments = documentsCaptor.getValue();
		assertThat(savedDocuments).hasSize(1);
		assertThat(savedDocuments.getFirst().getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(savedDocuments.getFirst().getNamespace()).isEqualTo(NAMESPACE);
		assertThat(savedDocuments.getFirst().getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(savedDocuments.getFirst().getParameterKey()).isEqualTo("address");
		assertThat(savedDocuments.getFirst().getValue()).containsEntry("city", "Sundsvall").containsEntry("zip", "85230");

		verifyNoMoreInteractions(errandsRepositoryMock, elasticRepositoryMock);
	}

	@Test
	void onUpsertErrandNotFound() {
		// Setup
		final var event = JsonParameterSyncEvent.upsert(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);

		// Mock
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.empty());

		// Call
		listener.onJsonParameterSync(event);

		// Verify
		verify(errandsRepositoryMock).findById(ERRAND_ID);
		verifyNoInteractions(elasticRepositoryMock);
		verifyNoMoreInteractions(errandsRepositoryMock);
	}

	@Test
	void onUpsertNoJsonParameters() {
		// Setup
		final var errand = ErrandEntity.create()
			.withId(ERRAND_ID)
			.withJsonParameters(emptyList());
		final var event = JsonParameterSyncEvent.upsert(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);

		// Mock
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(errand));

		// Call
		listener.onJsonParameterSync(event);

		// Verify
		verify(errandsRepositoryMock).findById(ERRAND_ID);
		verify(elasticRepositoryMock).deleteByErrandId(ERRAND_ID);
		verifyNoMoreInteractions(errandsRepositoryMock, elasticRepositoryMock);
	}

	@Test
	void onDeleteDeletesDocuments() {
		// Setup
		final var event = JsonParameterSyncEvent.delete(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);

		// Call
		listener.onJsonParameterSync(event);

		// Verify
		verify(elasticRepositoryMock).deleteByErrandId(ERRAND_ID);
		verifyNoInteractions(errandsRepositoryMock);
		verifyNoMoreInteractions(elasticRepositoryMock);
	}
}
