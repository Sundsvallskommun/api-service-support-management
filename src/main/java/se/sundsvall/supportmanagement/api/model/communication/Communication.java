package se.sundsvall.supportmanagement.api.model.communication;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import org.springframework.format.annotation.DateTimeFormat;

import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;

import io.swagger.v3.oas.annotations.media.Schema;

public class Communication {

	@Schema(description = "The communication ID", example = "12")
	private String communicationID;

	@Schema(description = "The errand number", example = "PRH-2022-000001")
	private String errandNumber;

	@Enumerated(EnumType.STRING)
	@Schema(description = "If the communication is inbound or outbound from the perspective of " +
		"case-data/e-service.", example = "INBOUND")
	private Direction direction;

	@Schema(description = "The message body", example = "Hello world")
	private String messageBody;

	@Schema(description = "The time the communication was sent", example = "2020-01-01 12:00:00")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private OffsetDateTime sent;

	@Schema(description = "The email-subject of the message", example = "Hello world")
	private String subject;

	@Schema(description = "The message was delivered by", example = "EMAIL")
	private CommunicationType communicationType;

	@Schema(description = "The mobile number of the recipient", example = "+46701234567")
	private String mobileNumber;

	@Schema(description = "The email of the user that sent the message", example = "kalle.anka@ankeborg.se")
	private String email;

	@Schema(description = "Signal if the message has been viewed or not", example = "true")
	private boolean viewed;
	
	@Schema(description = "List of communicationAttachments on the message")
	private List<CommunicationAttachment> communicationAttachments;

	public static Communication create() {
		return new Communication();
	}

	public String getCommunicationID() {
		return communicationID;
	}

	public void setCommunicationID(final String communicationID) {
		this.communicationID = communicationID;
	}

	public Communication withCommunicationID(final String communicationID) {
		this.communicationID = communicationID;
		return this;
	}

	public String getErrandNumber() {
		return errandNumber;
	}

	public void setErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
	}

	public Communication withErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
		return this;
	}

	public Direction getDirection() {
		return direction;
	}

	public void setDirection(final Direction direction) {
		this.direction = direction;
	}

	public Communication withDirection(final Direction direction) {
		this.direction = direction;
		return this;
	}

	public String getMessageBody() {
		return messageBody;
	}

	public void setMessageBody(final String messageBody) {
		this.messageBody = messageBody;
	}

	public Communication withMessageBody(final String messageBody) {
		this.messageBody = messageBody;
		return this;
	}

	public OffsetDateTime getSent() {
		return sent;
	}

	public void setSent(final OffsetDateTime sent) {
		this.sent = sent;
	}

	public Communication withSent(final OffsetDateTime sent) {
		this.sent = sent;
		return this;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(final String subject) {
		this.subject = subject;
	}

	public Communication withSubject(final String subject) {
		this.subject = subject;
		return this;
	}


	public CommunicationType getCommunicationType() {
		return communicationType;
	}

	public void setCommunicationType(final CommunicationType communicationType) {
		this.communicationType = communicationType;
	}

	public Communication withCommunicationType(final CommunicationType communicationType) {
		this.communicationType = communicationType;
		return this;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public void setMobileNumber(final String mobileNumber) {
		this.mobileNumber = mobileNumber;
	}

	public Communication withMobileNumber(final String mobileNumber) {
		this.mobileNumber = mobileNumber;
		return this;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(final String email) {
		this.email = email;
	}

	public Communication withEmail(final String email) {
		this.email = email;
		return this;
	}

	public boolean isViewed() {
		return viewed;
	}

	public void setViewed(final boolean viewed) {
		this.viewed = viewed;
	}

	public Communication withViewed(final boolean viewed) {
		this.viewed = viewed;
		return this;
	}

	public List<CommunicationAttachment> getCommunicationAttachments() {
		return communicationAttachments;
	}

	public void setCommunicationAttachments(final List<CommunicationAttachment> communicationAttachments) {
		this.communicationAttachments = communicationAttachments;
	}

	public Communication withCommunicationAttachments(final List<CommunicationAttachment> communicationAttachments) {
		this.communicationAttachments = communicationAttachments;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final Communication that = (Communication) o;
		return viewed == that.viewed && Objects.equals(communicationID, that.communicationID) && Objects.equals(errandNumber, that.errandNumber) && direction == that.direction && Objects.equals(messageBody, that.messageBody) && Objects.equals(sent, that.sent) && Objects.equals(subject, that.subject) && communicationType == that.communicationType && Objects.equals(mobileNumber, that.mobileNumber) && Objects.equals(email, that.email) && Objects.equals(communicationAttachments, that.communicationAttachments);
	}

	@Override
	public int hashCode() {
		return Objects.hash(communicationID, errandNumber, direction, messageBody, sent, subject, communicationType, mobileNumber, email, viewed, communicationAttachments);
	}

	@Override
	public String toString() {
		return "Communication{" +
			"communicationID='" + communicationID + '\'' +
			", errandNumber='" + errandNumber + '\'' +
			", direction=" + direction +
			", messageBody='" + messageBody + '\'' +
			", sent=" + sent +
			", subject='" + subject + '\'' +
			", communicationType=" + communicationType +
			", mobileNumber='" + mobileNumber + '\'' +
			", email='" + email + '\'' +
			", viewed=" + viewed +
			", communicationAttachments=" + communicationAttachments +
			'}';
	}

}
