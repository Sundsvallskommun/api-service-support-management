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

import se.sundsvall.supportmanagement.api.validation.UniqueExternalTagKeys;
import se.sundsvall.supportmanagement.api.validation.ValidClassification;
import se.sundsvall.supportmanagement.api.validation.ValidStatus;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Errand model")
public class Errand {

	@Schema(description = "Unique id for the errand", example = "f0882f1d-06bc-47fd-b017-1d8307f5ce95", accessMode = READ_ONLY)
	@Null(groups = { OnCreate.class, OnUpdate.class })
	private String id;

	@Schema(description = "Unique number for the errand", example = "KC-23010001", accessMode = READ_ONLY)
	@Null(groups = {OnCreate.class, OnUpdate.class})
	private String errandNumber;

	@Schema(description = "Title for the errand", example = "Title for the errand")
	@NotBlank(groups = OnCreate.class)
	private String title;

	@Schema(implementation = Priority.class)
	@NotNull(groups = OnCreate.class)
	private Priority priority;

	@ArraySchema(schema = @Schema(implementation = Stakeholder.class), uniqueItems = true)
	private List<Stakeholder> stakeholders;

	@ArraySchema(schema = @Schema(implementation = ExternalTag.class), uniqueItems = true)
	@UniqueExternalTagKeys
	@Valid
	private List<ExternalTag> externalTags;

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

	@Schema(description = "Timestamp when errand was created", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null(groups = { OnCreate.class, OnUpdate.class })
	private OffsetDateTime created;

	@Schema(description = "Timestamp when errand was last modified", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null(groups = { OnCreate.class, OnUpdate.class })
	private OffsetDateTime modified;

	@Schema(description = "Timestamp when errand was last touched (created or modified)", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null(groups = { OnCreate.class, OnUpdate.class })
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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Errand errand = (Errand) o;
		return Objects.equals(id, errand.id) && Objects.equals(title, errand.title) && priority == errand.priority &&
			Objects.equals(stakeholders, errand.stakeholders) && Objects.equals(externalTags, errand.externalTags) &&
			Objects.equals(classification, errand.classification) && Objects.equals(status, errand.status) &&
			Objects.equals(resolution, errand.resolution) && Objects.equals(description, errand.description) &&
			Objects.equals(reporterUserId, errand.reporterUserId) && Objects.equals(assignedUserId, errand.assignedUserId) &&
			Objects.equals(assignedGroupId, errand.assignedGroupId) && Objects.equals(escalationEmail, errand.escalationEmail) &&
			Objects.equals(created, errand.created) && Objects.equals(modified, errand.modified) &&
			Objects.equals(touched, errand.touched) && Objects.equals(errandNumber, errand.errandNumber);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandNumber, title, priority, stakeholders, externalTags, classification, status, resolution, description, reporterUserId, assignedUserId, assignedGroupId, escalationEmail, created, modified, touched);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Errand{");
		sb.append("id='").append(id).append('\'');
		sb.append(", errandNumber='").append(errandNumber).append('\'');
		sb.append(", title='").append(title).append('\'');
		sb.append(", priority=").append(priority);
		sb.append(", stakeholders=").append(stakeholders);
		sb.append(", externalTags=").append(externalTags);
		sb.append(", classification='").append(classification).append('\'');
		sb.append(", statusTag='").append(status).append('\'');
		sb.append(", resolution='").append(resolution).append('\'');
		sb.append(", description='").append(description).append('\'');
		sb.append(", reporterUserId='").append(reporterUserId).append('\'');
		sb.append(", assignedUserId='").append(assignedUserId).append('\'');
		sb.append(", assignedGroupId='").append(assignedGroupId).append('\'');
		sb.append(", escalationEmail='").append(escalationEmail).append('\'');
		sb.append(", created=").append(created);
		sb.append(", modified=").append(modified);
		sb.append(", touched=").append(touched);
		sb.append('}');
		return sb.toString();
	}
}
