package se.sundsvall.supportmanagement.api.model.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.time.OffsetDateTime;
import java.util.List;
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

	@Schema(
		description = "If set to true errands are mapped according to the fields configured for the roles held by the requesting user. Users holding no role, and all users when set to false, receive the full errand. If no value is set it defaults to false.",
		examples = "true")
	private boolean roleBasedMapping;

	@Schema(
		description = "If set to true the resources a user may reach are decided by the access mapper in addition to their labels. Leave false until the namespace has resource access configured there, otherwise no resource can be reached. If no value is set it defaults to false.",
		examples = "true")
	private boolean resourceAccessControl;

	@Valid
	@Schema(
		description = "What a user whose labels only grant them limited read for an errand may reach on it. Limited read always reaches the errand itself, listing resources here extends it beyond that, up to what read access gives. The fields apply instead of the fields of any role the user holds")
	private LimitedReadAccess limitedReadAccess;

	@Valid
	@Schema(description = "Access given to the reporter of an errand. Leaving it out means reporters get nothing beyond what their labels already grant")
	private ReporterAccess reporterAccess;

	@Valid
	@Schema(description = "Restricts the errand to the listed fields for holders of a role supplied by the access mapper, when role based mapping is active. A role that is not listed is not restricted and sees the errand in full")
	private List<RoleFieldRestriction> roleFieldRestrictions;

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

	public boolean isRoleBasedMapping() {
		return roleBasedMapping;
	}

	public void setRoleBasedMapping(final boolean roleBasedMapping) {
		this.roleBasedMapping = roleBasedMapping;
	}

	public NamespaceConfig withRoleBasedMapping(final boolean roleBasedMapping) {
		this.roleBasedMapping = roleBasedMapping;
		return this;
	}

	public boolean isResourceAccessControl() {
		return resourceAccessControl;
	}

	public void setResourceAccessControl(final boolean resourceAccessControl) {
		this.resourceAccessControl = resourceAccessControl;
	}

	public NamespaceConfig withResourceAccessControl(final boolean resourceAccessControl) {
		this.resourceAccessControl = resourceAccessControl;
		return this;
	}

	public LimitedReadAccess getLimitedReadAccess() {
		return limitedReadAccess;
	}

	public void setLimitedReadAccess(final LimitedReadAccess limitedReadAccess) {
		this.limitedReadAccess = limitedReadAccess;
	}

	public NamespaceConfig withLimitedReadAccess(final LimitedReadAccess limitedReadAccess) {
		this.limitedReadAccess = limitedReadAccess;
		return this;
	}

	public ReporterAccess getReporterAccess() {
		return reporterAccess;
	}

	public void setReporterAccess(final ReporterAccess reporterAccess) {
		this.reporterAccess = reporterAccess;
	}

	public NamespaceConfig withReporterAccess(final ReporterAccess reporterAccess) {
		this.reporterAccess = reporterAccess;
		return this;
	}

	public List<RoleFieldRestriction> getRoleFieldRestrictions() {
		return roleFieldRestrictions;
	}

	public void setRoleFieldRestrictions(final List<RoleFieldRestriction> roleFieldRestrictions) {
		this.roleFieldRestrictions = roleFieldRestrictions;
	}

	public NamespaceConfig withRoleFieldRestrictions(final List<RoleFieldRestriction> roleFieldRestrictions) {
		this.roleFieldRestrictions = roleFieldRestrictions;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(accessControl, created, displayName,
			limitedReadAccess, modified, municipalityId, namespace, notificationTTLInDays, notifyReporter, reporterAccess, resourceAccessControl, roleFieldRestrictions, roleBasedMapping, shortCode);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final NamespaceConfig other)) { return false; }
		return accessControl == other.accessControl && Objects.equals(created, other.created) && Objects.equals(displayName, other.displayName) && Objects.equals(modified, other.modified) && Objects.equals(municipalityId, other.municipalityId) && Objects
			.equals(namespace, other.namespace) && Objects.equals(notificationTTLInDays, other.notificationTTLInDays) && notifyReporter == other.notifyReporter && Objects.equals(limitedReadAccess, other.limitedReadAccess) && Objects.equals(reporterAccess,
				other.reporterAccess) && resourceAccessControl == other.resourceAccessControl && Objects.equals(roleFieldRestrictions,
					other.roleFieldRestrictions) && roleBasedMapping == other.roleBasedMapping
			&& Objects.equals(shortCode, other.shortCode);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("NamespaceConfig [namespace=").append(namespace).append(", municipalityId=").append(municipalityId).append(", displayName=").append(displayName).append(", shortCode=").append(shortCode).append(", notificationTTLInDays=").append(
			notificationTTLInDays).append(", created=").append(created).append(", modified=").append(modified).append(", accessControl=").append(accessControl).append(", notifyReporter=").append(notifyReporter).append(", roleBasedMapping=").append(
				roleBasedMapping).append(", resourceAccessControl=").append(resourceAccessControl).append(", limitedReadAccess=").append(limitedReadAccess).append(", reporterAccess=").append(reporterAccess).append(", roleFieldRestrictions=").append(
					roleFieldRestrictions)
			.append("]");
		return builder.toString();
	}
}
