package se.sundsvall.supportmanagement.api.model.errand;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import se.sundsvall.supportmanagement.api.model.parameter.ErrandParameter;
import se.sundsvall.supportmanagement.api.validation.UniqueExternalTagKeys;
import se.sundsvall.supportmanagement.api.validation.ValidClassification;
import se.sundsvall.supportmanagement.api.validation.ValidContactReason;
import se.sundsvall.supportmanagement.api.validation.ValidStatus;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Errand model")
public class Errand {

	@Schema(description = "Unique id for the errand", example = "f0882f1d-06bc-47fd-b017-1d8307f5ce95", accessMode = READ_ONLY)
	@Null(groups = {OnCreate.class, OnUpdate.class})
	private String id;

	@Schema(description = "Unique number for the errand", example = "KC-23010001", accessMode = READ_ONLY)
	@Null(groups = {OnCreate.class, OnUpdate.class})
	private String errandNumber;

	@Schema(description = "Title for the errand", example = "Title for the errand")
	@NotBlank(groups = OnCreate.class)
	private String title;

	@Schema(implementation = Priority.class)
	@NotNull(groups = OnCreate.class)
	@ArraySchema(schema = @Schema(implementation = Stakeholder.class), uniqueItems = true)
	private Priority priority;

	private List<Stakeholder> stakeholders;

	@ArraySchema(schema = @Schema(implementation = ExternalTag.class), uniqueItems = true)
	@UniqueExternalTagKeys(groups = OnCreate.class)
	@Valid
	private List<ExternalTag> externalTags;

	@ArraySchema(schema = @Schema(implementation = ErrandParameter.class, accessMode = READ_ONLY))
	private List<@Valid ErrandParameter> parameters;

	@Schema(implementation = Classification.class)
	@NotNull(groups = OnCreate.class)
	@Valid
	@ValidClassification
	private Classification classification;

	@Schema(description = "Status for the errand", example = "NEW_CASE")
	@NotBlank(groups = OnCreate.class)
	@ValidStatus
	private String status;

	@Schema(description = "Resolution status for closed errands. Value can be set to anything", example = "FIXED")
	private String resolution;

	@Schema(description = "Errand description text", example = "Order cake for everyone")
	private String description;

	@Schema(description = "User id for the person which has created the errand", example = "joe01doe")
	@NotBlank(groups = OnCreate.class)
	@Null(groups = OnUpdate.class)
	private String reporterUserId;

	@Schema(description = "Id for the user which currently is assigned to the errand if a user is assigned", example = "joe01doe")
	private String assignedUserId;

	@Schema(description = "Id for the group which is currently assigned to the errand if a group is assigned", example = "hardware support")
	private String assignedGroupId;

	@Schema(description = "Email address used for escalation of errand", example = "joe.doe@email.com")
	@Email
	private String escalationEmail;

	@Schema(description = "Contact reason for the errand", example = "The printer is not working")
	@ValidContactReason(groups = {OnCreate.class, OnUpdate.class}, nullable = true)
	private String contactReason;

	@Schema(description = "Suspension information")
	@Valid
	private Suspension suspension;

	@Schema(description = "Flag to indicate if the errand is business related", example = "true")
	@NotNull(groups = {OnCreate.class, OnUpdate.class})
	private Boolean businessRelated;

