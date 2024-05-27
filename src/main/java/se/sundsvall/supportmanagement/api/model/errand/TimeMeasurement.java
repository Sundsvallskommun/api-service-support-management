package se.sundsvall.supportmanagement.api.model.errand;

import java.time.OffsetDateTime;
import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Time measure model", accessMode = Schema.AccessMode.READ_ONLY)
public class TimeMeasurement {

	@Schema(description = "Status for the time measurement period", example = "Started")
	private String status;

	@Schema(description = "Start time for the time measurement period", example = "2021-09-01T12:00:00Z")
	private OffsetDateTime startTime;

	@Schema(description = "Stop time for the time measurement period", example = "2021-09-01T13:00:00Z")
	private OffsetDateTime stopTime;

	@Schema(description = "Description of the time measurement period", example = "Suspected while waiting for answer from customer")
	private String description;

	@Schema(description = "Administrator for the time measurement period", example = "JO12DOE")
	private String administrator;


	public static TimeMeasurement create() {
		return new TimeMeasurement();
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public TimeMeasurement withStatus(final String status) {
		this.status = status;
		return this;
	}

	public OffsetDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(final OffsetDateTime startTime) {
		this.startTime = startTime;
	}

	public TimeMeasurement withStartTime(final OffsetDateTime startTime) {
		this.startTime = startTime;
		return this;
	}

	public OffsetDateTime getStopTime() {
		return stopTime;
	}

	public void setStopTime(final OffsetDateTime stopTime) {
		this.stopTime = stopTime;
	}

	public TimeMeasurement withStopTime(final OffsetDateTime stopTime) {
		this.stopTime = stopTime;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public TimeMeasurement withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getAdministrator() {
		return administrator;
	}

	public void setAdministrator(final String administrator) {
		this.administrator = administrator;
	}

	public TimeMeasurement withAdministrator(final String administrator) {
		this.administrator = administrator;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(status, startTime, stopTime, description, administrator);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final TimeMeasurement that = (TimeMeasurement) o;
		return Objects.equals(status, that.status) && Objects.equals(startTime, that.startTime) && Objects.equals(stopTime, that.stopTime) && Objects.equals(description, that.description) && Objects.equals(administrator, that.administrator);
	}

	@Override
	public String toString() {
		return "TimeMeasurement{" +
			"status='" + status + '\'' +
			", startTime=" + startTime +
			", stopTime=" + stopTime +
			", description='" + description + '\'' +
			", administrator='" + administrator + '\'' +
			'}';
	}

}
