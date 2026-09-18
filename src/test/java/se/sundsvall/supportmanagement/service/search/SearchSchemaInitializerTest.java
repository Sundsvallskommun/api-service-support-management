package se.sundsvall.supportmanagement.service.search;

import jakarta.persistence.EntityManagerFactory;
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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

	private SearchSchemaInitializer initializer(final boolean enabled) {
		return new SearchSchemaInitializer(entityManagerFactoryMock, new SearchAvailability(enabled), Duration.ZERO);
	}

	@Test
	void nothingHappensWhenSearchIsDisabled() {
		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			initializer(false).onApplicationReady();

			search.verifyNoInteractions();
		}
	}

	@Test
	void schemaIsCreatedOrUpdated() {
		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			search.when(() -> Search.mapping(entityManagerFactoryMock)).thenReturn(searchMappingMock);
			when(searchMappingMock.scope(Object.class)).thenAnswer(_ -> searchScopeMock);
			when(searchScopeMock.schemaManager()).thenReturn(schemaManagerMock);

			initializer(true).createOrUpdateSchema();

			verify(schemaManagerMock).createOrUpdate();
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
		}
	}
}
