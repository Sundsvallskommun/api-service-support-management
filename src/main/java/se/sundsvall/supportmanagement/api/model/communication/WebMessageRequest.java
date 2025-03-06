package se.sundsvall.supportmanagement.api.model.communication;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Objects;

@Schema(description = "WebMessageRequest model")
public class WebMessageRequest {

	@Schema(description = "Indicates if the message is internal", example = "false")
	private boolean internal;

	@NotBlank
	@Schema(description = "Message in plain text", example = "Message in plain text", requiredMode = REQUIRED)
	private String message;

	@ArraySchema(schema = @Schema(description = "List with Base64 encoded web message attachments"))
	private List<@Valid WebMessageAttachment> attachments;

	@ArraySchema(schema = @Schema(description = "List with attachment ids"))
	private List<String> attachmentIds;

	public static WebMessageRequest create() {
		return new WebMessageRequest();
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public boolean getInternal() {
		return internal;
	}

	public void setInternal(final boolean internal) {
		this.internal = internal;
	}

	public WebMessageRequest withInternal(final boolean internal) {
		this.internal = internal;
		return this;
	}

	public WebMessageRequest withMessage(final String message) {
		this.message = message;
		return this;
	}

	public List<WebMessageAttachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(final List<WebMessageAttachment> attachments) {
		this.attachments = attachments;
	}

	public WebMessageRequest withAttachments(final List<WebMessageAttachment> attachments) {
		this.attachments = attachments;
		return this;
	}

	public List<String> getAttachmentIds() {
		return attachmentIds;
	}

	public void setAttachmentIds(final List<String> attachmentIds) {
		this.attachmentIds = attachmentIds;
	}

	public WebMessageRequest withAttachmentIds(final List<String> attachmentIds) {
		this.attachmentIds = attachmentIds;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final WebMessageRequest that = (WebMessageRequest) o;
		return Objects.equals(internal, that.internal) && Objects.equals(message, that.message) && Objects.equals(attachments, that.attachments) && Objects.equals(attachmentIds, that.attachmentIds);
	}

	@Override
	public int hashCode() {
		return Objects.hash(internal, message, attachments, attachmentIds);
	}

	@Override
	public String
		toString() {
		return "WebMessageRequest{" +
			"internal=" + internal +
			", message='" + message + '\'' +
			", attachments=" + attachments +
			", attachmentIds=" + attachmentIds +
			'}';
	}
}
