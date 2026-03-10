package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Status model")
public class Status {

	@Schema(description = "Name for the status", examples = "statusName")
	@NotBlank
	private String name;

	@Schema(description = "Display name for the status", examples = "Status name", types = {
		"string", "null"
	})
	private String displayName;

	@Schema(description = "External display name for the status", examples = "External status name", types = {
		"string", "null"
	})
	private String externalDisplayName;

	@Schema(description = "Timestamp when the status was created", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the status was last modified", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null
	private OffsetDateTime modified;

	public static Status create() {
		return new Status();
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Status withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public Status withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getExternalDisplayName() {
		return externalDisplayName;
	}

	public void setExternalDisplayName(final String externalDisplayName) {
		this.externalDisplayName = externalDisplayName;
	}

	public Status withExternalDisplayName(final String externalDisplayName) {
		this.externalDisplayName = externalDisplayName;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Status withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Status withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, displayName, externalDisplayName, modified, name);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Status other)) {
			return false;
		}
		return Objects.equals(created, other.created) && Objects.equals(displayName, other.displayName) && Objects.equals(externalDisplayName, other.externalDisplayName) && Objects.equals(modified, other.modified) && Objects.equals(name, other.name);
	}

	@Override
	public String toString() {
		return "Status{" +
			"name='" + name + '\'' +
			", displayName='" + displayName + '\'' +
			", externalDisplayName='" + externalDisplayName + '\'' +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
