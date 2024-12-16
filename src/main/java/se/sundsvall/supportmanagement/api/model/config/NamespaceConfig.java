package se.sundsvall.supportmanagement.api.model.config;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.time.OffsetDateTime;
import java.util.Objects;

@Schema(description = "Namespace configuration model")
public class NamespaceConfig {

	@Null
	@Schema(description = "Namespace", accessMode = READ_ONLY, example = "CONTACTCENTER")
	private String namespace;

	@Null
	@Schema(description = "Municipality connected to the namespace", accessMode = READ_ONLY, example = "2281")
	private String municipalityId;

	@NotNull
	@Schema(description = "Display name for the namespace", example = "Kontaktcenter")
	private String displayName;

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

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public NamespaceConfig withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public NamespaceConfig withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public NamespaceConfig withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getShortCode() {
		return shortCode;
	}

	public void setShortCode(final String shortCode) {
		this.shortCode = shortCode;
	}

	public NamespaceConfig withShortCode(final String shortCode) {
		this.shortCode = shortCode;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public NamespaceConfig withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public NamespaceConfig withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, displayName, modified, municipalityId, namespace, shortCode);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final NamespaceConfig other)) {
			return false;
		}
		return Objects.equals(created, other.created) && Objects.equals(displayName, other.displayName) && Objects.equals(modified, other.modified) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(namespace, other.namespace)
			&& Objects.equals(shortCode, other.shortCode);
	}

	@Override
	public String toString() {
		final String builder = "NamespaceConfig [namespace=" + namespace + ", municipalityId=" + municipalityId + ", displayName=" + displayName + ", shortCode=" + shortCode + ", created=" + created
			+ ", modified=" + modified + "]";
		return builder;
	}
}
