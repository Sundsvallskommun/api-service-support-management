package se.sundsvall.supportmanagement.service.mapper;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcess;
import se.sundsvall.supportmanagement.api.model.process.ProcessActivity;
import se.sundsvall.supportmanagement.api.model.process.ProcessError;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.INFO;

public final class ErrandProcessMapper {

	private ErrandProcessMapper() {}

	/**
	 * Maps an instance to the model served both under {@code /processes} and as the {@code process} field of an errand.
	 * <p>
	 * The three write only fields are left alone here, which together with nulls being dropped from the serialised form
	 * is what keeps them out of every read.
	 *
	 * @param  entity the instance to map.
	 * @return        the instance as it is read, or null when there is none.
	 */
	public static ErrandProcess toErrandProcess(final ErrandProcessEntity entity) {
		if (isNull(entity)) {
			return null;
		}

		return ErrandProcess.create()
			.withId(entity.getId())
			.withProcessService(entity.getProcessService())
			.withProcessKey(entity.getProcessKey())
			.withProcessInstanceId(entity.getProcessInstanceId())
			.withProcessStatus(entity.getProcessStatus())
			.withCurrentActivityId(entity.getCurrentActivityId())
			.withCurrentActivityName(entity.getCurrentActivityName())
			.withStarted(entity.getStarted())
			.withEnded(entity.getEnded())
			.withError(toProcessError(entity))
			.withCreated(entity.getCreated())
			.withModified(entity.getModified());
	}

	public static List<ErrandProcess> toErrandProcesses(final List<ErrandProcessEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(ErrandProcessMapper::toErrandProcess)
			.toList();
	}

	/**
	 * The error of an instance, or null when the row carries neither half of one. Kept apart from the row so that a
	 * process that never failed answers with no error object rather than with an empty one.
	 */
	private static ProcessError toProcessError(final ErrandProcessEntity entity) {
		if (isNull(entity.getErrorCode()) && isNull(entity.getErrorMessage())) {
			return null;
		}

		return ProcessError.create()
			.withCode(entity.getErrorCode())
			.withMessage(entity.getErrorMessage());
	}

	/**
	 * A new instance, with everything but the state, which {@link ErrandProcessEntity#applyStatus} owns.
	 *
	 * @param  namespace         the namespace of the errand.
	 * @param  municipalityId    the municipality of the errand.
	 * @param  errandId          the errand the process runs for.
	 * @param  processInstanceId the instance in the process engine, null when a start never produced one.
	 * @param  report            what the process reported.
	 * @return                   the row to insert, without its state.
	 */
	public static ErrandProcessEntity toErrandProcessEntity(final String namespace, final String municipalityId, final String errandId, final String processInstanceId, final ErrandProcess report) {
		return ErrandProcessEntity.create()
			.withErrandId(errandId)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withProcessService(report.getProcessService())
			.withProcessKey(report.getProcessKey())
			.withProcessInstanceId(processInstanceId)
			.withCurrentActivityId(report.getCurrentActivityId())
			.withCurrentActivityName(report.getCurrentActivityName())
			.withErrorCode(errorCodeOf(report))
			.withErrorMessage(errorMessageOf(report))
			.withStarted(report.getStarted());
	}

	/**
	 * Lays a report over an instance that already exists.
	 * <p>
	 * The report describes the whole state of the process, so the error is replaced rather than merged: an instance that
	 * reports itself running again after an incident was resolved by hand must not keep the message from the incident.
	 * The start time is the exception, since it is a fact about the instance rather than about its current state, and a
	 * later report that leaves it out is not saying the process never started. The service and the key of the process
	 * identify the row and are settled when it is created.
	 *
	 * @param entity the instance to update.
	 * @param report what the process reported.
	 * @param clock  the clock the end time is read from.
	 */
	public static void updateErrandProcessEntity(final ErrandProcessEntity entity, final ErrandProcess report, final Clock clock) {
		entity.setCurrentActivityId(report.getCurrentActivityId());
		entity.setCurrentActivityName(report.getCurrentActivityName());
		entity.setErrorCode(errorCodeOf(report));
		entity.setErrorMessage(errorMessageOf(report));

		ofNullable(report.getStarted()).ifPresent(entity::setStarted);

		entity.applyStatus(report.getProcessStatus(), clock);
	}

	private static String errorCodeOf(final ErrandProcess report) {
		return ofNullable(report.getError()).map(ProcessError::getCode).orElse(null);
	}

	private static String errorMessageOf(final ErrandProcess report) {
		return ofNullable(report.getError()).map(ProcessError::getMessage).orElse(null);
	}

	/**
	 * An entry of a report, as it is stored.
	 *
	 * @param  errandProcessId the instance the entry belongs to.
	 * @param  errandId        the errand the entry belongs to.
	 * @param  externalTaskId  the external task the report was made from, which is half the idempotency key.
	 * @param  activity        the entry as it was reported.
	 * @return                 the row to insert.
	 */
	public static ErrandProcessActivityEntity toErrandProcessActivityEntity(final String errandProcessId, final String errandId, final String externalTaskId, final ProcessActivity activity) {
		return ErrandProcessActivityEntity.create()
			.withErrandProcessId(errandProcessId)
			.withErrandId(errandId)
			.withExternalTaskId(externalTaskId)
			.withActivityType(activity.getActivityType())
			.withActivityId(activity.getActivityId())
			.withActivityName(activity.getActivityName())
			.withSeverity(ofNullable(activity.getSeverity()).orElse(INFO))
			.withMessage(activity.getMessage())
			.withErrorCode(activity.getErrorCode())
			.withOccurredAt(activity.getOccurredAt());
	}

	/**
	 * Maps entries for reading, resolving the instance each of them belongs to.
	 *
	 * @param  entities                 the entries to map.
	 * @param  processInstanceIdByRowId the instance id of every process row the entries point at. An entry pointing at
	 *                                  nothing keeps a null instance, which is what the entries written before any
	 *                                  process existed look like.
	 * @return                          the entries as they are read.
	 */
	public static List<ProcessActivity> toProcessActivities(final List<ErrandProcessActivityEntity> entities, final Map<String, String> processInstanceIdByRowId) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(entity -> toProcessActivity(entity, processInstanceIdByRowId))
			.toList();
	}

	private static ProcessActivity toProcessActivity(final ErrandProcessActivityEntity entity, final Map<String, String> processInstanceIdByRowId) {
		return ProcessActivity.create()
			.withId(entity.getId())
			.withProcessInstanceId(ofNullable(entity.getErrandProcessId()).map(processInstanceIdByRowId::get).orElse(null))
			.withActivityType(entity.getActivityType())
			.withActivityId(entity.getActivityId())
			.withActivityName(entity.getActivityName())
			.withSeverity(entity.getSeverity())
			.withMessage(entity.getMessage())
			.withErrorCode(entity.getErrorCode())
			.withOccurredAt(entity.getOccurredAt())
			.withCreated(entity.getCreated());
	}
}
