package se.sundsvall.supportmanagement.api.model.message;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import org.springframework.format.annotation.DateTimeFormat;

import io.swagger.v3.oas.annotations.media.Schema;


public class Message {

	@Schema(description = "The message ID", example = "12")
	private String messageID;

	@Schema(description = "The errand number", example = "PRH-2022-000001")
	private String errandNumber;

	@Enumerated(EnumType.STRING)
	@Schema(description = "If the message is inbound or outbound from the perspective of " +
		"case-data/e-service.", example = "INBOUND")
	private Direction direction;

	@Schema(description = "The message body", example = "Hello world")
	private String messageBody;

	@Schema(description = "The time the message was sent", example = "2020-01-01 12:00:00")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private OffsetDateTime sent;

	@Schema(description = "The email-subject of the message", example = "Hello world")
	private String subject;

	@Schema(description = "The username of the user that sent the message", example = "username")
	private String username;

	@Schema(description = "The first name of the user that sent the message", example = "Kalle")
	private String firstName;

	@Schema(description = "The last name of the user that sent the message", example = "Anka")
	private String lastName;

	@Schema(description = "The message was delivered by", example = "EMAIL")
	private MessageType messageType;

	@Schema(description = "The mobile number of the recipient", example = "+46701234567")
	private String mobileNumber;

	@Schema(description = "The email of the user that sent the message", example = "kalle.anka@ankeborg.se")
	private String email;

	@Schema(description = "The user ID of the user that sent the message", example = "12")
	private String userID;

	@Schema(description = "Signal if the message has been viewed or not", example = "true")
	private boolean viewed;


	@Schema(description = "List of messageAttachments on the message")
	private List<MessageAttachment> messageAttachments;

	public static Message create() {
		return new Message();
	}

	public String getMessageID() {
		return messageID;
	}

	public void setMessageID(final String messageID) {
		this.messageID = messageID;
	}

	public Message withMessageID(final String messageID) {
		this.messageID = messageID;
		return this;
	}

	public String getErrandNumber() {
		return errandNumber;
	}

	public void setErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
	}

	public Message withErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
		return this;
	}

	public Direction getDirection() {
		return direction;
	}

	public void setDirection(final Direction direction) {
		this.direction = direction;
	}

	public Message withDirection(final Direction direction) {
		this.direction = direction;
		return this;
	}

	public String getMessageBody() {
		return messageBody;
	}

	public void setMessageBody(final String messageBody) {
		this.messageBody = messageBody;
	}

	public Message withMessageBody(final String messageBody) {
		this.messageBody = messageBody;
		return this;
	}

	public OffsetDateTime getSent() {
		return sent;
	}

	public void setSent(final OffsetDateTime sent) {
		this.sent = sent;
	}

	public Message withSent(final OffsetDateTime sent) {
		this.sent = sent;
		return this;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(final String subject) {
		this.subject = subject;
	}

	public Message withSubject(final String subject) {
		this.subject = subject;
		return this;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	public Message withUsername(final String username) {
		this.username = username;
		return this;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}

	public Message withFirstName(final String firstName) {
		this.firstName = firstName;
		return this;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	public Message withLastName(final String lastName) {
		this.lastName = lastName;
		return this;
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(final MessageType messageType) {
		this.messageType = messageType;
	}

	public Message withMessageType(final MessageType messageType) {
		this.messageType = messageType;
		return this;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public void setMobileNumber(final String mobileNumber) {
		this.mobileNumber = mobileNumber;
	}

	public Message withMobileNumber(final String mobileNumber) {
		this.mobileNumber = mobileNumber;
		return this;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(final String email) {
		this.email = email;
	}

	public Message withEmail(final String email) {
		this.email = email;
		return this;
	}

	public String getUserID() {
		return userID;
	}

	public void setUserID(final String userID) {
		this.userID = userID;
	}

	public Message withUserID(final String userID) {
		this.userID = userID;
		return this;
	}

	public boolean isViewed() {
		return viewed;
	}

	public void setViewed(final boolean viewed) {
		this.viewed = viewed;
	}

	public Message withViewed(final boolean viewed) {
		this.viewed = viewed;
		return this;
	}

	public List<MessageAttachment> getMessageAttachments() {
		return messageAttachments;
	}

	public void setMessageAttachments(final List<MessageAttachment> messageAttachments) {
		this.messageAttachments = messageAttachments;
	}

	public Message withMessageAttachments(final List<MessageAttachment> messageAttachments) {
		this.messageAttachments = messageAttachments;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final Message message = (Message) o;
		return viewed == message.viewed && Objects.equals(messageID, message.messageID) && Objects.equals(errandNumber, message.errandNumber) && direction == message.direction && Objects.equals(messageBody, message.messageBody) && Objects.equals(sent, message.sent) && Objects.equals(subject, message.subject) && Objects.equals(username, message.username) && Objects.equals(firstName, message.firstName) && Objects.equals(lastName, message.lastName) && messageType == message.messageType && Objects.equals(mobileNumber, message.mobileNumber) && Objects.equals(email, message.email) && Objects.equals(userID, message.userID) && Objects.equals(messageAttachments, message.messageAttachments);
	}

	@Override
	public int hashCode() {
		return Objects.hash(messageID, errandNumber, direction, messageBody, sent, subject, username, firstName, lastName, messageType, mobileNumber, email, userID, viewed, messageAttachments);
	}

	@Override
	public String toString() {
		return "Message{" +
			"messageID='" + messageID + '\'' +
			", errandNumber='" + errandNumber + '\'' +
			", direction=" + direction +
			", messageBody='" + messageBody + '\'' +
			", sent='" + sent + '\'' +
			", subject='" + subject + '\'' +
			", username='" + username + '\'' +
			", firstName='" + firstName + '\'' +
			", lastName='" + lastName + '\'' +
			", messageType=" + messageType +
			", mobileNumber='" + mobileNumber + '\'' +
			", email='" + email + '\'' +
			", userID='" + userID + '\'' +
			", viewed=" + viewed +
			", messageAttachments=" + messageAttachments +
			'}';
	}

}
