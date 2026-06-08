package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Label mapping section: the selectable target labels shared by every mapping, plus one mapping suggestion per source label")
public class LabelMappingGroup {

	@Schema(description = "All selectable labels in the target namespace, shared by every mapping. Always present, may be empty", requiredMode = REQUIRED)
	private List<LabelCandidate> candidates;

	@Schema(description = "Mapping suggestions, one entry per source label. Always present, may be empty", requiredMode = REQUIRED)
	private List<LabelMapping> mappings;

	public static LabelMappingGroup create() {
		return new LabelMappingGroup();
	}

	public List<LabelCandidate> getCandidates() {
		return candidates;
	}

	public void setCandidates(final List<LabelCandidate> candidates) {
		this.candidates = candidates;
	}

	public LabelMappingGroup withCandidates(final List<LabelCandidate> candidates) {
		this.candidates = candidates;
		return this;
	}

	public List<LabelMapping> getMappings() {
		return mappings;
	}

	public void setMappings(final List<LabelMapping> mappings) {
		this.mappings = mappings;
	}

	public LabelMappingGroup withMappings(final List<LabelMapping> mappings) {
		this.mappings = mappings;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(candidates, mappings);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final LabelMappingGroup other)) {
			return false;
		}
		return Objects.equals(candidates, other.candidates) && Objects.equals(mappings, other.mappings);
	}

	@Override
	public String toString() {
		return "LabelMappingGroup [candidates=" + candidates + ", mappings=" + mappings + "]";
	}
}
