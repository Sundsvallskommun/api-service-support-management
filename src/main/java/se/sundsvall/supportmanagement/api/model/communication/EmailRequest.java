package se.sundsvall.supportmanagement.api.model.communication;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

public class EmailRequest {

	@NotNull
	@Email
	@Schema(description = "Email address for sender", example = "sender@sender.se", requiredMode = REQUIRED)
	private String sender;

	@Schema(description = "Optional displayname of sender on email. If left out, email will be displayed as sender name.", example = "Firstname Lastname", requiredMode = NOT_REQUIRED)
	private String senderName;

	@NotNull
	@Email
	@Schema(description = "Email address for recipient", example = "recipient@recipient.se", requiredMode = REQUIRED)
	private String recipient;

	@NotBlank
	@Schema(description = "Subject", example = "Subject", requiredMode = REQUIRED)
	private String subject;

	@NotBlank
	@Schema(description = "Message in html (optionally in BASE64 encoded format)", example = "<html>HTML-formatted message</html>", requiredMode = REQUIRED)
	private String htmlMessage;

	@NotBlank
	@Schema(description = "Message in plain text", example = "Message in plain text", requiredMode = REQUIRED)
	private String message;

	@ArraySchema(schema = @Schema(description = "List with Base64 encoded email attachments"))
	private List<@Valid EmailAttachment> attachments;

	public static EmailRequest create() {
		return new EmailRequest();
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public EmailRequest withSender(String sender) {
		this.sender = sender;
		return this;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}

	public EmailRequest withSenderName(String senderName) {
		this.senderName = senderName;
		return this;
	}

	public String getRecipient() {
		return recipient;
	}

	public EmailRequest withRecipient(String recipient) {
		this.recipient = recipient;
		return this;
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public EmailRequest withSubject(String subject) {
		this.subject = subject;
		return this;
	}

	public String getHtmlMessage() {
		return htmlMessage;
	}

	public void setHtmlMessage(String htmlMessage) {
		this.htmlMessage = htmlMessage;
	}

	public EmailRequest withHtmlMessage(String htmlMessage) {
		this.htmlMessage = htmlMessage;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public EmailRequest withMessage(String message) {
		this.message = message;
		return this;
	}

	public List<EmailAttachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<EmailAttachment> attachments) {
		this.attachments = attachments;
	}

	public EmailRequest withAttachments(List<EmailAttachment> attachments) {
		this.attachments = attachments;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(attachments, htmlMessage, message, recipient, sender, senderName, subject);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		EmailRequest other = (EmailRequest) obj;
		return Objects.equals(attachments, other.attachments) && Objects.equals(htmlMessage, other.htmlMessage) && Objects.equals(message, other.message) && Objects.equals(recipient, other.recipient) && Objects.equals(sender, other.sender) && Objects
			.equals(senderName, other.senderName) && Objects.equals(subject, other.subject);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("EmailRequest [sender=").append(sender).append(", senderName=").append(senderName).append(", recipient=").append(recipient).append(", subject=").append(subject).append(", htmlMessage=").append(htmlMessage).append(", message=")
			.append(message).append(", attachments=").append(attachments).append("]");
		return builder.toString();
	}
}
