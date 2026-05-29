package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Role model")
public class Role {

	@Schema(description = "Role ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "Name for the role. Used as key", examples = "roleName")
	@NotBlank
	private String name;

	@Schema(description = "Display name for the role", examples = "Role name", types = {
		"string", "null"
	})
	private String displayName;

	@Schema(description = "Sort order for the role", examples = "1", types = {
		"integer", "null"
	})
	private Integer sortOrder;

	@Schema(description = "Indicates if the role is deprecated", defaultValue = "false", examples = "true")
	private Boolean deprecated;

	@Schema(description = "Timestamp when the role was created", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the role was last modified", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null
	private OffsetDateTime modified;

	public static Role create() {
		return new Role();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Role withId(final String id) {
		this.id = id;
		return this;
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

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public Role withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public Role withSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
		return this;
	}

	public Boolean getDeprecated() {
		return deprecated;
	}

	public void setDeprecated(final Boolean deprecated) {
		this.deprecated = deprecated;
	}

	public Role withDeprecated(final Boolean deprecated) {
		this.deprecated = deprecated;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, deprecated, modified, id, name, displayName, sortOrder);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Role other)) {
			return false;
		}
		return Objects.equals(created, other.created) && Objects.equals(deprecated, other.deprecated) && Objects.equals(modified, other.modified) && Objects.equals(id, other.id) && Objects.equals(name, other.name) && Objects.equals(displayName,
			other.displayName)
			&& Objects.equals(sortOrder,
				other.sortOrder);
	}

	@Override
	public String toString() {
		return "Role{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", displayName='" + displayName + '\'' +
			", sortOrder=" + sortOrder +
			", deprecated=" + deprecated +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
