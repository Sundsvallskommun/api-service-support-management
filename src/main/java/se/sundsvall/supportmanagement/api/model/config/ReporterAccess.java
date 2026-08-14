package se.sundsvall.supportmanagement.api.model.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;

@Schema(description = "Access given to the reporter of an errand")
public class ReporterAccess {

	@Valid
	@Schema(description = "Resources the reporter may reach on their own errand, and what they may do with each. The access mapper knows nothing about reporters, so this is the only place their resources are granted")
	private List<ResourceAccess> resources;

	@Valid
	@Schema(description = "Fields of the errand exposed to its reporter. These widen whatever else applies, whether or not the namespace maps errands per role, since reporting an errand may never reduce what its reporter sees")
	private List<FieldAccess> fields;

	public static ReporterAccess create() {
		return new ReporterAccess();
	}

	public List<ResourceAccess> getResources() {
		return resources;
	}

	public void setResources(final List<ResourceAccess> resources) {
		this.resources = resources;
	}

	public ReporterAccess withResources(final List<ResourceAccess> resources) {
		this.resources = resources;
		return this;
	}

	public List<FieldAccess> getFields() {
		return fields;
	}

	public void setFields(final List<FieldAccess> fields) {
		this.fields = fields;
	}

	public ReporterAccess withFields(final List<FieldAccess> fields) {
		this.fields = fields;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(fields, resources);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final ReporterAccess other)) { return false; }
		return Objects.equals(fields, other.fields) && Objects.equals(resources, other.resources);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("ReporterAccess [resources=").append(resources).append(", fields=").append(fields).append("]");
		return builder.toString();
	}
}
