package se.sundsvall.supportmanagement.service.scheduler.softdeletederrands;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoftDeletedErrandSchedulerTest {

	private static final String JOB_NAME = "delete_soft_deleted_errands";

	@Mock
	private SoftDeletedErrandWorker softDeletedErrandWorkerMock;

	@Mock
	private Dept44HealthUtility healthUtilityMock;

	private SoftDeletedErrandScheduler scheduler;

	@BeforeEach
	void setUp() {
		scheduler = new SoftDeletedErrandScheduler(softDeletedErrandWorkerMock, healthUtilityMock, JOB_NAME);
	}

	@Test
	void deleteSoftDeletedErrands() {
		when(softDeletedErrandWorkerMock.deleteSoftDeletedErrands()).thenReturn(Optional.empty());

		scheduler.deleteSoftDeletedErrands();

		verify(softDeletedErrandWorkerMock).deleteSoftDeletedErrands();
		verifyNoMoreInteractions(softDeletedErrandWorkerMock);
		verifyNoInteractions(healthUtilityMock);
	}

	@Test
	void deleteSoftDeletedErrandsWhenANamespaceWasAborted() {
		final var account = "Deleting soft deleted errands was aborted in namespace NAMESPACE for municipality 2281";
		when(softDeletedErrandWorkerMock.deleteSoftDeletedErrands()).thenReturn(Optional.of(account));

		scheduler.deleteSoftDeletedErrands();

		verify(softDeletedErrandWorkerMock).deleteSoftDeletedErrands();
		verify(healthUtilityMock).setHealthIndicatorUnhealthy(JOB_NAME, account);
		verifyNoMoreInteractions(softDeletedErrandWorkerMock, healthUtilityMock);
	}
}
