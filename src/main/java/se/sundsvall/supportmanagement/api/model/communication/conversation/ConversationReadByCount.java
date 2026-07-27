package se.sundsvall.supportmanagement.api.model.communication.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Read-by statistics for a conversation")
public class ConversationReadByCount {

	@Schema(description = "The unique identifier of the conversation", accessMode = Schema.AccessMode.READ_ONLY, example = "2af1002e-008f-4bdc-924b-daaae31f1118")
	private String conversationId;

	@Schema(description = "Total number of messages in the conversation")
	private Integer messageCount;

	@Schema(description = "Per-identifier read count")
	private List<ReadByCountEntry> readByCount;

	@Schema(description = "Per-part read count")
	private List<PartReadByCountEntry> readByPartCount;

	public static ConversationReadByCount create() {
		return new ConversationReadByCount();
	}

	public String getConversationId() {
		return conversationId;
	}

	public void setConversationId(final String conversationId) {
		this.conversationId = conversationId;
	}

	public ConversationReadByCount withConversationId(final String conversationId) {
		this.conversationId = conversationId;
		return this;
	}

	public Integer getMessageCount() {
		return messageCount;
	}

	public void setMessageCount(final Integer messageCount) {
		this.messageCount = messageCount;
	}

	public ConversationReadByCount withMessageCount(final Integer messageCount) {
		this.messageCount = messageCount;
		return this;
	}

	public List<ReadByCountEntry> getReadByCount() {
		return readByCount;
	}

	public void setReadByCount(final List<ReadByCountEntry> readByCount) {
		this.readByCount = readByCount;
	}

	public ConversationReadByCount withReadByCount(final List<ReadByCountEntry> readByCount) {
		this.readByCount = readByCount;
		return this;
	}

	public List<PartReadByCountEntry> getReadByPartCount() {
		return readByPartCount;
	}

	public void setReadByPartCount(final List<PartReadByCountEntry> readByPartCount) {
		this.readByPartCount = readByPartCount;
	}

	public ConversationReadByCount withReadByPartCount(final List<PartReadByCountEntry> readByPartCount) {
		this.readByPartCount = readByPartCount;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(conversationId, messageCount, readByCount, readByPartCount);
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
		final var other = (ConversationReadByCount) obj;
		return Objects.equals(conversationId, other.conversationId)
			&& Objects.equals(messageCount, other.messageCount)
			&& Objects.equals(readByCount, other.readByCount)
			&& Objects.equals(readByPartCount, other.readByPartCount);
	}

	@Override
	public String toString() {
		return "ConversationReadByCount [conversationId=" + conversationId + ", messageCount=" + messageCount
			+ ", readByCount=" + readByCount + ", readByPartCount=" + readByPartCount + "]";
	}
}
