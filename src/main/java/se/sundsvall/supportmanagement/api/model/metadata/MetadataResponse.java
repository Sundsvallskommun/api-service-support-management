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

	@ArraySchema(schema = @Schema(implementation = Role.class))
	private List<Role> roles;

	public static MetadataResponse create() {
		return new MetadataResponse();
	}

	public List<Category> getCategories() {
		return categories;
	}

	public void setCategories(final List<Category> categories) {
		this.categories = categories;
	}

	public MetadataResponse withCategories(final List<Category> categories) {
		this.categories = categories;
		return this;
	}

	public List<ExternalIdType> getExternalIdTypes() {
		return externalIdTypes;
	}

	public void setExternalIdTypes(final List<ExternalIdType> externalIdTypes) {
		this.externalIdTypes = externalIdTypes;
	}

	public MetadataResponse withExternalIdTypes(final List<ExternalIdType> externalIdTypes) {
		this.externalIdTypes = externalIdTypes;
		return this;
	}

	public List<Status> getStatuses() {
		return statuses;
	}

	public void setStatuses(final List<Status> statuses) {
		this.statuses = statuses;
	}

	public MetadataResponse withStatuses(final List<Status> statuses) {
		this.statuses = statuses;
		return this;
	}

	public List<Role> getRoles() {
		return roles;
	}

	public void setRoles(final List<Role> roles) {
		this.roles = roles;
	}

	public MetadataResponse withRoles(final List<Role> roles) {
		this.roles = roles;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(categories, externalIdTypes, roles, statuses);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MetadataResponse other)) {
			return false;
		}
		return Objects.equals(categories, other.categories) && Objects.equals(externalIdTypes, other.externalIdTypes) && Objects.equals(roles, other.roles) && Objects.equals(statuses, other.statuses);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("MetadataResponse [categories=").append(categories).append(", externalIdTypes=").append(externalIdTypes).append(", statuses=").append(statuses).append(", roles=").append(roles).append("]");
		return builder.toString();
	}
}
