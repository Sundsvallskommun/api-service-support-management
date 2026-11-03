package se.sundsvall.supportmanagement.api.model.communication;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import se.sundsvall.supportmanagement.api.validation.ValidEmailRecipients;
import se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

@ValidEmailRecipients
@Schema(description = "EmailRequest model")
public class EmailRequest {

	@NotNull
	@Email
	@Schema(description = "Email address for sender", examples = "sender@sender.se", requiredMode = REQUIRED)
	private String sender;

	@Schema(description = "Optional display name of sender on email. If left out, email will be displayed as sender name.", examples = "Firstname Lastname", requiredMode = NOT_REQUIRED)
	private String senderName;

	@Email
	@Schema(description = "Email address for recipient. Deprecated, use 'recipients' instead. At least one of 'recipient' and 'recipients' must be set.", examples = "recipient@recipient.se", deprecated = true)
	private String recipient;

	@ArraySchema(schema = @Schema(description = "Email addresses for recipients (To). At least one of 'recipient' and 'recipients' must be set.", examples = "recipient@recipient.se"))
	private List<@NotNull @Email String> recipients;

	@ArraySchema(schema = @Schema(description = "Email addresses for carbon copy recipients (Cc)", examples = "cc-recipient@recipient.se"))
	private List<@NotNull @Email String> cc;

	@NotBlank
	@Schema(description = "Subject", examples = "Subject", requiredMode = REQUIRED)
	private String subject;

	@NotBlank
	@Schema(description = "Message in html (optionally in BASE64 encoded format)", examples = "<html>HTML-formatted message</html>", requiredMode = REQUIRED)
	private String htmlMessage;

	@NotBlank
	@Schema(description = "Message in plain text", examples = "Message in plain text", requiredMode = REQUIRED)
	private String message;

	@Schema(description = "Indicates if the message is internal", examples = "false")
	private boolean internal;

	@Schema(description = "Headers for keeping track of email conversations", examples = "{\"IN_REPLY_TO\": [\"reply-to@example.com\"], \"REFERENCES\": [\"reference1\", \"reference2\"], \"MESSAGE_ID\": [\"123456789\"]}")
	private Map<EmailHeader, List<String>> emailHeaders;

	@ArraySchema(schema = @Schema(description = "List with Base64 encoded email attachments"))
	private List<@Valid EmailAttachment> attachments;

	@ArraySchema(schema = @Schema(description = "List with attachment ids"))
	private List<String> attachmentIds;

	public static EmailRequest create() {
		return new EmailRequest();
	}

	public List<String> getAttachmentIds() {
		return attachmentIds;
	}

	public void setAttachmentIds(final List<String> attachmentIds) {
		this.attachmentIds = attachmentIds;
	}

	public EmailRequest withAttachmentIds(final List<String> attachmentIds) {
		this.attachmentIds = attachmentIds;
		return this;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(final String sender) {
		this.sender = sender;
	}

	public EmailRequest withSender(final String sender) {
		this.sender = sender;
		return this;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(final String senderName) {
		this.senderName = senderName;
	}

	public EmailRequest withSenderName(final String senderName) {
		this.senderName = senderName;
		return this;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(final String recipient) {
		this.recipient = recipient;
	}

	public EmailRequest withRecipient(final String recipient) {
		this.recipient = recipient;
		return this;
	}

	public List<String> getRecipients() {
		return recipients;
	}

	public void setRecipients(final List<String> recipients) {
		this.recipients = recipients;
	}

	public EmailRequest withRecipients(final List<String> recipients) {
		this.recipients = recipients;
		return this;
	}

	public List<String> getCc() {
		return cc;
	}

	public void setCc(final List<String> cc) {
		this.cc = cc;
	}

	public EmailRequest withCc(final List<String> cc) {
		this.cc = cc;
		return this;
	}

	/**
	 * Returns the recipients from the deprecated {@code recipient} and from {@code recipients}, in that order, without
	 * blank addresses and duplicates.
	 *
	 * @return the recipients, or an empty list if there are none
	 */
	public List<String> allRecipients() {
		return nonBlankDistinct(Stream.concat(Stream.ofNullable(recipient), ofNullable(recipients).orElse(emptyList()).stream()));
	}

	/**
	 * Returns the carbon copy recipients from {@code cc}, without blank addresses and duplicates.
	 *
	 * @return the carbon copy recipients, or an empty list if there are none
	 */
	public List<String> allCc() {
		return nonBlankDistinct(ofNullable(cc).orElse(emptyList()).stream());
	}

	private static List<String> nonBlankDistinct(final Stream<String> addresses) {
		return addresses
			.filter(StringUtils::isNotBlank)
			.distinct()
			.toList();
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(final String subject) {
		this.subject = subject;
	}

	public EmailRequest withSubject(final String subject) {
		this.subject = subject;
		return this;
	}

	public String getHtmlMessage() {
		return htmlMessage;
	}

	public void setHtmlMessage(final String htmlMessage) {
		this.htmlMessage = htmlMessage;
	}

	public EmailRequest withHtmlMessage(final String htmlMessage) {
		this.htmlMessage = htmlMessage;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public EmailRequest withMessage(final String message) {
		this.message = message;
		return this;
	}

	public List<EmailAttachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(final List<EmailAttachment> attachments) {
		this.attachments = attachments;
	}

	public EmailRequest withAttachments(final List<EmailAttachment> attachments) {
		this.attachments = attachments;
		return this;
	}

	public Map<EmailHeader, List<String>> getEmailHeaders() {
		return emailHeaders;
	}

	public void setEmailHeaders(final Map<EmailHeader, List<String>> emailHeaders) {
		this.emailHeaders = emailHeaders;
	}

	public EmailRequest withEmailHeaders(final Map<EmailHeader, List<String>> headers) {
		this.emailHeaders = headers;
		return this;
	}

	public boolean getInternal() {
		return internal;
	}

	public void setInternal(final boolean internal) {
		this.internal = internal;
	}

	public EmailRequest withInternal(final boolean internal) {
		this.internal = internal;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final var that = (EmailRequest) o;
		return Objects.equals(sender, that.sender)
			&& Objects.equals(senderName, that.senderName)
			&& Objects.equals(recipient, that.recipient)
			&& Objects.equals(recipients, that.recipients)
			&& Objects.equals(cc, that.cc)
			&& Objects.equals(subject, that.subject)
			&& Objects.equals(htmlMessage, that.htmlMessage)
			&& Objects.equals(message, that.message)
			&& Objects.equals(internal, that.internal)
			&& Objects.equals(emailHeaders, that.emailHeaders)
			&& Objects.equals(attachments, that.attachments)
			&& Objects.equals(attachmentIds, that.attachmentIds);
	}

	@Override
	public int hashCode() {
		return Objects.hash(sender, senderName, recipient, recipients, cc, subject, htmlMessage, message, internal, emailHeaders, attachments, attachmentIds);
	}

	@Override
	public String toString() {
		return "EmailRequest{" +
			"sender='" + sender + '\'' +
			", senderName='" + senderName + '\'' +
			", recipient='" + recipient + '\'' +
			", recipients=" + recipients +
			", cc=" + cc +
			", subject='" + subject + '\'' +
			", htmlMessage='" + htmlMessage + '\'' +
			", message='" + message + '\'' +
			", internal=" + internal +
			", emailHeaders=" + emailHeaders +
			", attachments=" + attachments +
			", attachmentIds=" + attachmentIds +
			'}';
	}
}
