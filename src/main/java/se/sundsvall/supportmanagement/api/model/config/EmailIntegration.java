package se.sundsvall.supportmanagement.api.model.config;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;
import se.sundsvall.supportmanagement.api.validation.ValidRole;
import se.sundsvall.supportmanagement.api.validation.ValidStatus;

@Schema(description = "Email integration config model")
public class EmailIntegration {
	@Schema(description = "If set to true emails will be fetched")
	@NotNull
	private Boolean enabled;

	@Schema(description = "Email sender if incoming mail is rejected", example = "noreply@sundsvall.se", nullable = true)
	private String errandClosedEmailSender;

	@Schema(description = "Message that will be sent when incoming mail is rejected", example = "Errand is closed. Please open a new errand.", nullable = true)
	private String errandClosedEmailTemplate;

	@Schema(description = "Email sender if incoming mail results in new errand", example = "test@sundsvall.se", nullable = true)
	private String errandNewEmailSender;

	@Schema(description = "Message that will be sent when new errand is created", example = "New errand is created.", nullable = true)
	private String errandNewEmailTemplate;

	@Schema(description = "Number of days before incoming mail is rejected. Measured from when the errand was last touched. " +
		"Rejection can only occur if status on errand equals 'inactiveStatus'.", example = "5", nullable = true)
	private Integer daysOfInactivityBeforeReject;

	@Schema(description = "Status set on errand when email results in a new errand", example = "NEW")
	@NotNull
	@ValidStatus
	private String statusForNew;

	@Schema(description = "Status on errand that will trigger a status change when email refers to an existing errand", example = "SOLVED", nullable = true)
	@ValidStatus
	private String triggerStatusChangeOn;

	@Schema(description = "Status that will be set on errand if status change is triggered. Can only be null if 'triggerStatusChangeOn' is null.", example = "OPEN", nullable = true)
	@ValidStatus
	private String statusChangeTo;

	@Schema(description = "Status of an inactive errand. This value relates to property 'daysOfInactivityBeforeReject'. " +
		"If set to null, no rejection mail will be sent", example = "SOLVED", nullable = true)
	@ValidStatus
	private String inactiveStatus;

	@Schema(description = "If true sender is added as stakeholder", example = "false", nullable = true)
	private Boolean addSenderAsStakeholder;

	@Schema(description = "Role set on stakeholder.", example = "APPLICANT", nullable = true)
	@ValidRole
	private String stakeholderRole;

	@Schema(description = "Channel set on created errands", example = "EMAIL", nullable = true)
	private String errandChannel;

