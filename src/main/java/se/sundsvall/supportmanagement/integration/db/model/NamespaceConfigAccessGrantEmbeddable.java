package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import se.sundsvall.supportmanagement.integration.db.model.enums.RoleAccessType;

import static java.sql.Types.LONGVARCHAR;

/**
 * A single grant within a namespace configuration. One row per grant, so a scope is extended by adding rows.
 * <p>
 * The scope is either a reserved {@link se.sundsvall.supportmanagement.integration.db.model.enums.AccessGrantScope},
 * resolved per errand by this service, or the name of a role supplied by the access mapper.
 * <p>
 * For a FIELD grant the value is an {@link se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField} name,
 * optionally followed by a colon and a key to expose a single entry of a keyed collection, e.g.
 * <code>PARAMETERS:contactChannel</code>. The value is split on the first colon only, so keys may themselves contain
 * colons. Access level is not used for FIELD grants.
 * <p>
 * For a RESOURCE grant the value is an
 * {@link se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource}
 * name and the access level states what the role may do with it.
 */
@Embeddable
public class NamespaceConfigAccessGrantEmbeddable {

	@Column(name = "`scope`", nullable = false)
	private String scope;

	@Column(name = "`type`", nullable = false)
	private RoleAccessType type;

	@Column(name = "`value`", nullable = false)
	@JdbcTypeCode(LONGVARCHAR)
	private String value;

	@Column(name = "access_level")
	private String accessLevel;

	public static NamespaceConfigAccessGrantEmbeddable create() {
		return new NamespaceConfigAccessGrantEmbeddable();
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public NamespaceConfigAccessGrantEmbeddable withScope(String scope) {
		this.scope = scope;
		return this;
	}

	public RoleAccessType getType() {
		return type;
	}

	public void setType(RoleAccessType type) {
		this.type = type;
	}

	public NamespaceConfigAccessGrantEmbeddable withType(RoleAccessType type) {
		this.type = type;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public NamespaceConfigAccessGrantEmbeddable withValue(String value) {
		this.value = value;
		return this;
	}

	public String getAccessLevel() {
		return accessLevel;
	}

	public void setAccessLevel(String accessLevel) {
		this.accessLevel = accessLevel;
	}

	public NamespaceConfigAccessGrantEmbeddable withAccessLevel(String accessLevel) {
		this.accessLevel = accessLevel;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(accessLevel, scope, type, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final NamespaceConfigAccessGrantEmbeddable other)) { return false; }
		return Objects.equals(accessLevel, other.accessLevel) && Objects.equals(scope, other.scope) && type == other.type && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("NamespaceConfigAccessGrantEmbeddable [scope=").append(scope).append(", type=").append(type).append(", value=").append(value).append(", accessLevel=").append(accessLevel).append("]");
		return builder.toString();
	}
}
