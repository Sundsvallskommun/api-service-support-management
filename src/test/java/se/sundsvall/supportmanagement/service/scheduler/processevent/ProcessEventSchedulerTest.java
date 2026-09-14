package se.sundsvall.supportmanagement.service.scheduler.processevent;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessEventSchedulerTest {

	private static final String JOB_NAME = "process_event_relay";

	@Mock
	private ProcessEventRelay relayMock;

	@Mock
	private ProcessEventCleanup cleanupMock;

	@Mock
	private Dept44HealthUtility healthUtilityMock;

	@InjectMocks
	private ProcessEventScheduler scheduler;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(scheduler, "relayJobName", JOB_NAME);
	}

	@Test
	@DisplayName("Verification that a run with nothing left over leaves the health indicator to the scheduling aspect")
	void aHealthyRun() {
		when(relayMock.findHealthFault()).thenReturn(Optional.empty());

		scheduler.relay();

		verify(relayMock).relay();
		verifyNoInteractions(healthUtilityMock);
	}

	@Test
	@DisplayName("Verification that what is left over after a run is what turns the health indicator")
	void anUnhealthyRun() {
		when(relayMock.findHealthFault()).thenReturn(Optional.of("the oldest undelivered process event is too old"));

		scheduler.relay();

		verify(healthUtilityMock).setHealthIndicatorUnhealthy(JOB_NAME, "the oldest undelivered process event is too old");
	}

	@Test
	@DisplayName("Verification that a run that fails is left to the scheduling aspect, which marks the job unhealthy itself")
	void aFailingRun() {
		doThrow(new IllegalStateException("the database is gone")).when(relayMock).relay();

		assertThatIllegalStateException().isThrownBy(scheduler::relay);

		verify(relayMock, never()).findHealthFault();
		verifyNoInteractions(healthUtilityMock);
	}

	@Test
	void theCleanupRemovesDeliveredRows() {
		scheduler.cleanUp();

		verify(cleanupMock).removeDelivered();
		verifyNoInteractions(relayMock, healthUtilityMock);
	}
}
