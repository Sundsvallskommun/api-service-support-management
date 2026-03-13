package se.sundsvall.supportmanagement.api.model.communication.conversation;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

@Schema(description = "MessageRequest model")
public class MessageRequest {

	@ValidUuid(nullable = true)
	@Schema(description = "The ID of the replied message", examples = "1aefbbb8-de82-414b-b5d7-ba7c5bbe4506")
	private String inReplyToMessageId;

	@NotBlank
	@Schema(description = "The content of the message.", examples = "Hello, how can I help you?")
	private String content;

	@ArraySchema(schema = @Schema(description = "List with attachment ids"))
	private List<String> attachmentIds;

	public static MessageRequest create() {
		return new MessageRequest();
	}

	public String getInReplyToMessageId() {
		return inReplyToMessageId;
	}

	public void setInReplyToMessageId(final String inReplyToMessageId) {
		this.inReplyToMessageId = inReplyToMessageId;
	}

	public MessageRequest withInReplyToMessageId(final String inReplyToMessageId) {
		this.inReplyToMessageId = inReplyToMessageId;
		return this;
	}

	public String getContent() {
		return content;
	}

	public void setContent(final String content) {
		this.content = content;
	}

	public MessageRequest withContent(final String content) {
		this.content = content;
		return this;
	}

	public List<String> getAttachmentIds() {
		return attachmentIds;
	}

	public void setAttachmentIds(final List<String> attachmentIds) {
		this.attachmentIds = attachmentIds;
	}

	public MessageRequest withAttachmentIds(final List<String> attachmentIds) {
		this.attachmentIds = attachmentIds;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(attachmentIds, content, inReplyToMessageId);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final MessageRequest other = (MessageRequest) obj;
		return Objects.equals(attachmentIds, other.attachmentIds) && Objects.equals(content, other.content) && Objects.equals(inReplyToMessageId, other.inReplyToMessageId);
	}

	@Override
	public String toString() {
		return "MessageRequest [inReplyToMessageId=" + inReplyToMessageId + ", content=" + content + ", attachmentIds=" + attachmentIds + "]";
	}
}
