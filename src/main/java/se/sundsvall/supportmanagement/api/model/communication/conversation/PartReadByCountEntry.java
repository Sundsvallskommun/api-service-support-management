package se.sundsvall.supportmanagement.api.model.communication.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Read count for a specific part")
public class PartReadByCountEntry {

	@Schema(description = "The part that read the messages", example = "KC-23010001")
	private String part;

	@Schema(description = "Number of messages read by this part")
	private Integer count;

	public static PartReadByCountEntry create() {
		return new PartReadByCountEntry();
	}

	public String getPart() {
		return part;
	}

	public void setPart(final String part) {
		this.part = part;
	}

	public PartReadByCountEntry withPart(final String part) {
		this.part = part;
		return this;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(final Integer count) {
		this.count = count;
	}

	public PartReadByCountEntry withCount(final Integer count) {
		this.count = count;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(part, count);
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
		final var other = (PartReadByCountEntry) obj;
		return Objects.equals(part, other.part) && Objects.equals(count, other.count);
	}

	@Override
	public String toString() {
		return "PartReadByCountEntry [part=" + part + ", count=" + count + "]";
	}
}
