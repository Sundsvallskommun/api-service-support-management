package se.sundsvall.supportmanagement.api.model.communication;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "WebMessageRequest model")
public class WebMessageRequest {

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

	public void setMessage(String message) {
		this.message = message;
	}

	public WebMessageRequest withMessage(String message) {
		this.message = message;
		return this;
	}

	public List<WebMessageAttachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<WebMessageAttachment> attachments) {
		this.attachments = attachments;
	}

	public WebMessageRequest withAttachments(List<WebMessageAttachment> attachments) {
		this.attachments = attachments;
		return this;
	}

	public List<String> getAttachmentIds() {
		return attachmentIds;
	}

	public void setAttachmentIds(List<String> attachmentIds) {
		this.attachmentIds = attachmentIds;
	}

	public WebMessageRequest withAttachmentIds(List<String> attachmentIds) {
		this.attachmentIds = attachmentIds;
		return this;
	}

	@Override
	public String toString() {
		return "WebMessageRequest{" +
			"message='" + message + '\'' +
			", attachments=" + attachments +
			", attachmentIds=" + attachmentIds +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		WebMessageRequest that = (WebMessageRequest) o;
		return Objects.equals(message, that.message) && Objects.equals(attachments, that.attachments) && Objects.equals(attachmentIds, that.attachmentIds);
	}

	@Override
	public int hashCode() {
		return Objects.hash(message, attachments, attachmentIds);
	}
}