	@Schema(description = "Timestamp when errand was created", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null(groups = {OnCreate.class, OnUpdate.class})
	private OffsetDateTime created;

	@Schema(description = "Timestamp when errand was last modified", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null(groups = {OnCreate.class, OnUpdate.class})
	private OffsetDateTime modified;

	@Schema(description = "Timestamp when errand was last touched (created or modified)", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null(groups = {OnCreate.class, OnUpdate.class})
	private OffsetDateTime touched;

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

	public List<ErrandParameter> getParameters() {
		return parameters;
	}

	public void setParameters(final List<ErrandParameter> parameters) {
		this.parameters = parameters;
	}

	public Errand withParameters(final List<ErrandParameter> parameters) {
		this.parameters = parameters;
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

	public List<Stakeholder> getStakeholders() {
		return stakeholders;
	}

	public void setStakeholders(List<Stakeholder> stakeholders) {
		this.stakeholders = stakeholders;
	}

	public Errand withStakeholders(List<Stakeholder> stakeholders) {
		this.stakeholders = stakeholders;
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

	public Classification getClassification() {
		return classification;
	}

	public void setClassification(Classification classification) {
		this.classification = classification;
	}

	public Errand withClassification(Classification classification) {
		this.classification = classification;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Errand withStatus(String status) {
		this.status = status;
		return this;
	}

	public String getResolution() {
		return resolution;
	}

	public void setResolution(String resolution) {
		this.resolution = resolution;
	}

	public Errand withResolution(String resolution) {
		this.resolution = resolution;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Errand withDescription(String description) {
		this.description = description;
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

	public String getEscalationEmail() {
		return escalationEmail;
	}

	public void setEscalationEmail(String escalationEmail) {
		this.escalationEmail = escalationEmail;
	}

	public Errand withEscalationEmail(String escalationEmail) {
		this.escalationEmail = escalationEmail;
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

	public OffsetDateTime getTouched() {
		return touched;
	}

	public void setTouched(OffsetDateTime touched) {
		this.touched = touched;
	}

	public Errand withTouched(OffsetDateTime touched) {
		this.touched = touched;
		return this;
	}

	public String getErrandNumber() {
		return errandNumber;
	}

	public void setErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
	}

	public Errand withErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
		return this;
	}

	public String getContactReason() {
		return contactReason;
	}

	public void setContactReason(final String contactReason) {
		this.contactReason = contactReason;
	}

	public Errand withContactReason(final String contactReason) {
		this.contactReason = contactReason;
		return this;
	}

	public Boolean getBusinessRelated() {
		return businessRelated;
	}

	public void setBusinessRelated(final Boolean businessRelated) {
		this.businessRelated = businessRelated;
	}

	public Errand withBusinessRelated(final Boolean businessRelated) {
		this.businessRelated = businessRelated;
		return this;
	}

	public Suspension getSuspension() {
		return suspension;
	}

	public void setSuspension(final Suspension suspension) {
		this.suspension = suspension;
	}

	public Errand withSuspension(final Suspension suspension) {
		this.suspension = suspension;
		return this;
	}

	@Override
	public String toString() {
		return "Errand{" +
			"id='" + id + '\'' +
			", errandNumber='" + errandNumber + '\'' +
			", title='" + title + '\'' +
			", priority=" + priority +
			", stakeholders=" + stakeholders +
			", externalTags=" + externalTags +
			", parameters=" + parameters +
			", classification=" + classification +
			", status='" + status + '\'' +
			", resolution='" + resolution + '\'' +
			", description='" + description + '\'' +
			", reporterUserId='" + reporterUserId + '\'' +
			", assignedUserId='" + assignedUserId + '\'' +
			", assignedGroupId='" + assignedGroupId + '\'' +
			", escalationEmail='" + escalationEmail + '\'' +
			", contactReason='" + contactReason + '\'' +
			", suspension=" + suspension +
			", businessRelated=" + businessRelated +
			", created=" + created +
			", modified=" + modified +
			", touched=" + touched +
			'}';
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final Errand errand = (Errand) o;
		return Objects.equals(id, errand.id) && Objects.equals(errandNumber, errand.errandNumber) && Objects.equals(title, errand.title) && priority == errand.priority && Objects.equals(stakeholders, errand.stakeholders) && Objects.equals(externalTags, errand.externalTags) && Objects.equals(parameters, errand.parameters) && Objects.equals(classification, errand.classification) && Objects.equals(status, errand.status) && Objects.equals(resolution, errand.resolution) && Objects.equals(description, errand.description) && Objects.equals(reporterUserId, errand.reporterUserId) && Objects.equals(assignedUserId, errand.assignedUserId) && Objects.equals(assignedGroupId, errand.assignedGroupId) && Objects.equals(escalationEmail, errand.escalationEmail) && Objects.equals(contactReason, errand.contactReason) && Objects.equals(suspension, errand.suspension) && Objects.equals(businessRelated, errand.businessRelated) && Objects.equals(created, errand.created) && Objects.equals(modified, errand.modified) && Objects.equals(touched, errand.touched);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandNumber, title, priority, stakeholders, externalTags, parameters, classification, status, resolution, description, reporterUserId, assignedUserId, assignedGroupId, escalationEmail, contactReason, suspension, businessRelated, created, modified, touched);
	}
}
