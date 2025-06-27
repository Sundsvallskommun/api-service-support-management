package se.sundsvall.supportmanagement.service.util;

import java.util.Objects;
import se.sundsvall.supportmanagement.integration.db.model.communication.ConversationEntity;

public class ConversationEvent {
	private String requestId;
	private ConversationEntity conversationEntity;

	public static ConversationEvent create() {
		return new ConversationEvent();
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(final String requestId) {
		this.requestId = requestId;
	}

	public ConversationEntity getConversationEntity() {
		return conversationEntity;
	}

	public void setConversationEntity(final ConversationEntity conversationEntity) {
		this.conversationEntity = conversationEntity;
	}

	public ConversationEvent withRequestId(final String requestId) {
		this.requestId = requestId;
		return this;
	}

	public ConversationEvent withConversationEntity(final ConversationEntity conversationEntity) {
		this.conversationEntity = conversationEntity;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final ConversationEvent that = (ConversationEvent) o;
		return Objects.equals(requestId, that.requestId) && Objects.equals(conversationEntity, that.conversationEntity);
	}

	@Override
	public int hashCode() {
		return Objects.hash(requestId, conversationEntity);
	}

	@Override
	public String toString() {
		return "ConversationEvent{" +
			"requestId='" + requestId + '\'' +
			", conversationEntity=" + conversationEntity +
			'}';
	}
}
