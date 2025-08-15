package se.sundsvall.supportmanagement.api.model.communication.conversation;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Schema(description = "Message model")
public class Message {

	@Schema(description = "Message ID", example = "1aefbbb8-de82-414b-b5d7-ba7c5bbe4506")
	private String id;

	@Schema(description = "The ID of the replied message", example = "1aefbbb8-de82-414b-b5d7-ba7c5bbe4506")
	private String inReplyToMessageId;

	@Schema(description = "The timestamp when the message was created.", example = "2023-01-01T12:00:00")
	private OffsetDateTime created;

	@Schema(description = "The participant who created the message.")
	private Identifier createdBy;

	@Schema(description = "The content of the message.", example = "Hello, how can I help you?")
	private String content;

	@ArraySchema(schema = @Schema(implementation = ReadBy.class, description = "A list of users who have read the message."))
	private List<ReadBy> readBy;

	@ArraySchema(schema = @Schema(implementation = Attachment.class, description = "A list of attachments associated with the message."))
	private List<Attachment> attachments;

	@Schema(description = "Type of message (user or system created)", example = "USER_CREATED", accessMode = Schema.AccessMode.READ_ONLY)
	private MessageType type;

	public static Message create() {
		return new Message();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Message withId(String id) {
		this.id = id;
		return this;
	}

	public String getInReplyToMessageId() {
		return inReplyToMessageId;
	}

	public void setInReplyToMessageId(String inReplyToMessageId) {
		this.inReplyToMessageId = inReplyToMessageId;
	}

	public Message withInReplyToMessageId(String inReplyToMessageId) {
		this.inReplyToMessageId = inReplyToMessageId;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public Message withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public Identifier getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Identifier createdBy) {
		this.createdBy = createdBy;
	}

	public Message withCreatedBy(Identifier createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Message withContent(String content) {
		this.content = content;
		return this;
	}

	public List<ReadBy> getReadBy() {
		return readBy;
	}

	public void setReadBy(List<ReadBy> readBy) {
		this.readBy = readBy;
	}

	public Message withReadBy(List<ReadBy> readBy) {
		this.readBy = readBy;
		return this;
	}

	public List<Attachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

	public Message withAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
		return this;
	}

	public MessageType getType() {
		return type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}

	public Message withType(MessageType type) {
		this.type = type;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(attachments, content, created, createdBy, id, inReplyToMessageId, readBy, type);
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
		Message other = (Message) obj;
		return Objects.equals(attachments, other.attachments) && Objects.equals(content, other.content) && Objects.equals(created, other.created) && Objects.equals(createdBy, other.createdBy) && Objects.equals(id, other.id) && Objects.equals(
			inReplyToMessageId, other.inReplyToMessageId) && Objects.equals(readBy, other.readBy) && Objects.equals(type, other.type);
	}

	@Override
	public String toString() {
		return "Message [id=" + id + ", inReplyToMessageId=" + inReplyToMessageId + ", created=" + created + ", createdBy=" + createdBy + ", content=" + content + ", readBy=" + readBy + ", attachments=" + attachments + ", type=" + type + "]";
	}
}
