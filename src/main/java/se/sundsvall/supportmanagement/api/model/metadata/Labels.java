package se.sundsvall.supportmanagement.api.model.metadata;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Schema(description = "Labels model", accessMode = READ_ONLY)
public class Labels {

	/**
	 * @deprecated
	 */
	@Deprecated(since = "11.0", forRemoval = true)
	@Schema(description = "Timestamp when the labels was created", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime created;

	/**
	 * @deprecated
	 */
	@Deprecated(since = "11.0", forRemoval = true)
	@Schema(description = "Timestamp when the labels was last modified", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime modified;

	@ArraySchema(schema = @Schema(implementation = Label.class))
	private List<Label> labelStructure;

	public static Labels create() {
		return new Labels();
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Labels withCreated(final OffsetDateTime created) {
		setCreated(created);
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Labels withModified(final OffsetDateTime modified) {
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
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Labels other)) {
			return false;
		}
		return Objects.equals(created, other.created) && Objects.equals(labelStructure, other.labelStructure) && Objects.equals(modified, other.modified);
	}

	@Override
	public String toString() {
		return "Labels [created=" + created + ", modified=" + modified + ", labelStructure=" + labelStructure + "]";
	}
}
