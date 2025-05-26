package se.sundsvall.supportmanagement.api.model.communication.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

@Schema(description = "MessageRequest model")
public class MessageRequest {

	@ValidUuid(nullable = true)
	@Schema(description = "The ID of the replied message", example = "1aefbbb8-de82-414b-b5d7-ba7c5bbe4506")
	private String inReplyToMessageId;

	@NotBlank
	@Schema(description = "The content of the message.", example = "Hello, how can I help you?")
	private String content;

	public static MessageRequest create() {
		return new MessageRequest();
	}

	public String getInReplyToMessageId() {
		return inReplyToMessageId;
	}

	public void setInReplyToMessageId(String inReplyToMessageId) {
		this.inReplyToMessageId = inReplyToMessageId;
	}

	public MessageRequest withInReplyToMessageId(String inReplyToMessageId) {
		this.inReplyToMessageId = inReplyToMessageId;
		return this;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public MessageRequest withContent(String content) {
		this.content = content;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(content, inReplyToMessageId);
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
		MessageRequest other = (MessageRequest) obj;
		return Objects.equals(content, other.content) && Objects.equals(inReplyToMessageId, other.inReplyToMessageId);
	}

	@Override
	public String toString() {
		return "MessageRequest [inReplyToMessageId=" + inReplyToMessageId + ", content=" + content + "]";
	}
}
