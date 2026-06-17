package se.sundsvall.supportmanagement.api.model.errand.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

@Schema(description = "Request body for handing over an errand to another namespace")
public class HandoverErrandRequest {

	@NotNull
	@Valid
	private HandoverTarget target;

	@NotNull
	@Valid
	private HandoverMapping mapping;

	@Valid
	private HandoverOverrides overrides;

	@Valid
	private HandoverInclude include;

	@Valid
	private HandoverSourceHandling sourceHandling;

	public static HandoverErrandRequest create() {
		return new HandoverErrandRequest();
	}

	public HandoverTarget getTarget() {
		return target;
	}

	public void setTarget(final HandoverTarget target) {
		this.target = target;
	}

	public HandoverErrandRequest withTarget(final HandoverTarget target) {
		this.target = target;
		return this;
	}

	public HandoverMapping getMapping() {
		return mapping;
	}

	public void setMapping(final HandoverMapping mapping) {
		this.mapping = mapping;
	}

	public HandoverErrandRequest withMapping(final HandoverMapping mapping) {
		this.mapping = mapping;
		return this;
	}

	public HandoverOverrides getOverrides() {
		return overrides;
	}

	public void setOverrides(final HandoverOverrides overrides) {
		this.overrides = overrides;
	}

	public HandoverErrandRequest withOverrides(final HandoverOverrides overrides) {
		this.overrides = overrides;
		return this;
	}

	public HandoverInclude getInclude() {
		return include;
	}

	public void setInclude(final HandoverInclude include) {
		this.include = include;
	}

	public HandoverErrandRequest withInclude(final HandoverInclude include) {
		this.include = include;
		return this;
	}

	public HandoverSourceHandling getSourceHandling() {
		return sourceHandling;
	}

	public void setSourceHandling(final HandoverSourceHandling sourceHandling) {
		this.sourceHandling = sourceHandling;
	}

	public HandoverErrandRequest withSourceHandling(final HandoverSourceHandling sourceHandling) {
		this.sourceHandling = sourceHandling;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final HandoverErrandRequest that = (HandoverErrandRequest) o;
		return Objects.equals(target, that.target) && Objects.equals(mapping, that.mapping)
			&& Objects.equals(overrides, that.overrides) && Objects.equals(include, that.include)
			&& Objects.equals(sourceHandling, that.sourceHandling);
	}

	@Override
	public int hashCode() {
		return Objects.hash(target, mapping, overrides, include, sourceHandling);
	}

	@Override
	public String toString() {
		return "HandoverErrandRequest{target=" + target + ", mapping=" + mapping + ", overrides=" + overrides
			+ ", include=" + include + ", sourceHandling=" + sourceHandling + "}";
	}
}
