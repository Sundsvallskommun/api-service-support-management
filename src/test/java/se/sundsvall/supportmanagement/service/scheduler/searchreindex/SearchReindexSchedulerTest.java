package se.sundsvall.supportmanagement.service.scheduler.searchreindex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.service.search.index.ErrandReindexService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class SearchReindexSchedulerTest {

	@Mock
	private ErrandReindexService reindexServiceMock;

	@Test
	void reindexErrandsWhenEnabled() {
		new SearchReindexScheduler(reindexServiceMock, true).reindexErrands();

		verify(reindexServiceMock).reindexEveryNamespace();
		verifyNoMoreInteractions(reindexServiceMock);
	}

	@Test
	void reindexErrandsWhenDisabled() {
		new SearchReindexScheduler(reindexServiceMock, false).reindexErrands();

		verifyNoInteractions(reindexServiceMock);
	}
}
