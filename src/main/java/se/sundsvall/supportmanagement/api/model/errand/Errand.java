package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import se.sundsvall.supportmanagement.api.validation.UniqueExternalTagKeys;
import se.sundsvall.supportmanagement.api.validation.ValidCategoryTag;
import se.sundsvall.supportmanagement.api.validation.ValidClientIdTag;
import se.sundsvall.supportmanagement.api.validation.ValidStatusTag;
import se.sundsvall.supportmanagement.api.validation.ValidTypeTag;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Errand model")
public class Errand {

	@Schema(description = "Unique id for the errand", example = "f0882f1d-06bc-47fd-b017-1d8307f5ce95", accessMode = READ_ONLY)
	@Null(groups = { OnCreate.class, OnUpdate.class })
	private String id;

	@Schema(description = "Title for the errand", example = "Title for the errand")
	@NotBlank(groups = OnCreate.class)
	private String title;

	@Schema(implementation = Priority.class)
	@NotNull(groups = OnCreate.class)
	private Priority priority;

	@Schema(implementation = Customer.class)
	@NotNull(groups = OnCreate.class)
	@Valid
	private Customer customer;

	@ArraySchema(schema = @Schema(implementation = ExternalTag.class), uniqueItems = true)
	@UniqueExternalTagKeys
	@Valid
	private List<ExternalTag> externalTags;

	@Schema(description = "System which owns the errand. Information for identifying the parent of an errand.", example = "CONTACTCENTER")
	@NotBlank(groups = OnCreate.class)
	@Null(groups = OnUpdate.class)
	@ValidClientIdTag
	private String clientIdTag;

	@Schema(description = "Category for the errand", example = "SUPPORT_CASE")
	@NotBlank(groups = OnCreate.class)
	@ValidCategoryTag
	private String categoryTag;

	@Schema(description = "Type of errand", example = "OTHER_ISSUES")
	@NotBlank(groups = OnCreate.class)
	@ValidTypeTag
	private String typeTag;

	@Schema(description = "Status for the errand", example = "NEW_CASE")
	@NotBlank(groups = OnCreate.class)
	@ValidStatusTag
	private String statusTag;

	@Schema(description = "User id for the person which has created the errand", example = "joe01doe")
	@NotBlank(groups = OnCreate.class)
	@Null(groups = OnUpdate.class)
	private String reporterUserId;

	@Schema(description = "Id for the user which currently is assigned to the errand if a user is assigned", example = "joe01doe")
	private String assignedUserId;

	@Schema(description = "Id for the group which is currently assigned to the errand if a group is assigned", example = "hardware support")
	private String assignedGroupId;

	@Schema(description = "Timestamp when errand was created", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null(groups = { OnCreate.class, OnUpdate.class })
	private OffsetDateTime created;

	@Schema(description = "Timestamp when errand was last modified", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null(groups = { OnCreate.class, OnUpdate.class })
	private OffsetDateTime modified;

	public static Errand create() {
		return new Errand();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Errand withId(String id) {
		this.id = id;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Errand withTitle(String title) {
		this.title = title;
		return this;
	}

	public Priority getPriority() {
		return priority;
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
	}

	public Errand withPriority(Priority priority) {
		this.priority = priority;
		return this;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public Errand withCustomer(Customer customer) {
		this.customer = customer;
		return this;
	}

	public List<ExternalTag> getExternalTags() {
		return externalTags;
	}

	public void setExternalTags(List<ExternalTag> externalTags) {
		this.externalTags = externalTags;
	}

	public Errand withExternalTags(List<ExternalTag> externalTags) {
		this.externalTags = externalTags;
		return this;
	}

	public String getClientIdTag() {
		return clientIdTag;
	}

	public void setClientIdTag(String clientIdTag) {
		this.clientIdTag = clientIdTag;
	}

	public Errand withClientIdTag(String clientIdTag) {
		this.clientIdTag = clientIdTag;
		return this;
	}

	public String getCategoryTag() {
		return categoryTag;
	}

	public void setCategoryTag(String categoryTag) {
		this.categoryTag = categoryTag;
	}

	public Errand withCategoryTag(String categoryTag) {
		this.categoryTag = categoryTag;
		return this;
	}

	public String getTypeTag() {
		return typeTag;
	}

	public void setTypeTag(String typeTag) {
		this.typeTag = typeTag;
	}

	public Errand withTypeTag(String typeTag) {
		this.typeTag = typeTag;
		return this;
	}

	public String getStatusTag() {
		return statusTag;
	}

	public void setStatusTag(String statusTag) {
		this.statusTag = statusTag;
	}

	public Errand withStatusTag(String statusTag) {
		this.statusTag = statusTag;
		return this;
	}

	public String getReporterUserId() {
		return reporterUserId;
	}

	public void setReporterUserId(String reporterUserId) {
		this.reporterUserId = reporterUserId;
	}

	public Errand withReporterUserId(String reporterUserId) {
		this.reporterUserId = reporterUserId;
		return this;
	}

	public String getAssignedUserId() {
		return assignedUserId;
	}

	public void setAssignedUserId(String assignedUserId) {
		this.assignedUserId = assignedUserId;
	}

	public Errand withAssignedUserId(String assignedUserId) {
		this.assignedUserId = assignedUserId;
		return this;
	}

	public String getAssignedGroupId() {
		return assignedGroupId;
	}

	public void setAssignedGroupId(String assignedGroupId) {
		this.assignedGroupId = assignedGroupId;
	}

	public Errand withAssignedGroupId(String assignedGroupId) {
		this.assignedGroupId = assignedGroupId;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public Errand withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public Errand withModified(OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(assignedGroupId, assignedUserId, categoryTag, created, customer, externalTags, id, modified, clientIdTag, priority, reporterUserId, statusTag, title, typeTag);
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
		Errand other = (Errand) obj;
		return Objects.equals(assignedGroupId, other.assignedGroupId) && Objects.equals(assignedUserId, other.assignedUserId) && Objects.equals(categoryTag, other.categoryTag) && Objects.equals(created, other.created) && Objects.equals(customer,
			other.customer) && Objects.equals(externalTags, other.externalTags) && Objects.equals(id, other.id) && Objects.equals(modified, other.modified) && Objects.equals(clientIdTag, other.clientIdTag) && priority == other.priority && Objects.equals(
				reporterUserId, other.reporterUserId) && Objects.equals(statusTag, other.statusTag) && Objects.equals(title, other.title) && Objects.equals(typeTag, other.typeTag);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Errand [id=").append(id).append(", title=").append(title).append(", priority=").append(priority).append(", customer=").append(customer).append(", externalTags=").append(externalTags).append(", clientIdTag=").append(clientIdTag)
			.append(", categoryTag=").append(categoryTag).append(", typeTag=").append(typeTag).append(", statusTag=").append(statusTag).append(", reporterUserId=").append(reporterUserId).append(", assignedUserId=").append(assignedUserId).append(
				", assignedGroupId=").append(assignedGroupId).append(", created=").append(created).append(", modified=").append(modified).append("]");
		return builder.toString();
	}
}
