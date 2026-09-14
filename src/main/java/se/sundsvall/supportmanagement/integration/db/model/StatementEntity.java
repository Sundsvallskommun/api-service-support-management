package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;

import static jakarta.persistence.CascadeType.MERGE;
import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * A request for a statement and the statement that came back, in the same row.
 * <p>
 * An unanswered statement is an answered one minus the response fields. Splitting them into two entities would have
 * given a relation that is always zero-to-one and a join on every read.
 */
@Entity
@Table(name = "statement",
	indexes = {
		@Index(name = "idx_statement_errand_id", columnList = "errand_id"),
		@Index(name = "idx_statement_ns_status", columnList = "municipality_id,namespace,status"),
		@Index(name = "idx_statement_due_at", columnList = "due_at"),
		@Index(name = "idx_statement_counterparty_external_id", columnList = "counterparty_external_id")
	})
@AssociationOverride(name = "errandEntity",
	joinColumns = @JoinColumn(name = "errand_id", nullable = false),
	foreignKey = @ForeignKey(name = "fk_statement_errand_id"))
public class StatementEntity extends AbstractErrandItemEntity<StatementEntity> {

	/**
	 * The counterparty. Deliberately its own fields rather than a foreign key to stakeholder: the body asked for a
	 * statement is almost never a party to the errand, and making it one blurs what a party is.
	 */
	@Column(name = "counterparty_name", nullable = false)
	private String counterpartyName;

	/**
	 * The identity follows {@link StakeholderEntity}: a free id plus a type from the external-id-type metadata of the
	 * namespace. One field for the identity, not two that can contradict each other.
	 */
	@Column(name = "counterparty_external_id")
	private String counterpartyExternalId;

	@Column(name = "counterparty_external_id_type", length = 128)
	private String counterpartyExternalIdType;

	/** The reference number of the counterparty, for cross reference in their system. */
	@Column(name = "counterparty_reference", length = 128)
	private String counterpartyReference;

	/** The assignment. The title carries the heading, this carries the question being asked. */
	@Column(name = "question", length = LONG32)
	private String question;

	@Column(name = "sent_at")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime sentAt;

	@Column(name = "reminded_at")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime remindedAt;

	@Column(name = "responded_at")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime respondedAt;

	/** One of the statement outcomes the namespace has registered, see {@link StatementOutcomeEntity}. */
	@Column(name = "outcome")
	private String outcome;

	@Column(name = "response_text", length = LONG32)
	private String responseText;

	/**
	 * The dispatch that carried the statement. Nullable and without a JPA relation: the statement is to survive the
	 * communication being cleaned up, and a paper dispatch has none.
	 */
	@Column(name = "communication_id", length = 36)
	private String communicationId;

	/**
	 * The attachments of the errand this statement uses.
	 * <p>
	 * A relation on top of the ownership of the errand rather than an ownership of its own: nothing cascades towards the
	 * attachment, and a row of the join table is removed by the database when either side goes. The statement owns the
	 * join table, so linking or unlinking an attachment moves its version like any other change to it.
	 * <p>
	 * A list rather than a set, since a set would hash the attachments, and an attachment hashes its file and its errand.
	 */
	@ManyToMany
	@JoinTable(name = "statement_attachment",
		joinColumns = @JoinColumn(name = "statement_id"),
		inverseJoinColumns = @JoinColumn(name = "attachment_id"),
		foreignKey = @ForeignKey(name = "fk_statement_attachment_statement_id", options = "on delete cascade"),
		inverseForeignKey = @ForeignKey(name = "fk_statement_attachment_attachment_id", options = "on delete cascade"),
		uniqueConstraints = @UniqueConstraint(name = "uq_statement_attachment_statement_id_attachment_id", columnNames = {
			"statement_id", "attachment_id"
		}),
		indexes = {
			@Index(name = "idx_statement_attachment_statement_id", columnList = "statement_id"),
			@Index(name = "idx_statement_attachment_attachment_id", columnList = "attachment_id")
		})
	@OrderBy("fileName")
	private List<AttachmentEntity> attachments;

	/**
	 * Links to the JSON parameters of the errand that belong to this statement.
	 * <p>
	 * MERGE and nothing else. A link is saved through its repository, and removed by the database when its parameter or
	 * its statement goes: were Hibernate to remove it in the same flush as the parameter, it would null the reference to
	 * the parameter first, which the column refuses.
	 */
	@OneToMany(mappedBy = "statementEntity", cascade = MERGE)
	private List<StatementJsonParameterEntity> jsonParameterLinks;

	public static StatementEntity create() {
		return new StatementEntity();
	}

	public String getCounterpartyName() {
		return counterpartyName;
	}

	public void setCounterpartyName(final String counterpartyName) {
		this.counterpartyName = counterpartyName;
	}

