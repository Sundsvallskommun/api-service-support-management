package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.groups.Default;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * Display name for a label classification. The classification is the key, and is matched against the classification of
 * the labels in the namespace, which is why it is demanded on creation only and never changed by an update.
 */
@Schema(description = "Label classification model")
public class LabelClassification {

	@Schema(description = "Label classification ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "Label classification. Used as key and matched against the classification of the labels. Ignored on update", examples = "subtype")
	@NotBlank
	private String classification;

	@Schema(description = "Display name for the label classification", examples = "Undertyp", types = {
		"string", "null"
	})
	private String displayName;

	@Schema(description = "Timestamp when the label classification was created", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null(groups = {
		Default.class, OnUpdate.class
	})
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the label classification was last modified", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null(groups = {
		Default.class, OnUpdate.class
	})
	private OffsetDateTime modified;

	public static LabelClassification create() {
		return new LabelClassification();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public LabelClassification withId(final String id) {
		this.id = id;
		return this;
	}

	public String getClassification() {
		return classification;
	}

	public void setClassification(final String classification) {
		this.classification = classification;
	}

	public LabelClassification withClassification(final String classification) {
		this.classification = classification;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public LabelClassification withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public LabelClassification withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public LabelClassification withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(classification, created, displayName, id, modified);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final LabelClassification other)) {
			return false;
		}
		return Objects.equals(classification, other.classification) && Objects.equals(created, other.created) && Objects.equals(displayName, other.displayName) && Objects.equals(id, other.id)
			&& Objects.equals(modified, other.modified);
	}

	@Override
	public String toString() {
		return "LabelClassification [id=" + id + ", classification=" + classification + ", displayName=" + displayName + ", created=" + created + ", modified=" + modified + "]";
	}
}
