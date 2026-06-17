package se.sundsvall.supportmanagement.api.model.errand.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;

@Schema(description = "Target system to handover the errand to")
public class HandoverTarget {

	@Schema(description = "Target namespace", example = "OTHER_NAMESPACE")
	@NotBlank
	private String namespace;

	@Schema(description = "Target municipality id", example = "2281")
	@ValidMunicipalityId
	private String municipalityId;

	public static HandoverTarget create() {
		return new HandoverTarget();
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public HandoverTarget withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public HandoverTarget withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final HandoverTarget that = (HandoverTarget) o;
		return Objects.equals(namespace, that.namespace) && Objects.equals(municipalityId, that.municipalityId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(namespace, municipalityId);
	}

	@Override
	public String toString() {
		return "HandoverTarget{namespace='" + namespace + "', municipalityId='" + municipalityId + "'}";
	}
}
