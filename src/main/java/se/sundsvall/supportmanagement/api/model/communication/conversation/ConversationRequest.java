package se.sundsvall.supportmanagement.api.model.communication.conversation;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

@Schema(description = "ConversationRequest model")
public class ConversationRequest {

	@NotBlank
	@Schema(description = "The message-exchange topic", examples = "The conversation topic")
	private String topic;

	@NotNull
	@Schema(description = "The conversation type")
	private ConversationType type;

	@ArraySchema(schema = @Schema(implementation = String.class, description = "List with relation ID:s"))
	private List<@ValidUuid String> relationIds;

	@ArraySchema(schema = @Schema(implementation = Identifier.class, description = "A list of participants in this conversation"))
	private List<@Valid Identifier> participants;

	@ArraySchema(schema = @Schema(implementation = KeyValues.class, description = "A list of metadata objects associated with the conversation"))
	private List<@Valid KeyValues> metadata;

	public static ConversationRequest create() {
		return new ConversationRequest();
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public ConversationRequest withTopic(String topic) {
		this.topic = topic;
		return this;
	}

	public ConversationType getType() {
		return type;
	}

	public void setType(ConversationType type) {
		this.type = type;
	}

	public ConversationRequest withType(ConversationType type) {
		this.type = type;
		return this;
	}

	public List<String> getRelationIds() {
		return relationIds;
	}

	public void setRelationIds(List<String> relationIds) {
		this.relationIds = relationIds;
	}

	public ConversationRequest withRelationIds(List<String> relationIds) {
		this.relationIds = relationIds;
		return this;
	}

	public List<Identifier> getParticipants() {
		return participants;
	}

	public void setParticipants(List<Identifier> participants) {
		this.participants = participants;
	}

	public ConversationRequest withParticipants(List<Identifier> participants) {
		this.participants = participants;
		return this;
	}

	public List<KeyValues> getMetadata() {
		return metadata;
	}

	public void setMetadata(List<KeyValues> metadata) {
		this.metadata = metadata;
	}

	public ConversationRequest withMetadata(List<KeyValues> metadata) {
		this.metadata = metadata;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(metadata, participants, relationIds, topic, type);
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
		ConversationRequest other = (ConversationRequest) obj;
		return Objects.equals(metadata, other.metadata) && Objects.equals(participants, other.participants) && Objects.equals(relationIds, other.relationIds) && Objects.equals(topic, other.topic) && type == other.type;
	}

	@Override
	public String toString() {
		return "ConversationRequest [topic=" + topic + ", type=" + type + ", relationIds=" + relationIds + ", participants=" + participants + ", metadata=" + metadata + "]";
	}

}