	public StatementEntity withCounterpartyName(final String counterpartyName) {
		this.counterpartyName = counterpartyName;
		return this;
	}

	public String getCounterpartyExternalId() {
		return counterpartyExternalId;
	}

	public void setCounterpartyExternalId(final String counterpartyExternalId) {
		this.counterpartyExternalId = counterpartyExternalId;
	}

	public StatementEntity withCounterpartyExternalId(final String counterpartyExternalId) {
		this.counterpartyExternalId = counterpartyExternalId;
		return this;
	}

	public String getCounterpartyExternalIdType() {
		return counterpartyExternalIdType;
	}

	public void setCounterpartyExternalIdType(final String counterpartyExternalIdType) {
		this.counterpartyExternalIdType = counterpartyExternalIdType;
	}

	public StatementEntity withCounterpartyExternalIdType(final String counterpartyExternalIdType) {
		this.counterpartyExternalIdType = counterpartyExternalIdType;
		return this;
	}

	public String getCounterpartyReference() {
		return counterpartyReference;
	}

	public void setCounterpartyReference(final String counterpartyReference) {
		this.counterpartyReference = counterpartyReference;
	}

	public StatementEntity withCounterpartyReference(final String counterpartyReference) {
		this.counterpartyReference = counterpartyReference;
		return this;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(final String question) {
		this.question = question;
	}

	public StatementEntity withQuestion(final String question) {
		this.question = question;
		return this;
	}

	public OffsetDateTime getSentAt() {
		return sentAt;
	}

	public void setSentAt(final OffsetDateTime sentAt) {
		this.sentAt = sentAt;
	}

	public StatementEntity withSentAt(final OffsetDateTime sentAt) {
		this.sentAt = sentAt;
		return this;
	}

	public OffsetDateTime getRemindedAt() {
		return remindedAt;
	}

	public void setRemindedAt(final OffsetDateTime remindedAt) {
		this.remindedAt = remindedAt;
	}

	public StatementEntity withRemindedAt(final OffsetDateTime remindedAt) {
		this.remindedAt = remindedAt;
		return this;
	}

	public OffsetDateTime getRespondedAt() {
		return respondedAt;
	}

	public void setRespondedAt(final OffsetDateTime respondedAt) {
		this.respondedAt = respondedAt;
	}

	public StatementEntity withRespondedAt(final OffsetDateTime respondedAt) {
		this.respondedAt = respondedAt;
		return this;
	}

	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(final String outcome) {
		this.outcome = outcome;
	}

	public StatementEntity withOutcome(final String outcome) {
		this.outcome = outcome;
		return this;
	}

	public String getResponseText() {
		return responseText;
	}

	public void setResponseText(final String responseText) {
		this.responseText = responseText;
	}

	public StatementEntity withResponseText(final String responseText) {
		this.responseText = responseText;
		return this;
	}

	public String getCommunicationId() {
		return communicationId;
	}

	public void setCommunicationId(final String communicationId) {
		this.communicationId = communicationId;
	}

	public StatementEntity withCommunicationId(final String communicationId) {
		this.communicationId = communicationId;
		return this;
	}

	public List<AttachmentEntity> getAttachments() {
		return attachments;
	}

	public void setAttachments(final List<AttachmentEntity> attachments) {
		this.attachments = attachments;
	}

	public StatementEntity withAttachments(final List<AttachmentEntity> attachments) {
		this.attachments = attachments;
		return this;
	}

	public List<StatementJsonParameterEntity> getJsonParameterLinks() {
		return jsonParameterLinks;
	}

	public void setJsonParameterLinks(final List<StatementJsonParameterEntity> jsonParameterLinks) {
		this.jsonParameterLinks = jsonParameterLinks;
	}

	public StatementEntity withJsonParameterLinks(final List<StatementJsonParameterEntity> jsonParameterLinks) {
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
		final StatementEntity that = (StatementEntity) o;
		return Objects.equals(counterpartyName, that.counterpartyName)
			&& Objects.equals(counterpartyExternalId, that.counterpartyExternalId)
			&& Objects.equals(counterpartyExternalIdType, that.counterpartyExternalIdType)
			&& Objects.equals(counterpartyReference, that.counterpartyReference)
			&& Objects.equals(question, that.question)
			&& Objects.equals(sentAt, that.sentAt)
			&& Objects.equals(remindedAt, that.remindedAt)
			&& Objects.equals(respondedAt, that.respondedAt)
			&& Objects.equals(outcome, that.outcome)
			&& Objects.equals(responseText, that.responseText)
			&& Objects.equals(communicationId, that.communicationId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), counterpartyName, counterpartyExternalId, counterpartyExternalIdType, counterpartyReference, question, sentAt, remindedAt, respondedAt, outcome, responseText,
			communicationId);
	}

	@Override
	public String toString() {
		return "StatementEntity{" + super.toString() +
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
			'}';
	}
}
