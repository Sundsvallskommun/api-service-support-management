package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.TimeZoneStorage;
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionMethod;
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionOutcome;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;
import static org.hibernate.type.SqlTypes.VARCHAR;

/**
 * A decision on an errand.
 * <p>
 * Fixed fields rather than a free document, because an administrative decision has a form that follows from the
 * administrative law and looks the same whether it concerns a building permit, income support or supervision: the
 * outcome, who made it, when, on what legal basis or delegation point, and why. That form belongs in the model, where
 * it is checked on the way in, shows up in the API specification and can be searched.
 */
@Entity
@Table(name = "decision",
	indexes = {
		@Index(name = "idx_decision_errand_id", columnList = "errand_id"),
		@Index(name = "idx_decision_ns_outcome", columnList = "municipality_id,namespace,outcome"),
		@Index(name = "idx_decision_valid_to", columnList = "valid_to")
	})
@AssociationOverride(name = "errandEntity",
	joinColumns = @JoinColumn(name = "errand_id", nullable = false),
	foreignKey = @ForeignKey(name = "fk_decision_errand_id"))
public class DecisionEntity extends AbstractErrandItemEntity<DecisionEntity> {

	@Enumerated(STRING)
	@JdbcTypeCode(VARCHAR)
	@Column(name = "outcome", length = 32, nullable = false)
	private DecisionOutcome outcome;

	/** MANUAL or AUTOMATIC. The difference has to be answerable afterwards. */
	@Enumerated(STRING)
	@JdbcTypeCode(VARCHAR)
	@Column(name = "method", length = 16, nullable = false)
	private DecisionMethod method;

	/** AD account when MANUAL, consumer name when AUTOMATIC. */
	@Column(name = "decided_by", nullable = false)
	private String decidedBy;

	/** The level of authority: delegate, board, committee, chair. Metadata of the namespace. */
	@Column(name = "decided_by_role", length = 128)
	private String decidedByRole;

	@Column(name = "decided_at", nullable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime decidedAt;

	@Column(name = "legal_basis")
	private String legalBasis;

	@Column(name = "delegation_reference", length = 64)
	private String delegationReference;

	/** The justification. Contains personal data - goes through role based field filtering. */
	@Column(name = "justification", length = LONG32)
	private String justification;

	@Column(name = "appealable")
	private Boolean appealable;

	/** Period of validity. LocalDate: validity is counted in days, not in points in time. */
	@Column(name = "valid_from")
	private LocalDate validFrom;

	@Column(name = "valid_to")
	private LocalDate validTo;

	/** The investigation the decision rests on. Nullable, and set to null rather than cascading when it is removed. */
	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "investigation_id", foreignKey = @ForeignKey(name = "fk_decision_investigation_id"))
	@OnDelete(action = OnDeleteAction.SET_NULL)
	private InvestigationEntity investigationEntity;

	/**
	 * The process row that made the decision. Nullable for manual decisions, and without a JPA relation.
	 * <p>
	 * Nothing writes it yet. It is the column the process integration sets when an automatic decision comes back from a
	 * process, and it is here so that the table does not need to change then.
	 */
	@Column(name = "errand_process_id", length = 36)
	private String errandProcessId;

	@OneToMany(mappedBy = "decisionEntity", cascade = ALL, orphanRemoval = true)
	@OrderBy("sortOrder")
	private List<DecisionTermEntity> terms;

	/** See {@link StatementEntity#getAttachments()} for why there is no PERSIST here. */
	@OneToMany(mappedBy = "decisionEntity", cascade = {
		MERGE, REMOVE
	}, orphanRemoval = true)
	@OrderBy("sortOrder")
	private List<DecisionAttachmentEntity> attachments;

	@OneToMany(mappedBy = "decisionEntity", cascade = {
		MERGE, REMOVE
	}, orphanRemoval = true)
	private List<DecisionJsonParameterEntity> jsonParameterLinks;

	public static DecisionEntity create() {
		return new DecisionEntity();
	}

	public DecisionOutcome getOutcome() {
		return outcome;
	}

	public void setOutcome(final DecisionOutcome outcome) {
		this.outcome = outcome;
	}

	public DecisionEntity withOutcome(final DecisionOutcome outcome) {
		this.outcome = outcome;
		return this;
	}

	public DecisionMethod getMethod() {
		return method;
	}

	public void setMethod(final DecisionMethod method) {
		this.method = method;
	}

	public DecisionEntity withMethod(final DecisionMethod method) {
		this.method = method;
		return this;
	}

	public String getDecidedBy() {
		return decidedBy;
	}

	public void setDecidedBy(final String decidedBy) {
		this.decidedBy = decidedBy;
	}

	public DecisionEntity withDecidedBy(final String decidedBy) {
		this.decidedBy = decidedBy;
		return this;
	}

	public String getDecidedByRole() {
		return decidedByRole;
	}

	public void setDecidedByRole(final String decidedByRole) {
		this.decidedByRole = decidedByRole;
	}

	public DecisionEntity withDecidedByRole(final String decidedByRole) {
		this.decidedByRole = decidedByRole;
		return this;
	}

	public OffsetDateTime getDecidedAt() {
		return decidedAt;
	}

