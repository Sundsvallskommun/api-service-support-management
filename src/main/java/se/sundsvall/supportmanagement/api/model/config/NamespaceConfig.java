package se.sundsvall.supportmanagement.api.model.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.time.OffsetDateTime;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Namespace configuration model")
public class NamespaceConfig {

	@Null
	@Schema(description = "Namespace", accessMode = READ_ONLY, examples = "CONTACTCENTER")
	private String namespace;

	@Null
	@Schema(description = "Municipality connected to the namespace", accessMode = READ_ONLY, examples = "2281")
	private String municipalityId;

	@NotNull
	@Schema(description = "Display name for the namespace", examples = "Kontaktcenter")
	private String displayName;

	@NotNull
	@Schema(description = "Prefix for errand numbers in this namespace", examples = "KC")
	private String shortCode;

	@Schema(description = "Time to live (in days) for notifications created in this namespace", examples = "40")
	private Integer notificationTTLInDays;

	@Schema(description = "Timestamp when the configuration was created", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the configuration was last modified", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime modified;

	@Schema(description = "If set to true access control will be enabled. If no value is set it defaults to false.", examples = "true")
	private boolean accessControl;

	@Schema(description = "If set to true notification will be sent to the stakeholder when stakeholder with reporter role recieves an internal message. If no value is set it defaults to false.", examples = "true")
	private boolean notifyReporter;

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

	public Integer getNotificationTTLInDays() {
		return notificationTTLInDays;
	}

	public void setNotificationTTLInDays(final Integer notificationTTLInDays) {
		this.notificationTTLInDays = notificationTTLInDays;
	}

	public NamespaceConfig withNotificationTTLInDays(final Integer notificationTTLInDays) {
		this.notificationTTLInDays = notificationTTLInDays;
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

	public boolean isAccessControl() {
		return accessControl;
	}

	public void setAccessControl(final boolean accessControl) {
		this.accessControl = accessControl;
	}

	public NamespaceConfig withAccessControl(final boolean accessControl) {
		this.accessControl = accessControl;
		return this;
	}

	public boolean isNotifyReporter() {
		return notifyReporter;
	}

	public void setNotifyReporter(final boolean notifyReporter) {
		this.notifyReporter = notifyReporter;
	}

	public NamespaceConfig withNotifyReporter(final boolean notifyReporter) {
		this.notifyReporter = notifyReporter;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(accessControl, created, displayName, modified, municipalityId, namespace, notificationTTLInDays, notifyReporter, shortCode);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final NamespaceConfig other)) { return false; }
		return accessControl == other.accessControl && Objects.equals(created, other.created) && Objects.equals(displayName, other.displayName) && Objects.equals(modified, other.modified) && Objects.equals(municipalityId, other.municipalityId) && Objects
			.equals(namespace, other.namespace) && Objects.equals(notificationTTLInDays, other.notificationTTLInDays) && notifyReporter == other.notifyReporter && Objects.equals(shortCode, other.shortCode);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("NamespaceConfig [namespace=").append(namespace).append(", municipalityId=").append(municipalityId).append(", displayName=").append(displayName).append(", shortCode=").append(shortCode).append(", notificationTTLInDays=").append(
			notificationTTLInDays).append(", created=").append(created).append(", modified=").append(modified).append(", accessControl=").append(accessControl).append(", notifyReporter=").append(notifyReporter).append("]");
		return builder.toString();
	}
}
