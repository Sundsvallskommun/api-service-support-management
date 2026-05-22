package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Category model")
public class Category {

	@Schema(description = "Category ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "Name for the category", examples = "Category name")
	@NotBlank(groups = OnCreate.class)
	private String name;

	@Schema(description = "Display name for the category", examples = "Displayed name")
	private String displayName;

	@Schema(description = "Sort order for the category", examples = "1", types = {
		"integer", "null"
	})
	private Integer sortOrder;

	@Schema(description = "", defaultValue = "false", examples = "true")
	private boolean deprecated = false;

	@ArraySchema(uniqueItems = true)
	@Valid
	private List<Type> types;

	@Schema(description = "Timestamp when the category was created", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the category was last modified", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null
	private OffsetDateTime modified;

	public static Category create() {
		return new Category();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Category withId(final String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Category withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public Category withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public Category withSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
		return this;
	}

	public boolean isDeprecated() {
		return deprecated;
	}

	public void setDeprecated(final boolean deprecated) {
		this.deprecated = deprecated;
	}

	public Category withDeprecated(final boolean deprecated) {
		this.deprecated = deprecated;
		return this;
	}

	public List<Type> getTypes() {
		return types;
	}

	public void setTypes(final List<Type> types) {
		this.types = types;
	}

	public Category withTypes(final List<Type> types) {
		this.types = types;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Category withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Category withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, deprecated, displayName, id, modified, name, sortOrder, types);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Category other = (Category) obj;
		return Objects.equals(created, other.created) && deprecated == other.deprecated && Objects.equals(displayName, other.displayName) && Objects.equals(id, other.id) && Objects.equals(modified, other.modified) && Objects.equals(name, other.name)
			&& Objects.equals(sortOrder,
				other.sortOrder) && Objects.equals(types, other.types);
	}

	@Override
	public String toString() {
		return "Category [id=" + id + ", name=" + name + ", displayName=" + displayName + ", sortOrder=" + sortOrder + ", deprecated=" + deprecated + ", types=" + types + ", created=" + created + ", modified=" + modified + "]";
	}
}
