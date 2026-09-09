package se.sundsvall.supportmanagement.api.model.access;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;
import se.sundsvall.supportmanagement.api.model.config.AccessLevel;

@Schema(
	description = """
		What the requesting user may do with one errand, so that a client can render only the controls their next request would be allowed to make.

		'fields' describes what a patch of the errand itself accepts. The endpoints serving a resource of the errand on its own are additionally governed by 'resources', so a keyed field may be writable through one of the two and not the other. External tags have no resource of their own and are reached through the errand alone.""")
public class ErrandAccess {

	@Schema(description = "What the user may do with the errand itself. Reaching this endpoint at all means at least limited read", examples = "RW")
	private AccessLevel level;

	@Schema(
		description = "What the user may do with each field of the errand they reach. A field that is not listed is not shown to them at all. Fields that cannot be restricted, such as phases and actions, are never listed and are always readable")
	private List<ErrandFieldAccess> fields;

	@Schema(description = "What the user may do with each resource of the errand they reach. A resource that is not listed is not reachable by them. The errand itself is reported as 'level' rather than listed here")
	private List<ErrandResourceAccess> resources;

	public static ErrandAccess create() {
		return new ErrandAccess();
	}

	public AccessLevel getLevel() {
		return level;
	}

	public void setLevel(final AccessLevel level) {
		this.level = level;
	}

	public ErrandAccess withLevel(final AccessLevel level) {
		this.level = level;
		return this;
	}

	public List<ErrandFieldAccess> getFields() {
		return fields;
	}

	public void setFields(final List<ErrandFieldAccess> fields) {
		this.fields = fields;
	}

	public ErrandAccess withFields(final List<ErrandFieldAccess> fields) {
		this.fields = fields;
		return this;
	}

	public List<ErrandResourceAccess> getResources() {
		return resources;
	}

	public void setResources(final List<ErrandResourceAccess> resources) {
		this.resources = resources;
	}

	public ErrandAccess withResources(final List<ErrandResourceAccess> resources) {
		this.resources = resources;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(fields, level, resources);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final ErrandAccess other)) { return false; }
		return Objects.equals(fields, other.fields) && level == other.level && Objects.equals(resources, other.resources);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("ErrandAccess [level=").append(level).append(", fields=").append(fields).append(", resources=").append(resources).append("]");
		return builder.toString();
	}
}
