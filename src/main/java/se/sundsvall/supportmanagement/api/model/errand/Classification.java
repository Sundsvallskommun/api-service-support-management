package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;

@Schema(description = "Classification model")
public class Classification {

	@Schema(description = "Category for the errand", examples = "SUPPORT_CASE")
	@NotBlank(groups = {
		OnCreate.class, OnUpdate.class
	})
	private String category;

	@Schema(description = "Type of errand", examples = "OTHER_ISSUES")
	@NotBlank(groups = {
		OnCreate.class, OnUpdate.class
	})
	private String type;

	public static Classification create() {
		return new Classification();
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(final String category) {
		this.category = category;
	}

	public Classification withCategory(final String category) {
		this.category = category;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public Classification withType(final String type) {
		this.type = type;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if ((o == null) || (getClass() != o.getClass())) {
			return false;
		}
		final Classification that = (Classification) o;
		return Objects.equals(category, that.category) && Objects.equals(type, that.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(category, type);
	}

	@Override
	public String toString() {
		return "Classification{" +
			"category='" + category + '\'' +
			", type='" + type + '\'' +
			'}';
	}
}
