package se.sundsvall.supportmanagement.service.mapper;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.EnumUtils;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcess;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcessReport;
import se.sundsvall.supportmanagement.api.model.process.ProcessActivity;
import se.sundsvall.supportmanagement.api.model.process.ProcessError;
import se.sundsvall.supportmanagement.api.model.process.ProcessSignal;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessSignalEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.INFO;

public final class ErrandProcessMapper {

	private ErrandProcessMapper() {}

	/**
	 * Maps an instance to the model served both under {@code /processes} and as the {@code process} field of an errand.
	 * <p>
	 * The signals are always set, so that a process waiting for no one says so with an empty list rather than by leaving
	 * the field out, and a process that has ended waits for no one.
	 *
	 * @param  entity  the instance to map.
	 * @param  signals what the instance waits for from a handler, in the order the process reported it.
	 * @return         the instance as it is read.
	 */
	public static ErrandProcess toErrandProcess(final ErrandProcessEntity entity, final List<ErrandProcessSignalEntity> signals) {
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
			.withAwaitingSignals(toProcessSignals(entity, signals))
			.withCreated(entity.getCreated())
			.withModified(entity.getModified());
	}

	/**
	 * Maps instances, each with its own signals.
	 *
	 * @param  entities           the instances to map.
	 * @param  signalsByProcessId the signals of the instances, keyed by the id of the process row. An instance with no
	 *                            entry waits for no one.
	 * @return                    the instances as they are read, in the order given.
	 */
	public static List<ErrandProcess> toErrandProcesses(final List<ErrandProcessEntity> entities, final Map<String, List<ErrandProcessSignalEntity>> signalsByProcessId) {
		return entities.stream()
			.map(entity -> toErrandProcess(entity, signalsByProcessId.getOrDefault(entity.getId(), emptyList())))
			.toList();
	}

	/**
	 * What an instance waits for, which is nothing once it has ended, whatever rows it left behind. A signal to an ended
	 * process is refused, and a process is ended by more than its own report - the relay ends one the process engine
	 * refuses - so the rule is held here, where every reading passes, rather than on each path that ends a process.
	 */
	private static List<ProcessSignal> toProcessSignals(final ErrandProcessEntity entity, final List<ErrandProcessSignalEntity> signals) {
		if (entity.getProcessStatus().isTerminal()) {
			return emptyList();
		}

		return signals.stream()
			.map(signal -> ProcessSignal.create()
				.withName(signal.getName())
				.withLabel(signal.getLabel()))
			.toList();
	}

	/**
	 * The reported state as the enum this service works in.
	 * <p>
	 * The report carries it as a string so that the published schema does not pin the set, and the value is held to that
	 * set by validation before it ever reaches here - so an unknown one is a bug rather than a bad request, and is left to
	 * fail as one.
	 *
	 * @param  report the report to read the state of.
	 * @return        the state the report carries.
	 */
	public static ProcessStatus toProcessStatus(final ErrandProcessReport report) {
		return EnumUtils.getEnum(ProcessStatus.class, report.getProcessStatus());
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
	public static ErrandProcessEntity toErrandProcessEntity(final String namespace, final String municipalityId, final String errandId, final String processInstanceId, final ErrandProcessReport report) {
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
	public static void updateErrandProcessEntity(final ErrandProcessEntity entity, final ErrandProcessReport report, final Clock clock) {
		entity.setCurrentActivityId(report.getCurrentActivityId());
		entity.setCurrentActivityName(report.getCurrentActivityName());
		entity.setErrorCode(errorCodeOf(report));
		entity.setErrorMessage(errorMessageOf(report));

		ofNullable(report.getStarted()).ifPresent(entity::setStarted);

		entity.applyStatus(toProcessStatus(report), clock);
	}

	private static String errorCodeOf(final ErrandProcessReport report) {
		return ofNullable(report.getError()).map(ProcessError::getCode).orElse(null);
	}

	private static String errorMessageOf(final ErrandProcessReport report) {
		return ofNullable(report.getError()).map(ProcessError::getMessage).orElse(null);
	}

	/**
	 * An entry of a report, as it is stored. An entry that states no severity is information.
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
			.withSeverity(EnumUtils.getEnum(ActivitySeverity.class, activity.getSeverity(), INFO))
			.withMessage(activity.getMessage())
			.withErrorCode(activity.getErrorCode())
			.withOccurredAt(activity.getOccurredAt());
	}

	/**
	 * Maps an entry for reading, resolving the instance it belongs to.
	 *
	 * @param  entity                   the entry to map.
	 * @param  processInstanceIdByRowId the instance id of every process row the entries of the page point at. An entry
	 *                                  pointing at nothing keeps a null instance, which is what the entries written
	 *                                  before any process existed look like.
	 * @return                          the entry as it is read.
	 */
	public static ProcessActivity toProcessActivity(final ErrandProcessActivityEntity entity, final Map<String, String> processInstanceIdByRowId) {
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
