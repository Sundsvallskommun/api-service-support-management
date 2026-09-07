package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * One errand change on its way to a process engine, written in the same transaction as the change itself.
 * <p>
 * There is no relation to the errand, neither here nor in the database: a DELETE event has to outlive the errand it is
 * about. There is no retry bookkeeping either, since delivery and acknowledgement share a transaction - an undelivered
 * row is its own receipt that the work remains.
 */
@Entity
@Table(name = "process_event_outbox",
	indexes = {
		@Index(name = "idx_peo_dispatch", columnList = "delivered_at, created"),
		@Index(name = "idx_peo_consumer", columnList = "process_service, delivered_at, created"),
		@Index(name = "idx_peo_guard", columnList = "errand_id, delivered_at, created")
	})
public class ProcessEventOutboxEntity {

	@Id
	@UuidGenerator
	@Column(name = "id", length = 36)
	private String id;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "errand_id", nullable = false, length = 36)
	private String errandId;

	/**
	 * Where the row is headed, taken from the namespace configuration when the row is written. The relay never looks the
	 * configuration up again, so a consumer changed afterwards does not redirect rows already written.
	 */
	@Column(name = "process_service", nullable = false, length = 64)
	private String processService;

	/** Required for CREATE and UPDATE. A DELETE is published without it, since the process engine matches on the errand. */
	@Column(name = "process_key", length = 128)
	private String processKey;

	@Column(name = "event_type", nullable = false, length = 64)
	private String eventType;

	@Column(name = "event_sub_type", nullable = false, length = 64)
	private String eventSubType;

	/** Whether the event may start a new process instance. Worked out once, when the row is published. */
	@Column(name = "start_allowed", nullable = false)
	private boolean startAllowed;

	/** The message name from the BPMN model, carried only by rows with the SIGNAL sub type. */
	@Column(name = "signal_name", length = 128)
	private String signalName;

	/** Kept for traceability only - it is not what decides whether a row is written. */
	@Column(name = "executed_by", length = 255)
	private String executedBy;

	@Column(name = "request_group_id", length = 36)
	private String requestGroupId;

	@Column(name = "created", nullable = false, columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	/**
	 * Set instead of removing the row, because the emergency brake counts delivered rows in a time window and needs them
	 * around for a while.
	 */
	@Column(name = "delivered_at", columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime deliveredAt;

	public static ProcessEventOutboxEntity create() {
		return new ProcessEventOutboxEntity();
	}

	@PrePersist
	void onCreate() {
		created = now(systemDefault()).truncatedTo(MILLIS);
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ProcessEventOutboxEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public ProcessEventOutboxEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public ProcessEventOutboxEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public ProcessEventOutboxEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getProcessService() {
		return processService;
	}

	public void setProcessService(final String processService) {
		this.processService = processService;
	}

	public ProcessEventOutboxEntity withProcessService(final String processService) {
		this.processService = processService;
		return this;
	}

	public String getProcessKey() {
		return processKey;
	}

	public void setProcessKey(final String processKey) {
		this.processKey = processKey;
	}

	public ProcessEventOutboxEntity withProcessKey(final String processKey) {
		this.processKey = processKey;
		return this;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(final String eventType) {
		this.eventType = eventType;
	}

	public ProcessEventOutboxEntity withEventType(final String eventType) {
		this.eventType = eventType;
		return this;
	}

	public String getEventSubType() {
		return eventSubType;
	}

	public void setEventSubType(final String eventSubType) {
		this.eventSubType = eventSubType;
	}

	public ProcessEventOutboxEntity withEventSubType(final String eventSubType) {
		this.eventSubType = eventSubType;
		return this;
	}

	public boolean isStartAllowed() {
		return startAllowed;
	}

	public void setStartAllowed(final boolean startAllowed) {
		this.startAllowed = startAllowed;
	}

	public ProcessEventOutboxEntity withStartAllowed(final boolean startAllowed) {
		this.startAllowed = startAllowed;
		return this;
	}

	public String getSignalName() {
		return signalName;
	}

	public void setSignalName(final String signalName) {
		this.signalName = signalName;
	}

	public ProcessEventOutboxEntity withSignalName(final String signalName) {
		this.signalName = signalName;
		return this;
	}

	public String getExecutedBy() {
		return executedBy;
	}

	public void setExecutedBy(final String executedBy) {
		this.executedBy = executedBy;
	}

	public ProcessEventOutboxEntity withExecutedBy(final String executedBy) {
		this.executedBy = executedBy;
		return this;
	}

	public String getRequestGroupId() {
		return requestGroupId;
	}

	public void setRequestGroupId(final String requestGroupId) {
		this.requestGroupId = requestGroupId;
	}

	public ProcessEventOutboxEntity withRequestGroupId(final String requestGroupId) {
		this.requestGroupId = requestGroupId;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public ProcessEventOutboxEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getDeliveredAt() {
		return deliveredAt;
	}

	public void setDeliveredAt(final OffsetDateTime deliveredAt) {
		this.deliveredAt = deliveredAt;
	}

	public ProcessEventOutboxEntity withDeliveredAt(final OffsetDateTime deliveredAt) {
		this.deliveredAt = deliveredAt;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, municipalityId, namespace, errandId, processService, processKey, eventType, eventSubType, startAllowed, signalName, executedBy, requestGroupId, created, deliveredAt);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final ProcessEventOutboxEntity other = (ProcessEventOutboxEntity) obj;
		return startAllowed == other.startAllowed
			&& Objects.equals(id, other.id)
			&& Objects.equals(municipalityId, other.municipalityId)
			&& Objects.equals(namespace, other.namespace)
			&& Objects.equals(errandId, other.errandId)
			&& Objects.equals(processService, other.processService)
			&& Objects.equals(processKey, other.processKey)
			&& Objects.equals(eventType, other.eventType)
			&& Objects.equals(eventSubType, other.eventSubType)
			&& Objects.equals(signalName, other.signalName)
			&& Objects.equals(executedBy, other.executedBy)
			&& Objects.equals(requestGroupId, other.requestGroupId)
			&& Objects.equals(created, other.created)
			&& Objects.equals(deliveredAt, other.deliveredAt);
	}

	@Override
	public String toString() {
		return "ProcessEventOutboxEntity{" +
			"id='" + id + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", errandId='" + errandId + '\'' +
			", processService='" + processService + '\'' +
			", processKey='" + processKey + '\'' +
			", eventType='" + eventType + '\'' +
			", eventSubType='" + eventSubType + '\'' +
			", startAllowed=" + startAllowed +
			", signalName='" + signalName + '\'' +
			", executedBy='" + executedBy + '\'' +
			", requestGroupId='" + requestGroupId + '\'' +
			", created=" + created +
			", deliveredAt=" + deliveredAt +
			'}';
	}
}
