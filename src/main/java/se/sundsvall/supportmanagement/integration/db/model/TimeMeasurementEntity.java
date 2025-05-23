package se.sundsvall.supportmanagement.integration.db.model;

import static jakarta.persistence.GenerationType.IDENTITY;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;

@Entity
@Table(name = "time_measurement")
public class TimeMeasurementEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	private Long id;

	@Column(name = "status")
	private String status;

	@Column(name = "start_time")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime startTime;

	@Column(name = "stop_time")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime stopTime;

	@Column(name = "description")
	private String description;

	@Column(name = "administrator")
	private String administrator;

	public static TimeMeasurementEntity create() {
		return new TimeMeasurementEntity();
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public TimeMeasurementEntity withId(final Long id) {
		this.id = id;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public TimeMeasurementEntity withStatus(final String status) {
		this.status = status;
		return this;
	}

	public OffsetDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(final OffsetDateTime startTime) {
		this.startTime = startTime;
	}

	public TimeMeasurementEntity withStartTime(final OffsetDateTime startTime) {
		this.startTime = startTime;
		return this;
	}

	public OffsetDateTime getStopTime() {
		return stopTime;
	}

	public void setStopTime(final OffsetDateTime stopTime) {
		this.stopTime = stopTime;
	}

	public TimeMeasurementEntity withStopTime(final OffsetDateTime stopTime) {
		this.stopTime = stopTime;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public TimeMeasurementEntity withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getAdministrator() {
		return administrator;
	}

	public void setAdministrator(final String administrator) {
		this.administrator = administrator;
	}

	public TimeMeasurementEntity withAdministrator(final String administrator) {
		this.administrator = administrator;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final TimeMeasurementEntity that = (TimeMeasurementEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(status, that.status) && Objects.equals(startTime, that.startTime) && Objects.equals(stopTime, that.stopTime) && Objects.equals(description, that.description) && Objects.equals(administrator,
			that.administrator);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, status, startTime, stopTime, description, administrator);
	}

	@Override
	public String toString() {
		return "TimeMeasurementEntity{" +
			"administrator='" + administrator + '\'' +
			", description='" + description + '\'' +
			", stopTime=" + stopTime +
			", startTime=" + startTime +
			", status='" + status + '\'' +
			", id=" + id +
			'}';
	}

}
