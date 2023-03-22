package se.sundsvall.supportmanagement.api.model.metadata;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "MetadataResponse model")
public class MetadataResponse {

	@ArraySchema(schema = @Schema(implementation = Category.class))
	private List<Category> categories;

	@ArraySchema(schema = @Schema(implementation = ExternalIdType.class))
	private List<ExternalIdType> externalIdTypes;

	@ArraySchema(schema = @Schema(implementation = Status.class))
	private List<Status> statuses;

	public static MetadataResponse create() {
		return new MetadataResponse();
	}

	public List<Category> getCategories() {
		return categories;
	}

	public void setCategories(List<Category> categories) {
		this.categories = categories;
	}

	public MetadataResponse withCategories(List<Category> categories) {
		this.categories = categories;
		return this;
	}

	public List<ExternalIdType> getExternalIdTypes() {
		return externalIdTypes;
	}

	public void setExternalIdTypes(List<ExternalIdType> externalIdTypes) {
		this.externalIdTypes = externalIdTypes;
	}

	public MetadataResponse withExternalIdTypes(List<ExternalIdType> externalIdTypes) {
		this.externalIdTypes = externalIdTypes;
		return this;
	}

	public List<Status> getStatuses() {
		return statuses;
	}

	public void setStatuses(List<Status> statuses) {
		this.statuses = statuses;
	}

	public MetadataResponse withStatuses(List<Status> statuses) {
		this.statuses = statuses;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(categories, externalIdTypes, statuses);
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
		MetadataResponse other = (MetadataResponse) obj;
		return Objects.equals(categories, other.categories) && Objects.equals(externalIdTypes, other.externalIdTypes) && Objects.equals(statuses, other.statuses);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MetadataResponse [categories=").append(categories).append(", externalIdTypes=").append(externalIdTypes).append(", statuses=").append(statuses).append("]");
		return builder.toString();
	}
}
