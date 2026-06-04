package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "A category/type pair describing an errand classification")
public class ClassificationOption {

	@Schema(description = "Category name", examples = "SUPPORT_CASE")
	private String category;

	@Schema(description = "Type name", examples = "OTHER_ISSUES")
	private String type;

	public static ClassificationOption create() {
		return new ClassificationOption();
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(final String category) {
		this.category = category;
	}

	public ClassificationOption withCategory(final String category) {
		this.category = category;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public ClassificationOption withType(final String type) {
		this.type = type;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(category, type);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final ClassificationOption other)) {
			return false;
		}
		return Objects.equals(category, other.category) && Objects.equals(type, other.type);
	}

	@Override
	public String toString() {
		return "ClassificationOption [category=" + category + ", type=" + type + "]";
	}
}
