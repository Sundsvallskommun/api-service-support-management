package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Namespace-bound fields that require a human to decide how they should be mapped to the target namespace")
public class MappingRequired {

	@Schema(description = "Status mapping suggestion")
	private StatusMapping status;

	@Schema(description = "Classification (category/type) mapping suggestion")
	private ClassificationMapping classification;

	@Schema(description = "Label mapping suggestions, one entry per source label. Always present, may be empty", requiredMode = REQUIRED)
	private List<LabelMapping> labels;

	@Schema(description = "Contact reason mapping suggestion")
	private ContactReasonMapping contactReason;

	public static MappingRequired create() {
		return new MappingRequired();
	}

	public StatusMapping getStatus() {
		return status;
	}

	public void setStatus(final StatusMapping status) {
		this.status = status;
	}

	public MappingRequired withStatus(final StatusMapping status) {
		this.status = status;
		return this;
	}

	public ClassificationMapping getClassification() {
		return classification;
	}

	public void setClassification(final ClassificationMapping classification) {
		this.classification = classification;
	}

	public MappingRequired withClassification(final ClassificationMapping classification) {
		this.classification = classification;
		return this;
	}

	public List<LabelMapping> getLabels() {
		return labels;
	}

	public void setLabels(final List<LabelMapping> labels) {
		this.labels = labels;
	}

	public MappingRequired withLabels(final List<LabelMapping> labels) {
		this.labels = labels;
		return this;
	}

	public ContactReasonMapping getContactReason() {
		return contactReason;
	}

	public void setContactReason(final ContactReasonMapping contactReason) {
		this.contactReason = contactReason;
	}

	public MappingRequired withContactReason(final ContactReasonMapping contactReason) {
		this.contactReason = contactReason;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(status, classification, labels, contactReason);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final MappingRequired other)) {
			return false;
		}
		return Objects.equals(status, other.status) && Objects.equals(classification, other.classification)
			&& Objects.equals(labels, other.labels) && Objects.equals(contactReason, other.contactReason);
	}

	@Override
	public String toString() {
		return "MappingRequired [status=" + status + ", classification=" + classification + ", labels=" + labels + ", contactReason=" + contactReason + "]";
	}
}
