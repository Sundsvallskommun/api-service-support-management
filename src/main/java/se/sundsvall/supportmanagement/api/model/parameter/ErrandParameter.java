package se.sundsvall.supportmanagement.api.model.parameter;

import java.util.Objects;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

public class ErrandParameter {

	@Schema(description = "Id of the parameter", example = "1234567890", accessMode = Schema.AccessMode.READ_ONLY)
	private String id;

	@NotBlank
	private String name;

	@NotBlank
	private String value;

	public static ErrandParameter create() {
		return new ErrandParameter();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ErrandParameter withId(final String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public ErrandParameter withName(final String name) {
		this.name = name;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public ErrandParameter withValue(final String value) {
		this.value = value;
		return this;
	}

	@Override
	public String toString() {
		return "ErrandParameter{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", value='" + value + '\'' +
			'}';
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final ErrandParameter that = (ErrandParameter) o;
		return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, value);
	}
}
