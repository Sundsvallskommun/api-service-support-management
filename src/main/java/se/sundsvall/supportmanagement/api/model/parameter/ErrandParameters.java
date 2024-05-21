package se.sundsvall.supportmanagement.api.model.parameter;

import java.util.List;
import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

public class ErrandParameters {

	@Schema(description = "The errand parameters", implementation = ErrandParameter.class)
	private List<ErrandParameter> parameters;

	public static ErrandParameters create() {
		return new ErrandParameters();
	}

	public List<ErrandParameter> getParameters() {
		return parameters;
	}

	public void setParameters(final List<ErrandParameter> parameters) {
		this.parameters = parameters;
	}

	public ErrandParameters withErrandParameters(final List<ErrandParameter> errandParameters) {
		this.parameters = errandParameters;
		return this;
	}

	@Override
	public String toString() {
		return "ErrandParameters{" +
			"parameters=" + parameters +
			'}';
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final ErrandParameters that = (ErrandParameters) o;
		return Objects.equals(parameters, that.parameters);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parameters);
	}
}
