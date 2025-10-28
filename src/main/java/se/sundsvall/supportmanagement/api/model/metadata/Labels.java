package se.sundsvall.supportmanagement.api.model.metadata;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Labels model", accessMode = READ_ONLY)
public class Labels {

	@ArraySchema(schema = @Schema(implementation = Label.class))
	private List<Label> labelStructure;

	public static Labels create() {
		return new Labels();
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
		return Objects.hash(labelStructure);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Labels other)) {
			return false;
		}
		return Objects.equals(labelStructure, other.labelStructure);
	}

	@Override
	public String toString() {
		return "Labels [labelStructure=" + labelStructure + "]";
	}
}
