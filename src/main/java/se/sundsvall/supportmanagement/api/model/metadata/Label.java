package se.sundsvall.supportmanagement.api.model.metadata;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

@Schema(description = "Label model")
public class Label {

	@Schema(description = "Label ID", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7")
	@ValidUuid(nullable = true)
	private String id;

	@Schema(description = "Label classification", example = "subtype")
	@NotBlank
	private String classification;

	@Schema(description = "Display name for the label", example = "Nyckelkort")
	private String displayName;

	/**
	 * @deprecated
	 */
	@Schema(description = "Name for the label", example = "keyCard")
	@NotBlank
	@Deprecated(since = "10.9", forRemoval = true)
	private String name;

	@Schema(description = "Resource path", example = "/parent/child/xxx", accessMode = READ_ONLY)
	private String resourcePath;

	@Schema(description = "Resource name", example = "xxx")
	@NotBlank
	private String resourceName;

	@ArraySchema(schema = @Schema(implementation = Label.class, $anchor = "Label"))
	private List<Label> labels;

	public static Label create() {
		return new Label();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Label withId(String id) {
		this.id = id;
		return this;
	}

	public String getClassification() {
		return classification;
	}

	public void setClassification(final String classification) {
		this.classification = classification;
	}

	public Label withClassification(final String classification) {
		setClassification(classification);
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public Label withDisplayName(final String displayName) {
		setDisplayName(displayName);
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Label withName(final String name) {
		setName(name);
		return this;
	}

	public String getResourcePath() {
		return resourcePath;
	}

	public void setResourcePath(String resourcePath) {
		this.resourcePath = resourcePath;
	}

	public Label withResourcePath(String resourcePath) {
		this.resourcePath = resourcePath;
		return this;
	}

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	public Label withResourceName(String resourceName) {
		this.resourceName = resourceName;
		return this;
	}

	public List<Label> getLabels() {
		return labels;
	}

	public void setLabels(final List<Label> labels) {
		this.labels = labels;
	}

	public Label withLabels(final List<Label> labels) {
		setLabels(labels);
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(classification, displayName, id, labels, name, resourceName, resourcePath);
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
		Label other = (Label) obj;
		return Objects.equals(classification, other.classification) && Objects.equals(displayName, other.displayName) && Objects.equals(id, other.id) && Objects.equals(labels, other.labels) && Objects.equals(name, other.name) && Objects.equals(
			resourceName, other.resourceName) && Objects.equals(resourcePath, other.resourcePath);
	}

	@Override
	public String toString() {
		return "Label [id=" + id + ", classification=" + classification + ", displayName=" + displayName + ", name=" + name + ", resourcePath=" + resourcePath + ", resourceName=" + resourceName + ", labels=" + labels + "]";
	}
}
