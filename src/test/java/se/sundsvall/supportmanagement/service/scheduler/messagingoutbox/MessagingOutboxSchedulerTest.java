package se.sundsvall.supportmanagement.service.scheduler.messagingoutbox;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.supportmanagement.integration.db.model.MessagingOutboxEntity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessagingOutboxSchedulerTest {

	@Mock
	private MessagingOutboxWorker workerMock;

	@Mock
	private Dept44HealthUtility healthUtilityMock;

	@InjectMocks
	private MessagingOutboxScheduler scheduler;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(scheduler, "jobName", "process_messaging_outbox");
	}

	@Test
	void processOutbox() {
		final var entry1 = MessagingOutboxEntity.create().withId("entry-1");
		final var entry2 = MessagingOutboxEntity.create().withId("entry-2");

		when(workerMock.fetchProcessable()).thenReturn(List.of(entry1, entry2));

		scheduler.processOutbox();

		verify(workerMock).fetchProcessable();
		verify(workerMock).process(entry1);
		verify(workerMock).process(entry2);
		verifyNoMoreInteractions(workerMock, healthUtilityMock);
	}

	@Test
	void processOutboxWithNoEntries() {
		when(workerMock.fetchProcessable()).thenReturn(List.of());

		scheduler.processOutbox();

		verify(workerMock).fetchProcessable();
		verifyNoMoreInteractions(workerMock, healthUtilityMock);
	}

	@Test
	void processOutboxLogsErrorAndContinuesOnException() {
		final var entry1 = MessagingOutboxEntity.create().withId("entry-1");
		final var entry2 = MessagingOutboxEntity.create().withId("entry-2");

		when(workerMock.fetchProcessable()).thenReturn(List.of(entry1, entry2));
		doThrow(new RuntimeException("send failed")).when(workerMock).process(entry1);

		scheduler.processOutbox();

		verify(workerMock).fetchProcessable();
		verify(workerMock, times(2)).process(any());
		verify(healthUtilityMock).setHealthIndicatorUnhealthy(eq("process_messaging_outbox"), any(String.class));
		verifyNoMoreInteractions(workerMock, healthUtilityMock);
	}
}
