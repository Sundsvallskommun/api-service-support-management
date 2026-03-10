package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "ExternalIdType model")
public class ExternalIdType {

	@Schema(description = "Name for the external id type", examples = "PRIVATE")
	@NotBlank
	private String name;

	@Schema(description = "Display name for the external id type", examples = "External id type name", types = {
		"string", "null"
	})
	private String displayName;

	@Schema(description = "Timestamp when the external id type was created", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the external id type was last modified", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null
	private OffsetDateTime modified;

	public static ExternalIdType create() {
		return new ExternalIdType();
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public ExternalIdType withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public ExternalIdType withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public ExternalIdType withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public ExternalIdType withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, displayName, modified, name);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final ExternalIdType other)) {
			return false;
		}
		return Objects.equals(created, other.created) && Objects.equals(displayName, other.displayName) && Objects.equals(modified, other.modified) && Objects.equals(name, other.name);
	}

	@Override
	public String toString() {
		return "ExternalIdType{" +
			"name='" + name + '\'' +
			", displayName='" + displayName + '\'' +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
