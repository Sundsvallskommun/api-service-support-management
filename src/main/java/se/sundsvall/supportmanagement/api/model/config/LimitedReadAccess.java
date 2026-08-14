package se.sundsvall.supportmanagement.api.model.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;

@Schema(description = "What limited read means within the namespace")
public class LimitedReadAccess {

	@Schema(
		description = "Resources reachable on an errand the labels of the user only grant them limited read for. The errand itself is always reachable, listing resources here extends limited read beyond it. Read only, so no access level is given per resource",
		examples = "[\"COMMUNICATION\"]")
	private List<ProtectedResource> resources;

	@Valid
	@Schema(description = "Fields of the errand exposed, applying instead of the fields of any role the user holds, when role based mapping is active")
	private List<FieldAccess> fields;

	public static LimitedReadAccess create() {
		return new LimitedReadAccess();
	}

	public List<ProtectedResource> getResources() {
		return resources;
	}

	public void setResources(final List<ProtectedResource> resources) {
		this.resources = resources;
	}

	public LimitedReadAccess withResources(final List<ProtectedResource> resources) {
		this.resources = resources;
		return this;
	}

	public List<FieldAccess> getFields() {
		return fields;
	}

	public void setFields(final List<FieldAccess> fields) {
		this.fields = fields;
	}

	public LimitedReadAccess withFields(final List<FieldAccess> fields) {
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
		if (!(obj instanceof final LimitedReadAccess other)) { return false; }
		return Objects.equals(fields, other.fields) && Objects.equals(resources, other.resources);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("LimitedReadAccess [resources=").append(resources).append(", fields=").append(fields).append("]");
		return builder.toString();
	}
}
