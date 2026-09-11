package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
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
 * An investigation of an errand.
 * <p>
 * The sections are read here but written through their own resource, since each has a life of its own and an
 * assessment that is asked about across errands.
 */
@Schema(description = "Investigation model")
public class Investigation {

	@Schema(description = "Investigation ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "Type of investigation, as registered for the namespace", maxLength = 128, examples = "SUITABILITY")
	@Size(max = 128, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String type;

	@Schema(description = "Life cycle status", examples = "ACTIVE")
	@NotNull(groups = {
		Default.class, OnCreate.class
	})
	@OneOf(value = {
		"DRAFT", "ACTIVE", "COMPLETED", "CANCELLED"
	}, nullable = true, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String status;

	@Schema(description = "Heading of the investigation", maxLength = 255, examples = "Utredning av personlig lämplighet")
	@Size(max = 255, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String title;

	@Schema(description = "Description of the investigation", examples = "Utredning inför beslut om serveringstillstånd")
	private String description;

	@Schema(description = "Deadline for the investigation", examples = "2021-10-01T12:00:00Z")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime dueAt;

	@Schema(description = "Timestamp when the investigation was concluded", examples = "2021-10-01T12:00:00Z")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime completedAt;

	@Schema(description = "Investigator (ad-username)", maxLength = 255, examples = "jo12doe")
	@Size(max = 255, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String investigatorUserId;

	@Schema(description = "Timestamp when the investigation was started", examples = "2021-09-01T12:00:00Z")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime startedAt;

	@Schema(description = "Summary of what has been examined", examples = "Kontroll av ekonomi och lokal har genomförts.")
	private String summary;

	@Schema(description = "The overall assessment", examples = "Sökanden bedöms uppfylla kraven.")
	private String conclusion;

	@Schema(description = "The proposed decision, expressed in the language of the decision", examples = "APPROVAL", nullable = true)
	@OneOf(value = {
		"APPROVAL", "PARTIAL_APPROVAL", "REJECTION", "DISMISSAL", "DISCONTINUATION", "OTHER"
	}, nullable = true, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String recommendation;

	@Schema(description = "Motivation for the recommendation", examples = "Inga brister har konstaterats.")
	private String recommendationMotivation;

	@ArraySchema(schema = @Schema(implementation = InvestigationSection.class, accessMode = READ_ONLY),
		arraySchema = @Schema(description = "Sections of the investigation, written through their own resource"))
	private List<InvestigationSection> sections;

	@ArraySchema(schema = @Schema(implementation = ArtefactAttachment.class, accessMode = READ_ONLY),
		arraySchema = @Schema(description = "Attachments of the errand linked to this investigation"))
	private List<ArtefactAttachment> attachments;

	@Schema(description = "User who created the investigation", examples = "jo12doe", accessMode = READ_ONLY)
	private String createdBy;

	@Schema(description = "User who last modified the investigation", examples = "jo12doe", accessMode = READ_ONLY)
	private String modifiedBy;

	@Schema(description = "Timestamp when the investigation was created", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the investigation was last modified", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime modified;

	@Schema(description = "Version of the investigation, carried as the ETag of the resource", examples = "0", accessMode = READ_ONLY)
	private Long version;

	public static Investigation create() {
		return new Investigation();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Investigation withId(final String id) {
		this.id = id;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public Investigation withType(final String type) {
		this.type = type;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public Investigation withStatus(final String status) {
		this.status = status;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public Investigation withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public Investigation withDescription(final String description) {
		this.description = description;
		return this;
	}

	public OffsetDateTime getDueAt() {
		return dueAt;
	}

	public void setDueAt(final OffsetDateTime dueAt) {
		this.dueAt = dueAt;
	}

	public Investigation withDueAt(final OffsetDateTime dueAt) {
		this.dueAt = dueAt;
		return this;
	}

	public OffsetDateTime getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(final OffsetDateTime completedAt) {
		this.completedAt = completedAt;
	}

	public Investigation withCompletedAt(final OffsetDateTime completedAt) {
		this.completedAt = completedAt;
		return this;
	}

	public String getInvestigatorUserId() {
		return investigatorUserId;
	}

	public void setInvestigatorUserId(final String investigatorUserId) {
		this.investigatorUserId = investigatorUserId;
	}

	public Investigation withInvestigatorUserId(final String investigatorUserId) {
		this.investigatorUserId = investigatorUserId;
		return this;
	}

	public OffsetDateTime getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(final OffsetDateTime startedAt) {
		this.startedAt = startedAt;
	}

	public Investigation withStartedAt(final OffsetDateTime startedAt) {
		this.startedAt = startedAt;
		return this;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(final String summary) {
		this.summary = summary;
	}

	public Investigation withSummary(final String summary) {
		this.summary = summary;
		return this;
	}

	public String getConclusion() {
		return conclusion;
	}

	public void setConclusion(final String conclusion) {
		this.conclusion = conclusion;
	}

	public Investigation withConclusion(final String conclusion) {
		this.conclusion = conclusion;
		return this;
	}

	public String getRecommendation() {
		return recommendation;
	}

	public void setRecommendation(final String recommendation) {
		this.recommendation = recommendation;
	}

	public Investigation withRecommendation(final String recommendation) {
		this.recommendation = recommendation;
		return this;
	}

	public String getRecommendationMotivation() {
		return recommendationMotivation;
	}

	public void setRecommendationMotivation(final String recommendationMotivation) {
		this.recommendationMotivation = recommendationMotivation;
	}

	public Investigation withRecommendationMotivation(final String recommendationMotivation) {
		this.recommendationMotivation = recommendationMotivation;
		return this;
	}

	public List<InvestigationSection> getSections() {
		return sections;
	}

	public void setSections(final List<InvestigationSection> sections) {
		this.sections = sections;
	}

	public Investigation withSections(final List<InvestigationSection> sections) {
		this.sections = sections;
		return this;
	}

	public List<ArtefactAttachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(final List<ArtefactAttachment> attachments) {
		this.attachments = attachments;
	}

	public Investigation withAttachments(final List<ArtefactAttachment> attachments) {
		this.attachments = attachments;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public Investigation withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public Investigation withModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Investigation withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Investigation withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(final Long version) {
		this.version = version;
	}

	public Investigation withVersion(final Long version) {
		this.version = version;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, type, status, title, description, dueAt, completedAt, investigatorUserId, startedAt, summary, conclusion, recommendation, recommendationMotivation, sections, attachments, createdBy, modifiedBy, created, modified, version);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Investigation other)) {
			return false;
		}
		return Objects.equals(id, other.id)
			&& Objects.equals(type, other.type)
			&& Objects.equals(status, other.status)
			&& Objects.equals(title, other.title)
			&& Objects.equals(description, other.description)
			&& Objects.equals(dueAt, other.dueAt)
			&& Objects.equals(completedAt, other.completedAt)
			&& Objects.equals(investigatorUserId, other.investigatorUserId)
			&& Objects.equals(startedAt, other.startedAt)
			&& Objects.equals(summary, other.summary)
			&& Objects.equals(conclusion, other.conclusion)
			&& Objects.equals(recommendation, other.recommendation)
			&& Objects.equals(recommendationMotivation, other.recommendationMotivation)
			&& Objects.equals(sections, other.sections)
			&& Objects.equals(attachments, other.attachments)
			&& Objects.equals(createdBy, other.createdBy)
			&& Objects.equals(modifiedBy, other.modifiedBy)
			&& Objects.equals(created, other.created)
			&& Objects.equals(modified, other.modified)
			&& Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		return "Investigation{" +
			"id='" + id + '\'' +
			", type='" + type + '\'' +
			", status='" + status + '\'' +
			", title='" + title + '\'' +
			", description='" + description + '\'' +
			", dueAt=" + dueAt +
			", completedAt=" + completedAt +
			", investigatorUserId='" + investigatorUserId + '\'' +
			", startedAt=" + startedAt +
			", summary='" + summary + '\'' +
			", conclusion='" + conclusion + '\'' +
			", recommendation='" + recommendation + '\'' +
			", recommendationMotivation='" + recommendationMotivation + '\'' +
			", sections=" + sections +
			", attachments=" + attachments +
			", createdBy='" + createdBy + '\'' +
			", modifiedBy='" + modifiedBy + '\'' +
			", created=" + created +
			", modified=" + modified +
			", version=" + version +
			'}';
	}
}
