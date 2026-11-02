package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * A signal a process instance waits for from a handler right now - a gate it can be stepped past by hand.
 * <p>
 * The rows of an instance are replaced by every report of the process, so no rows at all means the process waits for
 * no person. The name is the message name in the process model and is relayed back as it came; this service interprets
 * none of them.
 * <p>
 * The instance is referenced by id only, like the other process tables, and the foreign key lives in the database
 * alone.
 */
@Entity
@Table(name = "errand_process_signal",
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_eps_process_name", columnNames = {
			"errand_process_id", "name"
		})
	})
public class ErrandProcessSignalEntity {

	@Id
	@UuidGenerator
	@Column(name = "id", length = 36)
	private String id;

	@Column(name = "errand_process_id", nullable = false, length = 36)
	private String errandProcessId;

	/**
	 * The message name, compared byte for byte, trailing spaces included, as the process engine compares message names.
	 */
	@Column(name = "name", nullable = false, length = 128, columnDefinition = "varchar(128) character set utf8mb4 collate utf8mb4_nopad_bin")
	private String name;

	@Column(name = "label", length = 255)
	private String label;

	/** The position of the signal in the report, which is the order the handler is offered them in. */
	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(name = "created", nullable = false, columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	public static ErrandProcessSignalEntity create() {
		return new ErrandProcessSignalEntity();
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

	public ErrandProcessSignalEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getErrandProcessId() {
		return errandProcessId;
	}

	public void setErrandProcessId(final String errandProcessId) {
		this.errandProcessId = errandProcessId;
	}

	public ErrandProcessSignalEntity withErrandProcessId(final String errandProcessId) {
		this.errandProcessId = errandProcessId;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public ErrandProcessSignalEntity withName(final String name) {
		this.name = name;
		return this;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

	public ErrandProcessSignalEntity withLabel(final String label) {
		this.label = label;
		return this;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(final int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public ErrandProcessSignalEntity withSortOrder(final int sortOrder) {
		this.sortOrder = sortOrder;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public ErrandProcessSignalEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandProcessId, name, label, sortOrder, created);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final ErrandProcessSignalEntity other = (ErrandProcessSignalEntity) obj;
		return Objects.equals(id, other.id)
			&& Objects.equals(errandProcessId, other.errandProcessId)
			&& Objects.equals(name, other.name)
			&& Objects.equals(label, other.label)
			&& sortOrder == other.sortOrder
			&& Objects.equals(created, other.created);
	}

	@Override
	public String toString() {
		return "ErrandProcessSignalEntity{" +
			"id='" + id + '\'' +
			", errandProcessId='" + errandProcessId + '\'' +
			", name='" + name + '\'' +
			", label='" + label + '\'' +
			", sortOrder=" + sortOrder +
			", created=" + created +
			'}';
	}
}
