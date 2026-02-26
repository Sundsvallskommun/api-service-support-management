package se.sundsvall.supportmanagement.service.scheduler.action;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.supportmanagement.integration.db.model.ErrandActionEntity;

@ExtendWith(MockitoExtension.class)
class ActionSchedulerTest {

	@Mock
	private ActionWorker actionWorkerMock;

	@Mock
	private Dept44HealthUtility healthUtilityMock;

	@InjectMocks
	private ActionScheduler actionScheduler;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(actionScheduler, "jobName", "process_actions");
	}

	@Test
	void processActions() {
		final var action1 = ErrandActionEntity.create().withId("action-1");
		final var action2 = ErrandActionEntity.create().withId("action-2");

		when(actionWorkerMock.getExpiredActions()).thenReturn(List.of(action1, action2));

		actionScheduler.processActions();

		verify(actionWorkerMock).getExpiredActions();
		verify(actionWorkerMock).processAction(action1);
		verify(actionWorkerMock).processAction(action2);
		verifyNoMoreInteractions(actionWorkerMock, healthUtilityMock);
	}

	@Test
	void processActionsWithNoExpiredActions() {
		when(actionWorkerMock.getExpiredActions()).thenReturn(List.of());

		actionScheduler.processActions();

		verify(actionWorkerMock).getExpiredActions();
		verifyNoMoreInteractions(actionWorkerMock, healthUtilityMock);
	}

	@Test
	void processActionsThrowsExceptionAndContinuesProcessing() {
		final var action1 = ErrandActionEntity.create().withId("action-1");
		final var action2 = ErrandActionEntity.create().withId("action-2");

		when(actionWorkerMock.getExpiredActions()).thenReturn(List.of(action1, action2));
		doThrow(new RuntimeException("test error")).when(actionWorkerMock).processAction(action1);

		actionScheduler.processActions();

		verify(actionWorkerMock).getExpiredActions();
		verify(actionWorkerMock, times(2)).processAction(any());
		verify(healthUtilityMock).setHealthIndicatorUnhealthy(eq("process_actions"), any(String.class));
		verifyNoMoreInteractions(actionWorkerMock, healthUtilityMock);
	}
}
