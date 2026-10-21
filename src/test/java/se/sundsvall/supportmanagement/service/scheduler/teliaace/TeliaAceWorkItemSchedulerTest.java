package se.sundsvall.supportmanagement.service.scheduler.teliaace;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.supportmanagement.integration.db.model.TeliaAceWorkItemEntity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeliaAceWorkItemSchedulerTest {

	private static final String JOB_NAME = "deliver_telia_ace_work_items";

	@Mock
	private TeliaAceWorkItemWorker workerMock;

	@Mock
	private Dept44HealthUtility healthUtilityMock;

	@InjectMocks
	private TeliaAceWorkItemScheduler scheduler;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(scheduler, "jobName", JOB_NAME);
	}

	@Test
	void processWorkItems_processesEachFetchedEntry() {
		final var item1 = TeliaAceWorkItemEntity.create().withId("id-1");
		final var item2 = TeliaAceWorkItemEntity.create().withId("id-2");
		when(workerMock.fetchProcessable()).thenReturn(List.of(item1, item2));

		scheduler.processWorkItems();

		verify(workerMock).fetchProcessable();
		verify(workerMock).process(item1);
		verify(workerMock).process(item2);
		verifyNoMoreInteractions(workerMock, healthUtilityMock);
	}

	@Test
	void processWorkItems_whenProcessThrows_setsUnhealthyAndContinuesWithOtherItems() {
		final var failing = TeliaAceWorkItemEntity.create().withId("id-1");
		final var succeeding = TeliaAceWorkItemEntity.create().withId("id-2");
		when(workerMock.fetchProcessable()).thenReturn(List.of(failing, succeeding));
		doThrow(new RuntimeException("ACE unavailable")).when(workerMock).process(failing);

		scheduler.processWorkItems();

		verify(workerMock).process(failing);
		verify(workerMock).process(succeeding);
		verify(healthUtilityMock).setHealthIndicatorUnhealthy(eq(JOB_NAME), any(String.class));
		verifyNoMoreInteractions(workerMock, healthUtilityMock);
	}
}
