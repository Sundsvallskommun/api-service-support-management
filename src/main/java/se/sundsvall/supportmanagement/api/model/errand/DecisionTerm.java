package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import java.util.Objects;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * A term attached to a decision - a numbered line of text with a category.
 */
@Schema(description = "Decision term model")
public class DecisionTerm {

	@Schema(description = "Term ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "Order the term is shown in", examples = "1")
	private Integer sortOrder;

	@Schema(description = "Category of the term, as registered for the namespace", maxLength = 128, examples = "serveringstid")
	@Size(max = 128, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String category;

	@Schema(description = "The text of the term", examples = "Servering får ske mellan 11.00 och 01.00.")
	@NotBlank(groups = {
		Default.class, OnCreate.class
	})
	private String text;

	public static DecisionTerm create() {
		return new DecisionTerm();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public DecisionTerm withId(final String id) {
		this.id = id;
		return this;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public DecisionTerm withSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
		return this;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(final String category) {
		this.category = category;
	}

	public DecisionTerm withCategory(final String category) {
		this.category = category;
		return this;
	}

	public String getText() {
		return text;
	}

	public void setText(final String text) {
		this.text = text;
	}

	public DecisionTerm withText(final String text) {
		this.text = text;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, sortOrder, category, text);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final DecisionTerm other)) {
			return false;
		}
		return Objects.equals(id, other.id)
			&& Objects.equals(sortOrder, other.sortOrder)
			&& Objects.equals(category, other.category)
			&& Objects.equals(text, other.text);
	}

	@Override
	public String toString() {
		return "DecisionTerm{" +
			"id='" + id + '\'' +
			", sortOrder=" + sortOrder +
			", category='" + category + '\'' +
			", text='" + text + '\'' +
			'}';
	}
}
