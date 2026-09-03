package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.groups.Default;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "MeasureType model")
public class MeasureType {

	@Schema(description = "MeasureType ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "Immutable key referenced by Measure.type. Required when creating. May be omitted or repeated, but not changed, when updating. Change displayName to rename the label", examples = "INTERVENTION")
	@NotBlank(groups = {
		Default.class, OnCreate.class
	})
	private String name;

	@Schema(description = "Display name for the measure type", examples = "Intervention", types = {
		"string", "null"
	})
	private String displayName;

	@Schema(description = "Group that this measure type belongs to. Required when creating", examples = "MANAGERS")
	@NotBlank(groups = {
		Default.class, OnCreate.class
	})
	private String measureGroup;

	@Schema(description = "Sort order for the measure type", examples = "1", types = {
		"integer", "null"
	})
	private Integer sortOrder;

	@Schema(description = "Indicates if the measure type is deprecated", defaultValue = "false", examples = "true")
	private Boolean deprecated;

	@Schema(description = "Timestamp when the measure type was created", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null(groups = {
		Default.class, OnUpdate.class
	})
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the measure type was last modified", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null(groups = {
		Default.class, OnUpdate.class
	})
	private OffsetDateTime modified;

	public static MeasureType create() {
		return new MeasureType();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public MeasureType withId(final String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public MeasureType withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public MeasureType withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getMeasureGroup() {
		return measureGroup;
	}

	public void setMeasureGroup(final String measureGroup) {
		this.measureGroup = measureGroup;
	}

	public MeasureType withMeasureGroup(final String measureGroup) {
		this.measureGroup = measureGroup;
		return this;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public MeasureType withSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
		return this;
	}

	public Boolean getDeprecated() {
		return deprecated;
	}

	public void setDeprecated(final Boolean deprecated) {
		this.deprecated = deprecated;
	}

	public MeasureType withDeprecated(final Boolean deprecated) {
		this.deprecated = deprecated;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public MeasureType withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public MeasureType withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, deprecated, id, measureGroup, modified, name, displayName, sortOrder);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final MeasureType other)) {
			return false;
		}
		return Objects.equals(created, other.created) && Objects.equals(deprecated, other.deprecated) && Objects.equals(id, other.id) && Objects.equals(measureGroup, other.measureGroup) && Objects.equals(modified, other.modified)
			&& Objects.equals(name, other.name) && Objects.equals(displayName, other.displayName) && Objects.equals(sortOrder, other.sortOrder);
	}

	@Override
	public String toString() {
		return "MeasureType{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", displayName='" + displayName + '\'' +
			", measureGroup='" + measureGroup + '\'' +
			", sortOrder=" + sortOrder +
			", deprecated=" + deprecated +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
