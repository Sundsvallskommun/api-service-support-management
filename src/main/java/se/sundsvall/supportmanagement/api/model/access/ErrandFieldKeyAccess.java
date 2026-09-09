package se.sundsvall.supportmanagement.api.model.access;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import se.sundsvall.supportmanagement.api.model.config.AccessLevel;

@Schema(description = "What the requesting user may do with one key of a keyed field of an errand")
public class ErrandFieldKeyAccess {

	@Schema(description = "Key of the keyed collection", examples = "contactChannel")
	private String key;

	@Schema(description = "What the user may do with the key. Never wider than what they may do with the errand itself", examples = "RW")
	private AccessLevel level;

	public static ErrandFieldKeyAccess create() {
		return new ErrandFieldKeyAccess();
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public ErrandFieldKeyAccess withKey(final String key) {
		this.key = key;
		return this;
	}

	public AccessLevel getLevel() {
		return level;
	}

	public void setLevel(final AccessLevel level) {
		this.level = level;
	}

	public ErrandFieldKeyAccess withLevel(final AccessLevel level) {
		this.level = level;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, level);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final ErrandFieldKeyAccess other)) { return false; }
		return Objects.equals(key, other.key) && level == other.level;
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("ErrandFieldKeyAccess [key=").append(key).append(", level=").append(level).append("]");
		return builder.toString();
	}
}
