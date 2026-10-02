package se.sundsvall.supportmanagement.service.scheduler.notificationdispatch;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.service.scheduler.emaildispatch.SubscriberEmailService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchSchedulerTest {

	private static final String JOB_NAME = "process_notification_dispatch";

	@Mock
	private NotificationDispatchWorker workerMock;

	@Mock
	private SubscriberEmailService subscriberEmailServiceMock;

	@Mock
	private Dept44HealthUtility healthUtilityMock;

	@InjectMocks
	private NotificationDispatchScheduler scheduler;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(scheduler, "jobName", JOB_NAME);
	}

	@AfterEach
	void verifyNoMoreMockInteractions() {
		verifyNoMoreInteractions(workerMock, subscriberEmailServiceMock, healthUtilityMock);
	}

	@Test
	void processDispatch_delegatesToWorkerAndThenSendsPendingEmails() {
		final var entry = NotificationDispatchEntity.create().withId("some-id").withErrandId("errand-1");
		when(workerMock.fetchProcessable()).thenReturn(List.of(entry));
		when(subscriberEmailServiceMock.findPendingSubscriberIds()).thenReturn(List.of("subscriber-1", "subscriber-2"));

		scheduler.processDispatch();

		final InOrder inOrder = inOrder(workerMock, subscriberEmailServiceMock);
		inOrder.verify(workerMock).fetchProcessable();
		inOrder.verify(workerMock).processGroup(List.of(entry));
		inOrder.verify(subscriberEmailServiceMock).findPendingSubscriberIds();
		inOrder.verify(subscriberEmailServiceMock).sendPending("subscriber-1");
		inOrder.verify(subscriberEmailServiceMock).sendPending("subscriber-2");
	}

	@Test
	void processDispatch_groupsEntriesByErrandIdRegardlessOfRequestGroupId() {
		final var entry1 = NotificationDispatchEntity.create().withId("id-1").withErrandId("errand-A").withRequestGroupId("group-1");
		final var entry2 = NotificationDispatchEntity.create().withId("id-2").withErrandId("errand-A").withRequestGroupId("group-1");
		final var entry3 = NotificationDispatchEntity.create().withId("id-3").withErrandId("errand-A").withRequestGroupId("group-2");
		final var entry4 = NotificationDispatchEntity.create().withId("id-4").withErrandId("errand-B").withRequestGroupId("group-1");
		when(workerMock.fetchProcessable()).thenReturn(List.of(entry1, entry2, entry3, entry4));

		scheduler.processDispatch();

		verify(workerMock).fetchProcessable();
		verify(workerMock).processGroup(List.of(entry1, entry2, entry3));
		verify(workerMock).processGroup(List.of(entry4));
		verify(subscriberEmailServiceMock).findPendingSubscriberIds();
	}

	@Test
	void processDispatch_processGroupThrows_setsUnhealthyAndContinuesWithOtherErrandsAndEmails() {
		final var failing = NotificationDispatchEntity.create().withId("id-1").withErrandId("errand-1");
		final var succeeding = NotificationDispatchEntity.create().withId("id-2").withErrandId("errand-2");
		when(workerMock.fetchProcessable()).thenReturn(List.of(failing, succeeding));
		doThrow(new RuntimeException("channel error")).when(workerMock).processGroup(List.of(failing));
		doNothing().when(workerMock).processGroup(List.of(succeeding));
		when(subscriberEmailServiceMock.findPendingSubscriberIds()).thenReturn(List.of("subscriber-1"));

		scheduler.processDispatch();

		verify(workerMock).fetchProcessable();
		verify(workerMock).processGroup(List.of(failing));
		verify(workerMock).processGroup(List.of(succeeding));
		verify(healthUtilityMock).setHealthIndicatorUnhealthy(eq(JOB_NAME), any(String.class));
		verify(subscriberEmailServiceMock).findPendingSubscriberIds();
		verify(subscriberEmailServiceMock).sendPending("subscriber-1");
	}

	@Test
	void processDispatch_sendPendingThrows_setsUnhealthyAndContinuesWithOtherSubscribers() {
		when(workerMock.fetchProcessable()).thenReturn(List.of());
		when(subscriberEmailServiceMock.findPendingSubscriberIds()).thenReturn(List.of("subscriber-1", "subscriber-2"));
		doThrow(new RuntimeException("messaging error")).when(subscriberEmailServiceMock).sendPending("subscriber-1");

		scheduler.processDispatch();

		verify(workerMock).fetchProcessable();
		verify(subscriberEmailServiceMock).findPendingSubscriberIds();
		verify(subscriberEmailServiceMock).sendPending("subscriber-1");
		verify(subscriberEmailServiceMock).sendPending("subscriber-2");
		verify(healthUtilityMock).setHealthIndicatorUnhealthy(JOB_NAME, "Error sending email dispatch: messaging error");
	}

	@Test
	void processDispatch_fetchProcessableThrows_bubblesUpToTheSchedulingAspect() {
		doThrow(new RuntimeException("db error")).when(workerMock).fetchProcessable();

		assertThatThrownBy(() -> scheduler.processDispatch())
			.isInstanceOf(RuntimeException.class)
			.hasMessage("db error");

		verify(workerMock).fetchProcessable();
	}
}
