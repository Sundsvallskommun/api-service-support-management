package se.sundsvall.supportmanagement.service.scheduler.notificationdispatch;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchSchedulerTest {

	private static final String JOB_NAME = "process_notification_dispatch";

	@Mock
	private NotificationDispatchWorker workerMock;

	@Mock
	private Dept44HealthUtility healthUtilityMock;

	@InjectMocks
	private NotificationDispatchScheduler scheduler;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(scheduler, "jobName", JOB_NAME);
	}

	@Test
	void processDispatch_delegatesToWorker() {
		final var entry = NotificationDispatchEntity.create().withId("some-id").withErrandId("errand-1");
		when(workerMock.fetchProcessable()).thenReturn(List.of(entry));

		scheduler.processDispatch();

		verify(healthUtilityMock).setHealthIndicatorHealthy(JOB_NAME);
		verify(workerMock).fetchProcessable();
		verify(workerMock).processGroup(List.of(entry));
		verify(workerMock).cleanUpDeadLetters();
		verifyNoMoreInteractions(workerMock, healthUtilityMock);
	}

	@Test
	void processDispatch_groupsEntriesByErrandId() {
		final var entry1 = NotificationDispatchEntity.create().withId("id-1").withErrandId("errand-A");
		final var entry2 = NotificationDispatchEntity.create().withId("id-2").withErrandId("errand-A");
		final var entry3 = NotificationDispatchEntity.create().withId("id-3").withErrandId("errand-B");
		when(workerMock.fetchProcessable()).thenReturn(List.of(entry1, entry2, entry3));

		scheduler.processDispatch();

		verify(healthUtilityMock).setHealthIndicatorHealthy(JOB_NAME);
		verify(workerMock).processGroup(List.of(entry1, entry2));
		verify(workerMock).processGroup(List.of(entry3));
		verify(workerMock).cleanUpDeadLetters();
		verifyNoMoreInteractions(workerMock, healthUtilityMock);
	}

	@Test
	void processDispatch_processGroupThrows_setsUnhealthyAndContinuesToCleanUp() {
		final var entry = NotificationDispatchEntity.create().withId("some-id").withErrandId("errand-1");
		when(workerMock.fetchProcessable()).thenReturn(List.of(entry));
		doThrow(new RuntimeException("channel error")).when(workerMock).processGroup(any());

		scheduler.processDispatch();

		verify(healthUtilityMock).setHealthIndicatorHealthy(JOB_NAME);
		verify(workerMock).fetchProcessable();
		verify(workerMock).processGroup(any());
		verify(healthUtilityMock).setHealthIndicatorUnhealthy(eq(JOB_NAME), any(String.class));
		verify(workerMock).cleanUpDeadLetters();
		verifyNoMoreInteractions(workerMock, healthUtilityMock);
	}

	@Test
	void processDispatch_fetchProcessableThrows_setsUnhealthyAndSkipsCleanUp() {
		doThrow(new RuntimeException("db error")).when(workerMock).fetchProcessable();

		scheduler.processDispatch();

		verify(healthUtilityMock).setHealthIndicatorHealthy(JOB_NAME);
		verify(workerMock).fetchProcessable();
		verify(healthUtilityMock).setHealthIndicatorUnhealthy(eq(JOB_NAME), any(String.class));
		verifyNoMoreInteractions(workerMock, healthUtilityMock);
	}
}
