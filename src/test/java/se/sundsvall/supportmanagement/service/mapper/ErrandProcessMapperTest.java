package se.sundsvall.supportmanagement.service.mapper;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcess;
import se.sundsvall.supportmanagement.api.model.process.ProcessActivity;
import se.sundsvall.supportmanagement.api.model.process.ProcessError;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.ERROR;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.INFO;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.RUNNING;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toErrandProcess;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toErrandProcessActivityEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toErrandProcessEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toErrandProcesses;
import static se.sundsvall.supportmanagement.service.mapper.ErrandProcessMapper.toProcessActivities;
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

		final var process = toErrandProcess(entity);

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
		assertThat(process.getCreated()).isEqualTo(created);
		assertThat(process.getModified()).isEqualTo(modified);
	}

	@Test
	void toErrandProcessLeavesTheReportOnlyFieldsUnset() {
		final var entity = ErrandProcessEntity.create().withId("id").withProcessKey("alkt-ansokan");
		entity.applyStatus(RUNNING, CLOCK);

		assertThat(toErrandProcess(entity))
			.extracting(ErrandProcess::getExternalTaskId, ErrandProcess::getErrandVersion, ErrandProcess::getActivities)
			.containsOnlyNulls();
	}

	@Test
	void aProcessThatNeverFailedCarriesNoErrorObject() {
		final var entity = ErrandProcessEntity.create().withId("id");
		entity.applyStatus(RUNNING, CLOCK);

		assertThat(toErrandProcess(entity).getError()).isNull();
	}

	@Test
	void toErrandProcessHandlesNull() {
		assertThat(toErrandProcess(null)).isNull();
		assertThat(toErrandProcesses(null)).isEmpty();
	}

	@Test
	void toErrandProcessesKeepsTheOrderItIsGiven() {
		final var first = ErrandProcessEntity.create().withId("first");
		final var second = ErrandProcessEntity.create().withId("second");
		first.applyStatus(COMPLETED, CLOCK);
		second.applyStatus(RUNNING, CLOCK);

		assertThat(toErrandProcesses(List.of(first, second)))
			.extracting(ErrandProcess::getId)
			.containsExactly("first", "second");
	}

	@Test
	void toErrandProcessEntityCarriesEverythingButTheState() {
		final var started = now(systemDefault());

		final var entity = toErrandProcessEntity("NAMESPACE", "2281", "errandId", "processInstanceId", ErrandProcess.create()
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

		updateErrandProcessEntity(entity, ErrandProcess.create().withProcessStatus(RUNNING), CLOCK);

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

		updateErrandProcessEntity(entity, ErrandProcess.create().withProcessStatus(COMPLETED), CLOCK);

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

		final var activities = toProcessActivities(List.of(withInstance, withoutInstance), Map.of("processRowId", "processInstanceId"));

		assertThat(activities)
			.extracting(ProcessActivity::getId, ProcessActivity::getProcessInstanceId, ProcessActivity::getSeverity)
			.containsExactly(
				tuple("withInstance", "processInstanceId", INFO.name()),
				tuple("withoutInstance", null, ERROR.name()));
	}

	@Test
	void toProcessActivitiesHandlesNull() {
		assertThat(toProcessActivities(null, Map.of())).isEmpty();
	}
}
