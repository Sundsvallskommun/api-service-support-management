package se.sundsvall.supportmanagement.api.model.errand;

import java.time.OffsetDateTime;
import java.util.Objects;

import jakarta.validation.constraints.FutureOrPresent;

import org.springframework.format.annotation.DateTimeFormat;

import se.sundsvall.supportmanagement.api.validation.ValidSuspend;

import io.swagger.v3.oas.annotations.media.Schema;

@ValidSuspend
public class Suspend {

	@Schema(description = "Timestamp when the suspension wears off", example = "2000-10-31T01:30:00.000+02:00")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@FutureOrPresent
	private OffsetDateTime suspendedTo;

	@Schema(description = "Timestamp when the suspension started", example = "2000-10-31T01:30:00.000+02:00")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private OffsetDateTime suspendedFrom;

	public static Suspend create() {
		return new Suspend();
	}

	public OffsetDateTime getSuspendedTo() {
		return suspendedTo;
	}

	public void setSuspendedTo(final OffsetDateTime suspendedTo) {
		this.suspendedTo = suspendedTo;
	}

	public Suspend withSuspendedTo(final OffsetDateTime suspendedTo) {
		this.suspendedTo = suspendedTo;
		return this;
	}

	public OffsetDateTime getSuspendedFrom() {
		return suspendedFrom;
	}

	public void setSuspendedFrom(final OffsetDateTime suspendedFrom) {
		this.suspendedFrom = suspendedFrom;
	}

	public Suspend withSuspendedFrom(final OffsetDateTime suspendedFrom) {
		this.suspendedFrom = suspendedFrom;
		return this;
	}

	@Override
	public String toString() {
		return "Suspend{" +
			"suspendedTo=" + suspendedTo +
			", suspendedFrom=" + suspendedFrom +
			'}';
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final Suspend suspend = (Suspend) o;
		return Objects.equals(suspendedTo, suspend.suspendedTo) && Objects.equals(suspendedFrom, suspend.suspendedFrom);
	}

	@Override
	public int hashCode() {
		return Objects.hash(suspendedTo, suspendedFrom);
	}
}
