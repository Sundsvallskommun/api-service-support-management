package se.sundsvall.supportmanagement.api.model.config.action;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;

@Schema(name = "ActionParameter", description = "Errand action parameter model")
public class Parameter {

	@Schema(description = "Parameter key")
	@NotBlank
	private String key;

	@Schema(description = "Parameter values. Each value can have a maximum length of 2000 characters")
	@Valid
	private List<@Size(max = 2000) String> values;

	public static Parameter create() {
		return new Parameter();
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Parameter withKey(final String key) {
		this.key = key;
		return this;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(List<String> values) {
		this.values = values;
	}

	public Parameter withValues(final List<String> values) {
		this.values = values;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		Parameter parameter = (Parameter) o;
		return Objects.equals(key, parameter.key) && Objects.equals(values, parameter.values);
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, values);
	}

	@Override
	public String toString() {
		return "Parameter{" +
			"key='" + key + '\'' +
			", values=" + values +
			'}';
	}
}
