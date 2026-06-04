package se.sundsvall.supportmanagement.api.model.errand.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Schema(description = "Response body for a successful errand handover")
public class HandoverErrand {

	@Schema(description = "Id of the newly created errand in the target system", example = "f0882f1d-06bc-47fd-b017-1d8307f5ce95")
	private String newErrandId;

	@Schema(description = "Errand number of the newly created errand", example = "KC-23010001")
	private String newErrandNumber;

	@Schema(description = "Target namespace and municipality the errand was handed over to")
	private HandoverTarget target;

	@Schema(description = "Id of the relation created between the source and target errand")
	private String relationId;

	@Schema(description = "Field mappings that were applied when creating the new errand")
	private Map<String, String> appliedMappings;

	@Schema(description = "Non-fatal warnings that occurred during handover")
	private List<String> warnings;

	public static HandoverErrand create() {
		return new HandoverErrand();
	}

	public String getNewErrandId() {
		return newErrandId;
	}

	public void setNewErrandId(final String newErrandId) {
		this.newErrandId = newErrandId;
	}

	public HandoverErrand withNewErrandId(final String newErrandId) {
		this.newErrandId = newErrandId;
		return this;
	}

	public String getNewErrandNumber() {
		return newErrandNumber;
	}

	public void setNewErrandNumber(final String newErrandNumber) {
		this.newErrandNumber = newErrandNumber;
	}

	public HandoverErrand withNewErrandNumber(final String newErrandNumber) {
		this.newErrandNumber = newErrandNumber;
		return this;
	}

	public HandoverTarget getTarget() {
		return target;
	}

	public void setTarget(final HandoverTarget target) {
		this.target = target;
	}

	public HandoverErrand withTarget(final HandoverTarget target) {
		this.target = target;
		return this;
	}

	public String getRelationId() {
		return relationId;
	}

	public void setRelationId(final String relationId) {
		this.relationId = relationId;
	}

	public HandoverErrand withRelationId(final String relationId) {
		this.relationId = relationId;
		return this;
	}

	public Map<String, String> getAppliedMappings() {
		return appliedMappings;
	}

	public void setAppliedMappings(final Map<String, String> appliedMappings) {
		this.appliedMappings = appliedMappings;
	}

	public HandoverErrand withAppliedMappings(final Map<String, String> appliedMappings) {
		this.appliedMappings = appliedMappings;
		return this;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public void setWarnings(final List<String> warnings) {
		this.warnings = warnings;
	}

	public HandoverErrand withWarnings(final List<String> warnings) {
		this.warnings = warnings;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final HandoverErrand that = (HandoverErrand) o;
		return Objects.equals(newErrandId, that.newErrandId) && Objects.equals(newErrandNumber, that.newErrandNumber)
			&& Objects.equals(target, that.target) && Objects.equals(relationId, that.relationId)
			&& Objects.equals(appliedMappings, that.appliedMappings) && Objects.equals(warnings, that.warnings);
	}

	@Override
	public int hashCode() {
		return Objects.hash(newErrandId, newErrandNumber, target, relationId, appliedMappings, warnings);
	}

	@Override
	public String toString() {
		return "HandoverErrand{newErrandId='" + newErrandId + "', newErrandNumber='" + newErrandNumber
			+ "', target=" + target + ", relationId='" + relationId + "', appliedMappings=" + appliedMappings
			+ ", warnings=" + warnings + "}";
	}
}
