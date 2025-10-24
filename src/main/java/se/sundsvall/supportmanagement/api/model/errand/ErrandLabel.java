package se.sundsvall.supportmanagement.api.model.errand;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

@Schema(description = "Errand label model")
public class ErrandLabel {

	@Schema(description = "Label ID", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7")
	@ValidUuid(nullable = true)
	private String id;

	@Schema(description = "Label classification", example = "subtype", accessMode = READ_ONLY)
	private String classification;

	@Schema(description = "Display name for the label", example = "Nyckelkort", accessMode = READ_ONLY)
	private String displayName;

	@Schema(description = "Resource path", example = "/parent/child/xxx", accessMode = READ_ONLY)
	private String resourcePath;

	@Schema(description = "Resource name", example = "keycard", accessMode = READ_ONLY)
	private String resourceName;

	public static ErrandLabel create() {
		return new ErrandLabel();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ErrandLabel withId(String id) {
		this.id = id;
		return this;
	}

	public String getClassification() {
		return classification;
	}

	public void setClassification(final String classification) {
		this.classification = classification;
	}

	public ErrandLabel withClassification(final String classification) {
		setClassification(classification);
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public ErrandLabel withDisplayName(final String displayName) {
		setDisplayName(displayName);
		return this;
	}

	public String getResourcePath() {
		return resourcePath;
	}

	public void setResourcePath(String resourcePath) {
		this.resourcePath = resourcePath;
	}

	public ErrandLabel withResourcePath(String resourcePath) {
		this.resourcePath = resourcePath;
		return this;
	}

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	public ErrandLabel withResourceName(String resourceName) {
		this.resourceName = resourceName;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(classification, displayName, id, resourceName, resourcePath);
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
		ErrandLabel other = (ErrandLabel) obj;
		return Objects.equals(classification, other.classification) && Objects.equals(displayName, other.displayName) && Objects.equals(id, other.id) && Objects.equals(resourceName, other.resourceName) && Objects.equals(resourcePath, other.resourcePath);
	}

	@Override
	public String toString() {
		return "ErrandLabel [id=" + id + ", classification=" + classification + ", displayName=" + displayName + ", resourcePath=" + resourcePath + ", resourceName=" + resourceName + "]";
	}
}
