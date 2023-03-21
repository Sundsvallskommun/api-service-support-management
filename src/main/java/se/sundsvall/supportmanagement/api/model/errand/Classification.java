package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;

import javax.validation.constraints.NotBlank;
import java.util.Objects;

@Schema(description = "Classification model")
public class Classification {
	@Schema(description = "Category for the errand", example = "SUPPORT_CASE")
	@NotBlank(groups = OnCreate.class)
	private String category;

	@Schema(description = "Type of errand", example = "OTHER_ISSUES")
	@NotBlank(groups = OnCreate.class)
	private String type;

	public static Classification create() {
		return new Classification();
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Classification withCategory(String category) {
		this.category = category;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Classification withType(String type) {
		this.type = type;
		return this;
	}
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Classification that = (Classification) o;
		return Objects.equals(category, that.category) && Objects.equals(type, that.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(category, type);
	}

	public String toString() {
		final StringBuilder sb = new StringBuilder("Classification{");
		sb.append("category='").append(category).append('\'');
		sb.append(", type='").append(type).append('\'');
		sb.append('}');
		return sb.toString();
	}
}
