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

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * The state of a process instance attached to an errand.
 * <p>
 * The errand is referenced by id only, and the foreign key lives in the database alone. A JPA relation would put the
 * process into the errand aggregate, and a revision snapshot copies the whole errand - every report from a work step
 * would then produce a revision that says nothing about the errand.
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

	/** What a live instance carries in its marker. The value is irrelevant; that it is not null is the whole point. */
	private static final byte ALIVE = 1;

	@Id
	@UuidGenerator
	@Column(name = "id", length = 36)
	private String id;

	/** Length follows errand.id, which is varchar(255) rather than a plain uuid column. */
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

	/** Null while the start never succeeded, which is why the row can exist without one. */
	@Column(name = "process_instance_id", length = 64)
	private String processInstanceId;

	@Enumerated(EnumType.STRING)
	@Column(name = "process_status", nullable = false, length = 32)
	private ProcessStatus processStatus;

	@Column(name = "current_activity_id", length = 255)
	private String currentActivityId;

	@Column(name = "current_activity_name", length = 255)
	private String currentActivityName;

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
	 * Set while the instance lives, null once it is terminal. Null is distinct in a unique index, so
	 * {@code uq_ep_one_active_per_errand} lets an errand keep any number of finished instances but only one live one.
	 * Owned by {@link #applyStatus(ProcessStatus, Clock)}.
	 */
	@Column(name = "active_marker")
	private Byte activeMarker;

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
	 * The only way to set the status, and therefore the only place where the active marker and the end time can be told
	 * what the status means. Set past this method the marker is left behind, and the database rule of one live instance
	 * per errand is lost with it.
	 * <p>
	 * A status that is alive again clears the end time, which is what happens when an incident is resolved by hand and a
	 * FAILED instance resumes. It also gives the marker back, so the instance can find the place taken by another one
	 * started meanwhile - the unique constraint says so, and the caller has to tell the handler which instance is in the
	 * way.
	 *
	 * @param status the state the process is in.
	 * @param clock  the clock the end time is read from.
	 */
	public void applyStatus(final ProcessStatus status, final Clock clock) {
		final var terminal = status.isTerminal();

		this.processStatus = status;
		this.activeMarker = terminal ? null : ALIVE;
		this.ended = terminal ? now(clock).truncatedTo(MILLIS) : null;
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

	public Byte getActiveMarker() {
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
		return Objects.hash(id, errandId, municipalityId, namespace, processService, processKey, processInstanceId, processStatus, currentActivityId, currentActivityName, errorCode, errorMessage, started, ended, activeMarker, created, modified);
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
