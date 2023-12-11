package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;


@Entity
@Table(name = "communication",
	indexes = {@Index(name = "idx_errand_number", columnList = "errand_number")},
	uniqueConstraints = {@UniqueConstraint(name = "uq_errand_number", columnNames = {"errand_number"})})
public class CommunicationEntity {

	@Id
	@Column(name = "communication_id")
	private String communicationID;

	@Column(name = "errand_number")
	private String errandNumber;

	@Column(name = "direction")
	@Enumerated(EnumType.STRING)
	private Direction direction;

	@Column(name = "external_case_id")
	private String externalCaseID;

	@Column(name = "subject")
	private String subject;

	@Column(name = "message_body")
	private String messageBody;

	@Column(name = "sent")
	private OffsetDateTime sent;

	@Column(name = "message_type")
	@Enumerated(EnumType.STRING)
	private CommunicationType communicationType;

	@Column(name = "mobile_number")
	private String mobileNumber;

	@Column(name = "email")
	private String email;

	@Column(name = "viewed")
	private boolean viewed;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "communicationID")
	private List<CommunicationAttachmentEntity> attachments;

	public static CommunicationEntity create() {
		return new CommunicationEntity();
	}

	public String getCommunicationID() {
		return communicationID;
	}

	public void setCommunicationID(final String communicationID) {
		this.communicationID = communicationID;
	}

	public CommunicationEntity withCommunicationID(final String communicationID) {
		this.communicationID = communicationID;
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

	public CommunicationType getCommunicationType() {
		return communicationType;
	}

	public void setCommunicationType(final CommunicationType communicationType) {
		this.communicationType = communicationType;
	}

	public CommunicationEntity withCommunicationType(final CommunicationType communicationType) {
		this.communicationType = communicationType;
		return this;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public void setMobileNumber(final String mobileNumber) {
		this.mobileNumber = mobileNumber;
	}

	public CommunicationEntity withMobileNumber(final String mobileNumber) {
		this.mobileNumber = mobileNumber;
		return this;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(final String email) {
		this.email = email;
	}

	public CommunicationEntity withEmail(final String email) {
		this.email = email;
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
		this.attachments = attachments;
	}

	public CommunicationEntity withAttachments(final List<CommunicationAttachmentEntity> attachments) {
		this.attachments = attachments;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final CommunicationEntity that = (CommunicationEntity) o;
		return viewed == that.viewed && Objects.equals(communicationID, that.communicationID) && Objects.equals(errandNumber, that.errandNumber) && direction == that.direction && Objects.equals(externalCaseID, that.externalCaseID) && Objects.equals(subject, that.subject) && Objects.equals(messageBody, that.messageBody) && Objects.equals(sent, that.sent) && communicationType == that.communicationType && Objects.equals(mobileNumber, that.mobileNumber) && Objects.equals(email, that.email) && Objects.equals(attachments, that.attachments);
	}

	@Override
	public int hashCode() {
		return Objects.hash(communicationID, errandNumber, direction, externalCaseID, subject, messageBody, sent, communicationType, mobileNumber, email, viewed, attachments);
	}

	@Override
	public String toString() {
		return "CommunicationEntity{" +
			"communicationID='" + communicationID + '\'' +
			", errandNumber='" + errandNumber + '\'' +
			", direction=" + direction +
			", externalCaseID='" + externalCaseID + '\'' +
			", subject='" + subject + '\'' +
			", messageBody='" + messageBody + '\'' +
			", sent=" + sent +
			", communicationType=" + communicationType +
			", mobileNumber='" + mobileNumber + '\'' +
			", email='" + email + '\'' +
			", viewed=" + viewed +
			", attachments=" + attachments +
			'}';
	}

}
