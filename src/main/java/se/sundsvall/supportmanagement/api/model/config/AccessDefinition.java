package se.sundsvall.supportmanagement.api.model.config;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(
	description = """
		The values the access configuration of a namespace accepts.

		Published as data rather than as an enum of this API, so that exposing a new field or guarding a new resource does not alter the contract. A client configuring access reads the values from here instead of from the schema.""")
public class AccessDefinition {

	@Schema(description = "Fields access may be configured for")
	private List<AccessFieldDefinition> fields;

	@Schema(description = "Resources access may be configured for")
	private List<AccessResourceDefinition> resources;

	public static AccessDefinition create() {
		return new AccessDefinition();
	}

	public List<AccessFieldDefinition> getFields() {
		return fields;
	}

	public void setFields(final List<AccessFieldDefinition> fields) {
		this.fields = fields;
	}

	public AccessDefinition withFields(final List<AccessFieldDefinition> fields) {
		this.fields = fields;
		return this;
	}

	public List<AccessResourceDefinition> getResources() {
		return resources;
	}

	public void setResources(final List<AccessResourceDefinition> resources) {
		this.resources = resources;
	}

	public AccessDefinition withResources(final List<AccessResourceDefinition> resources) {
		this.resources = resources;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(fields, resources);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final AccessDefinition other)) { return false; }
		return Objects.equals(fields, other.fields) && Objects.equals(resources, other.resources);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("AccessDefinition [fields=").append(fields).append(", resources=").append(resources).append("]");
		return builder.toString();
	}
}
