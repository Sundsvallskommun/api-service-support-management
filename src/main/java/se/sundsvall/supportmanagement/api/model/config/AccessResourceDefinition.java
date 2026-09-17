package se.sundsvall.supportmanagement.api.model.config;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Resource that access may be configured for")
public class AccessResourceDefinition {

	@Schema(description = "Value to configure the resource with", examples = "COMMUNICATION")
	private String resource;

	@Schema(
		description = "Path the resource is guarded on. Access patterns of the access mapper are matched against it, and the access of an errand reports it",
		examples = "errand/communication")
	private String path;

	@Schema(description = "If the resource belongs to an errand rather than to the namespace itself", examples = "true")
	private boolean errandScoped;

	public static AccessResourceDefinition create() {
		return new AccessResourceDefinition();
	}

	public String getResource() {
		return resource;
	}

	public void setResource(final String resource) {
		this.resource = resource;
	}

	public AccessResourceDefinition withResource(final String resource) {
		this.resource = resource;
		return this;
	}

	public String getPath() {
		return path;
	}

	public void setPath(final String path) {
		this.path = path;
	}

	public AccessResourceDefinition withPath(final String path) {
		this.path = path;
		return this;
	}

	public boolean isErrandScoped() {
		return errandScoped;
	}

	public void setErrandScoped(final boolean errandScoped) {
		this.errandScoped = errandScoped;
	}

	public AccessResourceDefinition withErrandScoped(final boolean errandScoped) {
		this.errandScoped = errandScoped;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(errandScoped, path, resource);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final AccessResourceDefinition other)) { return false; }
		return errandScoped == other.errandScoped && Objects.equals(path, other.path) && Objects.equals(resource, other.resource);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("AccessResourceDefinition [resource=").append(resource).append(", path=").append(path).append(", errandScoped=").append(errandScoped).append("]");
		return builder.toString();
	}
}
