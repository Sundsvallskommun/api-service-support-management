package se.sundsvall.supportmanagement.api.model.parameter;

import java.util.List;
import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

public class ErrandParameters {

	@Schema(description = "The errand parameters", implementation = ErrandParameter.class)
	private List<ErrandParameter> errandParameters;

	public static ErrandParameters create() {
		return new ErrandParameters();
	}

	public List<ErrandParameter> getErrandParameters() {
		return errandParameters;
	}

	public void setErrandParameters(final List<ErrandParameter> errandParameters) {
		this.errandParameters = errandParameters;
	}

	public ErrandParameters withErrandParameters(final List<ErrandParameter> errandParameters) {
		this.errandParameters = errandParameters;
		return this;
	}

	@Override
	public String toString() {
		return "ErrandParameters{" +
			"errandParameters=" + errandParameters +
			'}';
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final ErrandParameters that = (ErrandParameters) o;
		return Objects.equals(errandParameters, that.errandParameters);
	}

	@Override
	public int hashCode() {
		return Objects.hash(errandParameters);
	}
}
