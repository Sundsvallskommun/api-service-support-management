package se.sundsvall.supportmanagement.api.model.access;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import se.sundsvall.supportmanagement.api.model.config.AccessLevel;

@Schema(description = "What the requesting user may do with one resource of an errand")
public class ErrandResourceAccess {

	@Schema(
		description = "Resource of the errand, named by the path access is granted on. A resource the user does not reach at all is not listed",
		examples = "errand/communication")
	private String resource;

	@Schema(description = "What the user may do with the resource", examples = "R")
	private AccessLevel level;

	public static ErrandResourceAccess create() {
		return new ErrandResourceAccess();
	}

	public String getResource() {
		return resource;
	}

	public void setResource(final String resource) {
		this.resource = resource;
	}

	public ErrandResourceAccess withResource(final String resource) {
		this.resource = resource;
		return this;
	}

	public AccessLevel getLevel() {
		return level;
	}

	public void setLevel(final AccessLevel level) {
		this.level = level;
	}

	public ErrandResourceAccess withLevel(final AccessLevel level) {
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
		if (!(obj instanceof final ErrandResourceAccess other)) { return false; }
		return level == other.level && Objects.equals(resource, other.resource);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("ErrandResourceAccess [resource=").append(resource).append(", level=").append(level).append("]");
		return builder.toString();
	}
}
