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

	@Mock
	private NotificationDispatchWorker workerMock;

	@Mock
	private Dept44HealthUtility healthUtilityMock;

	@InjectMocks
	private NotificationDispatchScheduler scheduler;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(scheduler, "jobName", "process_notification_dispatch");
	}

	@Test
	void processDispatch_delegatesToWorker() {
		final var entry = NotificationDispatchEntity.create().withId("some-id").withErrandId("errand-1");
		when(workerMock.fetchProcessable()).thenReturn(List.of(entry));

		scheduler.processDispatch();

		verify(workerMock).fetchProcessable();
		verify(workerMock).processGroup(List.of(entry));
		verifyNoMoreInteractions(workerMock, healthUtilityMock);
	}

	@Test
	void processDispatch_groupsEntriesByErrandId() {
		final var entry1 = NotificationDispatchEntity.create().withId("id-1").withErrandId("errand-A");
		final var entry2 = NotificationDispatchEntity.create().withId("id-2").withErrandId("errand-A");
		final var entry3 = NotificationDispatchEntity.create().withId("id-3").withErrandId("errand-B");
		when(workerMock.fetchProcessable()).thenReturn(List.of(entry1, entry2, entry3));

		scheduler.processDispatch();

		verify(workerMock).processGroup(List.of(entry1, entry2));
		verify(workerMock).processGroup(List.of(entry3));
		verifyNoMoreInteractions(workerMock, healthUtilityMock);
	}

	@Test
	void processDispatch_workerThrows_setsHealthIndicatorUnhealthy() {
		final var entry = NotificationDispatchEntity.create().withId("some-id").withErrandId("errand-1");
		when(workerMock.fetchProcessable()).thenReturn(List.of(entry));
		doThrow(new RuntimeException("channel error")).when(workerMock).processGroup(any());

		scheduler.processDispatch();

		verify(workerMock).fetchProcessable();
		verify(workerMock).processGroup(any());
		verify(healthUtilityMock).setHealthIndicatorUnhealthy(eq("process_notification_dispatch"), any(String.class));
		verifyNoMoreInteractions(workerMock, healthUtilityMock);
	}
}
