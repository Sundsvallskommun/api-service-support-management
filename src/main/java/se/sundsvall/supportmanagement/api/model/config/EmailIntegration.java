package se.sundsvall.supportmanagement.api.model.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import se.sundsvall.supportmanagement.api.validation.ValidRole;
import se.sundsvall.supportmanagement.api.validation.ValidStatus;

import java.time.OffsetDateTime;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

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

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public EmailIntegration withEnabled(Boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	public String getErrandClosedEmailSender() {
		return errandClosedEmailSender;
	}

	public void setErrandClosedEmailSender(String errandClosedEmailSender) {
		this.errandClosedEmailSender = errandClosedEmailSender;
	}

	public String getErrandClosedEmailTemplate() {
		return errandClosedEmailTemplate;
	}

	public void setErrandClosedEmailTemplate(String errandClosedEmailTemplate) {
		this.errandClosedEmailTemplate = errandClosedEmailTemplate;
	}

	public EmailIntegration withErrandClosedEmailSender(String errandClosedEmailSender) {
		this.errandClosedEmailSender = errandClosedEmailSender;
		return this;
	}

	public String getErrandNewEmailSender() {
		return errandNewEmailSender;
	}

	public void setErrandNewEmailSender(String errandNewEmailSender) {
		this.errandNewEmailSender = errandNewEmailSender;
	}

	public EmailIntegration withErrandNewEmailSender(String errandNewEmailSender) {
		this.errandNewEmailSender = errandNewEmailSender;
		return this;
	}

	public String getErrandNewEmailTemplate() {
		return errandNewEmailTemplate;
	}

	public void setErrandNewEmailTemplate(String errandNewEmailTemplate) {
		this.errandNewEmailTemplate = errandNewEmailTemplate;
	}

	public EmailIntegration withErrandNewEmailTemplate(String errandNewEmailTemplate) {
		this.errandNewEmailTemplate = errandNewEmailTemplate;
		return this;
	}

	public EmailIntegration withErrandClosedEmailTemplate(String errandClosedEmailTemplate) {
		this.errandClosedEmailTemplate = errandClosedEmailTemplate;
		return this;
	}

	public Integer getDaysOfInactivityBeforeReject() {
		return daysOfInactivityBeforeReject;
	}

	public void setDaysOfInactivityBeforeReject(Integer daysOfInactivityBeforeReject) {
		this.daysOfInactivityBeforeReject = daysOfInactivityBeforeReject;
	}

	public EmailIntegration withDaysOfInactivityBeforeReject(Integer daysOfInactivityBeforeReject) {
		this.daysOfInactivityBeforeReject = daysOfInactivityBeforeReject;
		return this;
	}

	public String getStatusForNew() {
		return statusForNew;
	}

	public void setStatusForNew(String statusForNew) {
		this.statusForNew = statusForNew;
	}

	public EmailIntegration withStatusForNew(String statusForNew) {
		this.statusForNew = statusForNew;
		return this;
	}

	public String getTriggerStatusChangeOn() {
		return triggerStatusChangeOn;
	}

	public void setTriggerStatusChangeOn(String triggerStatusChangeOn) {
		this.triggerStatusChangeOn = triggerStatusChangeOn;
	}

	public EmailIntegration withTriggerStatusChangeOn(String triggerStatusChangeOn) {
		this.triggerStatusChangeOn = triggerStatusChangeOn;
		return this;
	}

	public String getStatusChangeTo() {
		return statusChangeTo;
	}

	public void setStatusChangeTo(String statusChangeTo) {
		this.statusChangeTo = statusChangeTo;
	}

	public EmailIntegration withStatusChangeTo(String statusChangeTo) {
		this.statusChangeTo = statusChangeTo;
		return this;
	}

	public String getInactiveStatus() {
		return inactiveStatus;
	}

	public void setInactiveStatus(String inactiveStatus) {
		this.inactiveStatus = inactiveStatus;
	}

	public EmailIntegration withInactiveStatus(String inactiveStatus) {
		this.inactiveStatus = inactiveStatus;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public EmailIntegration withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public EmailIntegration withModified(OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public Boolean getAddSenderAsStakeholder() {
		return addSenderAsStakeholder;
	}

	public void setAddSenderAsStakeholder(Boolean addSenderAsStakeholder) {
		this.addSenderAsStakeholder = addSenderAsStakeholder;
	}

	public EmailIntegration withAddSenderAsStakeholder(Boolean addSenderAsStakeholder) {
		this.addSenderAsStakeholder = addSenderAsStakeholder;
		return this;
	}

	public String getStakeholderRole() {
		return stakeholderRole;
	}

	public void setStakeholderRole(String stakeholderRole) {
		this.stakeholderRole = stakeholderRole;
	}

	public EmailIntegration withStakeholderRole(String stakeholderRole) {
		this.stakeholderRole = stakeholderRole;
		return this;
	}

	public String getErrandChannel() {
		return errandChannel;
	}

	public void setErrandChannel(String errandChannel) {
		this.errandChannel = errandChannel;
	}

	public EmailIntegration withErrandChannel(String errandChannel) {
		this.errandChannel = errandChannel;
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
		EmailIntegration that = (EmailIntegration) o;
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
		final StringBuilder sb = new StringBuilder("EmailIntegration{");
		sb.append("enabled=").append(enabled);
		sb.append(", errandClosedEmailSender='").append(errandClosedEmailSender).append('\'');
		sb.append(", errandClosedEmailTemplate='").append(errandClosedEmailTemplate).append('\'');
		sb.append(", errandNewEmailSender='").append(errandNewEmailSender).append('\'');
		sb.append(", errandNewEmailTemplate='").append(errandNewEmailTemplate).append('\'');
		sb.append(", daysOfInactivityBeforeReject=").append(daysOfInactivityBeforeReject);
		sb.append(", statusForNew='").append(statusForNew).append('\'');
		sb.append(", triggerStatusChangeOn='").append(triggerStatusChangeOn).append('\'');
		sb.append(", statusChangeTo='").append(statusChangeTo).append('\'');
		sb.append(", inactiveStatus='").append(inactiveStatus).append('\'');
		sb.append(", addSenderAsStakeholder=").append(addSenderAsStakeholder);
		sb.append(", stakeholderRole='").append(stakeholderRole).append('\'');
		sb.append(", errandChannel='").append(errandChannel).append('\'');
		sb.append(", created=").append(created);
		sb.append(", modified=").append(modified);
		sb.append('}');
		return sb.toString();
	}
}
