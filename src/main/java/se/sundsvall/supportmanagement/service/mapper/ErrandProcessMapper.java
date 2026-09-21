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
import se.sundsvall.supportmanagement.api.model.process.ProcessStartable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessSignalEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;
import se.sundsvall.supportmanagement.service.model.ProcessStartOptions;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.INFO;

public final class ErrandProcessMapper {

	private ErrandProcessMapper() {}

	/**
	 * Maps an instance to the model served both under {@code /processes} and as the {@code process} field of an errand.
	 * <p>
	 * The signals are always set: a process waiting for no one has an empty list, and a process that has ended waits for
	 * no one.
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
	 * Maps whether a process may be started for an errand to the {@code startable} field of the process overview.
	 *
	 * @param  options whether a start is possible, and the keys it may name.
	 * @return         the answer as it is read.
	 */
	public static ProcessStartable toProcessStartable(final ProcessStartOptions options) {
		return ProcessStartable.create()
			.withStatus(options.status())
			.withProcessKeys(options.processKeys());
	}

	/**
	 * What an instance waits for, which is nothing once it has ended, whatever rows it left behind.
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
	 * The report carries the state as a string, which validation has held to the set of states before it reaches here.
	 *
	 * @param  report the report to read the state of.
	 * @return        the state the report carries, or null when it names none.
	 */
	public static ProcessStatus toProcessStatus(final ErrandProcessReport report) {
		return EnumUtils.getEnum(ProcessStatus.class, report.getProcessStatus());
	}

	/**
	 * The error of an instance, or null when the row carries neither half of one.
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
	 * The current activity and the error are replaced by those of the report, so a report without an error clears the
	 * stored one, and the state is applied. The start time is kept when the report leaves it out. The service and the key
	 * of the process identify the row and are left as they were set when it was created.
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
