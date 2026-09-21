package se.sundsvall.supportmanagement.service.search;

import jakarta.persistence.EntityManager;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.config.SearchProperties;
import se.sundsvall.supportmanagement.service.AccessControlService;
import se.sundsvall.supportmanagement.service.search.index.ErrandIndexModel;
import se.sundsvall.supportmanagement.service.search.index.SearchAvailability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

/**
 * What the service settles before the index is asked. The search itself is exercised by ErrandSearchIT, since the
 * search DSL is not worth mocking.
 */
@ExtendWith(MockitoExtension.class)
class ErrandSearchServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private EntityManager entityManagerMock;

	@Mock
	private AccessControlService accessControlServiceMock;

	@Mock
	private ErrandSearchAccess searchAccessMock;

	@Mock
	private ErrandSearchPredicates predicatesMock;

	private ErrandSearchService service(final boolean enabled) {
		return new ErrandSearchService(entityManagerMock, accessControlServiceMock, searchAccessMock, predicatesMock, new SearchAvailability(enabled),
			new SearchProperties(10000, new SearchProperties.Reindex(Duration.ofHours(6))));
	}

	@Test
	void searchWhenDisabled() {
		final var e = assertThrows(ThrowableProblem.class, () -> service(false).search(NAMESPACE, MUNICIPALITY_ID, "query", PageRequest.of(0, 20)));

		assertThat(e.getStatus()).isEqualTo(SERVICE_UNAVAILABLE);
		verifyNoInteractions(entityManagerMock, accessControlServiceMock, searchAccessMock, predicatesMock);
	}

	@Test
	void searchBeyondResultWindow() {
		final var e = assertThrows(ThrowableProblem.class, () -> service(true).search(NAMESPACE, MUNICIPALITY_ID, "query", PageRequest.of(500, 100)));

		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getDetail()).isEqualTo("Page 500 of size 100 reaches beyond the 10000 results a search can page through. Narrow the search instead");
		verifyNoInteractions(entityManagerMock, accessControlServiceMock, searchAccessMock, predicatesMock);
	}

	@Test
	void searchWithUnsupportedSort() {
		final var pageable = PageRequest.of(0, 20, Sort.by("created").descending().and(Sort.by("description")));

		final var e = assertThrows(ThrowableProblem.class, () -> service(true).search(NAMESPACE, MUNICIPALITY_ID, "query", pageable));

		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getDetail()).isEqualTo("Sorting on 'description' is not supported by search. Sortable properties are: " +
			"[assignedGroupId, assignedUserId, category, channel, created, errandNumber, modified, priority, reporterUserId, resolution, status, suspendedFrom, suspendedTo, title, touched, type]");
		verifyNoInteractions(entityManagerMock, accessControlServiceMock, searchAccessMock, predicatesMock);
	}

	@Test
	void sortablePropertiesMapToIndexFields() {
		assertThat(ErrandIndexModel.sortableProperties()).containsExactly("assignedGroupId", "assignedUserId", "category", "channel", "created", "errandNumber", "modified", "priority",
			"reporterUserId", "resolution", "status", "suspendedFrom", "suspendedTo", "title", "touched", "type");
		assertThat(ErrandIndexModel.sortField("title")).contains("title_sort");
		assertThat(ErrandIndexModel.sortField("category")).contains("category");
		assertThat(ErrandIndexModel.sortField("suspendedTo")).contains("suspendedTo");
		assertThat(ErrandIndexModel.sortField("description")).isEmpty();
	}
}
