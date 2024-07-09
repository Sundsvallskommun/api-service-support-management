package se.sundsvall.supportmanagement.integration.db.model;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Length;
import org.hibernate.annotations.TimeZoneStorage;

import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;


@Entity
@Table(name = "communication",
	indexes = {@Index(name = "idx_errand_number", columnList = "errand_number")})
public class CommunicationEntity {

	@Id
	@Column(name = "id")
	private String id;

	@Column(name = "sender")
	private String sender;

	@Column(name = "errand_number")
	private String errandNumber;

	@Column(name = "direction")
	@Enumerated(EnumType.STRING)
	private Direction direction;

	@Column(name = "external_case_id")
	private String externalCaseID;

	@Column(name = "subject")
	private String subject;

	@Column(name = "message_body", length = Length.LONG32)
	private String messageBody;

	@Column(name = "sent")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime sent;

	@Column(name = "type")
	@Enumerated(EnumType.STRING)
	private CommunicationType type;

	@Column(name = "target")
	private String target;

	@Column(name = "viewed")
	private boolean viewed;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "communicationEntity")
	private List<CommunicationAttachmentEntity> attachments;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "communication_id", referencedColumnName = "id",
		foreignKey = @ForeignKey(name = "fk_email_header_email_id"))
	private List<CommunicationEmailHeaderEntity> emailHeaders;

	@ManyToMany
	@JoinTable(
		name = "communication_errand_attachment",
		joinColumns = {@JoinColumn(name = "communication_id")},
		inverseJoinColumns = {@JoinColumn(name = "errand_attachment_id")}
	)
	private List<AttachmentEntity> errandAttachments;

	public static CommunicationEntity create() {
		return new CommunicationEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public CommunicationEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public List<AttachmentEntity> getErrandAttachments() {
		return errandAttachments;
	}

	public void setErrandAttachments(final List<AttachmentEntity> errandAttachments) {
		this.errandAttachments = errandAttachments;
	}

	public CommunicationEntity withErrandAttachments(final List<AttachmentEntity> errandAttachments) {
		this.errandAttachments = errandAttachments;
		return this;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(final String sender) {
		this.sender = sender;
	}

	public CommunicationEntity withSender(final String sender) {
		this.sender = sender;
		return this;
	}

	public String getErrandNumber() {
		return errandNumber;
	}

	public void setErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
	}

	public CommunicationEntity withErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
		return this;
	}

	public Direction getDirection() {
		return direction;
	}

	public void setDirection(final Direction direction) {
		this.direction = direction;
	}

	public CommunicationEntity withDirection(final Direction direction) {
		this.direction = direction;
		return this;
	}

	public String getExternalCaseID() {
		return externalCaseID;
	}

	public void setExternalCaseID(final String externalCaseID) {
		this.externalCaseID = externalCaseID;
	}

	public CommunicationEntity withExternalCaseID(final String externalCaseID) {
		this.externalCaseID = externalCaseID;
		return this;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(final String subject) {
		this.subject = subject;
	}

	public CommunicationEntity withSubject(final String subject) {
		this.subject = subject;
		return this;
	}

	public String getMessageBody() {
		return messageBody;
	}

	public void setMessageBody(final String messageBody) {
		this.messageBody = messageBody;
	}

	public CommunicationEntity withMessageBody(final String messageBody) {
		this.messageBody = messageBody;
		return this;
	}

	public OffsetDateTime getSent() {
		return sent;
	}

	public void setSent(final OffsetDateTime sent) {
		this.sent = sent;
	}

	public CommunicationEntity withSent(final OffsetDateTime sent) {
		this.sent = sent;
		return this;
	}

	public CommunicationType getType() {
		return type;
	}

	public void setType(final CommunicationType type) {
		this.type = type;
	}

	public CommunicationEntity withType(final CommunicationType type) {
		this.type = type;
		return this;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(final String target) {
		this.target = target;
	}

	public CommunicationEntity withTarget(final String target) {
		this.target = target;
		return this;
	}

	public boolean isViewed() {
		return viewed;
	}

	public void setViewed(final boolean viewed) {
		this.viewed = viewed;
	}

	public CommunicationEntity withViewed(final boolean viewed) {
		this.viewed = viewed;
		return this;
	}

	public List<CommunicationAttachmentEntity> getAttachments() {
		return attachments;
	}

	public void setAttachments(final List<CommunicationAttachmentEntity> attachments) {
		if (attachments != null) {
			attachments.forEach(attachment -> attachment.withCommunicationEntity(this));
		}
		this.attachments = attachments;
	}

	public CommunicationEntity withAttachments(final List<CommunicationAttachmentEntity> attachments) {
		setAttachments(attachments);
		return this;
	}

	public List<CommunicationEmailHeaderEntity> getEmailHeaders() {
		return emailHeaders;
	}

	public void setEmailHeaders(final List<CommunicationEmailHeaderEntity> emailHeaders) {
		this.emailHeaders = emailHeaders;
	}

	public CommunicationEntity withEmailHeaders(final List<CommunicationEmailHeaderEntity> emailHeaders) {
		this.emailHeaders = emailHeaders;
		return this;
	}

	@Override
	public String toString() {
		return "CommunicationEntity{" +
			"id='" + id + '\'' +
			", sender='" + sender + '\'' +
			", errandNumber='" + errandNumber + '\'' +
			", direction=" + direction +
			", externalCaseID='" + externalCaseID + '\'' +
			", subject='" + subject + '\'' +
			", messageBody='" + messageBody + '\'' +
			", sent=" + sent +
			", type=" + type +
			", target='" + target + '\'' +
			", viewed=" + viewed +
			", attachments=" + attachments +
			", emailHeaders=" + emailHeaders +
			", errandAttachments=" + errandAttachments +
			'}';
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final CommunicationEntity that = (CommunicationEntity) o;
		return viewed == that.viewed && Objects.equals(id, that.id) && Objects.equals(sender, that.sender) && Objects.equals(errandNumber, that.errandNumber) && direction == that.direction && Objects.equals(externalCaseID, that.externalCaseID) && Objects.equals(subject, that.subject) && Objects.equals(messageBody, that.messageBody) && Objects.equals(sent, that.sent) && type == that.type && Objects.equals(target, that.target) && Objects.equals(attachments, that.attachments) && Objects.equals(emailHeaders, that.emailHeaders) && Objects.equals(errandAttachments, that.errandAttachments);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, sender, errandNumber, direction, externalCaseID, subject, messageBody, sent, type, target, viewed, attachments, emailHeaders, errandAttachments);
	}
}
