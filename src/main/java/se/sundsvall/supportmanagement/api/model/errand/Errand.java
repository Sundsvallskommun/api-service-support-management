package se.sundsvall.supportmanagement.api.model.errand;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import se.sundsvall.supportmanagement.api.model.notification.Notification;
import se.sundsvall.supportmanagement.api.validation.UniqueExternalTagKeys;
import se.sundsvall.supportmanagement.api.validation.ValidClassification;
import se.sundsvall.supportmanagement.api.validation.ValidContactReason;
import se.sundsvall.supportmanagement.api.validation.ValidStatus;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;

@Schema(description = "Errand model")
public class Errand {

	@Schema(description = "Unique id for the errand", example = "f0882f1d-06bc-47fd-b017-1d8307f5ce95", accessMode = READ_ONLY)
	@Null(groups = {
		OnCreate.class, OnUpdate.class
	})
	private String id;

	@Schema(description = "Unique number for the errand", example = "KC-23010001", accessMode = READ_ONLY)
	@Null(groups = {
		OnCreate.class, OnUpdate.class
	})
	private String errandNumber;

	@Schema(description = "Title for the errand", example = "Title for the errand")
	@NotBlank(groups = OnCreate.class)
	private String title;

	@Schema(implementation = Priority.class)
	@NotNull(groups = OnCreate.class)
	@ArraySchema(schema = @Schema(implementation = Stakeholder.class), uniqueItems = true)
	private Priority priority;

	@Valid
	private List<Stakeholder> stakeholders;

	@ArraySchema(schema = @Schema(implementation = ExternalTag.class), uniqueItems = true)
	@UniqueExternalTagKeys(groups = OnCreate.class)
	@Valid
	private List<ExternalTag> externalTags;

	@Schema(description = "Parameters for the errand")
	@Valid
	private List<Parameter> parameters;

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

	@Schema(description = "The channel from which the errand originated", maxLength = 255, example = "THE_CHANNEL")
	@Size(max = 255)
	private String channel;

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
	@ValidContactReason(groups = {
		OnCreate.class, OnUpdate.class
	}, nullable = true)
	private String contactReason;

	@Schema(description = "Contact reason description for the errand", maxLength = 4096, example = "The printer is not working since the power cord is missing")
	@Size(max = 4096)
	private String contactReasonDescription;

	@Schema(description = "Suspension information")
	@Valid
	private Suspension suspension;

	@Schema(description = "Flag to indicate if the errand is business related", example = "true")
	private Boolean businessRelated;

	@Schema(description = "List of labels for the errand", example = "[\"label1\",\"label2\"]")
	private List<String> labels;

	@Schema(description = "List of active notifications for the errand", accessMode = READ_ONLY)
	@Null(groups = {
		OnCreate.class, OnUpdate.class
	})
	private List<Notification> notifications;

