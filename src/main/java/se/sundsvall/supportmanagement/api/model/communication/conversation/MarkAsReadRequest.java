package se.sundsvall.supportmanagement.api.model.communication.conversation;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

@Schema(description = "MarkAsReadRequest model")
public class MarkAsReadRequest {

	@NotEmpty
	@ArraySchema(schema = @Schema(description = "List of message IDs to mark as read", example = "b82bd8ac-1507-4d9a-958d-369261eecc15"))
	private List<@ValidUuid String> messageIds;

	public static MarkAsReadRequest create() {
		return new MarkAsReadRequest();
	}

	public List<String> getMessageIds() {
		return messageIds;
	}

	public void setMessageIds(final List<String> messageIds) {
		this.messageIds = messageIds;
	}

	public MarkAsReadRequest withMessageIds(final List<String> messageIds) {
		this.messageIds = messageIds;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(messageIds);
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
		final var other = (MarkAsReadRequest) obj;
		return Objects.equals(messageIds, other.messageIds);
	}

	@Override
	public String toString() {
		return "MarkAsReadRequest [messageIds=" + messageIds + "]";
	}
}
