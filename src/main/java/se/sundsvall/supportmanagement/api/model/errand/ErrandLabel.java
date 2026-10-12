package se.sundsvall.supportmanagement.api.model.errand;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.supportmanagement.api.model.metadata.LabelAttribute;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Errand label model")
public class ErrandLabel {

	@Schema(description = "Label ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7")
	@ValidUuid(groups = {
		OnCreate.class, OnUpdate.class
	})
	private String id;

	@Schema(description = "Label version for optimistic concurrency control. When set, validated against the current version in DB on errand create/update — mismatch yields 412.")
	private Long version;

	@Schema(description = "Label classification", examples = "subtype", accessMode = READ_ONLY)
	private String classification;

	@Schema(description = "Display name for the label classification, as registered for the namespace", examples = "Undertyp", accessMode = READ_ONLY)
	private String classificationDisplayName;

	@Schema(description = "Display name for the label", examples = "Nyckelkort", accessMode = READ_ONLY)
	private String displayName;

	@Schema(description = "Resource path", examples = "/parent/child/xxx", accessMode = READ_ONLY)
	private String resourcePath;

	@Schema(description = "Resource name", examples = "keycard", accessMode = READ_ONLY)
	private String resourceName;

	@Schema(description = "Attributes of the label, as registered for the namespace. Changed through the label metadata, never through the errand", accessMode = READ_ONLY)
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<LabelAttribute> attributes;

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

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public ErrandLabel withVersion(Long version) {
		this.version = version;
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

	public String getClassificationDisplayName() {
		return classificationDisplayName;
	}

	public void setClassificationDisplayName(final String classificationDisplayName) {
		this.classificationDisplayName = classificationDisplayName;
	}

	public ErrandLabel withClassificationDisplayName(final String classificationDisplayName) {
		setClassificationDisplayName(classificationDisplayName);
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

	public List<LabelAttribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(final List<LabelAttribute> attributes) {
		this.attributes = attributes;
	}

	public ErrandLabel withAttributes(final List<LabelAttribute> attributes) {
		setAttributes(attributes);
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(attributes, classification, classificationDisplayName, displayName, id, resourceName, resourcePath, version);
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
		return Objects.equals(attributes, other.attributes) && Objects.equals(classification, other.classification) && Objects.equals(classificationDisplayName, other.classificationDisplayName) && Objects.equals(displayName, other.displayName)
			&& Objects.equals(id, other.id) && Objects.equals(resourceName, other.resourceName) && Objects.equals(resourcePath, other.resourcePath) && Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		return "ErrandLabel [id=" + id + ", version=" + version + ", classification=" + classification + ", classificationDisplayName=" + classificationDisplayName + ", displayName=" + displayName + ", resourcePath=" + resourcePath + ", resourceName="
			+ resourceName + ", attributes=" + attributes + "]";
	}
}