	public void setDecidedAt(final OffsetDateTime decidedAt) {
		this.decidedAt = decidedAt;
	}

	public DecisionEntity withDecidedAt(final OffsetDateTime decidedAt) {
		this.decidedAt = decidedAt;
		return this;
	}

	public String getLegalBasis() {
		return legalBasis;
	}

	public void setLegalBasis(final String legalBasis) {
		this.legalBasis = legalBasis;
	}

	public DecisionEntity withLegalBasis(final String legalBasis) {
		this.legalBasis = legalBasis;
		return this;
	}

	public String getDelegationReference() {
		return delegationReference;
	}

	public void setDelegationReference(final String delegationReference) {
		this.delegationReference = delegationReference;
	}

	public DecisionEntity withDelegationReference(final String delegationReference) {
		this.delegationReference = delegationReference;
		return this;
	}

	public String getJustification() {
		return justification;
	}

	public void setJustification(final String justification) {
		this.justification = justification;
	}

	public DecisionEntity withJustification(final String justification) {
		this.justification = justification;
		return this;
	}

	public Boolean getAppealable() {
		return appealable;
	}

	public void setAppealable(final Boolean appealable) {
		this.appealable = appealable;
	}

	public DecisionEntity withAppealable(final Boolean appealable) {
		this.appealable = appealable;
		return this;
	}

	public LocalDate getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(final LocalDate validFrom) {
		this.validFrom = validFrom;
	}

	public DecisionEntity withValidFrom(final LocalDate validFrom) {
		this.validFrom = validFrom;
		return this;
	}

	public LocalDate getValidTo() {
		return validTo;
	}

	public void setValidTo(final LocalDate validTo) {
		this.validTo = validTo;
	}

	public DecisionEntity withValidTo(final LocalDate validTo) {
		this.validTo = validTo;
		return this;
	}

	public InvestigationEntity getInvestigationEntity() {
		return investigationEntity;
	}

	public void setInvestigationEntity(final InvestigationEntity investigationEntity) {
		this.investigationEntity = investigationEntity;
	}

	public DecisionEntity withInvestigationEntity(final InvestigationEntity investigationEntity) {
		this.investigationEntity = investigationEntity;
		return this;
	}

	public String getErrandProcessId() {
		return errandProcessId;
	}

	public void setErrandProcessId(final String errandProcessId) {
		this.errandProcessId = errandProcessId;
	}

	public DecisionEntity withErrandProcessId(final String errandProcessId) {
		this.errandProcessId = errandProcessId;
		return this;
	}

	public List<DecisionTermEntity> getTerms() {
		return terms;
	}

	public void setTerms(final List<DecisionTermEntity> terms) {
		this.terms = terms;
	}

	public DecisionEntity withTerms(final List<DecisionTermEntity> terms) {
		this.terms = terms;
		return this;
	}

	public List<DecisionAttachmentEntity> getAttachments() {
		return attachments;
	}

	public void setAttachments(final List<DecisionAttachmentEntity> attachments) {
		this.attachments = attachments;
	}

	public DecisionEntity withAttachments(final List<DecisionAttachmentEntity> attachments) {
		this.attachments = attachments;
		return this;
	}

	public List<DecisionJsonParameterEntity> getJsonParameterLinks() {
		return jsonParameterLinks;
	}

	public void setJsonParameterLinks(final List<DecisionJsonParameterEntity> jsonParameterLinks) {
		this.jsonParameterLinks = jsonParameterLinks;
	}

	public DecisionEntity withJsonParameterLinks(final List<DecisionJsonParameterEntity> jsonParameterLinks) {
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
		final DecisionEntity that = (DecisionEntity) o;
		return (outcome == that.outcome)
			&& (method == that.method)
			&& Objects.equals(decidedBy, that.decidedBy)
			&& Objects.equals(decidedByRole, that.decidedByRole)
			&& Objects.equals(decidedAt, that.decidedAt)
			&& Objects.equals(legalBasis, that.legalBasis)
			&& Objects.equals(delegationReference, that.delegationReference)
			&& Objects.equals(justification, that.justification)
			&& Objects.equals(appealable, that.appealable)
			&& Objects.equals(validFrom, that.validFrom)
			&& Objects.equals(validTo, that.validTo)
			&& Objects.equals(errandProcessId, that.errandProcessId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), outcome, method, decidedBy, decidedByRole, decidedAt, legalBasis, delegationReference, justification, appealable, validFrom, validTo, errandProcessId);
	}

	@Override
	public String toString() {
		return "DecisionEntity{" + super.toString() +
			", outcome=" + outcome +
			", method=" + method +
			", decidedBy='" + decidedBy + '\'' +
			", decidedByRole='" + decidedByRole + '\'' +
			", decidedAt=" + decidedAt +
			", legalBasis='" + legalBasis + '\'' +
			", delegationReference='" + delegationReference + '\'' +
			", justification='" + justification + '\'' +
			", appealable=" + appealable +
			", validFrom=" + validFrom +
			", validTo=" + validTo +
			", investigationEntity=" + (investigationEntity != null ? investigationEntity.getId() : "null") +
			", errandProcessId='" + errandProcessId + '\'' +
			'}';
	}
}
