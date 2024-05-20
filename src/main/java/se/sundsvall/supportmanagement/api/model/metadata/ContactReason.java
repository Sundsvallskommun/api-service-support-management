package se.sundsvall.supportmanagement.api.model.metadata;

import java.time.OffsetDateTime;
import java.util.Objects;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Contact reason model")
public class ContactReason {

	@Schema(description = "Reason for contact", example = "Segt internet")
	@NotBlank
	private String reason;

	@Schema(description = "Timestamp when the contact reason was created", example = "2000-10-31T01:30:00.000+02:00", accessMode = Schema.AccessMode.READ_ONLY)
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the contact reason was last modified", example = "2000-10-31T01:30:00.000+02:00", accessMode = Schema.AccessMode.READ_ONLY)
	private OffsetDateTime modified;

	public static ContactReason create() {
		return new ContactReason();
	}

	public String getReason() {
		return reason;
	}

	public void setReason(final String reason) {
		this.reason = reason;
	}

	public ContactReason withReason(final String reason) {
		this.reason = reason;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public ContactReason withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public ContactReason withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final ContactReason that = (ContactReason) o;
		return Objects.equals(reason, that.reason);
	}

	@Override
	public int hashCode() {
		return Objects.hash(reason);
	}

	@Override
	public String toString() {
		return "ContactReason{" +
			"reason='" + reason + '\'' +
			'}';
	}
}
