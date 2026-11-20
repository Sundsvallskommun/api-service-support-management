package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * A resolution code an errand may be closed with, registered per namespace. The name is the key that the resolution of
 * an errand holds. It cannot be changed, and a code in use on errands can only be deprecated, not deleted.
 */
@Schema(description = "ResolutionCode model")
public class ResolutionCode {

	@Schema(description = "ResolutionCode ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "Name for the resolution code. Used as key, unique within the namespace, and cannot be changed once created", examples = "SOLVED")
	@NotBlank(groups = OnCreate.class)
	private String name;

	@Schema(description = "Display name for the resolution code", examples = "Löst", types = {
		"string", "null"
	})
	private String displayName;

	@Schema(description = "Description of the resolution code", examples = "The errand has been solved", types = {
		"string", "null"
	})
	@Size(max = 1024, groups = {
		Default.class, OnCreate.class
	})
	private String description;

	@Schema(description = "Sort order for the resolution code", examples = "1", types = {
		"integer", "null"
	})
	private Integer sortOrder;

	@Schema(description = "Indicates if the resolution code is deprecated. A deprecated code is no longer accepted on errands", defaultValue = "false", examples = "true")
	private Boolean deprecated;

	@Schema(description = "Timestamp when the resolution code was created", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null(groups = {
		Default.class, OnCreate.class
	})
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the resolution code was last modified", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null(groups = {
		Default.class, OnCreate.class
	})
	private OffsetDateTime modified;

	public static ResolutionCode create() {
		return new ResolutionCode();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ResolutionCode withId(final String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public ResolutionCode withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public ResolutionCode withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public ResolutionCode withDescription(final String description) {
		this.description = description;
		return this;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public ResolutionCode withSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
		return this;
	}

	public Boolean getDeprecated() {
		return deprecated;
	}

	public void setDeprecated(final Boolean deprecated) {
		this.deprecated = deprecated;
	}

	public ResolutionCode withDeprecated(final Boolean deprecated) {
		this.deprecated = deprecated;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public ResolutionCode withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public ResolutionCode withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, displayName, description, sortOrder, deprecated, created, modified);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final ResolutionCode other)) {
			return false;
		}
		return Objects.equals(id, other.id)
			&& Objects.equals(name, other.name)
			&& Objects.equals(displayName, other.displayName)
			&& Objects.equals(description, other.description)
			&& Objects.equals(sortOrder, other.sortOrder)
			&& Objects.equals(deprecated, other.deprecated)
			&& Objects.equals(created, other.created)
			&& Objects.equals(modified, other.modified);
	}

	@Override
	public String toString() {
		return "ResolutionCode{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", displayName='" + displayName + '\'' +
			", description='" + description + '\'' +
			", sortOrder=" + sortOrder +
			", deprecated=" + deprecated +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
