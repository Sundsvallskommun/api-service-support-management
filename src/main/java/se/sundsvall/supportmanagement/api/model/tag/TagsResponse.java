package se.sundsvall.supportmanagement.api.model.tag;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "TagsResponse model")
public class TagsResponse {

	@ArraySchema(schema = @Schema(description = "Category tags", example = "SUPPORT_CASE"))
	private List<String> categoryTags;

	@ArraySchema(schema = @Schema(description = "Status tags", example = "SOLVED"))
	private List<String> statusTags;

	@ArraySchema(schema = @Schema(description = "Type tags", example = "OTHER_ISSUES"))
	private List<String> typeTags;

	public static TagsResponse create() {
		return new TagsResponse();
	}

	public List<String> getCategoryTags() {
		return categoryTags;
	}

	public void setCategoryTags(List<String> categoryTags) {
		this.categoryTags = categoryTags;
	}

	public TagsResponse withCategoryTags(List<String> categoryTags) {
		this.categoryTags = categoryTags;
		return this;
	}

	public List<String> getStatusTags() {
		return statusTags;
	}

	public void setStatusTags(List<String> statusTags) {
		this.statusTags = statusTags;
	}

	public TagsResponse withStatusTags(List<String> statusTags) {
		this.statusTags = statusTags;
		return this;
	}

	public List<String> getTypeTags() {
		return typeTags;
	}

	public void setTypeTags(List<String> typeTags) {
		this.typeTags = typeTags;
	}

	public TagsResponse withTypeTags(List<String> typeTags) {
		this.typeTags = typeTags;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(categoryTags, statusTags, typeTags);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TagsResponse other = (TagsResponse) obj;
		return Objects.equals(categoryTags, other.categoryTags) && Objects.equals(statusTags, other.statusTags) && Objects.equals(typeTags, other.typeTags);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TagsResponse [categoryTags=").append(categoryTags).append(", statusTags=").append(statusTags).append(", typeTags=").append(typeTags).append("]");
		return builder.toString();
	}
}
