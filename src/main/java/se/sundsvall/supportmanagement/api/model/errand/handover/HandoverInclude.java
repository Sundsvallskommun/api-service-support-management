package se.sundsvall.supportmanagement.api.model.errand.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Flags controlling what data is copied from the source errand")
public class HandoverInclude {

	@Schema(description = "Include stakeholders", defaultValue = "false")
	private boolean stakeholders;

	@Schema(description = "Include external tags", defaultValue = "false")
	private boolean externalTags;

	@Schema(description = "Include parameters", defaultValue = "false")
	private boolean parameters;

	@Schema(description = "Include JSON parameters", defaultValue = "false")
	private boolean jsonParameters;

	@Schema(description = "Include attachments", defaultValue = "false")
	private boolean attachments;

	@Schema(description = "Include business related flag", defaultValue = "false")
	private boolean businessRelated;

	@Schema(description = "Include escalation email", defaultValue = "false")
	private boolean escalationEmail;

	@Schema(description = "Include contact reason description", defaultValue = "false")
	private boolean contactReasonDescription;

	public static HandoverInclude create() {
		return new HandoverInclude();
	}

	public boolean isStakeholders() {
		return stakeholders;
	}

	public void setStakeholders(final boolean stakeholders) {
		this.stakeholders = stakeholders;
	}

	public HandoverInclude withStakeholders(final boolean stakeholders) {
		this.stakeholders = stakeholders;
		return this;
	}

	public boolean isExternalTags() {
		return externalTags;
	}

	public void setExternalTags(final boolean externalTags) {
		this.externalTags = externalTags;
	}

	public HandoverInclude withExternalTags(final boolean externalTags) {
		this.externalTags = externalTags;
		return this;
	}

	public boolean isParameters() {
		return parameters;
	}

	public void setParameters(final boolean parameters) {
		this.parameters = parameters;
	}

	public HandoverInclude withParameters(final boolean parameters) {
		this.parameters = parameters;
		return this;
	}

	public boolean isJsonParameters() {
		return jsonParameters;
	}

	public void setJsonParameters(final boolean jsonParameters) {
		this.jsonParameters = jsonParameters;
	}

	public HandoverInclude withJsonParameters(final boolean jsonParameters) {
		this.jsonParameters = jsonParameters;
		return this;
	}

	public boolean isAttachments() {
		return attachments;
	}

	public void setAttachments(final boolean attachments) {
		this.attachments = attachments;
	}

	public HandoverInclude withAttachments(final boolean attachments) {
		this.attachments = attachments;
		return this;
	}

	public boolean isBusinessRelated() {
		return businessRelated;
	}

	public void setBusinessRelated(final boolean businessRelated) {
		this.businessRelated = businessRelated;
	}

	public HandoverInclude withBusinessRelated(final boolean businessRelated) {
		this.businessRelated = businessRelated;
		return this;
	}

	public boolean isEscalationEmail() {
		return escalationEmail;
	}

	public void setEscalationEmail(final boolean escalationEmail) {
		this.escalationEmail = escalationEmail;
	}

	public HandoverInclude withEscalationEmail(final boolean escalationEmail) {
		this.escalationEmail = escalationEmail;
		return this;
	}

	public boolean isContactReasonDescription() {
		return contactReasonDescription;
	}

	public void setContactReasonDescription(final boolean contactReasonDescription) {
		this.contactReasonDescription = contactReasonDescription;
	}

	public HandoverInclude withContactReasonDescription(final boolean contactReasonDescription) {
		this.contactReasonDescription = contactReasonDescription;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final HandoverInclude that = (HandoverInclude) o;
		return stakeholders == that.stakeholders && externalTags == that.externalTags && parameters == that.parameters
			&& jsonParameters == that.jsonParameters && attachments == that.attachments
			&& businessRelated == that.businessRelated && escalationEmail == that.escalationEmail
			&& contactReasonDescription == that.contactReasonDescription;
	}

	@Override
	public int hashCode() {
		return Objects.hash(stakeholders, externalTags, parameters, jsonParameters, attachments, businessRelated, escalationEmail, contactReasonDescription);
	}

	@Override
	public String toString() {
		return "HandoverInclude{stakeholders=" + stakeholders + ", externalTags=" + externalTags + ", parameters=" + parameters
			+ ", jsonParameters=" + jsonParameters + ", attachments=" + attachments + ", businessRelated=" + businessRelated
			+ ", escalationEmail=" + escalationEmail + ", contactReasonDescription=" + contactReasonDescription + "}";
	}
}