	@Schema(description = "Timestamp when errand was created", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null(groups = {
		OnCreate.class, OnUpdate.class
	})
	private OffsetDateTime created;

	@Schema(description = "Timestamp when errand was last modified", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null(groups = {
		OnCreate.class, OnUpdate.class
	})
	private OffsetDateTime modified;

	@Schema(description = "Timestamp when errand was last touched (created or modified)", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null(groups = {
		OnCreate.class, OnUpdate.class
	})
	private OffsetDateTime touched;

	public static Errand create() {
		return new Errand();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Errand withId(final String id) {
		this.id = id;
		return this;
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

	public void setParameters(final List<Parameter> parameters) {
		this.parameters = parameters;
	}

	public Errand withParameters(final List<Parameter> parameters) {
		this.parameters = parameters;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public Errand withTitle(final String title) {
		this.title = title;
		return this;
	}

	public Priority getPriority() {
		return priority;
	}

	public void setPriority(final Priority priority) {
		this.priority = priority;
	}

	public Errand withPriority(final Priority priority) {
		this.priority = priority;
		return this;
	}

	public List<Stakeholder> getStakeholders() {
		return stakeholders;
	}

	public void setStakeholders(final List<Stakeholder> stakeholders) {
		this.stakeholders = stakeholders;
	}

	public Errand withStakeholders(final List<Stakeholder> stakeholders) {
		this.stakeholders = stakeholders;
		return this;
	}

	public List<ExternalTag> getExternalTags() {
		return externalTags;
	}

	public void setExternalTags(final List<ExternalTag> externalTags) {
		this.externalTags = externalTags;
	}

	public Errand withExternalTags(final List<ExternalTag> externalTags) {
		this.externalTags = externalTags;
		return this;
	}

	public Classification getClassification() {
		return classification;
	}

	public void setClassification(final Classification classification) {
		this.classification = classification;
	}

	public Errand withClassification(final Classification classification) {
		this.classification = classification;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public Errand withStatus(final String status) {
		this.status = status;
		return this;
	}

	public String getResolution() {
		return resolution;
	}

	public void setResolution(final String resolution) {
		this.resolution = resolution;
	}

	public Errand withResolution(final String resolution) {
		this.resolution = resolution;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public Errand withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(final String channel) {
		this.channel = channel;
	}

	public Errand withChannel(final String channel) {
		this.channel = channel;
		return this;
	}

	public String getReporterUserId() {
		return reporterUserId;
	}

	public void setReporterUserId(final String reporterUserId) {
		this.reporterUserId = reporterUserId;
	}

	public Errand withReporterUserId(final String reporterUserId) {
		this.reporterUserId = reporterUserId;
		return this;
	}

	public String getAssignedUserId() {
		return assignedUserId;
	}

	public void setAssignedUserId(final String assignedUserId) {
		this.assignedUserId = assignedUserId;
	}

	public Errand withAssignedUserId(final String assignedUserId) {
		this.assignedUserId = assignedUserId;
		return this;
	}

	public String getAssignedGroupId() {
		return assignedGroupId;
	}

	public void setAssignedGroupId(final String assignedGroupId) {
		this.assignedGroupId = assignedGroupId;
	}

	public Errand withAssignedGroupId(final String assignedGroupId) {
		this.assignedGroupId = assignedGroupId;
		return this;
	}

	public String getEscalationEmail() {
		return escalationEmail;
	}

	public void setEscalationEmail(final String escalationEmail) {
		this.escalationEmail = escalationEmail;
	}

	public Errand withEscalationEmail(final String escalationEmail) {
		this.escalationEmail = escalationEmail;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Errand withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Errand withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public OffsetDateTime getTouched() {
		return touched;
	}

	public void setTouched(final OffsetDateTime touched) {
		this.touched = touched;
	}

	public Errand withTouched(final OffsetDateTime touched) {
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

	public String getContactReasonDescription() {
		return contactReasonDescription;
	}

	public void setContactReasonDescription(final String contactReasonDescription) {
		this.contactReasonDescription = contactReasonDescription;
	}

	public Errand withContactReasonDescription(final String contactReasonDescription) {
		this.contactReasonDescription = contactReasonDescription;
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

	public List<String> getLabels() {
		return labels;
	}

	public void setLabels(final List<String> labels) {
		this.labels = labels;
	}

	public Errand withLabels(final List<String> labels) {
		this.labels = labels;
		return this;
	}

	public List<Notification> getNotifications() {
		return notifications;
	}

	public void setNotifications(final List<Notification> notifications) {
		this.notifications = notifications;
	}

	public Errand withNotifications(final List<Notification> notifications) {
		this.notifications = notifications;
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
	public int hashCode() {
		return Objects.hash(assignedGroupId, assignedUserId, businessRelated, channel, classification, contactReason, contactReasonDescription, created, description, errandNumber, escalationEmail, externalTags, id, labels, notifications, modified,
			parameters, priority, reporterUserId, resolution, stakeholders, status, suspension, title, touched);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Errand other)) {
			return false;
		}
		return Objects.equals(assignedGroupId, other.assignedGroupId) && Objects.equals(assignedUserId, other.assignedUserId) && Objects.equals(businessRelated, other.businessRelated) && Objects.equals(channel, other.channel) && Objects.equals(
			classification, other.classification) && Objects.equals(contactReason, other.contactReason) && Objects.equals(contactReasonDescription, other.contactReasonDescription) && Objects.equals(created, other.created) && Objects.equals(description,
				other.description) && Objects.equals(errandNumber, other.errandNumber) && Objects.equals(escalationEmail, other.escalationEmail) && Objects.equals(externalTags, other.externalTags) && Objects.equals(id, other.id) && Objects.equals(labels,
					other.labels) && Objects.equals(notifications, other.notifications) && Objects.equals(modified, other.modified) && Objects.equals(parameters, other.parameters) && (priority == other.priority) && Objects.equals(reporterUserId,
						other.reporterUserId) && Objects.equals(resolution, other.resolution) && Objects.equals(stakeholders, other.stakeholders) && Objects.equals(status, other.status) && Objects.equals(suspension, other.suspension) && Objects.equals(
							title, other.title)
			&& Objects.equals(touched, other.touched);
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
			", channel='" + channel + '\'' +
			", reporterUserId='" + reporterUserId + '\'' +
			", assignedUserId='" + assignedUserId + '\'' +
			", assignedGroupId='" + assignedGroupId + '\'' +
			", escalationEmail='" + escalationEmail + '\'' +
			", contactReason='" + contactReason + '\'' +
			", contactReasonDescription='" + contactReasonDescription + '\'' +
			", suspension=" + suspension +
			", businessRelated=" + businessRelated +
			", labels=" + labels +
			", notifications=" + notifications +
			", created=" + created +
			", modified=" + modified +
			", touched=" + touched +
			'}';
	}

}
