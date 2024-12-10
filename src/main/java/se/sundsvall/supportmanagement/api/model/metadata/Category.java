package se.sundsvall.supportmanagement.api.model.metadata;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

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

@Schema(description = "Category model")
public class Category {

	@Schema(description = "Name for the category", example = "categoryname")
	@NotBlank(groups = OnCreate.class)
	private String name;

	@Schema(description = "Displayname for the category", example = "Displayed name")
	private String displayName;

	@ArraySchema(schema = @Schema(implementation = Type.class), uniqueItems = true)
	@Valid
	private List<Type> types;

	@Schema(description = "Timestamp when the category was created", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the category was last modified", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null
	private OffsetDateTime modified;

	public static Category create() {
		return new Category();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Category withName(String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Category withDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}

	public List<Type> getTypes() {
		return types;
	}

	public void setTypes(List<Type> types) {
		this.types = types;
	}

	public Category withTypes(List<Type> types) {
		this.types = types;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public Category withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public Category withModified(OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, displayName, modified, name, types);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Category other = (Category) obj;
		return Objects.equals(created, other.created) && Objects.equals(displayName, other.displayName) && Objects.equals(modified, other.modified) && Objects.equals(name, other.name) && Objects.equals(types, other.types);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Category [name=").append(name).append(", displayName=").append(displayName).append(", types=").append(types).append(", created=").append(created).append(", modified=").append(modified).append("]");
		return builder.toString();
	}
}
