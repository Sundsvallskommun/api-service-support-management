package se.sundsvall.supportmanagement.service.search.index;

import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.time.Duration;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchSchemaInitializerTest {

	@Mock
	private EntityManagerFactory entityManagerFactoryMock;

	@Mock
	private SearchMapping searchMappingMock;

	@Mock
	private SearchScope<Object> searchScopeMock;

	@Mock
	private SearchSchemaManager schemaManagerMock;

	@Mock
	private OpenSearchClient openSearchMock;

	@Mock
	private SearchIndexHealth indexHealthMock;

	private static final String REFUSAL = """
		HSEARCH400007: Elasticsearch request failed: HSEARCH400090: Elasticsearch response indicates a failure.
		Request: PUT /errand-000001/_mapping with parameters {}
		Response: 400 'Bad Request' from 'localhost:41649' with body
		{
		  "error": {
		    "root_cause": [
		      {
		        "type": "illegal_argument_exception",
		        "reason": "mapper [title] cannot be changed from type [keyword] to [text]"
		      }
		    ]
		  }
		}""";

	private SearchSchemaInitializer initializer(final boolean enabled) {
		return new SearchSchemaInitializer(entityManagerFactoryMock, new SearchAvailability(enabled), openSearchMock, indexHealthMock, Duration.ZERO);
	}

	@Test
	void nothingHappensWhenSearchIsDisabled() {
		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			initializer(false).onApplicationReady();

			search.verifyNoInteractions();
		}
	}

	@Test
	void schemaOfAnIndexThatIsThereIsUpdatedAndTheIndexLookedAt() throws IOException {
		when(openSearchMock.errandIndexExists()).thenReturn(true);
		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			search.when(() -> Search.mapping(entityManagerFactoryMock)).thenReturn(searchMappingMock);
			when(searchMappingMock.scope(Object.class)).thenAnswer(_ -> searchScopeMock);
			when(searchScopeMock.schemaManager()).thenReturn(schemaManagerMock);

			initializer(true).createOrUpdateSchema();

			verify(schemaManagerMock).createOrUpdate();
			verify(indexHealthMock).inspect();
			verify(indexHealthMock, never()).created();
		}
	}

	@Test
	void indexThatIsNotThereIsCreatedOfTheCurrentLayout() throws IOException {
		when(openSearchMock.errandIndexExists()).thenReturn(false);
		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			search.when(() -> Search.mapping(entityManagerFactoryMock)).thenReturn(searchMappingMock);
			when(searchMappingMock.scope(Object.class)).thenAnswer(_ -> searchScopeMock);
			when(searchScopeMock.schemaManager()).thenReturn(schemaManagerMock);

			initializer(true).createOrUpdateSchema();

			verify(schemaManagerMock).createOrUpdate();
			verify(indexHealthMock).created();
		}
	}

	@Test
	void mappingRefusedByTheClusterIsReportedAndNotTriedAgain() throws IOException {
		when(openSearchMock.errandIndexExists()).thenReturn(true);
		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			search.when(() -> Search.mapping(entityManagerFactoryMock)).thenReturn(searchMappingMock);
			when(searchMappingMock.scope(Object.class)).thenAnswer(_ -> searchScopeMock);
			when(searchScopeMock.schemaManager()).thenReturn(schemaManagerMock);
			doThrow(new IllegalStateException("Schema management failed", new IllegalStateException(REFUSAL))).when(schemaManagerMock).createOrUpdate();

			initializer(true).createOrUpdateSchema();

			verify(schemaManagerMock).createOrUpdate();
			verify(indexHealthMock).mappingNotUpdated("mapper [title] cannot be changed from type [keyword] to [text]");
			verify(indexHealthMock, never()).inspect();
		}
	}

	@Test
	void triesAgainWhenTheClusterIsNotThereYet() {
		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			search.when(() -> Search.mapping(entityManagerFactoryMock)).thenReturn(searchMappingMock);
			when(searchMappingMock.scope(Object.class)).thenAnswer(_ -> searchScopeMock);
			when(searchScopeMock.schemaManager()).thenReturn(schemaManagerMock);
			doThrow(new IllegalStateException("not yet")).doThrow(new IllegalStateException("still not")).doNothing().when(schemaManagerMock).createOrUpdate();

			initializer(true).createOrUpdateSchema();

			verify(schemaManagerMock, times(3)).createOrUpdate();
		}
	}

	@Test
	void givesUpAfterTheLastAttempt() {
		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			search.when(() -> Search.mapping(entityManagerFactoryMock)).thenReturn(searchMappingMock);
			when(searchMappingMock.scope(Object.class)).thenAnswer(_ -> searchScopeMock);
			when(searchScopeMock.schemaManager()).thenReturn(schemaManagerMock);
			doThrow(new IllegalStateException("never")).when(schemaManagerMock).createOrUpdate();

			initializer(true).createOrUpdateSchema();

			verify(schemaManagerMock, times(SearchSchemaInitializer.ATTEMPTS)).createOrUpdate();
			verifyNoInteractions(indexHealthMock);
		}
	}

	@Test
	void aFailureThatIsNotAnAnswerOfTheClusterIsNoRefusal() {
		assertThat(SearchSchemaInitializer.refusalOf(new IllegalStateException("Connection refused"))).isEmpty();
		assertThat(SearchSchemaInitializer.refusalOf(new IllegalStateException("Response: 503 'Service Unavailable' with body {\"reason\": \"blocked\"}"))).isEmpty();
	}

	@Test
	void aRefusalWithoutAReasonIsReportedByItsFirstLine() {
		assertThat(SearchSchemaInitializer.refusalOf(new IllegalStateException("Request failed\nResponse: 400 'Bad Request'"))).contains("Request failed");
	}
}
