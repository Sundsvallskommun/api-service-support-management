package se.sundsvall.supportmanagement.service.scheduler.softdeletederrands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class SoftDeletedErrandSchedulerTest {

	@Mock
	private SoftDeletedErrandWorker softDeletedErrandWorkerMock;

	@InjectMocks
	private SoftDeletedErrandScheduler scheduler;

	@Test
	void deleteSoftDeletedErrands() {
		scheduler.deleteSoftDeletedErrands();

		verify(softDeletedErrandWorkerMock).deleteSoftDeletedErrands();
		verifyNoMoreInteractions(softDeletedErrandWorkerMock);
	}
}
