package se.sundsvall.supportmanagement.api.model.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Objects;

@Schema(description = "Restricts the errand to a set of fields for holders of one role")
public class RoleFieldRestriction {

	@NotBlank
	@Schema(description = "Role the restriction applies to, as supplied by the access mapper", examples = "FIRST_LINE_CASE_OFFICER")
	private String role;

	@Valid
	@Schema(description = "Fields the errand is restricted to. Applies to errands the user has read or read/write for, an errand they only have limited read for uses limitedReadAccess instead")
	private List<FieldAccess> fields;

	public static RoleFieldRestriction create() {
		return new RoleFieldRestriction();
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public RoleFieldRestriction withRole(final String role) {
		this.role = role;
		return this;
	}

	public List<FieldAccess> getFields() {
		return fields;
	}

	public void setFields(final List<FieldAccess> fields) {
		this.fields = fields;
	}

	public RoleFieldRestriction withFields(final List<FieldAccess> fields) {
		this.fields = fields;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(fields, role);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final RoleFieldRestriction other)) { return false; }
		return Objects.equals(fields, other.fields) && Objects.equals(role, other.role);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("RoleFieldRestriction [role=").append(role).append(", fields=").append(fields).append("]");
		return builder.toString();
	}
}
