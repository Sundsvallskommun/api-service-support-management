package se.sundsvall.supportmanagement.api.model.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;

@Schema(description = "Resource a role may reach, and what it may do with it")
public class ResourceAccess {

	@NotNull
	@Schema(description = "Resource to grant access to", examples = "COMMUNICATION")
	private ProtectedResource resource;

	@NotNull
	@Schema(description = "Access level granted for the resource", examples = "R")
	private AccessLevel level;

	public static ResourceAccess create() {
		return new ResourceAccess();
	}

	public ProtectedResource getResource() {
		return resource;
	}

	public void setResource(final ProtectedResource resource) {
		this.resource = resource;
	}

	public ResourceAccess withResource(final ProtectedResource resource) {
		this.resource = resource;
		return this;
	}

	public AccessLevel getLevel() {
		return level;
	}

	public void setLevel(final AccessLevel level) {
		this.level = level;
	}

	public ResourceAccess withLevel(final AccessLevel level) {
		this.level = level;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(level, resource);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final ResourceAccess other)) { return false; }
		return level == other.level && resource == other.resource;
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("ResourceAccess [resource=").append(resource).append(", level=").append(level).append("]");
		return builder.toString();
	}
}
