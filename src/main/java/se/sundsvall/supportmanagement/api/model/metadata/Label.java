package se.sundsvall.supportmanagement.api.model.metadata;

import java.util.List;
import java.util.Objects;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Label model")
public class Label {

	@Schema(description = "Label classification", example = "subtype")
	@NotBlank
	private String classification;

	@Schema(description = "Display name for the label", example = "Nyckelkort")
	private String displayName;

	@Schema(description = "Name for the label", example = "keycard")
	@NotBlank
	private String name;

	@ArraySchema(schema = @Schema(implementation = Label.class, $anchor = "Label"))
	private List<Label> labels;

	public static Label create() {
		return new Label();
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
		return Objects.hash(classification, displayName, labels, name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Label)) {
			return false;
		}
		Label other = (Label) obj;
		return Objects.equals(classification, other.classification) && Objects.equals(displayName, other.displayName) && Objects.equals(labels, other.labels) && Objects.equals(name, other.name);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Label [classification=").append(classification).append(", displayName=").append(displayName).append(", name=").append(name).append(", labels=").append(labels).append("]");
		return builder.toString();
	}
}
