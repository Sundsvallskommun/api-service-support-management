package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.supportmanagement.integration.db.model.enums.SectionAssessment;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;
import static org.hibernate.type.SqlTypes.VARCHAR;

/**
 * One assessed area of an investigation.
 * <p>
 * The assessment is a column and not a JSON key because it is the question the business asks: how many investigations
 * had a deficiency in the financial section? The rest of the section is free text, and whoever needs more structure
 * puts it in a JSON parameter with a registered schema.
 */
@Entity
@Table(name = "investigation_section",
	indexes = @Index(name = "idx_investigation_section_investigation_id", columnList = "investigation_id"),
	uniqueConstraints = @UniqueConstraint(name = "uq_investigation_section_investigation_id_section_key", columnNames = {
		"investigation_id", "section_key"
	}))
public class InvestigationSectionEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "investigation_id", nullable = false, foreignKey = @ForeignKey(name = "fk_investigation_section_investigation_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private InvestigationEntity investigationEntity;

	/** The key of the business, stable over time: "personal", "financial", "premises". */
	@Column(name = "section_key", nullable = false, length = 64)
	private String sectionKey;

	/** The heading shown. May change without the section key doing so. */
	@Column(name = "heading")
	private String heading;

	@Column(name = "sort_order")
	private Integer sortOrder;

	/** The only structured thing in the section, and therefore the only thing that can be counted. */
	@Enumerated(STRING)
	@JdbcTypeCode(VARCHAR)
	@Column(name = "assessment", length = 32, nullable = false)
	private SectionAssessment assessment;

	@Column(name = "text", length = LONG32)
	private String text;

	@Column(name = "completed_by")
	private String completedBy;

	@Column(name = "completed_at")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime completedAt;

	/** Structured content when free text is not enough: a checklist, test results, measurements. */
	@OneToMany(mappedBy = "investigationSectionEntity", cascade = {
		MERGE, REMOVE
	}, orphanRemoval = true)
	private List<InvestigationSectionJsonParameterEntity> jsonParameterLinks;

	public static InvestigationSectionEntity create() {
		return new InvestigationSectionEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public InvestigationSectionEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public InvestigationEntity getInvestigationEntity() {
		return investigationEntity;
	}

	public void setInvestigationEntity(final InvestigationEntity investigationEntity) {
		this.investigationEntity = investigationEntity;
	}

	public InvestigationSectionEntity withInvestigationEntity(final InvestigationEntity investigationEntity) {
		this.investigationEntity = investigationEntity;
		return this;
	}

	public String getSectionKey() {
		return sectionKey;
	}

	public void setSectionKey(final String sectionKey) {
		this.sectionKey = sectionKey;
	}

	public InvestigationSectionEntity withSectionKey(final String sectionKey) {
		this.sectionKey = sectionKey;
		return this;
	}

	public String getHeading() {
		return heading;
	}

	public void setHeading(final String heading) {
		this.heading = heading;
	}

	public InvestigationSectionEntity withHeading(final String heading) {
		this.heading = heading;
		return this;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public InvestigationSectionEntity withSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
		return this;
	}

	public SectionAssessment getAssessment() {
		return assessment;
	}

	public void setAssessment(final SectionAssessment assessment) {
		this.assessment = assessment;
	}

	public InvestigationSectionEntity withAssessment(final SectionAssessment assessment) {
		this.assessment = assessment;
		return this;
	}

	public String getText() {
		return text;
	}

	public void setText(final String text) {
		this.text = text;
	}

	public InvestigationSectionEntity withText(final String text) {
		this.text = text;
		return this;
	}

	public String getCompletedBy() {
		return completedBy;
	}

	public void setCompletedBy(final String completedBy) {
		this.completedBy = completedBy;
	}

	public InvestigationSectionEntity withCompletedBy(final String completedBy) {
		this.completedBy = completedBy;
		return this;
	}

	public OffsetDateTime getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(final OffsetDateTime completedAt) {
		this.completedAt = completedAt;
	}

	public InvestigationSectionEntity withCompletedAt(final OffsetDateTime completedAt) {
		this.completedAt = completedAt;
		return this;
	}

	public List<InvestigationSectionJsonParameterEntity> getJsonParameterLinks() {
		return jsonParameterLinks;
	}

	public void setJsonParameterLinks(final List<InvestigationSectionJsonParameterEntity> jsonParameterLinks) {
		this.jsonParameterLinks = jsonParameterLinks;
	}

	public InvestigationSectionEntity withJsonParameterLinks(final List<InvestigationSectionJsonParameterEntity> jsonParameterLinks) {
		this.jsonParameterLinks = jsonParameterLinks;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, sectionKey, heading, sortOrder, assessment, text, completedBy, completedAt);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final InvestigationSectionEntity other)) {
			return false;
		}
		return Objects.equals(id, other.id)
			&& Objects.equals(sectionKey, other.sectionKey)
			&& Objects.equals(heading, other.heading)
			&& Objects.equals(sortOrder, other.sortOrder)
			&& (assessment == other.assessment)
			&& Objects.equals(text, other.text)
			&& Objects.equals(completedBy, other.completedBy)
			&& Objects.equals(completedAt, other.completedAt);
	}

	@Override
	public String toString() {
		return "InvestigationSectionEntity{" +
			"id='" + id + '\'' +
			", investigationEntity=" + (investigationEntity != null ? investigationEntity.getId() : "null") +
			", sectionKey='" + sectionKey + '\'' +
			", heading='" + heading + '\'' +
			", sortOrder=" + sortOrder +
			", assessment=" + assessment +
			", text='" + text + '\'' +
			", completedBy='" + completedBy + '\'' +
			", completedAt=" + completedAt +
			'}';
	}
}
