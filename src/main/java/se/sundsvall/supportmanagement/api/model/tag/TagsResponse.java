package se.sundsvall.supportmanagement.api.model.tag;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "TagsResponse model")
public class TagsResponse {

	@ArraySchema(schema = @Schema(description = "Category tags", example = "SUPPORT_CASE"))
	private List<String> categoryTags;

	@ArraySchema(schema = @Schema(description = "Status tags", example = "SOLVED"))
	private List<String> statusTags;

	@ArraySchema(schema = @Schema(description = "Type tags", example = "OTHER_ISSUES"))
	private List<String> typeTags;

	@ArraySchema(schema = @Schema(description = "Client-id tags", example = "JIRA-FORM"))
	private List<String> clientIdTags;

	public static TagsResponse create() {
		return new TagsResponse();
	}

	public List<String> getCategoryTags() {
		return categoryTags;
	}

	public void setCategoryTags(final List<String> categoryTags) {
		this.categoryTags = categoryTags;
	}

	public TagsResponse withCategoryTags(final List<String> categoryTags) {
		this.categoryTags = categoryTags;
		return this;
	}

	public List<String> getStatusTags() {
		return statusTags;
	}

	public void setStatusTags(final List<String> statusTags) {
		this.statusTags = statusTags;
	}

	public TagsResponse withStatusTags(final List<String> statusTags) {
		this.statusTags = statusTags;
		return this;
	}

	public List<String> getTypeTags() {
		return typeTags;
	}

	public void setTypeTags(final List<String> typeTags) {
		this.typeTags = typeTags;
	}

	public TagsResponse withTypeTags(final List<String> typeTags) {
		this.typeTags = typeTags;
		return this;
	}

	public List<String> getClientIdTags() {
		return clientIdTags;
	}

	public void setClientIdTags(final List<String> clientIdTags) {
		this.clientIdTags = clientIdTags;
	}

	public TagsResponse withClientIdTags(final List<String> clientIdTags) {
		this.clientIdTags = clientIdTags;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(categoryTags, clientIdTags, statusTags, typeTags);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final var other = (TagsResponse) obj;
		return Objects.equals(categoryTags, other.categoryTags) && Objects.equals(clientIdTags, other.clientIdTags) && Objects.equals(statusTags, other.statusTags) && Objects.equals(typeTags, other.typeTags);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("TagsResponse [categoryTags=").append(categoryTags).append(", statusTags=").append(statusTags).append(", typeTags=").append(typeTags).append(", clientIdTags=").append(clientIdTags).append("]");
		return builder.toString();
	}
}
