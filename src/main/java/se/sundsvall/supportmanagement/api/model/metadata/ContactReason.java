package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Contact reason model")
public class ContactReason {

	@Schema(description = "ID", examples = "123", accessMode = READ_ONLY)
	private Long id;

	@Schema(description = "Reason for contact", examples = "Segt internet")
	@NotBlank
	private String reason;

	@Schema(description = "Timestamp when the contact reason was created", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the contact reason was last modified", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime modified;

	public static ContactReason create() {
		return new ContactReason();
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public ContactReason withId(final Long id) {
		this.id = id;
		return this;
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
	public int hashCode() {
		return Objects.hash(created, id, modified, reason);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final ContactReason other)) {
			return false;
		}
		return Objects.equals(created, other.created) && Objects.equals(id, other.id) && Objects.equals(modified, other.modified) && Objects.equals(reason, other.reason);
	}

	@Override
	public String toString() {
		return "ContactReason{" +
			"id=" + id +
			", reason='" + reason + '\'' +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
