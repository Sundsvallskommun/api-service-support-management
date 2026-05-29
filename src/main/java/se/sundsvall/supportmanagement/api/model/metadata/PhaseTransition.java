package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Phase transition model")
public class PhaseTransition {

	@Schema(description = "Transition ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", accessMode = READ_ONLY)
	@ValidUuid(nullable = true)
	private String id;

	@Schema(description = "Target phase ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7")
	@NotBlank
	@ValidUuid
	private String targetPhaseId;

	@Schema(description = "Target phase name", examples = "INVESTIGATION", accessMode = READ_ONLY)
	private String targetPhaseName;

	@Schema(description = "Target phase display name", examples = "Utredning", accessMode = READ_ONLY)
	private String targetPhaseDisplayName;

	@Schema(description = "Description of the transition", examples = "Skicka till utredning")
	private String description;

	@Schema(description = "Indicates if the phase transition is deprecated", defaultValue = "false", examples = "true")
	private Boolean deprecated;

	public static PhaseTransition create() {
		return new PhaseTransition();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public PhaseTransition withId(final String id) {
		this.id = id;
		return this;
	}

	public String getTargetPhaseId() {
		return targetPhaseId;
	}

	public void setTargetPhaseId(final String targetPhaseId) {
		this.targetPhaseId = targetPhaseId;
	}

	public PhaseTransition withTargetPhaseId(final String targetPhaseId) {
		this.targetPhaseId = targetPhaseId;
		return this;
	}

	public String getTargetPhaseName() {
		return targetPhaseName;
	}

	public void setTargetPhaseName(final String targetPhaseName) {
		this.targetPhaseName = targetPhaseName;
	}

	public PhaseTransition withTargetPhaseName(final String targetPhaseName) {
		this.targetPhaseName = targetPhaseName;
		return this;
	}

	public String getTargetPhaseDisplayName() {
		return targetPhaseDisplayName;
	}

	public void setTargetPhaseDisplayName(final String targetPhaseDisplayName) {
		this.targetPhaseDisplayName = targetPhaseDisplayName;
	}

	public PhaseTransition withTargetPhaseDisplayName(final String targetPhaseDisplayName) {
		this.targetPhaseDisplayName = targetPhaseDisplayName;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public PhaseTransition withDescription(final String description) {
		this.description = description;
		return this;
	}

	public Boolean getDeprecated() {
		return deprecated;
	}

	public void setDeprecated(final Boolean deprecated) {
		this.deprecated = deprecated;
	}

	public PhaseTransition withDeprecated(final Boolean deprecated) {
		this.deprecated = deprecated;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(deprecated, description, id, targetPhaseDisplayName, targetPhaseId, targetPhaseName);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final PhaseTransition other)) {
			return false;
		}
		return Objects.equals(deprecated, other.deprecated) && Objects.equals(description, other.description) && Objects.equals(id, other.id) && Objects.equals(targetPhaseDisplayName, other.targetPhaseDisplayName) && Objects.equals(targetPhaseId,
			other.targetPhaseId)
			&& Objects.equals(
				targetPhaseName, other.targetPhaseName);
	}

	@Override
	public String toString() {
		return "PhaseTransition{" +
			"id='" + id + '\'' +
			", targetPhaseId='" + targetPhaseId + '\'' +
			", targetPhaseName='" + targetPhaseName + '\'' +
			", targetPhaseDisplayName='" + targetPhaseDisplayName + '\'' +
			", description='" + description + '\'' +
			", deprecated=" + deprecated +
			'}';
	}
}
