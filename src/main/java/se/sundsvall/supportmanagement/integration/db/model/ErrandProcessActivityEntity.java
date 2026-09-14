package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Objects.isNull;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.INFO;

/**
 * Append-only log of what the process did, and of what kept it from doing anything.
 * <p>
 * Process agnostic on purpose: the type and the identifier of an activity are free strings that SM stores without
 * interpreting them, so a new process engine needs no change here.
 */
@Entity
@Table(name = "errand_process_activity",
	indexes = {
		@Index(name = "idx_epa_process_occurred", columnList = "errand_process_id, occurred_at"),
		@Index(name = "idx_epa_errand_occurred", columnList = "errand_id, occurred_at"),
		@Index(name = "idx_epa_retention", columnList = "created")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_epa_idempotency", columnNames = {
			"errand_process_id", "external_task_id", "activity_id"
		})
	})
public class ErrandProcessActivityEntity {

	/**
	 * Public and used by the annotation below, so that a writer building a message out of values it does not control can
	 * cut it to fit rather than have the insert refused. An entry that reports a fault may not cause one.
	 */
	public static final int MESSAGE_LENGTH = 2048;

	@Id
	@UuidGenerator
	@Column(name = "id", length = 36)
	private String id;

	/**
	 * Null for the entries written when no process instance exists - an ambiguous label, or the emergency brake. Those
	 * are precisely the entries that explain why no process started, so they cannot be made to depend on one.
	 */
	@Column(name = "errand_process_id", length = 36)
	private String errandProcessId;

	@Column(name = "errand_id", nullable = false, length = 255)
	private String errandId;

	@Column(name = "external_task_id", length = 64)
	private String externalTaskId;

	@Column(name = "activity_type", nullable = false, length = 64)
	private String activityType;

	@Column(name = "activity_id", length = 255)
	private String activityId;

	@Column(name = "activity_name", length = 255)
	private String activityName;

	@Enumerated(EnumType.STRING)
	@Column(name = "severity", nullable = false, length = 16)
	private ActivitySeverity severity;

	@Column(name = "message", length = MESSAGE_LENGTH)
	private String message;

	@Column(name = "error_code", length = 64)
	private String errorCode;

	/** The clock of the process, as opposed to {@link #created}, which is the clock of SM. */
	@Column(name = "occurred_at", nullable = false, columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime occurredAt;

	@Column(name = "created", nullable = false, columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	public static ErrandProcessActivityEntity create() {
		return new ErrandProcessActivityEntity();
	}

	@PrePersist
	void onCreate() {
		created = now(systemDefault()).truncatedTo(MILLIS);

		// The column default never fires, since Hibernate writes every mapped column on insert.
		if (isNull(severity)) {
			severity = INFO;
		}
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ErrandProcessActivityEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getErrandProcessId() {
		return errandProcessId;
	}

	public void setErrandProcessId(final String errandProcessId) {
		this.errandProcessId = errandProcessId;
	}

	public ErrandProcessActivityEntity withErrandProcessId(final String errandProcessId) {
		this.errandProcessId = errandProcessId;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public ErrandProcessActivityEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getExternalTaskId() {
		return externalTaskId;
	}

	public void setExternalTaskId(final String externalTaskId) {
		this.externalTaskId = externalTaskId;
	}

	public ErrandProcessActivityEntity withExternalTaskId(final String externalTaskId) {
		this.externalTaskId = externalTaskId;
		return this;
	}

	public String getActivityType() {
		return activityType;
	}

	public void setActivityType(final String activityType) {
		this.activityType = activityType;
	}

	public ErrandProcessActivityEntity withActivityType(final String activityType) {
		this.activityType = activityType;
		return this;
	}

	public String getActivityId() {
		return activityId;
	}

	public void setActivityId(final String activityId) {
		this.activityId = activityId;
	}

	public ErrandProcessActivityEntity withActivityId(final String activityId) {
		this.activityId = activityId;
		return this;
	}

	public String getActivityName() {
		return activityName;
	}

	public void setActivityName(final String activityName) {
		this.activityName = activityName;
	}

	public ErrandProcessActivityEntity withActivityName(final String activityName) {
		this.activityName = activityName;
		return this;
	}

	public ActivitySeverity getSeverity() {
		return severity;
	}

	public void setSeverity(final ActivitySeverity severity) {
		this.severity = severity;
	}

	public ErrandProcessActivityEntity withSeverity(final ActivitySeverity severity) {
		this.severity = severity;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public ErrandProcessActivityEntity withMessage(final String message) {
		this.message = message;
		return this;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(final String errorCode) {
		this.errorCode = errorCode;
	}

	public ErrandProcessActivityEntity withErrorCode(final String errorCode) {
		this.errorCode = errorCode;
		return this;
	}

	public OffsetDateTime getOccurredAt() {
		return occurredAt;
	}

	public void setOccurredAt(final OffsetDateTime occurredAt) {
		this.occurredAt = occurredAt;
	}

	public ErrandProcessActivityEntity withOccurredAt(final OffsetDateTime occurredAt) {
		this.occurredAt = occurredAt;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public ErrandProcessActivityEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandProcessId, errandId, externalTaskId, activityType, activityId, activityName, severity, message, errorCode, occurredAt, created);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final ErrandProcessActivityEntity other = (ErrandProcessActivityEntity) obj;
		return Objects.equals(id, other.id)
			&& Objects.equals(errandProcessId, other.errandProcessId)
			&& Objects.equals(errandId, other.errandId)
			&& Objects.equals(externalTaskId, other.externalTaskId)
			&& Objects.equals(activityType, other.activityType)
			&& Objects.equals(activityId, other.activityId)
			&& Objects.equals(activityName, other.activityName)
			&& severity == other.severity
			&& Objects.equals(message, other.message)
			&& Objects.equals(errorCode, other.errorCode)
			&& Objects.equals(occurredAt, other.occurredAt)
			&& Objects.equals(created, other.created);
	}

	@Override
	public String toString() {
		return "ErrandProcessActivityEntity{" +
			"id='" + id + '\'' +
			", errandProcessId='" + errandProcessId + '\'' +
			", errandId='" + errandId + '\'' +
			", externalTaskId='" + externalTaskId + '\'' +
			", activityType='" + activityType + '\'' +
			", activityId='" + activityId + '\'' +
			", activityName='" + activityName + '\'' +
			", severity=" + severity +
			", message='" + message + '\'' +
			", errorCode='" + errorCode + '\'' +
			", occurredAt=" + occurredAt +
			", created=" + created +
			'}';
	}
}
