package se.sundsvall.supportmanagement.api.model.communication.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Read count for a specific identifier")
public class ReadByCountEntry {

	@Schema(description = "The identifier that read the messages")
	private Identifier identifier;

	@Schema(description = "Number of messages read by this identifier")
	private Integer count;

	public static ReadByCountEntry create() {
		return new ReadByCountEntry();
	}

	public Identifier getIdentifier() {
		return identifier;
	}

	public void setIdentifier(final Identifier identifier) {
		this.identifier = identifier;
	}

	public ReadByCountEntry withIdentifier(final Identifier identifier) {
		this.identifier = identifier;
		return this;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(final Integer count) {
		this.count = count;
	}

	public ReadByCountEntry withCount(final Integer count) {
		this.count = count;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(identifier, count);
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
		final var other = (ReadByCountEntry) obj;
		return Objects.equals(identifier, other.identifier) && Objects.equals(count, other.count);
	}

	@Override
	public String toString() {
		return "ReadByCountEntry [identifier=" + identifier + ", count=" + count + "]";
	}
}
