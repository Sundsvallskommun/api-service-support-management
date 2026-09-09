package se.sundsvall.supportmanagement.api.model.communication;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "BulkEmailRequest model")
public class BulkEmailRequest {

	@NotNull
	@Email
	@Schema(description = "Email address for sender", example = "sender@sender.se", requiredMode = REQUIRED)
	private String sender;

	@Schema(description = "Optional display name of sender on email. If left out, email will be displayed as sender name.", example = "Firstname Lastname", requiredMode = NOT_REQUIRED)
	private String senderName;

	@NotEmpty
	@ArraySchema(schema = @Schema(description = "Email addresses for recipients", example = "recipient@recipient.se", requiredMode = REQUIRED))
	private List<@NotNull @Email String> recipients;

	@NotBlank
	@Schema(description = "Subject", example = "Subject", requiredMode = REQUIRED)
	private String subject;

	@NotBlank
	@Schema(description = "Message in html (optionally in BASE64 encoded format)", example = "<html>HTML-formatted message</html>", requiredMode = REQUIRED)
	private String htmlMessage;

	@NotBlank
	@Schema(description = "Message in plain text", example = "Message in plain text", requiredMode = REQUIRED)
	private String message;

	@Schema(description = "Headers for keeping track of email conversations", example = "{\"IN_REPLY_TO\": [\"reply-to@example.com\"], \"REFERENCES\": [\"reference1\", \"reference2\"], \"MESSAGE_ID\": [\"123456789\"]}")
	private Map<EmailHeader, List<String>> emailHeaders;

	@ArraySchema(schema = @Schema(description = "List with Base64 encoded email attachments"))
	private List<@Valid EmailAttachment> attachments;

	@ArraySchema(schema = @Schema(description = "List with attachment ids"))
	private List<String> attachmentIds;

	public static BulkEmailRequest create() {
		return new BulkEmailRequest();
	}

	public String getSender() {
		return sender;
	}

	public void setSender(final String sender) {
		this.sender = sender;
	}

	public BulkEmailRequest withSender(final String sender) {
		this.sender = sender;
		return this;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(final String senderName) {
		this.senderName = senderName;
	}

	public BulkEmailRequest withSenderName(final String senderName) {
		this.senderName = senderName;
		return this;
	}

	public List<String> getRecipients() {
		return recipients;
	}

	public void setRecipients(final List<String> recipients) {
		this.recipients = recipients;
	}

	public BulkEmailRequest withRecipients(final List<String> recipients) {
		this.recipients = recipients;
		return this;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(final String subject) {
		this.subject = subject;
	}

	public BulkEmailRequest withSubject(final String subject) {
		this.subject = subject;
		return this;
	}

	public String getHtmlMessage() {
		return htmlMessage;
	}

	public void setHtmlMessage(final String htmlMessage) {
		this.htmlMessage = htmlMessage;
	}

	public BulkEmailRequest withHtmlMessage(final String htmlMessage) {
		this.htmlMessage = htmlMessage;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public BulkEmailRequest withMessage(final String message) {
		this.message = message;
		return this;
	}

	public Map<EmailHeader, List<String>> getEmailHeaders() {
		return emailHeaders;
	}

	public void setEmailHeaders(final Map<EmailHeader, List<String>> emailHeaders) {
		this.emailHeaders = emailHeaders;
	}

	public BulkEmailRequest withEmailHeaders(final Map<EmailHeader, List<String>> emailHeaders) {
		this.emailHeaders = emailHeaders;
		return this;
	}

	public List<EmailAttachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(final List<EmailAttachment> attachments) {
		this.attachments = attachments;
	}

	public BulkEmailRequest withAttachments(final List<EmailAttachment> attachments) {
		this.attachments = attachments;
		return this;
	}

	public List<String> getAttachmentIds() {
		return attachmentIds;
	}

	public void setAttachmentIds(final List<String> attachmentIds) {
		this.attachmentIds = attachmentIds;
	}

	public BulkEmailRequest withAttachmentIds(final List<String> attachmentIds) {
		this.attachmentIds = attachmentIds;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final var that = (BulkEmailRequest) o;
		return Objects.equals(sender, that.sender)
			&& Objects.equals(senderName, that.senderName)
			&& Objects.equals(recipients, that.recipients)
			&& Objects.equals(subject, that.subject)
			&& Objects.equals(htmlMessage, that.htmlMessage)
			&& Objects.equals(message, that.message)
			&& Objects.equals(emailHeaders, that.emailHeaders)
			&& Objects.equals(attachments, that.attachments)
			&& Objects.equals(attachmentIds, that.attachmentIds);
	}

	@Override
	public int hashCode() {
		return Objects.hash(sender, senderName, recipients, subject, htmlMessage, message, emailHeaders, attachments, attachmentIds);
	}

	@Override
	public String toString() {
		return "BulkEmailRequest{" +
			"sender='" + sender + '\'' +
			", senderName='" + senderName + '\'' +
			", recipients=" + recipients +
			", subject='" + subject + '\'' +
			", htmlMessage='" + htmlMessage + '\'' +
			", message='" + message + '\'' +
			", emailHeaders=" + emailHeaders +
			", attachments=" + attachments +
			", attachmentIds=" + attachmentIds +
			'}';
	}
}
