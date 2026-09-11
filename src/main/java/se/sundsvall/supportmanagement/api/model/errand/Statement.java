package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import se.sundsvall.dept44.common.validators.annotation.OneOf;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * A request for a statement and the statement that came back.
 * <p>
 * The required fields are demanded on creation only. Patching says nothing about the fields it leaves out, which is why
 * the OnUpdate group omits them - but every value that is supplied is checked in both, since an unknown one would
 * otherwise reach the mapper and surface as a 500 rather than the bad request it is.
 */
@Schema(description = "Statement model")
public class Statement {

	@Schema(description = "Statement ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "Type of statement, as registered for the namespace", maxLength = 128, examples = "REFERRAL")
	@Size(max = 128, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String type;

	@Schema(description = "Life cycle status", examples = "DRAFT")
	@NotNull(groups = {
		Default.class, OnCreate.class
	})
	@OneOf(value = {
		"DRAFT", "ACTIVE", "COMPLETED", "CANCELLED"
	}, nullable = true, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String status;

	@Schema(description = "Heading of the statement", maxLength = 255, examples = "Remiss till miljökontoret")
	@Size(max = 255, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String title;

	@Schema(description = "Description of the statement", examples = "Remiss avseende serveringstillstånd")
	private String description;

	@Schema(description = "Deadline for the response", examples = "2021-10-01T12:00:00Z")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime dueAt;

	@Schema(description = "Timestamp when the statement was concluded", examples = "2021-10-01T12:00:00Z")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime completedAt;

	@Schema(description = "Name of the counterparty asked for a statement", maxLength = 255, examples = "Miljökontoret")
	@NotBlank(groups = {
		Default.class, OnCreate.class
	})
	@Size(max = 255, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String counterpartyName;

	@Schema(description = "Identity of the counterparty", maxLength = 255, examples = "2120002411")
	@Size(max = 255, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String counterpartyExternalId;

	@Schema(description = "Type of the counterparty identity, as registered for the namespace", maxLength = 128, examples = "ORGANIZATION_NUMBER")
	@Size(max = 128, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String counterpartyExternalIdType;

	@Schema(description = "Reference number of the counterparty, for cross reference in their system", maxLength = 128, examples = "MK-2021-0042")
	@Size(max = 128, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String counterpartyReference;

	@Schema(description = "The question being asked", examples = "Finns det hinder mot serveringstillstånd på adressen?")
	private String question;

	@Schema(description = "Timestamp when the statement was sent", examples = "2021-09-01T12:00:00Z")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime sentAt;

	@Schema(description = "Timestamp of the most recent reminder", examples = "2021-09-20T12:00:00Z")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime remindedAt;

	@Schema(description = "Timestamp when the response was registered", examples = "2021-09-28T12:00:00Z")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime respondedAt;

	@Schema(description = "Outcome of the response", examples = "SUPPORTS", nullable = true)
	@OneOf(value = {
		"SUPPORTS", "SUPPORTS_WITH_CONDITIONS", "NO_OBJECTION", "OPPOSES", "NOT_APPLICABLE", "NO_RESPONSE"
	}, nullable = true, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String outcome;

	@Schema(description = "The response text", examples = "Miljökontoret har inget att erinra.")
	private String responseText;

	@Schema(description = "Id of the communication that carried the statement", maxLength = 36, examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7")
	@Size(max = 36, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String communicationId;

	@ArraySchema(schema = @Schema(implementation = ArtefactAttachment.class, accessMode = READ_ONLY),
		arraySchema = @Schema(description = "Attachments of the errand linked to this statement"))
	private List<ArtefactAttachment> attachments;

	@Schema(description = "User who created the statement", examples = "jo12doe", accessMode = READ_ONLY)
	private String createdBy;

	@Schema(description = "User who last modified the statement", examples = "jo12doe", accessMode = READ_ONLY)
	private String modifiedBy;

	@Schema(description = "Timestamp when the statement was created", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the statement was last modified", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime modified;

	@Schema(description = "Version of the statement, carried as the ETag of the resource", examples = "0", accessMode = READ_ONLY)
	private Long version;

	public static Statement create() {
		return new Statement();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Statement withId(final String id) {
		this.id = id;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public Statement withType(final String type) {
		this.type = type;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public Statement withStatus(final String status) {
		this.status = status;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public Statement withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public Statement withDescription(final String description) {
		this.description = description;
		return this;
	}

	public OffsetDateTime getDueAt() {
		return dueAt;
	}

	public void setDueAt(final OffsetDateTime dueAt) {
		this.dueAt = dueAt;
	}

	public Statement withDueAt(final OffsetDateTime dueAt) {
		this.dueAt = dueAt;
		return this;
	}

	public OffsetDateTime getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(final OffsetDateTime completedAt) {
		this.completedAt = completedAt;
	}

	public Statement withCompletedAt(final OffsetDateTime completedAt) {
		this.completedAt = completedAt;
		return this;
	}

	public String getCounterpartyName() {
		return counterpartyName;
	}

	public void setCounterpartyName(final String counterpartyName) {
		this.counterpartyName = counterpartyName;
	}

	public Statement withCounterpartyName(final String counterpartyName) {
		this.counterpartyName = counterpartyName;
		return this;
	}

	public String getCounterpartyExternalId() {
		return counterpartyExternalId;
	}

	public void setCounterpartyExternalId(final String counterpartyExternalId) {
		this.counterpartyExternalId = counterpartyExternalId;
	}

	public Statement withCounterpartyExternalId(final String counterpartyExternalId) {
		this.counterpartyExternalId = counterpartyExternalId;
		return this;
	}

	public String getCounterpartyExternalIdType() {
		return counterpartyExternalIdType;
	}

	public void setCounterpartyExternalIdType(final String counterpartyExternalIdType) {
		this.counterpartyExternalIdType = counterpartyExternalIdType;
	}

	public Statement withCounterpartyExternalIdType(final String counterpartyExternalIdType) {
		this.counterpartyExternalIdType = counterpartyExternalIdType;
		return this;
	}

	public String getCounterpartyReference() {
		return counterpartyReference;
	}

	public void setCounterpartyReference(final String counterpartyReference) {
		this.counterpartyReference = counterpartyReference;
	}

	public Statement withCounterpartyReference(final String counterpartyReference) {
		this.counterpartyReference = counterpartyReference;
		return this;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(final String question) {
		this.question = question;
	}

	public Statement withQuestion(final String question) {
		this.question = question;
		return this;
	}

	public OffsetDateTime getSentAt() {
		return sentAt;
	}

	public void setSentAt(final OffsetDateTime sentAt) {
		this.sentAt = sentAt;
	}

	public Statement withSentAt(final OffsetDateTime sentAt) {
		this.sentAt = sentAt;
		return this;
	}

	public OffsetDateTime getRemindedAt() {
		return remindedAt;
	}

	public void setRemindedAt(final OffsetDateTime remindedAt) {
		this.remindedAt = remindedAt;
	}

	public Statement withRemindedAt(final OffsetDateTime remindedAt) {
		this.remindedAt = remindedAt;
		return this;
	}

	public OffsetDateTime getRespondedAt() {
		return respondedAt;
	}

	public void setRespondedAt(final OffsetDateTime respondedAt) {
		this.respondedAt = respondedAt;
	}

	public Statement withRespondedAt(final OffsetDateTime respondedAt) {
		this.respondedAt = respondedAt;
		return this;
	}

	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(final String outcome) {
		this.outcome = outcome;
	}

	public Statement withOutcome(final String outcome) {
		this.outcome = outcome;
		return this;
	}

	public String getResponseText() {
		return responseText;
	}

	public void setResponseText(final String responseText) {
		this.responseText = responseText;
	}

	public Statement withResponseText(final String responseText) {
		this.responseText = responseText;
		return this;
	}

	public String getCommunicationId() {
		return communicationId;
	}

	public void setCommunicationId(final String communicationId) {
		this.communicationId = communicationId;
	}

	public Statement withCommunicationId(final String communicationId) {
		this.communicationId = communicationId;
		return this;
	}

	public List<ArtefactAttachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(final List<ArtefactAttachment> attachments) {
		this.attachments = attachments;
	}

	public Statement withAttachments(final List<ArtefactAttachment> attachments) {
		this.attachments = attachments;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public Statement withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public Statement withModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Statement withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Statement withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(final Long version) {
		this.version = version;
	}

	public Statement withVersion(final Long version) {
		this.version = version;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, type, status, title, description, dueAt, completedAt, counterpartyName, counterpartyExternalId, counterpartyExternalIdType, counterpartyReference, question, sentAt,
			remindedAt, respondedAt, outcome, responseText, communicationId, attachments, createdBy, modifiedBy, created, modified, version);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Statement other)) {
			return false;
		}
		return Objects.equals(id, other.id)
			&& Objects.equals(type, other.type)
			&& Objects.equals(status, other.status)
			&& Objects.equals(title, other.title)
			&& Objects.equals(description, other.description)
			&& Objects.equals(dueAt, other.dueAt)
			&& Objects.equals(completedAt, other.completedAt)
			&& Objects.equals(counterpartyName, other.counterpartyName)
			&& Objects.equals(counterpartyExternalId, other.counterpartyExternalId)
			&& Objects.equals(counterpartyExternalIdType, other.counterpartyExternalIdType)
			&& Objects.equals(counterpartyReference, other.counterpartyReference)
			&& Objects.equals(question, other.question)
			&& Objects.equals(sentAt, other.sentAt)
			&& Objects.equals(remindedAt, other.remindedAt)
			&& Objects.equals(respondedAt, other.respondedAt)
			&& Objects.equals(outcome, other.outcome)
			&& Objects.equals(responseText, other.responseText)
			&& Objects.equals(communicationId, other.communicationId)
			&& Objects.equals(attachments, other.attachments)
			&& Objects.equals(createdBy, other.createdBy)
			&& Objects.equals(modifiedBy, other.modifiedBy)
			&& Objects.equals(created, other.created)
			&& Objects.equals(modified, other.modified)
			&& Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		return "Statement{" +
			"id='" + id + '\'' +
			", type='" + type + '\'' +
			", status='" + status + '\'' +
			", title='" + title + '\'' +
			", description='" + description + '\'' +
			", dueAt=" + dueAt +
			", completedAt=" + completedAt +
			", counterpartyName='" + counterpartyName + '\'' +
			", counterpartyExternalId='" + counterpartyExternalId + '\'' +
			", counterpartyExternalIdType='" + counterpartyExternalIdType + '\'' +
			", counterpartyReference='" + counterpartyReference + '\'' +
			", question='" + question + '\'' +
			", sentAt=" + sentAt +
			", remindedAt=" + remindedAt +
			", respondedAt=" + respondedAt +
			", outcome='" + outcome + '\'' +
			", responseText='" + responseText + '\'' +
			", communicationId='" + communicationId + '\'' +
			", attachments=" + attachments +
			", createdBy='" + createdBy + '\'' +
			", modifiedBy='" + modifiedBy + '\'' +
			", created=" + created +
			", modified=" + modified +
			", version=" + version +
			'}';
	}
}
