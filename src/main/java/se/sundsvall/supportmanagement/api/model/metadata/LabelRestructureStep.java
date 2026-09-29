package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Objects;

/**
 * One change in a {@link LabelRestructureRequest}. Every path addresses a label by its chain of resourceName
 * segments from the root (e.g. {@code ["SOCIAL_SERVICES","ELDERLY_CARE","SPECIAL_HOUSING"]}) rather than by id, so
 * that a step can reference a label an earlier step in the same request just added or moved, without the caller
 * having to round-trip a generated id between steps.
 * <p>
 * Which of the fields below apply depends on {@link #type} - enforced by {@code @ValidRestructureStep} rather than
 * splitting this into five request classes, matching this codebase's existing flat-DTO style (see {@link Label},
 * {@link LabelMoveRequest}) rather than Jackson polymorphic subtypes.
 */
@Schema(description = "One step of a label-tree restructure")
public class LabelRestructureStep {

	/**
	 * Every path segment addresses one {@code resourceName}, so it is held to the same charset that constrains a
	 * persisted label's own {@code resourceName} ({@link Label#getResourceName()}) - in particular, excluding
	 * {@code /}, which {@link LabelTreeSnapshot#join(List)} would otherwise fold into the joined path indistinguishably
	 * from a genuine extra segment.
	 */
	private static final String SEGMENT_PATTERN = "[A-Z0-9_]+";
	private static final String SEGMENT_PATTERN_MESSAGE = "can only contain A-Z, 0-9 and _";

	@NotNull
	@Schema(description = "The kind of change this step makes", requiredMode = Schema.RequiredMode.REQUIRED)
	private LabelRestructureStepType type;

	@NotEmpty
	@ArraySchema(arraySchema = @Schema(
		description = "Path (resourceName segments from the root) of the label this step acts on. The label added, renamed or deleted for ADD/RENAME/DELETE; the label moved for MOVE; the merge destination for MERGE",
		requiredMode = Schema.RequiredMode.REQUIRED))
	private List<@NotBlank @Pattern(regexp = SEGMENT_PATTERN, message = SEGMENT_PATTERN_MESSAGE) String> path;

	@ArraySchema(arraySchema = @Schema(description = "MOVE only: path of the new parent, or null/absent to move to root"))
	private List<@NotBlank @Pattern(regexp = SEGMENT_PATTERN, message = SEGMENT_PATTERN_MESSAGE) String> destinationParentPath;

	@ArraySchema(arraySchema = @Schema(description = "MERGE only: paths of the source labels merged into path"))
	private List<@NotEmpty List<@NotBlank @Pattern(regexp = SEGMENT_PATTERN, message = SEGMENT_PATTERN_MESSAGE) String>> sourcePaths;

	@Schema(description = "ADD: display name of the new label. RENAME: the new display name. MOVE: an optional new display name to set at the same time")
	private String displayName;

	@Schema(description = "ADD only: classification of the new label", examples = "SUBTYPE")
	private String classification;

	@Schema(description = "MOVE only: an optional new resourceName to set at the same time (used for a combined move+rename)")
	@Pattern(regexp = SEGMENT_PATTERN, message = SEGMENT_PATTERN_MESSAGE)
	private String newResourceName;

	public static LabelRestructureStep create() {
		return new LabelRestructureStep();
	}

	public LabelRestructureStepType getType() {
		return type;
	}

	public void setType(final LabelRestructureStepType type) {
		this.type = type;
	}

	public LabelRestructureStep withType(final LabelRestructureStepType type) {
		this.type = type;
		return this;
	}

	public List<String> getPath() {
		return path;
	}

	public void setPath(final List<String> path) {
		this.path = path;
	}

	public LabelRestructureStep withPath(final List<String> path) {
		this.path = path;
		return this;
	}

	public List<String> getDestinationParentPath() {
		return destinationParentPath;
	}

	public void setDestinationParentPath(final List<String> destinationParentPath) {
		this.destinationParentPath = destinationParentPath;
	}

	public LabelRestructureStep withDestinationParentPath(final List<String> destinationParentPath) {
		this.destinationParentPath = destinationParentPath;
		return this;
	}

	public List<List<String>> getSourcePaths() {
		return sourcePaths;
	}

	public void setSourcePaths(final List<List<String>> sourcePaths) {
		this.sourcePaths = sourcePaths;
	}

	public LabelRestructureStep withSourcePaths(final List<List<String>> sourcePaths) {
		this.sourcePaths = sourcePaths;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public LabelRestructureStep withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getClassification() {
		return classification;
	}

	public void setClassification(final String classification) {
		this.classification = classification;
	}

	public LabelRestructureStep withClassification(final String classification) {
		this.classification = classification;
		return this;
	}

	public String getNewResourceName() {
		return newResourceName;
	}

	public void setNewResourceName(final String newResourceName) {
		this.newResourceName = newResourceName;
	}

	public LabelRestructureStep withNewResourceName(final String newResourceName) {
		this.newResourceName = newResourceName;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, path, destinationParentPath, sourcePaths, displayName, classification, newResourceName);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final LabelRestructureStep other)) {
			return false;
		}
		return type == other.type && Objects.equals(path, other.path) && Objects.equals(destinationParentPath, other.destinationParentPath) &&
			Objects.equals(sourcePaths, other.sourcePaths) && Objects.equals(displayName, other.displayName) &&
			Objects.equals(classification, other.classification) && Objects.equals(newResourceName, other.newResourceName);
	}

	@Override
	public String toString() {
		return "LabelRestructureStep[type=" + type + ", path=" + path + ", destinationParentPath=" + destinationParentPath + ", sourcePaths=" + sourcePaths +
			", displayName=" + displayName + ", classification=" + classification + ", newResourceName=" + newResourceName + "]";
	}
}
