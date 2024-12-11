package se.sundsvall.supportmanagement.api.model.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "MetadataResponse model")
public class MetadataResponse {

	@ArraySchema(schema = @Schema(implementation = Category.class))
	private List<Category> categories;

	@ArraySchema(schema = @Schema(implementation = ExternalIdType.class))
	private List<ExternalIdType> externalIdTypes;

	@Schema(implementation = Labels.class)
	private Labels labels;

	@ArraySchema(schema = @Schema(implementation = Status.class))
	private List<Status> statuses;

	@ArraySchema(schema = @Schema(implementation = Role.class))
	private List<Role> roles;

	@ArraySchema(schema = @Schema(implementation = ContactReason.class))
	private List<ContactReason> contactReasons;

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

	public List<ContactReason> getContactReasons() {
		return contactReasons;
	}

	public void setContactReasons(final List<ContactReason> contactReasons) {
		this.contactReasons = contactReasons;
	}

	public MetadataResponse withContactReasons(final List<ContactReason> contactReasons) {
		this.contactReasons = contactReasons;
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

	public Labels getLabels() {
		return labels;
	}

	public void setLabels(final Labels labels) {
		this.labels = labels;
	}

	public MetadataResponse withLabels(final Labels labels) {
		this.labels = labels;
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
	public String toString() {
		return "MetadataResponse{" +
			"categories=" + categories +
			", externalIdTypes=" + externalIdTypes +
			", labels=" + labels +
			", statuses=" + statuses +
			", roles=" + roles +
			", contactReasons=" + contactReasons +
			'}';
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final MetadataResponse that = (MetadataResponse) o;
		return Objects.equals(categories, that.categories) && Objects.equals(externalIdTypes, that.externalIdTypes) && Objects.equals(labels, that.labels) && Objects.equals(statuses, that.statuses) && Objects.equals(roles, that.roles) && Objects.equals(
			contactReasons, that.contactReasons);
	}

	@Override
	public int hashCode() {
		return Objects.hash(categories, externalIdTypes, labels, statuses, roles, contactReasons);
	}
}
