package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import se.sundsvall.dept44.common.validators.annotation.OneOf;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * The required fields are demanded of every measure that is not being patched on its own resource.
 * <p>
 * The OnUpdate group, used when a single measure is patched, omits the required fields. Everywhere else - creating a
 * measure, and carrying measures on the errand - a measure has to be complete. The accept value is checked wherever it
 * is supplied.
 */
@Schema(description = "Measure model")
public class Measure {

	@Schema(description = "Measure ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "Responsible user (ad-username)", examples = "jo12doe")
	private String responsibleUser;

	@Schema(description = "Type of measure", examples = "INTERVENTION")
	@NotBlank(groups = {
		Default.class, OnCreate.class
	})
	private String type;

	@Schema(description = "Planned start date", examples = "2021-09-01T12:00:00Z")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime plannedStart;

	@Schema(description = "Planned completion date", examples = "2021-10-01T12:00:00Z")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime plannedComplete;

	@Schema(description = "Execution date", examples = "2021-09-15T12:00:00Z")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime executed;

	@Schema(description = "User who added the measure", examples = "jo12doe")
	@NotBlank(groups = {
		Default.class, OnCreate.class
	})
	private String addedByUser;

	@Schema(description = "Role of the user who added the measure", examples = "MANAGER")
	@NotBlank(groups = {
		Default.class, OnCreate.class
	})
	private String addedByRole;

	@Schema(description = "Goal of the measure", examples = "Improve response time")
	@Size(max = 3000)
	private String goal;

	@Schema(description = "Description of the measure", examples = "Detailed description of the measure")
	@Size(max = 3000)
	private String description;

	@Schema(description = "Accept status", examples = "TRUE", nullable = true)
	@OneOf(value = {
		"TRUE", "FALSE", "REWORK"
	}, nullable = true, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String accept;

	@Schema(description = "Motivation for the accept decision", examples = "The measure is approved")
	private String acceptMotivation;

	@Schema(description = "Timestamp when the measure was created", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the measure was last modified", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime modified;

	@Schema(description = "Life cycle status. Defaults to ACTIVE when omitted", examples = "ACTIVE")
	@OneOf(value = {
		"DRAFT", "ACTIVE", "COMPLETED", "CANCELLED"
	}, nullable = true, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String status;

	@Schema(description = "Heading of the measure", maxLength = 255, examples = "Installera brandvarnare")
	@Size(max = 255, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String title;

	@Schema(description = "Deadline for the measure", examples = "2021-10-01T12:00:00Z")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime dueAt;

	@Schema(description = "Timestamp when the measure was concluded", examples = "2021-10-01T12:00:00Z")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime completedAt;

	@Schema(description = "Outcome once the measure has been carried out", examples = "COMPLETED", nullable = true)
	@OneOf(value = {
		"COMPLETED", "PARTIALLY_COMPLETED", "NOT_COMPLETED", "NOT_APPLICABLE"
	}, nullable = true, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String result;

	@Schema(description = "Description of the outcome", examples = "Brandvarnare installerad och kontrollerad.")
	private String resultText;

	@Schema(description = "Id of the decision the measure follows from", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", nullable = true)
	@ValidUuid(nullable = true)
	private String decisionId;

	@Schema(description = "Id of the statement the measure follows from", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", nullable = true)
	@ValidUuid(nullable = true)
	private String statementId;

	@ArraySchema(schema = @Schema(implementation = ErrandAttachment.class, accessMode = READ_ONLY),
		arraySchema = @Schema(description = "Attachments of the errand linked to this measure. Filled in by the measure resource and left out where the measure is part of the errand"))
	private List<ErrandAttachment> attachments;

	@Schema(description = "User who created the measure", examples = "jo12doe", accessMode = READ_ONLY)
	private String createdBy;

	@Schema(description = "User who last modified the measure", examples = "jo12doe", accessMode = READ_ONLY)
	private String modifiedBy;

	@Schema(description = "Version of the measure, carried as the ETag of the resource", examples = "0", accessMode = READ_ONLY)
	private Long version;

	public static Measure create() {
		return new Measure();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Measure withId(final String id) {
		this.id = id;
		return this;
	}

	public String getResponsibleUser() {
		return responsibleUser;
	}

	public void setResponsibleUser(final String responsibleUser) {
		this.responsibleUser = responsibleUser;
	}

	public Measure withResponsibleUser(final String responsibleUser) {
		this.responsibleUser = responsibleUser;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public Measure withType(final String type) {
		this.type = type;
		return this;
	}

	public OffsetDateTime getPlannedStart() {
		return plannedStart;
	}

	public void setPlannedStart(final OffsetDateTime plannedStart) {
		this.plannedStart = plannedStart;
	}

	public Measure withPlannedStart(final OffsetDateTime plannedStart) {
		this.plannedStart = plannedStart;
		return this;
	}

	public OffsetDateTime getPlannedComplete() {
		return plannedComplete;
	}

	public void setPlannedComplete(final OffsetDateTime plannedComplete) {
		this.plannedComplete = plannedComplete;
	}

	public Measure withPlannedComplete(final OffsetDateTime plannedComplete) {
		this.plannedComplete = plannedComplete;
		return this;
	}

	public OffsetDateTime getExecuted() {
		return executed;
	}

	public void setExecuted(final OffsetDateTime executed) {
		this.executed = executed;
	}

	public Measure withExecuted(final OffsetDateTime executed) {
		this.executed = executed;
		return this;
	}

	public String getAddedByUser() {
		return addedByUser;
	}

	public void setAddedByUser(final String addedByUser) {
		this.addedByUser = addedByUser;
	}

	public Measure withAddedByUser(final String addedByUser) {
		this.addedByUser = addedByUser;
		return this;
	}

	public String getAddedByRole() {
		return addedByRole;
	}

	public void setAddedByRole(final String addedByRole) {
		this.addedByRole = addedByRole;
	}

	public Measure withAddedByRole(final String addedByRole) {
		this.addedByRole = addedByRole;
		return this;
	}

	public String getGoal() {
		return goal;
	}

	public void setGoal(final String goal) {
		this.goal = goal;
	}

	public Measure withGoal(final String goal) {
		this.goal = goal;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public Measure withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getAccept() {
		return accept;
	}

	public void setAccept(final String accept) {
		this.accept = accept;
	}

	public Measure withAccept(final String accept) {
		this.accept = accept;
		return this;
	}

	public String getAcceptMotivation() {
		return acceptMotivation;
	}

	public void setAcceptMotivation(final String acceptMotivation) {
		this.acceptMotivation = acceptMotivation;
	}

	public Measure withAcceptMotivation(final String acceptMotivation) {
		this.acceptMotivation = acceptMotivation;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Measure withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Measure withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public Measure withStatus(final String status) {
		this.status = status;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public Measure withTitle(final String title) {
		this.title = title;
		return this;
	}

	public OffsetDateTime getDueAt() {
		return dueAt;
	}

	public void setDueAt(final OffsetDateTime dueAt) {
		this.dueAt = dueAt;
	}

	public Measure withDueAt(final OffsetDateTime dueAt) {
		this.dueAt = dueAt;
		return this;
	}

	public OffsetDateTime getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(final OffsetDateTime completedAt) {
		this.completedAt = completedAt;
	}

	public Measure withCompletedAt(final OffsetDateTime completedAt) {
		this.completedAt = completedAt;
		return this;
	}

	public String getResult() {
		return result;
	}

	public void setResult(final String result) {
		this.result = result;
	}

	public Measure withResult(final String result) {
		this.result = result;
		return this;
	}

	public String getResultText() {
		return resultText;
	}

	public void setResultText(final String resultText) {
		this.resultText = resultText;
	}

	public Measure withResultText(final String resultText) {
		this.resultText = resultText;
		return this;
	}

	public String getDecisionId() {
		return decisionId;
	}

	public void setDecisionId(final String decisionId) {
		this.decisionId = decisionId;
	}

	public Measure withDecisionId(final String decisionId) {
		this.decisionId = decisionId;
		return this;
	}

	public String getStatementId() {
		return statementId;
	}

	public void setStatementId(final String statementId) {
		this.statementId = statementId;
	}

	public Measure withStatementId(final String statementId) {
		this.statementId = statementId;
		return this;
	}

	public List<ErrandAttachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(final List<ErrandAttachment> attachments) {
		this.attachments = attachments;
	}

	public Measure withAttachments(final List<ErrandAttachment> attachments) {
		this.attachments = attachments;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public Measure withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public Measure withModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(final Long version) {
		this.version = version;
	}

	public Measure withVersion(final Long version) {
		this.version = version;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, responsibleUser, type, plannedStart, plannedComplete, executed, addedByUser, addedByRole, goal, description, accept, acceptMotivation, created, modified, status, title, dueAt, completedAt,
			result, resultText, decisionId, statementId, attachments, createdBy, modifiedBy, version);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Measure other)) {
			return false;
		}
		return Objects.equals(id, other.id)
			&& Objects.equals(responsibleUser, other.responsibleUser)
			&& Objects.equals(type, other.type)
			&& Objects.equals(plannedStart, other.plannedStart)
			&& Objects.equals(plannedComplete, other.plannedComplete)
			&& Objects.equals(executed, other.executed)
			&& Objects.equals(addedByUser, other.addedByUser)
			&& Objects.equals(addedByRole, other.addedByRole)
			&& Objects.equals(goal, other.goal)
			&& Objects.equals(description, other.description)
			&& Objects.equals(accept, other.accept)
			&& Objects.equals(acceptMotivation, other.acceptMotivation)
			&& Objects.equals(created, other.created)
			&& Objects.equals(modified, other.modified)
			&& Objects.equals(status, other.status)
			&& Objects.equals(title, other.title)
			&& Objects.equals(dueAt, other.dueAt)
			&& Objects.equals(completedAt, other.completedAt)
			&& Objects.equals(result, other.result)
			&& Objects.equals(resultText, other.resultText)
			&& Objects.equals(decisionId, other.decisionId)
			&& Objects.equals(statementId, other.statementId)
			&& Objects.equals(attachments, other.attachments)
			&& Objects.equals(createdBy, other.createdBy)
			&& Objects.equals(modifiedBy, other.modifiedBy)
			&& Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		return "Measure{" +
			"id='" + id + '\'' +
			", responsibleUser='" + responsibleUser + '\'' +
			", type='" + type + '\'' +
			", plannedStart=" + plannedStart +
			", plannedComplete=" + plannedComplete +
			", executed=" + executed +
			", addedByUser='" + addedByUser + '\'' +
			", addedByRole='" + addedByRole + '\'' +
			", goal='" + goal + '\'' +
			", description='" + description + '\'' +
			", accept='" + accept + '\'' +
			", acceptMotivation='" + acceptMotivation + '\'' +
			", created=" + created +
			", modified=" + modified +
			", status='" + status + '\'' +
			", title='" + title + '\'' +
			", dueAt=" + dueAt +
			", completedAt=" + completedAt +
			", result='" + result + '\'' +
			", resultText='" + resultText + '\'' +
			", decisionId='" + decisionId + '\'' +
			", statementId='" + statementId + '\'' +
			", attachments=" + attachments +
			", createdBy='" + createdBy + '\'' +
			", modifiedBy='" + modifiedBy + '\'' +
			", version=" + version +
			'}';
	}
}
