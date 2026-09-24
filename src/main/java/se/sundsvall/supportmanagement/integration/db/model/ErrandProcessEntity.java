package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;

import static java.lang.Boolean.TRUE;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Objects.nonNull;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * The state of a process instance attached to an errand.
 * <p>
 * The errand is referenced by id only, and the foreign key lives in the database alone. The process is not part of the
 * errand aggregate, and a change to it produces no revision of the errand.
 */
@Entity
@Table(name = "errand_process",
	indexes = {
		@Index(name = "idx_ep_errand_id", columnList = "errand_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_ep_process_instance_id", columnNames = "process_instance_id"),
		@UniqueConstraint(name = "uq_ep_one_active_per_errand", columnNames = {
			"errand_id", "active_marker"
		})
	})
public class ErrandProcessEntity {

	@Id
	@UuidGenerator
	@Column(name = "id", length = 36)
	private String id;

	@Column(name = "errand_id", nullable = false, length = 255)
	private String errandId;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "process_service", nullable = false, length = 64)
	private String processService;

	@Column(name = "process_key", nullable = false, length = 128)
	private String processKey;

	@Column(name = "process_instance_id", length = 64)
	private String processInstanceId;

	@Enumerated(EnumType.STRING)
	@Column(name = "process_status", nullable = false, length = 32)
	private ProcessStatus processStatus;

	@Column(name = "current_activity_id", length = 255)
	private String currentActivityId;

	@Column(name = "current_activity_name", length = 255)
	private String currentActivityName;

	/**
	 * The external task that reported RUNNING and has not reported since. Cleared when that task reports again.
	 * <p>
	 * A second task reporting RUNNING while another still stands here is recognised as a parallel branch, which the
	 * process models are not allowed to have.
	 */
	@Column(name = "outstanding_external_task_id", length = 64)
	private String outstandingExternalTaskId;

	@Column(name = "error_code", length = 64)
	private String errorCode;

	@Column(name = "error_message", length = 2048)
	private String errorMessage;

	@Column(name = "started", columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime started;

	@Column(name = "ended", columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime ended;

	/**
	 * TRUE while the instance lives and null once it is terminal, never FALSE, so that
	 * {@code uq_ep_one_active_per_errand} allows an errand any number of finished instances but only one live one. Owned
	 * by {@link #applyStatus(ProcessStatus, Clock)}.
	 */
	@Column(name = "active_marker")
	private Boolean activeMarker;

	@Column(name = "created", nullable = false, columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified", columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	public static ErrandProcessEntity create() {
		return new ErrandProcessEntity();
	}

	@PrePersist
	void onCreate() {
		created = now(systemDefault()).truncatedTo(MILLIS);
	}

	@PreUpdate
	void onUpdate() {
		modified = now(systemDefault()).truncatedTo(MILLIS);
	}

	/**
	 * The only way to set the status. Sets the active marker and the end time to match it: a terminal status clears the
	 * marker and sets the end time from the clock.
	 * <p>
	 * A status that is alive again, as when an incident is resolved by hand and a FAILED instance resumes, clears the end
	 * time and gives the marker back. The instance can then find the place taken by another one started meanwhile - the
	 * unique constraint says so, and the caller has to tell the handler which instance is in the way.
	 * <p>
	 * A terminal status reported again keeps the end time it was given the first time.
	 *
	 * @param status the state the process is in.
	 * @param clock  the clock the end time is read from.
	 */
	public void applyStatus(final ProcessStatus status, final Clock clock) {
		final var terminal = status.isTerminal();
		final var repeated = status == processStatus && nonNull(ended);

		this.processStatus = status;
		this.activeMarker = terminal ? null : TRUE;

		if (!terminal) {
			this.ended = null;
		} else if (!repeated) {
			this.ended = now(clock).truncatedTo(MILLIS);
		}
	}

	/**
	 * Whether the instance is alive, which is what its active marker says.
	 *
	 * @return true while the status is not terminal.
	 */
	public boolean isLive() {
		return nonNull(activeMarker);
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ErrandProcessEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public ErrandProcessEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public ErrandProcessEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public ErrandProcessEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getProcessService() {
		return processService;
	}

	public void setProcessService(final String processService) {
		this.processService = processService;
	}

	public ErrandProcessEntity withProcessService(final String processService) {
		this.processService = processService;
		return this;
	}

	public String getProcessKey() {
		return processKey;
	}

	public void setProcessKey(final String processKey) {
		this.processKey = processKey;
	}

	public ErrandProcessEntity withProcessKey(final String processKey) {
		this.processKey = processKey;
		return this;
	}

	public String getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(final String processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public ErrandProcessEntity withProcessInstanceId(final String processInstanceId) {
		this.processInstanceId = processInstanceId;
		return this;
	}

	public ProcessStatus getProcessStatus() {
		return processStatus;
	}

	public String getCurrentActivityId() {
		return currentActivityId;
	}

	public void setCurrentActivityId(final String currentActivityId) {
		this.currentActivityId = currentActivityId;
	}

	public ErrandProcessEntity withCurrentActivityId(final String currentActivityId) {
		this.currentActivityId = currentActivityId;
		return this;
	}

	public String getCurrentActivityName() {
		return currentActivityName;
	}

	public void setCurrentActivityName(final String currentActivityName) {
		this.currentActivityName = currentActivityName;
	}

	public ErrandProcessEntity withCurrentActivityName(final String currentActivityName) {
		this.currentActivityName = currentActivityName;
		return this;
	}

	public String getOutstandingExternalTaskId() {
		return outstandingExternalTaskId;
	}

	public void setOutstandingExternalTaskId(final String outstandingExternalTaskId) {
		this.outstandingExternalTaskId = outstandingExternalTaskId;
	}

	public ErrandProcessEntity withOutstandingExternalTaskId(final String outstandingExternalTaskId) {
		this.outstandingExternalTaskId = outstandingExternalTaskId;
		return this;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(final String errorCode) {
		this.errorCode = errorCode;
	}

	public ErrandProcessEntity withErrorCode(final String errorCode) {
		this.errorCode = errorCode;
		return this;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(final String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public ErrandProcessEntity withErrorMessage(final String errorMessage) {
		this.errorMessage = errorMessage;
		return this;
	}

	public OffsetDateTime getStarted() {
		return started;
	}

	public void setStarted(final OffsetDateTime started) {
		this.started = started;
	}

	public ErrandProcessEntity withStarted(final OffsetDateTime started) {
		this.started = started;
		return this;
	}

	public OffsetDateTime getEnded() {
		return ended;
	}

	public Boolean getActiveMarker() {
		return activeMarker;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public ErrandProcessEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public ErrandProcessEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, municipalityId, namespace, processService, processKey, processInstanceId, processStatus, currentActivityId, currentActivityName, outstandingExternalTaskId, errorCode, errorMessage, started, ended, activeMarker,
			created, modified);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final ErrandProcessEntity other = (ErrandProcessEntity) obj;
		return Objects.equals(id, other.id)
			&& Objects.equals(errandId, other.errandId)
			&& Objects.equals(municipalityId, other.municipalityId)
			&& Objects.equals(namespace, other.namespace)
			&& Objects.equals(processService, other.processService)
			&& Objects.equals(processKey, other.processKey)
			&& Objects.equals(processInstanceId, other.processInstanceId)
			&& processStatus == other.processStatus
			&& Objects.equals(currentActivityId, other.currentActivityId)
			&& Objects.equals(currentActivityName, other.currentActivityName)
			&& Objects.equals(outstandingExternalTaskId, other.outstandingExternalTaskId)
			&& Objects.equals(errorCode, other.errorCode)
			&& Objects.equals(errorMessage, other.errorMessage)
			&& Objects.equals(started, other.started)
			&& Objects.equals(ended, other.ended)
			&& Objects.equals(activeMarker, other.activeMarker)
			&& Objects.equals(created, other.created)
			&& Objects.equals(modified, other.modified);
	}

	@Override
	public String toString() {
		return "ErrandProcessEntity{" +
			"id='" + id + '\'' +
			", errandId='" + errandId + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", processService='" + processService + '\'' +
			", processKey='" + processKey + '\'' +
			", processInstanceId='" + processInstanceId + '\'' +
			", processStatus=" + processStatus +
			", currentActivityId='" + currentActivityId + '\'' +
			", currentActivityName='" + currentActivityName + '\'' +
			", outstandingExternalTaskId='" + outstandingExternalTaskId + '\'' +
			", errorCode='" + errorCode + '\'' +
			", errorMessage='" + errorMessage + '\'' +
			", started=" + started +
			", ended=" + ended +
			", activeMarker=" + activeMarker +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
