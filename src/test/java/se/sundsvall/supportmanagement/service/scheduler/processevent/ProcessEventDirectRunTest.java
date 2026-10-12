package se.sundsvall.supportmanagement.service.scheduler.processevent;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties.DirectRun;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties.LoopGuard;
import se.sundsvall.supportmanagement.integration.pwalkt.PwAlktUnavailableException;
import se.sundsvall.supportmanagement.service.ProcessEventWritten;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ProcessEventDirectRunTest {

	private static final String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";

	@Mock
	private ProcessEventRelay relayMock;

	@Mock
	private TaskExecutor executorMock;

	@Captor
	private ArgumentCaptor<Runnable> runCaptor;

	@Test
	@DisplayName("Verification that a direct run switched off leaves the row to the scheduled run")
	void aDirectRunSwitchedOff() {
		directRun(false).onProcessEventWritten(new ProcessEventWritten(ERRAND_ID));

		verifyNoInteractions(executorMock, relayMock);
	}

	@Test
	@DisplayName("Verification that the delivery is handed to the pool rather than made in the thread that committed the errand, and runs under a request id of its own")
	void theDeliveryIsHandedToThePool() {
		directRun(true).onProcessEventWritten(new ProcessEventWritten(ERRAND_ID));

		verify(executorMock).execute(runCaptor.capture());
		verifyNoInteractions(relayMock);

		final var requestIdDuringRun = new AtomicReference<String>();
		doAnswer(_ -> {
			requestIdDuringRun.set(RequestId.get());
			return null;
		}).when(relayMock).relayErrand(ERRAND_ID);

		runCaptor.getValue().run();

		verify(relayMock).relayErrand(ERRAND_ID);
		assertThat(requestIdDuringRun.get()).isNotBlank();
		assertThat(RequestId.get()).isNull();
	}

	@Test
	@DisplayName("Verification that a run that does not reach pw-alkt is left to the scheduled run")
	void aRunThatDoesNotReachPwAlkt() {
		directRun(true).onProcessEventWritten(new ProcessEventWritten(ERRAND_ID));
		verify(executorMock).execute(runCaptor.capture());
		doThrow(new PwAlktUnavailableException(false, new IllegalStateException("503"))).when(relayMock).relayErrand(ERRAND_ID);

		assertThatNoException().isThrownBy(runCaptor.getValue()::run);
		assertThat(RequestId.get()).isNull();
	}

	@Test
	@DisplayName("Verification that an unforeseen failure in a run is left to the scheduled run as well")
	void aRunThatFailsUnforeseen() {
		directRun(true).onProcessEventWritten(new ProcessEventWritten(ERRAND_ID));
		verify(executorMock).execute(runCaptor.capture());
		doThrow(new IllegalStateException("the database is gone")).when(relayMock).relayErrand(ERRAND_ID);

		assertThatNoException().isThrownBy(runCaptor.getValue()::run);
		assertThat(RequestId.get()).isNull();
	}

	private ProcessEventDirectRun directRun(final boolean enabled) {
		final var properties = new ProcessEngineProperties(new LoopGuard(20, Duration.ofMinutes(10)), new DirectRun(enabled, 2, 4, 500));

		return new ProcessEventDirectRun(relayMock, executorMock, properties);
	}
}
