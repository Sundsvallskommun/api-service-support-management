package se.sundsvall.supportmanagement.api.model.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;
import se.sundsvall.supportmanagement.integration.db.model.enums.EntityType;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Validation model")
public class Validation {

	@Schema(description = "Type of metadata that the validation applies to", accessMode = READ_ONLY, examples = "STATUS")
	private EntityType type;

	@NotNull
	@Schema(description = "Signals if values of the type are validated against the metadata of the namespace when errands are created or updated", examples = "true")
	private Boolean validated;

	@Schema(description = "Timestamp when the validation was created", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the validation was last modified", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime modified;

	public static Validation create() {
		return new Validation();
	}

	public EntityType getType() {
		return type;
	}

	public void setType(final EntityType type) {
		this.type = type;
	}

	public Validation withType(final EntityType type) {
		this.type = type;
		return this;
	}

	public Boolean getValidated() {
		return validated;
	}

	public void setValidated(final Boolean validated) {
		this.validated = validated;
	}

	public Validation withValidated(final Boolean validated) {
		this.validated = validated;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Validation withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Validation withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, modified, type, validated);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final Validation other)) { return false; }
		return Objects.equals(created, other.created) && Objects.equals(modified, other.modified) && type == other.type && Objects.equals(validated, other.validated);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("Validation [type=").append(type).append(", validated=").append(validated).append(", created=").append(created).append(", modified=").append(modified).append("]");
		return builder.toString();
	}
}
