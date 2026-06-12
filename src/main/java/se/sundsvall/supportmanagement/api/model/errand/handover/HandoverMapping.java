package se.sundsvall.supportmanagement.api.model.errand.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.supportmanagement.api.model.errand.Classification;

@Schema(description = "Field mappings to apply when creating the errand in the target system")
public class HandoverMapping {

	@Schema(description = "Status to set on the new errand", example = "NEW_CASE")
	private String status;

	@Valid
	private Classification classification;

	@Schema(description = "Label UUIDs to apply on the new errand")
	private List<@ValidUuid String> labels;

	@Schema(description = "Contact reason to set on the new errand", example = "Printer issue")
	private String contactReason;

	@Schema(description = "Channel to set on the new errand", example = "WEB_UI")
	private String channel;

	public static HandoverMapping create() {
		return new HandoverMapping();
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public HandoverMapping withStatus(final String status) {
		this.status = status;
		return this;
	}

	public Classification getClassification() {
		return classification;
	}

	public void setClassification(final Classification classification) {
		this.classification = classification;
	}

	public HandoverMapping withClassification(final Classification classification) {
		this.classification = classification;
		return this;
	}

	public List<String> getLabels() {
		return labels;
	}

	public void setLabels(final List<String> labels) {
		this.labels = labels;
	}

	public HandoverMapping withLabels(final List<String> labels) {
		this.labels = labels;
		return this;
	}

	public String getContactReason() {
		return contactReason;
	}

	public void setContactReason(final String contactReason) {
		this.contactReason = contactReason;
	}

	public HandoverMapping withContactReason(final String contactReason) {
		this.contactReason = contactReason;
		return this;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(final String channel) {
		this.channel = channel;
	}

	public HandoverMapping withChannel(final String channel) {
		this.channel = channel;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final HandoverMapping that = (HandoverMapping) o;
		return Objects.equals(status, that.status) && Objects.equals(classification, that.classification)
			&& Objects.equals(labels, that.labels) && Objects.equals(contactReason, that.contactReason)
			&& Objects.equals(channel, that.channel);
	}

	@Override
	public int hashCode() {
		return Objects.hash(status, classification, labels, contactReason, channel);
	}

	@Override
	public String toString() {
		return "HandoverMapping{status='" + status + "', classification=" + classification + ", labels=" + labels
			+ ", contactReason='" + contactReason + "', channel='" + channel + "'}";
	}
}
