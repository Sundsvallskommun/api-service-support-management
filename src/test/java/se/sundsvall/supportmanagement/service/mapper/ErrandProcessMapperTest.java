package se.sundsvall.supportmanagement.service.mapper;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcess;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcessReport;
import se.sundsvall.supportmanagement.api.model.process.ProcessActivity;
import se.sundsvall.supportmanagement.api.model.process.ProcessError;
import se.sundsvall.supportmanagement.api.model.process.ProcessSignal;
import se.sundsvall.supportmanagement.api.model.process.ProcessStartable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessSignalEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;
import se.sundsvall.supportmanagement.service.model.ProcessStartOptions;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static se.sundsvall.supportmanagement.api.model.process.ProcessStartability.AVAILABLE;
import static se.sundsvall.supportmanagement.api.model.process.ProcessStartability.PROCESS_COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.ERROR;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.INFO;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.RUNNING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.WAITING;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toErrandProcess;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toErrandProcessActivityEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toErrandProcessEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toErrandProcesses;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toProcessActivity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toProcessStartable;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.updateErrandProcessEntity;

class ErrandProcessMapperTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-14T10:15:30.000Z"), ZoneId.of("UTC"));

	@Test
	void toErrandProcessMapsEveryReadableField() {
		final var started = now(systemDefault());
		final var created = started.plusSeconds(1);
		final var modified = started.plusSeconds(2);

		final var entity = ErrandProcessEntity.create()
			.withId("id")
			.withErrandId("errandId")
			.withProcessService("pw-alkt")
			.withProcessKey("alkt-ansokan")
			.withProcessInstanceId("processInstanceId")
			.withCurrentActivityId("investigation_phase")
			.withCurrentActivityName("Utredning")
			.withErrorCode("INCIDENT")
			.withErrorMessage("Timeout")
			.withStarted(started)
			.withCreated(created)
			.withModified(modified);
		entity.applyStatus(RUNNING, CLOCK);

		final var process = toErrandProcess(entity, List.of(
			ErrandProcessSignalEntity.create().withName("granskning-godkand").withLabel("Godkänn granskning"),
			ErrandProcessSignalEntity.create().withName("granskning-avvisad")));

		assertThat(process.getId()).isEqualTo("id");
		assertThat(process.getProcessService()).isEqualTo("pw-alkt");
		assertThat(process.getProcessKey()).isEqualTo("alkt-ansokan");
		assertThat(process.getProcessInstanceId()).isEqualTo("processInstanceId");
		assertThat(process.getProcessStatus()).isEqualTo(RUNNING.name());
		assertThat(process.getCurrentActivityId()).isEqualTo("investigation_phase");
		assertThat(process.getCurrentActivityName()).isEqualTo("Utredning");
		assertThat(process.getStarted()).isEqualTo(started);
		assertThat(process.getEnded()).isNull();
		assertThat(process.getError()).isEqualTo(ProcessError.create().withCode("INCIDENT").withMessage("Timeout"));
		assertThat(process.getAwaitingSignals()).containsExactly(
			ProcessSignal.create().withName("granskning-godkand").withLabel("Godkänn granskning"),
			ProcessSignal.create().withName("granskning-avvisad"));
		assertThat(process.getCreated()).isEqualTo(created);
		assertThat(process.getModified()).isEqualTo(modified);
	}

	/**
	 * A signal to an ended process is refused, so a button offered for one could only ever fail. Held here because more
	 * than a report ends a process: the relay does too, and leaves the rows behind.
	 */
	@ParameterizedTest
	@EnumSource(value = ProcessStatus.class, names = {
		"COMPLETED", "FAILED"
	})
	void aProcessThatHasEndedWaitsForNoOneWhateverRowsItLeftBehind(final ProcessStatus status) {
		final var entity = ErrandProcessEntity.create().withId("id");
		entity.applyStatus(status, CLOCK);

		assertThat(toErrandProcess(entity, List.of(ErrandProcessSignalEntity.create().withName("granskning-godkand"))).getAwaitingSignals())
			.isNotNull()
			.isEmpty();
	}

	@Test
	void aProcessThatNeverFailedCarriesNoErrorObject() {
		final var entity = ErrandProcessEntity.create().withId("id");
		entity.applyStatus(RUNNING, CLOCK);

		assertThat(toErrandProcess(entity, List.of()).getError()).isNull();
	}

	@Test
	void toErrandProcessesKeepsTheOrderItIsGivenAndGivesEachInstanceItsOwnSignals() {
		final var first = ErrandProcessEntity.create().withId("first");
		final var second = ErrandProcessEntity.create().withId("second");
		first.applyStatus(COMPLETED, CLOCK);
		second.applyStatus(WAITING, CLOCK);

		final var signal = ErrandProcessSignalEntity.create().withErrandProcessId("second").withName("granskning-godkand");

		assertThat(toErrandProcesses(List.of(first, second), Map.of("second", List.of(signal))))
			.extracting(ErrandProcess::getId, process -> process.getAwaitingSignals().stream().map(ProcessSignal::getName).toList())
			.containsExactly(
				tuple("first", List.of()),
				tuple("second", List.of("granskning-godkand")));
	}

	@Test
	void toProcessStartableCarriesTheStatusAsItsNameAndTheKeys() {
		assertThat(toProcessStartable(new ProcessStartOptions(AVAILABLE, List.of("alkt-ansokan", "alkt-tillsyn"))))
			.isEqualTo(ProcessStartable.create().withStatus(AVAILABLE).withProcessKeys(List.of("alkt-ansokan", "alkt-tillsyn")));
		assertThat(toProcessStartable(ProcessStartOptions.unavailable(PROCESS_COMPLETED)))
			.satisfies(startable -> {
				assertThat(startable.getStatus()).isEqualTo("PROCESS_COMPLETED");
				assertThat(startable.getProcessKeys()).isEmpty();
			});
	}

	@Test
	void toErrandProcessEntityCarriesEverythingButTheState() {
		final var started = now(systemDefault());

		final var entity = toErrandProcessEntity("NAMESPACE", "2281", "errandId", "processInstanceId", ErrandProcessReport.create()
			.withProcessService("pw-alkt")
			.withProcessKey("alkt-ansokan")
			.withCurrentActivityId("investigation_phase")
			.withCurrentActivityName("Utredning")
			.withError(ProcessError.create().withCode("INCIDENT").withMessage("Timeout"))
			.withStarted(started));

		assertThat(entity.getNamespace()).isEqualTo("NAMESPACE");
		assertThat(entity.getMunicipalityId()).isEqualTo("2281");
		assertThat(entity.getErrandId()).isEqualTo("errandId");
		assertThat(entity.getProcessInstanceId()).isEqualTo("processInstanceId");
		assertThat(entity.getProcessService()).isEqualTo("pw-alkt");
		assertThat(entity.getProcessKey()).isEqualTo("alkt-ansokan");
		assertThat(entity.getCurrentActivityId()).isEqualTo("investigation_phase");
		assertThat(entity.getCurrentActivityName()).isEqualTo("Utredning");
		assertThat(entity.getErrorCode()).isEqualTo("INCIDENT");
		assertThat(entity.getErrorMessage()).isEqualTo("Timeout");
		assertThat(entity.getStarted()).isEqualTo(started);
		assertThat(entity.getProcessStatus()).isNull();
	}

	@Test
	void aReportWithoutAnErrorClearsTheOneAlreadyStored() {
		final var entity = ErrandProcessEntity.create()
			.withProcessKey("alkt-ansokan")
			.withErrorCode("INCIDENT")
			.withErrorMessage("Timeout");
		entity.applyStatus(FAILED, CLOCK);

		updateErrandProcessEntity(entity, ErrandProcessReport.create().withProcessStatus(RUNNING), CLOCK);

		assertThat(entity.getErrorCode()).isNull();
		assertThat(entity.getErrorMessage()).isNull();
		assertThat(entity.getProcessStatus()).isEqualTo(RUNNING);
		assertThat(entity.getEnded()).isNull();
	}

	@Test
	void aLaterReportLeavingOutTheStartTimeKeepsTheOneAlreadyStored() {
		final var started = now(systemDefault());
		final var entity = ErrandProcessEntity.create().withStarted(started);
		entity.applyStatus(RUNNING, CLOCK);

		updateErrandProcessEntity(entity, ErrandProcessReport.create().withProcessStatus(COMPLETED), CLOCK);

		assertThat(entity.getStarted()).isEqualTo(started);
		assertThat(entity.getEnded()).isEqualTo(OffsetDateTime.now(CLOCK));
	}

	@Test
	void toErrandProcessActivityEntityDefaultsTheSeverityToInfo() {
		final var occurredAt = now(systemDefault());

		final var entity = toErrandProcessActivityEntity("processRowId", "errandId", "externalTaskId", ProcessActivity.create()
			.withActivityType("PHASE")
			.withActivityId("review_phase")
			.withActivityName("Granskning")
			.withMessage("message")
			.withErrorCode("errorCode")
			.withOccurredAt(occurredAt));

		assertThat(entity.getErrandProcessId()).isEqualTo("processRowId");
		assertThat(entity.getErrandId()).isEqualTo("errandId");
		assertThat(entity.getExternalTaskId()).isEqualTo("externalTaskId");
		assertThat(entity.getActivityType()).isEqualTo("PHASE");
		assertThat(entity.getActivityId()).isEqualTo("review_phase");
		assertThat(entity.getActivityName()).isEqualTo("Granskning");
		assertThat(entity.getSeverity()).isEqualTo(INFO);
		assertThat(entity.getMessage()).isEqualTo("message");
		assertThat(entity.getErrorCode()).isEqualTo("errorCode");
		assertThat(entity.getOccurredAt()).isEqualTo(occurredAt);
	}

	@Test
	void anEntryWithoutAProcessRowIsReadWithoutAnInstance() {
		final var occurredAt = now(systemDefault());
		final var withInstance = ErrandProcessActivityEntity.create()
			.withId("withInstance")
			.withErrandProcessId("processRowId")
			.withSeverity(INFO)
			.withOccurredAt(occurredAt);
		final var withoutInstance = ErrandProcessActivityEntity.create()
			.withId("withoutInstance")
			.withSeverity(ERROR)
			.withOccurredAt(occurredAt);

		final var processInstanceIds = Map.of("processRowId", "processInstanceId");

		assertThat(List.of(toProcessActivity(withInstance, processInstanceIds), toProcessActivity(withoutInstance, processInstanceIds)))
			.extracting(ProcessActivity::getId, ProcessActivity::getProcessInstanceId, ProcessActivity::getSeverity)
			.containsExactly(
				tuple("withInstance", "processInstanceId", INFO.name()),
				tuple("withoutInstance", null, ERROR.name()));
	}

	@Test
	void toErrandProcessActivityEntityKeepsAReportedSeverity() {
		final var entity = toErrandProcessActivityEntity("processRowId", "errandId", null, ProcessActivity.create()
			.withActivityType("INCIDENT")
			.withSeverity(ERROR)
			.withOccurredAt(now(systemDefault())));

		assertThat(entity.getSeverity()).isEqualTo(ERROR);
	}
}
