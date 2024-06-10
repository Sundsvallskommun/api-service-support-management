package se.sundsvall.supportmanagement.api.model.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Namespace configuration model")
public class NamespaceConfig {

	@NotNull
	@Schema(description = "Prefix for errand numbers in this namespace", example = "KC")
	private String shortCode;

	@Schema(description = "Timestamp when the configuration was created", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the configuration was last modified", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime modified;

	public static NamespaceConfig create() {
		return new NamespaceConfig();
	}

	public String getShortCode() {
		return shortCode;
	}

	public void setShortCode(String shortCode) {
		this.shortCode = shortCode;
	}

	public NamespaceConfig withShortCode(String shortCode) {
		this.shortCode = shortCode;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public NamespaceConfig withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public NamespaceConfig withModified(OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		NamespaceConfig that = (NamespaceConfig) o;
		return Objects.equals(shortCode, that.shortCode) && Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(shortCode, created, modified);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("NamespaceConfig{");
		sb.append("shortCode='").append(shortCode).append('\'');
		sb.append(", created=").append(created);
		sb.append(", modified=").append(modified);
		sb.append('}');
		return sb.toString();
	}
}