	@Schema(description = "Timestamp when the configuration was created", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the configuration was last modified", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime modified;

	public static EmailIntegration create() {
		return new EmailIntegration();
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(final Boolean enabled) {
		this.enabled = enabled;
	}

	public EmailIntegration withEnabled(final Boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	public String getErrandClosedEmailSender() {
		return errandClosedEmailSender;
	}

	public void setErrandClosedEmailSender(final String errandClosedEmailSender) {
		this.errandClosedEmailSender = errandClosedEmailSender;
	}

	public String getErrandClosedEmailTemplate() {
		return errandClosedEmailTemplate;
	}

	public void setErrandClosedEmailTemplate(final String errandClosedEmailTemplate) {
		this.errandClosedEmailTemplate = errandClosedEmailTemplate;
	}

	public EmailIntegration withErrandClosedEmailSender(final String errandClosedEmailSender) {
		this.errandClosedEmailSender = errandClosedEmailSender;
		return this;
	}

	public String getErrandNewEmailSender() {
		return errandNewEmailSender;
	}

	public void setErrandNewEmailSender(final String errandNewEmailSender) {
		this.errandNewEmailSender = errandNewEmailSender;
	}

	public EmailIntegration withErrandNewEmailSender(final String errandNewEmailSender) {
		this.errandNewEmailSender = errandNewEmailSender;
		return this;
	}

	public String getErrandNewEmailTemplate() {
		return errandNewEmailTemplate;
	}

	public void setErrandNewEmailTemplate(final String errandNewEmailTemplate) {
		this.errandNewEmailTemplate = errandNewEmailTemplate;
	}

	public EmailIntegration withErrandNewEmailTemplate(final String errandNewEmailTemplate) {
		this.errandNewEmailTemplate = errandNewEmailTemplate;
		return this;
	}

	public EmailIntegration withErrandClosedEmailTemplate(final String errandClosedEmailTemplate) {
		this.errandClosedEmailTemplate = errandClosedEmailTemplate;
		return this;
	}

	public Integer getDaysOfInactivityBeforeReject() {
		return daysOfInactivityBeforeReject;
	}

	public void setDaysOfInactivityBeforeReject(final Integer daysOfInactivityBeforeReject) {
		this.daysOfInactivityBeforeReject = daysOfInactivityBeforeReject;
	}

	public EmailIntegration withDaysOfInactivityBeforeReject(final Integer daysOfInactivityBeforeReject) {
		this.daysOfInactivityBeforeReject = daysOfInactivityBeforeReject;
		return this;
	}

	public String getStatusForNew() {
		return statusForNew;
	}

	public void setStatusForNew(final String statusForNew) {
		this.statusForNew = statusForNew;
	}

	public EmailIntegration withStatusForNew(final String statusForNew) {
		this.statusForNew = statusForNew;
		return this;
	}

	public String getTriggerStatusChangeOn() {
		return triggerStatusChangeOn;
	}

	public void setTriggerStatusChangeOn(final String triggerStatusChangeOn) {
		this.triggerStatusChangeOn = triggerStatusChangeOn;
	}

	public EmailIntegration withTriggerStatusChangeOn(final String triggerStatusChangeOn) {
		this.triggerStatusChangeOn = triggerStatusChangeOn;
		return this;
	}

	public String getStatusChangeTo() {
		return statusChangeTo;
	}

	public void setStatusChangeTo(final String statusChangeTo) {
		this.statusChangeTo = statusChangeTo;
	}

	public EmailIntegration withStatusChangeTo(final String statusChangeTo) {
		this.statusChangeTo = statusChangeTo;
		return this;
	}

	public String getInactiveStatus() {
		return inactiveStatus;
	}

	public void setInactiveStatus(final String inactiveStatus) {
		this.inactiveStatus = inactiveStatus;
	}

	public EmailIntegration withInactiveStatus(final String inactiveStatus) {
		this.inactiveStatus = inactiveStatus;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public EmailIntegration withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public EmailIntegration withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public Boolean getAddSenderAsStakeholder() {
		return addSenderAsStakeholder;
	}

	public void setAddSenderAsStakeholder(final Boolean addSenderAsStakeholder) {
		this.addSenderAsStakeholder = addSenderAsStakeholder;
	}

	public EmailIntegration withAddSenderAsStakeholder(final Boolean addSenderAsStakeholder) {
		this.addSenderAsStakeholder = addSenderAsStakeholder;
		return this;
	}

	public String getStakeholderRole() {
		return stakeholderRole;
	}

	public void setStakeholderRole(final String stakeholderRole) {
		this.stakeholderRole = stakeholderRole;
	}

	public EmailIntegration withStakeholderRole(final String stakeholderRole) {
		this.stakeholderRole = stakeholderRole;
		return this;
	}

	public String getErrandChannel() {
		return errandChannel;
	}

	public void setErrandChannel(final String errandChannel) {
		this.errandChannel = errandChannel;
	}

	public EmailIntegration withErrandChannel(final String errandChannel) {
		this.errandChannel = errandChannel;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final EmailIntegration that = (EmailIntegration) o;
		return Objects.equals(enabled, that.enabled)
			&& Objects.equals(errandClosedEmailSender, that.errandClosedEmailSender)
			&& Objects.equals(errandClosedEmailTemplate, that.errandClosedEmailTemplate)
			&& Objects.equals(errandNewEmailSender, that.errandNewEmailSender)
			&& Objects.equals(errandNewEmailTemplate, that.errandNewEmailTemplate)
			&& Objects.equals(daysOfInactivityBeforeReject, that.daysOfInactivityBeforeReject)
			&& Objects.equals(statusForNew, that.statusForNew)
			&& Objects.equals(triggerStatusChangeOn, that.triggerStatusChangeOn)
			&& Objects.equals(statusChangeTo, that.statusChangeTo)
			&& Objects.equals(inactiveStatus, that.inactiveStatus)
			&& Objects.equals(addSenderAsStakeholder, that.addSenderAsStakeholder)
			&& Objects.equals(stakeholderRole, that.stakeholderRole)
			&& Objects.equals(errandChannel, that.errandChannel)
			&& Objects.equals(created, that.created)
			&& Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(enabled, errandClosedEmailSender, errandClosedEmailTemplate, errandNewEmailSender, errandNewEmailTemplate,
			daysOfInactivityBeforeReject, statusForNew, triggerStatusChangeOn, statusChangeTo, inactiveStatus, addSenderAsStakeholder,
			stakeholderRole, errandChannel, created, modified);
	}

	@Override
	public String toString() {
		return "EmailIntegration{" +
			"enabled=" + enabled +
			", errandClosedEmailSender='" + errandClosedEmailSender + '\'' +
			", errandClosedEmailTemplate='" + errandClosedEmailTemplate + '\'' +
			", errandNewEmailSender='" + errandNewEmailSender + '\'' +
			", errandNewEmailTemplate='" + errandNewEmailTemplate + '\'' +
			", daysOfInactivityBeforeReject=" + daysOfInactivityBeforeReject +
			", statusForNew='" + statusForNew + '\'' +
			", triggerStatusChangeOn='" + triggerStatusChangeOn + '\'' +
			", statusChangeTo='" + statusChangeTo + '\'' +
			", inactiveStatus='" + inactiveStatus + '\'' +
			", addSenderAsStakeholder=" + addSenderAsStakeholder +
			", stakeholderRole='" + stakeholderRole + '\'' +
			", errandChannel='" + errandChannel + '\'' +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
