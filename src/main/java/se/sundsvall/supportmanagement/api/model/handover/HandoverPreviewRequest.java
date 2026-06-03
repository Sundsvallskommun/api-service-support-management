package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@Schema(description = "Request describing which namespace a source errand should be previewed for handover to")
public class HandoverPreviewRequest {

	@Schema(description = "Namespace the errand should be handed over to", examples = "OTHER_NAMESPACE", requiredMode = REQUIRED)
	@NotBlank
	@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE)
	private String targetNamespace;

	@Schema(description = "Municipality id the errand should be handed over to", examples = "2281", requiredMode = REQUIRED)
	@ValidMunicipalityId
	private String targetMunicipalityId;

	public static HandoverPreviewRequest create() {
		return new HandoverPreviewRequest();
	}

	public String getTargetNamespace() {
		return targetNamespace;
	}

	public void setTargetNamespace(final String targetNamespace) {
		this.targetNamespace = targetNamespace;
	}

	public HandoverPreviewRequest withTargetNamespace(final String targetNamespace) {
		this.targetNamespace = targetNamespace;
		return this;
	}

	public String getTargetMunicipalityId() {
		return targetMunicipalityId;
	}

	public void setTargetMunicipalityId(final String targetMunicipalityId) {
		this.targetMunicipalityId = targetMunicipalityId;
	}

	public HandoverPreviewRequest withTargetMunicipalityId(final String targetMunicipalityId) {
		this.targetMunicipalityId = targetMunicipalityId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(targetNamespace, targetMunicipalityId);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final HandoverPreviewRequest other)) {
			return false;
		}
		return Objects.equals(targetNamespace, other.targetNamespace) && Objects.equals(targetMunicipalityId, other.targetMunicipalityId);
	}

	@Override
	public String toString() {
		return "HandoverPreviewRequest [targetNamespace=" + targetNamespace + ", targetMunicipalityId=" + targetMunicipalityId + "]";
	}
}
