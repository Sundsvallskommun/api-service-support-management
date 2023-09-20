package se.sundsvall.supportmanagement.api.model.metadata;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Null;

@Schema(description = "Labels model")
public class Labels {

	@Schema(description = "Timestamp when the external id type was created", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the external id type was last modified", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null
	private OffsetDateTime modified;

	@ArraySchema(schema = @Schema(implementation = Label.class))
	private List<Label> labelStructure;

	public static Labels create() {
		return new Labels();
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public Labels withCreated(OffsetDateTime created) {
		setCreated(created);
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public Labels withModified(OffsetDateTime modified) {
		setModified(modified);
		return this;
	}

	public List<Label> getLabelStructure() {
		return labelStructure;
	}

	public void setLabelStructure(final List<Label> labelStructure) {
		this.labelStructure = labelStructure;
	}

	public Labels withLabelStructure(final List<Label> labelStructure) {
		setLabelStructure(labelStructure);
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, labelStructure, modified);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Labels)) {
			return false;
		}
		Labels other = (Labels) obj;
		return Objects.equals(created, other.created) && Objects.equals(labelStructure, other.labelStructure) && Objects.equals(modified, other.modified);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Labels [created=").append(created).append(", modified=").append(modified).append(", labelStructure=").append(labelStructure).append("]");
		return builder.toString();
	}
}
