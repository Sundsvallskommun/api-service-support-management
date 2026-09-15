package se.sundsvall.supportmanagement.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties.DirectRun;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties.LoopGuard;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessActivityRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity.MESSAGE_LENGTH;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.ERROR;

@ExtendWith(MockitoExtension.class)
class ProcessErrorLogTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-14T08:00:00.123456Z"), ZoneId.of("UTC"));
	private static final OffsetDateTime NOW = OffsetDateTime.now(CLOCK).truncatedTo(MILLIS);
	private static final Duration WINDOW = Duration.ofMinutes(10);
	private static final String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
	private static final String ACTIVITY_TYPE = "DELIVERY";
	private static final String ERROR_CODE = "PROCESS_KEY_NOT_DEPLOYED";

	@Mock
	private ErrandProcessActivityRepository activityRepositoryMock;

	@Captor
	private ArgumentCaptor<ErrandProcessActivityEntity> entryCaptor;

	@Test
	@DisplayName("Verification that an entry is written when none for the same fault stands on the errand inside the window")
	void anEntryIsWritten() {
		when(activityRepositoryMock.existsByErrandIdAndErrorCodeAndCreatedAfter(ERRAND_ID, ERROR_CODE, NOW.minus(WINDOW))).thenReturn(false);

		errorLog().writeOncePerWindow(ERRAND_ID, "process-1", ACTIVITY_TYPE, ERROR_CODE, "what is wrong");

		verify(activityRepositoryMock).save(entryCaptor.capture());
		assertThat(entryCaptor.getValue()).satisfies(entry -> {
			assertThat(entry.getErrandId()).isEqualTo(ERRAND_ID);
			assertThat(entry.getErrandProcessId()).isEqualTo("process-1");
			assertThat(entry.getActivityType()).isEqualTo(ACTIVITY_TYPE);
			assertThat(entry.getSeverity()).isEqualTo(ERROR);
			assertThat(entry.getErrorCode()).isEqualTo(ERROR_CODE);
			assertThat(entry.getMessage()).isEqualTo("what is wrong");
			assertThat(entry.getOccurredAt()).isEqualTo(NOW);
		});
	}

	@Test
	@DisplayName("Verification that a fault without a process instance is written on the errand alone")
	void anEntryWithoutAnInstance() {
		when(activityRepositoryMock.existsByErrandIdAndErrorCodeAndCreatedAfter(ERRAND_ID, ERROR_CODE, NOW.minus(WINDOW))).thenReturn(false);

		errorLog().writeOncePerWindow(ERRAND_ID, null, ACTIVITY_TYPE, ERROR_CODE, "what is wrong");

		verify(activityRepositoryMock).save(entryCaptor.capture());
		assertThat(entryCaptor.getValue().getErrandProcessId()).isNull();
	}

	@Test
	@DisplayName("Verification that no second entry for the same fault is written inside the window, however often the fault repeats")
	void noSecondEntryInsideTheWindow() {
		when(activityRepositoryMock.existsByErrandIdAndErrorCodeAndCreatedAfter(ERRAND_ID, ERROR_CODE, NOW.minus(WINDOW))).thenReturn(true);

		errorLog().writeOncePerWindow(ERRAND_ID, null, ACTIVITY_TYPE, ERROR_CODE, "what is wrong");

		verify(activityRepositoryMock, never()).save(any());
	}

	@Test
	@DisplayName("Verification that a message longer than its column is cut to fit, since an entry reporting a fault may not cause one")
	void theMessageIsCutToFit() {
		when(activityRepositoryMock.existsByErrandIdAndErrorCodeAndCreatedAfter(ERRAND_ID, ERROR_CODE, NOW.minus(WINDOW))).thenReturn(false);

		errorLog().writeOncePerWindow(ERRAND_ID, null, ACTIVITY_TYPE, ERROR_CODE, "x".repeat(MESSAGE_LENGTH + 10));

		verify(activityRepositoryMock).save(entryCaptor.capture());
		assertThat(entryCaptor.getValue().getMessage()).hasSize(MESSAGE_LENGTH);
	}

	private ProcessErrorLog errorLog() {
		return new ProcessErrorLog(activityRepositoryMock, new ProcessEngineProperties(new LoopGuard(20, WINDOW), new DirectRun(true, 2, 4, 500)), CLOCK);
	}
}
