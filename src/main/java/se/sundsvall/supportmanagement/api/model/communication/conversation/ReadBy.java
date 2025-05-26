package se.sundsvall.supportmanagement.api.model.communication.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;

@Schema(description = "Readby model")
public class ReadBy {

	@Schema(description = "The identifier of the person who read the message.", example = "joe01doe")
	private Identifier identifier;

	@Schema(description = "The timestamp when the message was read.", example = "2023-01-01T12:00:00+01:00")
	private OffsetDateTime readAt;

	public static ReadBy create() {
		return new ReadBy();
	}

	public Identifier getIdentifier() {
		return identifier;
	}

	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

	public ReadBy withIdentifier(Identifier identifier) {
		this.identifier = identifier;
		return this;
	}

	public OffsetDateTime getReadAt() {
		return readAt;
	}

	public void setReadAt(OffsetDateTime readAt) {
		this.readAt = readAt;
	}

	public ReadBy withReadAt(OffsetDateTime readAt) {
		this.readAt = readAt;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(identifier, readAt);
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
		ReadBy other = (ReadBy) obj;
		return Objects.equals(identifier, other.identifier) && Objects.equals(readAt, other.readAt);
	}

	@Override
	public String toString() {
		return "ReadBy [identifier=" + identifier + ", readAt=" + readAt + "]";
	}
}
