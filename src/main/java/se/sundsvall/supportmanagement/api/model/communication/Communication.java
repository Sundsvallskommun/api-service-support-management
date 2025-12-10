package se.sundsvall.supportmanagement.api.model.communication;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;
import se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader;

public class Communication {

	@Schema(description = "The communication ID", examples = "12")
	private String communicationID;

	@Schema(description = "Sender of the communication.", examples = "Test Testsson")
	private String sender;

	@Schema(description = "The errand number", examples = "PRH-2022-000001")
	private String errandNumber;

	@Schema(description = "If the communication is inbound or outbound from the perspective of case-data/e-service.", examples = "INBOUND")
	private Direction direction;

	@Schema(description = "The message body", examples = "Hello world")
	private String messageBody;

	@Schema(description = "The message body in HTML format", examples = "<p>Hello world</p>")
	private String htmlMessageBody;

	@Schema(description = "The time the communication was sent", examples = "2020-01-01 12:00:00")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private OffsetDateTime sent;

	@Schema(description = "The email-subject of the communication", examples = "Hello world")
	private String subject;

	@Schema(description = "The communication was delivered by", examples = "EMAIL")
	private CommunicationType communicationType;

	@Schema(description = "The mobile number or email adress the communication was sent to", examples = "+46701740605")
	private String target;

	@Schema(description = "The recipients of the communication, if email", examples = "[\"kalle.anka@ankeborg.se\"]")
	private List<String> recipients;

	@Schema(description = "Indicates if the communication is internal", examples = "false")
	private boolean internal;

	@Schema(description = "Signal if the communication has been viewed or not", examples = "true")
	private Boolean viewed;

	@Schema(description = "Headers for keeping track of email conversations", examples = "{\"IN_REPLY_TO\": [\"reply-to@example.com\"], \"REFERENCES\": [\"reference1\", \"reference2\"], \"MESSAGE_ID\": [\"123456789\"]}")
	private Map<EmailHeader, List<String>> emailHeaders;

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

	public String getSender() {
		return sender;
	}

	public void setSender(final String sender) {
		this.sender = sender;
	}

	public Communication withSender(final String sender) {
		this.sender = sender;
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

	public String getHtmlMessageBody() {
		return htmlMessageBody;
	}

	public void setHtmlMessageBody(final String htmlMessageBody) {
		this.htmlMessageBody = htmlMessageBody;
	}

	public Communication withHtmlMessageBody(final String htmlMessageBody) {
		this.htmlMessageBody = htmlMessageBody;
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

	public boolean isInternal() {
		return internal;
	}

	public void setInternal(final boolean internal) {
		this.internal = internal;
	}

	public Communication withInternal(final boolean internal) {
		this.internal = internal;
		return this;
	}

	public Communication withCommunicationType(final CommunicationType communicationType) {
		this.communicationType = communicationType;
		return this;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(final String target) {
		this.target = target;
	}

	public Communication withTarget(final String target) {
		this.target = target;
		return this;
	}

	public List<String> getRecipients() {
		return recipients;
	}

	public void setRecipients(final List<String> recipients) {
		this.recipients = recipients;
	}

	public Communication withRecipients(final List<String> recipients) {
		this.recipients = recipients;
		return this;
	}

	public Boolean getViewed() {
		return viewed;
	}

	public void setViewed(final Boolean viewed) {
		this.viewed = viewed;
	}

	public Communication withViewed(final Boolean viewed) {
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

	public Map<EmailHeader, List<String>> getEmailHeaders() {
		return emailHeaders;
	}

	public void setEmailHeaders(final Map<EmailHeader, List<String>> emailHeaders) {
		this.emailHeaders = emailHeaders;
	}

	public Communication withEmailHeaders(final Map<EmailHeader, List<String>> headers) {
		this.emailHeaders = headers;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Communication that = (Communication) o;
		return internal == that.internal && Objects.equals(communicationID, that.communicationID) && Objects.equals(sender, that.sender) && Objects.equals(errandNumber, that.errandNumber) && direction == that.direction
			&& Objects.equals(messageBody, that.messageBody) && Objects.equals(htmlMessageBody, that.htmlMessageBody) && Objects.equals(sent, that.sent) && Objects.equals(subject, that.subject)
			&& communicationType == that.communicationType && Objects.equals(target, that.target) && Objects.equals(recipients, that.recipients) && Objects.equals(viewed, that.viewed) && Objects.equals(emailHeaders,
				that.emailHeaders) && Objects.equals(communicationAttachments, that.communicationAttachments);
	}

	@Override
	public int hashCode() {
		return Objects.hash(communicationID, sender, errandNumber, direction, messageBody, htmlMessageBody, sent, subject, communicationType, target, recipients, internal, viewed, emailHeaders, communicationAttachments);
	}

	@Override
	public String toString() {
		return "Communication{" +
			"communicationID='" + communicationID + '\'' +
			", sender='" + sender + '\'' +
			", errandNumber='" + errandNumber + '\'' +
			", direction=" + direction +
			", messageBody='" + messageBody + '\'' +
			", htmlMessageBody='" + htmlMessageBody + '\'' +
			", sent=" + sent +
			", subject='" + subject + '\'' +
			", communicationType=" + communicationType +
			", target='" + target + '\'' +
			", recipients=" + recipients +
			", internal=" + internal +
			", viewed=" + viewed +
			", emailHeaders=" + emailHeaders +
			", communicationAttachments=" + communicationAttachments +
			'}';
	}
}
