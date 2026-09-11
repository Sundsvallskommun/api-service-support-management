package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import se.sundsvall.dept44.common.validators.annotation.OneOf;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * A decision on an errand.
 * <p>
 * The outcome, who made it, when, on what legal basis and why are fixed fields rather than a free document, because an
 * administrative decision has a form that follows from the law. Whether the decision was made by a person or by a
 * process is recorded in the method, and has to remain answerable afterwards.
 */
@Schema(description = "Decision model")
public class Decision {

	@Schema(description = "Decision ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "Type of decision, as registered for the namespace", maxLength = 128, examples = "PERMIT")
	@Size(max = 128, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String type;

	@Schema(description = "Life cycle status", examples = "COMPLETED")
	@NotNull(groups = {
		Default.class, OnCreate.class
	})
	@OneOf(value = {
		"DRAFT", "ACTIVE", "COMPLETED", "CANCELLED"
	}, nullable = true, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String status;

	@Schema(description = "Heading of the decision", maxLength = 255, examples = "Beslut om serveringstillstånd")
	@Size(max = 255, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String title;

	@Schema(description = "Description of the decision", examples = "Stadigvarande serveringstillstånd till allmänheten")
	private String description;

	@Schema(description = "Deadline for the decision", examples = "2021-10-01T12:00:00Z")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime dueAt;

	@Schema(description = "Timestamp when the decision was concluded", examples = "2021-10-01T12:00:00Z")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime completedAt;

	@Schema(description = "Outcome of the decision", examples = "APPROVAL")
	@NotNull(groups = {
		Default.class, OnCreate.class
	})
	@OneOf(value = {
		"APPROVAL", "PARTIAL_APPROVAL", "REJECTION", "DISMISSAL", "DISCONTINUATION", "OTHER"
	}, nullable = true, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String outcome;

	@Schema(description = "How the decision was made", examples = "MANUAL")
	@NotNull(groups = {
		Default.class, OnCreate.class
	})
	@OneOf(value = {
		"MANUAL", "AUTOMATIC"
	}, nullable = true, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String method;

	@Schema(description = "Who made the decision - an ad-account when manual, a consumer name when automatic", maxLength = 255, examples = "jo12doe")
	@NotBlank(groups = {
		Default.class, OnCreate.class
	})
	@Size(max = 255, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String decidedBy;

	@Schema(description = "Level of authority, as registered for the namespace", maxLength = 128, examples = "DELEGATE")
	@Size(max = 128, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String decidedByRole;

	@Schema(description = "Timestamp when the decision was made", examples = "2021-09-14T10:12:00+02:00")
	@NotNull(groups = {
		Default.class, OnCreate.class
	})
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime decidedAt;

	@Schema(description = "The legal basis of the decision", maxLength = 255, examples = "8 kap. 12 § alkohollagen")
	@Size(max = 255, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String legalBasis;

	@Schema(description = "The delegation point the decision was made under", maxLength = 64, examples = "3.2.1")
	@Size(max = 64, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String delegationReference;

	@Schema(description = "The justification of the decision", examples = "Sökanden uppfyller kraven på lämplighet.")
	private String justification;

	@Schema(description = "Whether the decision can be appealed", examples = "true")
	private Boolean appealable;

	@Schema(description = "First day the decision is valid", examples = "2021-10-01")
	private LocalDate validFrom;

	@Schema(description = "Last day the decision is valid", examples = "2022-09-30")
	private LocalDate validTo;

	@Schema(description = "Id of the investigation the decision rests on", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", nullable = true)
	@ValidUuid(nullable = true)
	private String investigationId;

	@Schema(description = "Id of the process row that made the decision", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", accessMode = READ_ONLY)
	private String errandProcessId;

	@ArraySchema(schema = @Schema(implementation = DecisionTerm.class, accessMode = READ_ONLY),
		arraySchema = @Schema(description = "Terms of the decision, written through their own resource"))
	private List<DecisionTerm> terms;

	@ArraySchema(schema = @Schema(implementation = ArtefactAttachment.class, accessMode = READ_ONLY),
		arraySchema = @Schema(description = "Attachments of the errand linked to this decision"))
	private List<ArtefactAttachment> attachments;

	@Schema(description = "User who created the decision", examples = "jo12doe", accessMode = READ_ONLY)
	private String createdBy;

	@Schema(description = "User who last modified the decision", examples = "jo12doe", accessMode = READ_ONLY)
	private String modifiedBy;

	@Schema(description = "Timestamp when the decision was created", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the decision was last modified", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime modified;

	@Schema(description = "Version of the decision, carried as the ETag of the resource", examples = "0", accessMode = READ_ONLY)
	private Long version;

	public static Decision create() {
		return new Decision();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Decision withId(final String id) {
		this.id = id;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public Decision withType(final String type) {
		this.type = type;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public Decision withStatus(final String status) {
		this.status = status;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public Decision withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public Decision withDescription(final String description) {
		this.description = description;
		return this;
	}

	public OffsetDateTime getDueAt() {
		return dueAt;
	}

	public void setDueAt(final OffsetDateTime dueAt) {
		this.dueAt = dueAt;
	}

	public Decision withDueAt(final OffsetDateTime dueAt) {
		this.dueAt = dueAt;
		return this;
	}

	public OffsetDateTime getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(final OffsetDateTime completedAt) {
		this.completedAt = completedAt;
	}

	public Decision withCompletedAt(final OffsetDateTime completedAt) {
		this.completedAt = completedAt;
		return this;
	}

	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(final String outcome) {
		this.outcome = outcome;
	}

	public Decision withOutcome(final String outcome) {
		this.outcome = outcome;
		return this;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(final String method) {
		this.method = method;
	}

	public Decision withMethod(final String method) {
		this.method = method;
		return this;
	}

	public String getDecidedBy() {
		return decidedBy;
	}

	public void setDecidedBy(final String decidedBy) {
		this.decidedBy = decidedBy;
	}

	public Decision withDecidedBy(final String decidedBy) {
		this.decidedBy = decidedBy;
		return this;
	}

	public String getDecidedByRole() {
		return decidedByRole;
	}

	public void setDecidedByRole(final String decidedByRole) {
		this.decidedByRole = decidedByRole;
	}

	public Decision withDecidedByRole(final String decidedByRole) {
		this.decidedByRole = decidedByRole;
		return this;
	}

	public OffsetDateTime getDecidedAt() {
		return decidedAt;
	}

	public void setDecidedAt(final OffsetDateTime decidedAt) {
		this.decidedAt = decidedAt;
	}

	public Decision withDecidedAt(final OffsetDateTime decidedAt) {
		this.decidedAt = decidedAt;
		return this;
	}

	public String getLegalBasis() {
		return legalBasis;
	}

	public void setLegalBasis(final String legalBasis) {
		this.legalBasis = legalBasis;
	}

	public Decision withLegalBasis(final String legalBasis) {
		this.legalBasis = legalBasis;
		return this;
	}

	public String getDelegationReference() {
		return delegationReference;
	}

	public void setDelegationReference(final String delegationReference) {
		this.delegationReference = delegationReference;
	}

	public Decision withDelegationReference(final String delegationReference) {
		this.delegationReference = delegationReference;
		return this;
	}

	public String getJustification() {
		return justification;
	}

	public void setJustification(final String justification) {
		this.justification = justification;
	}

	public Decision withJustification(final String justification) {
		this.justification = justification;
		return this;
	}

	public Boolean getAppealable() {
		return appealable;
	}

	public void setAppealable(final Boolean appealable) {
		this.appealable = appealable;
	}

	public Decision withAppealable(final Boolean appealable) {
		this.appealable = appealable;
		return this;
	}

	public LocalDate getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(final LocalDate validFrom) {
		this.validFrom = validFrom;
	}

	public Decision withValidFrom(final LocalDate validFrom) {
		this.validFrom = validFrom;
		return this;
	}

	public LocalDate getValidTo() {
		return validTo;
	}

	public void setValidTo(final LocalDate validTo) {
		this.validTo = validTo;
	}

	public Decision withValidTo(final LocalDate validTo) {
		this.validTo = validTo;
		return this;
	}

	public String getInvestigationId() {
		return investigationId;
	}

	public void setInvestigationId(final String investigationId) {
		this.investigationId = investigationId;
	}

	public Decision withInvestigationId(final String investigationId) {
		this.investigationId = investigationId;
		return this;
	}

	public String getErrandProcessId() {
		return errandProcessId;
	}

	public void setErrandProcessId(final String errandProcessId) {
		this.errandProcessId = errandProcessId;
	}

	public Decision withErrandProcessId(final String errandProcessId) {
		this.errandProcessId = errandProcessId;
		return this;
	}

	public List<DecisionTerm> getTerms() {
		return terms;
	}

	public void setTerms(final List<DecisionTerm> terms) {
		this.terms = terms;
	}

	public Decision withTerms(final List<DecisionTerm> terms) {
		this.terms = terms;
		return this;
	}

	public List<ArtefactAttachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(final List<ArtefactAttachment> attachments) {
		this.attachments = attachments;
	}

	public Decision withAttachments(final List<ArtefactAttachment> attachments) {
		this.attachments = attachments;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public Decision withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public Decision withModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Decision withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Decision withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(final Long version) {
		this.version = version;
	}

	public Decision withVersion(final Long version) {
		this.version = version;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, type, status, title, description, dueAt, completedAt, outcome, method, decidedBy, decidedByRole, decidedAt, legalBasis, delegationReference, justification, appealable, validFrom, validTo, investigationId, errandProcessId,
			terms, attachments, createdBy, modifiedBy, created, modified, version);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Decision other)) {
			return false;
		}
		return Objects.equals(id, other.id)
			&& Objects.equals(type, other.type)
			&& Objects.equals(status, other.status)
			&& Objects.equals(title, other.title)
			&& Objects.equals(description, other.description)
			&& Objects.equals(dueAt, other.dueAt)
			&& Objects.equals(completedAt, other.completedAt)
			&& Objects.equals(outcome, other.outcome)
			&& Objects.equals(method, other.method)
			&& Objects.equals(decidedBy, other.decidedBy)
			&& Objects.equals(decidedByRole, other.decidedByRole)
			&& Objects.equals(decidedAt, other.decidedAt)
			&& Objects.equals(legalBasis, other.legalBasis)
			&& Objects.equals(delegationReference, other.delegationReference)
			&& Objects.equals(justification, other.justification)
			&& Objects.equals(appealable, other.appealable)
			&& Objects.equals(validFrom, other.validFrom)
			&& Objects.equals(validTo, other.validTo)
			&& Objects.equals(investigationId, other.investigationId)
			&& Objects.equals(errandProcessId, other.errandProcessId)
			&& Objects.equals(terms, other.terms)
			&& Objects.equals(attachments, other.attachments)
			&& Objects.equals(createdBy, other.createdBy)
			&& Objects.equals(modifiedBy, other.modifiedBy)
			&& Objects.equals(created, other.created)
			&& Objects.equals(modified, other.modified)
			&& Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		return "Decision{" +
			"id='" + id + '\'' +
			", type='" + type + '\'' +
			", status='" + status + '\'' +
			", title='" + title + '\'' +
			", description='" + description + '\'' +
			", dueAt=" + dueAt +
			", completedAt=" + completedAt +
			", outcome='" + outcome + '\'' +
			", method='" + method + '\'' +
			", decidedBy='" + decidedBy + '\'' +
			", decidedByRole='" + decidedByRole + '\'' +
			", decidedAt=" + decidedAt +
			", legalBasis='" + legalBasis + '\'' +
			", delegationReference='" + delegationReference + '\'' +
			", justification='" + justification + '\'' +
			", appealable=" + appealable +
			", validFrom=" + validFrom +
			", validTo=" + validTo +
			", investigationId='" + investigationId + '\'' +
			", errandProcessId='" + errandProcessId + '\'' +
			", terms=" + terms +
			", attachments=" + attachments +
			", createdBy='" + createdBy + '\'' +
			", modifiedBy='" + modifiedBy + '\'' +
			", created=" + created +
			", modified=" + modified +
			", version=" + version +
			'}';
	}
}
