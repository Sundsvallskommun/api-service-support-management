package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TimeZoneStorage;
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionOutcome;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;
import static org.hibernate.type.SqlTypes.VARCHAR;

/**
 * An investigation of an errand.
 * <p>
 * The frame is shared; the areas being assessed are rows, not columns. One line of business has four of them, another
 * has three others - and neither should force a migration on the other.
 */
@Entity
@Table(name = "investigation",
	indexes = {
		@Index(name = "idx_investigation_errand_id", columnList = "errand_id"),
		@Index(name = "idx_investigation_ns_status", columnList = "municipality_id,namespace,status"),
		@Index(name = "idx_investigation_due_at", columnList = "due_at")
	})
@AssociationOverride(name = "errandEntity",
	joinColumns = @JoinColumn(name = "errand_id", nullable = false),
	foreignKey = @ForeignKey(name = "fk_investigation_errand_id"))
public class InvestigationEntity extends AbstractErrandItemEntity<InvestigationEntity> {

	@Column(name = "investigator_user_id")
	private String investigatorUserId;

	@Column(name = "started_at")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime startedAt;

	/** A summary of what has been examined. */
	@Column(name = "summary", length = LONG32)
	private String summary;

	/** The overall assessment - the answer of the investigation to the question of the errand. */
	@Column(name = "conclusion", length = LONG32)
	private String conclusion;

	/**
	 * The proposed decision, expressed in the language of the decision. The same enum as {@code DecisionEntity.outcome}
	 * on purpose: it makes it answerable how often the decision follows what the investigation proposed.
	 */
	@Enumerated(STRING)
	@JdbcTypeCode(VARCHAR)
	@Column(name = "recommendation", length = 32)
	private DecisionOutcome recommendation;

	@Column(name = "recommendation_motivation", length = LONG32)
	private String recommendationMotivation;

	@OneToMany(mappedBy = "investigationEntity", cascade = ALL, orphanRemoval = true)
	@OrderBy("sortOrder")
	private List<InvestigationSectionEntity> sections;

	/** See {@link StatementEntity#getAttachments()} for why there is no PERSIST here. */
	@OneToMany(mappedBy = "investigationEntity", cascade = {
		MERGE, REMOVE
	}, orphanRemoval = true)
	@OrderBy("sortOrder")
	private List<InvestigationAttachmentEntity> attachments;

	@OneToMany(mappedBy = "investigationEntity", cascade = {
		MERGE, REMOVE
	}, orphanRemoval = true)
	private List<InvestigationJsonParameterEntity> jsonParameterLinks;

	public static InvestigationEntity create() {
		return new InvestigationEntity();
	}

	public String getInvestigatorUserId() {
		return investigatorUserId;
	}

	public void setInvestigatorUserId(final String investigatorUserId) {
		this.investigatorUserId = investigatorUserId;
	}

	public InvestigationEntity withInvestigatorUserId(final String investigatorUserId) {
		this.investigatorUserId = investigatorUserId;
		return this;
	}

	public OffsetDateTime getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(final OffsetDateTime startedAt) {
		this.startedAt = startedAt;
	}

	public InvestigationEntity withStartedAt(final OffsetDateTime startedAt) {
		this.startedAt = startedAt;
		return this;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(final String summary) {
		this.summary = summary;
	}

	public InvestigationEntity withSummary(final String summary) {
		this.summary = summary;
		return this;
	}

	public String getConclusion() {
		return conclusion;
	}

	public void setConclusion(final String conclusion) {
		this.conclusion = conclusion;
	}

	public InvestigationEntity withConclusion(final String conclusion) {
		this.conclusion = conclusion;
		return this;
	}

	public DecisionOutcome getRecommendation() {
		return recommendation;
	}

	public void setRecommendation(final DecisionOutcome recommendation) {
		this.recommendation = recommendation;
	}

	public InvestigationEntity withRecommendation(final DecisionOutcome recommendation) {
		this.recommendation = recommendation;
		return this;
	}

	public String getRecommendationMotivation() {
		return recommendationMotivation;
	}

	public void setRecommendationMotivation(final String recommendationMotivation) {
		this.recommendationMotivation = recommendationMotivation;
	}

	public InvestigationEntity withRecommendationMotivation(final String recommendationMotivation) {
		this.recommendationMotivation = recommendationMotivation;
		return this;
	}

	public List<InvestigationSectionEntity> getSections() {
		return sections;
	}

	public void setSections(final List<InvestigationSectionEntity> sections) {
		this.sections = sections;
	}

	public InvestigationEntity withSections(final List<InvestigationSectionEntity> sections) {
		this.sections = sections;
		return this;
	}

	public List<InvestigationAttachmentEntity> getAttachments() {
		return attachments;
	}

	public void setAttachments(final List<InvestigationAttachmentEntity> attachments) {
		this.attachments = attachments;
	}

	public InvestigationEntity withAttachments(final List<InvestigationAttachmentEntity> attachments) {
		this.attachments = attachments;
		return this;
	}

	public List<InvestigationJsonParameterEntity> getJsonParameterLinks() {
		return jsonParameterLinks;
	}

	public void setJsonParameterLinks(final List<InvestigationJsonParameterEntity> jsonParameterLinks) {
		this.jsonParameterLinks = jsonParameterLinks;
	}

	public InvestigationEntity withJsonParameterLinks(final List<InvestigationJsonParameterEntity> jsonParameterLinks) {
		this.jsonParameterLinks = jsonParameterLinks;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!super.equals(o)) {
			return false;
		}
		final InvestigationEntity that = (InvestigationEntity) o;
		return Objects.equals(investigatorUserId, that.investigatorUserId)
			&& Objects.equals(startedAt, that.startedAt)
			&& Objects.equals(summary, that.summary)
			&& Objects.equals(conclusion, that.conclusion)
			&& (recommendation == that.recommendation)
			&& Objects.equals(recommendationMotivation, that.recommendationMotivation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), investigatorUserId, startedAt, summary, conclusion, recommendation, recommendationMotivation);
	}

	@Override
	public String toString() {
		return "InvestigationEntity{" + super.toString() +
			", investigatorUserId='" + investigatorUserId + '\'' +
			", startedAt=" + startedAt +
			", summary='" + summary + '\'' +
			", conclusion='" + conclusion + '\'' +
			", recommendation=" + recommendation +
			", recommendationMotivation='" + recommendationMotivation + '\'' +
			'}';
	}
}
