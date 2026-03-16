package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Phase model")
public class Phase {

	@Schema(description = "Phase ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", accessMode = READ_ONLY)
	@ValidUuid(nullable = true)
	private String id;

	@Schema(description = "Phase name", examples = "INVESTIGATION")
	@NotBlank
	private String name;

	@Schema(description = "Display name for the phase", examples = "Utredning")
	private String displayName;

	@Schema(description = "Description of the phase", examples = "Fas för utredning")
	private String description;

	@Schema(description = "Order of the phase in the process (0 = initial phase)", examples = {
		"0"
	})
	private Integer phaseOrder;

	@Schema(description = "Allowed statuses in this phase", examples = "[\"IN_PROGRESS\", \"WAITING\"]")
	private List<String> allowedStatuses;

	@Schema(description = "Transitions from this phase", accessMode = READ_ONLY)
	private List<PhaseTransition> transitions;

	@Schema(description = "Timestamp when the phase was created", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@Null
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the phase was last modified", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@Null
	private OffsetDateTime modified;

	public static Phase create() {
		return new Phase();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Phase withId(final String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Phase withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public Phase withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public Phase withDescription(final String description) {
		this.description = description;
		return this;
	}

	public Integer getPhaseOrder() {
		return phaseOrder;
	}

	public void setPhaseOrder(final Integer phaseOrder) {
		this.phaseOrder = phaseOrder;
	}

	public Phase withPhaseOrder(final Integer phaseOrder) {
		this.phaseOrder = phaseOrder;
		return this;
	}

	public List<String> getAllowedStatuses() {
		return allowedStatuses;
	}

	public void setAllowedStatuses(final List<String> allowedStatuses) {
		this.allowedStatuses = allowedStatuses;
	}

	public Phase withAllowedStatuses(final List<String> allowedStatuses) {
		this.allowedStatuses = allowedStatuses;
		return this;
	}

	public List<PhaseTransition> getTransitions() {
		return transitions;
	}

	public void setTransitions(final List<PhaseTransition> transitions) {
		this.transitions = transitions;
	}

	public Phase withTransitions(final List<PhaseTransition> transitions) {
		this.transitions = transitions;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Phase withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Phase withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(allowedStatuses, created, description, displayName, id, modified, name, phaseOrder, transitions);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Phase other)) {
			return false;
		}
		return Objects.equals(allowedStatuses, other.allowedStatuses) && Objects.equals(created, other.created) && Objects.equals(description, other.description) && Objects.equals(displayName, other.displayName) && Objects.equals(id, other.id)
			&& Objects.equals(modified, other.modified) && Objects.equals(name, other.name) && Objects.equals(phaseOrder, other.phaseOrder) && Objects.equals(transitions, other.transitions);
	}

	@Override
	public String toString() {
		return "Phase{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", displayName='" + displayName + '\'' +
			", description='" + description + '\'' +
			", phaseOrder=" + phaseOrder +
			", allowedStatuses=" + allowedStatuses +
			", transitions=" + transitions +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
