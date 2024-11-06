package se.sundsvall.supportmanagement.api.model.metadata;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import java.time.OffsetDateTime;
import java.util.Objects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Role model")
public class Role {

	@Schema(description = "Name for the role. Used as key", example = "roleName")
	@NotBlank
	private String name;

	@Schema(description = "Display name for the role", example = "Role name", nullable = true)
	private String displayName;

	@Schema(description = "Timestamp when the role was created", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the role was last modified", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null
	private OffsetDateTime modified;

	public static Role create() {
		return new Role();
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Role withName(final String name) {
		this.name = name;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Role withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Role withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Role withDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, modified, name, displayName);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Role other)) {
			return false;
		}
		return Objects.equals(created, other.created) && Objects.equals(modified, other.modified) && Objects.equals(name, other.name) && Objects.equals(displayName, other.displayName);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Role [name=").append(name)
			.append(", displayName=").append(displayName)
			.append(", created=").append(created)
			.append(", modified=").append(modified)
			.append("]");
		return builder.toString();
	}
}
