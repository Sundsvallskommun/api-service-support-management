package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;

import static jakarta.persistence.GenerationType.IDENTITY;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "email_worker_config",
	indexes = {
		@Index(name = "idx_namespace_municipality_id", columnList = "namespace, municipality_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_namespace_municipality_id", columnNames = {
			"namespace", "municipality_id"
		})
	})
public class EmailWorkerConfigEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "enabled", nullable = false)
	private boolean enabled;

	@Column(name = "errand_closed_email_sender")
	private String errandClosedEmailSender;

	@Column(name = "errand_closed_email_template", length = 5000)
	private String errandClosedEmailTemplate;

	@Column(name = "errand_closed_email_html_template", columnDefinition = "TEXT")
	private String errandClosedEmailHTMLTemplate;

	@Column(name = "errand_new_email_sender")
	private String errandNewEmailSender;

	@Column(name = "errand_new_email_template", length = 5000)
	private String errandNewEmailTemplate;

	@Column(name = "errand_new_email_html_template", columnDefinition = "TEXT")
	private String errandNewEmailHTMLTemplate;

	@Column(name = "days_of_inactivity_before_reject")
	private Integer daysOfInactivityBeforeReject;

	@Column(name = "status_for_new")
	private String statusForNew;

	@Column(name = "trigger_status_change_on")
	private String triggerStatusChangeOn;

	@Column(name = "status_change_to")
	private String statusChangeTo;

	@Column(name = "inactive_status")
	private String inactiveStatus;

	@Column(name = "add_sender_as_stakeholder")
	private boolean addSenderAsStakeholder;

	@Column(name = "stakeholder_role")
	private String stakeholderRole;

	@Column(name = "errand_channel")
	private String errandChannel;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	public static EmailWorkerConfigEntity create() {
		return new EmailWorkerConfigEntity();
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public EmailWorkerConfigEntity withId(final Long id) {
		setId(id);
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public EmailWorkerConfigEntity withMunicipalityId(final String municipalityId) {
		setMunicipalityId(municipalityId);
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public EmailWorkerConfigEntity withNamespace(final String namespace) {
		setNamespace(namespace);
		return this;
	}

	public boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	public EmailWorkerConfigEntity withEnabled(final Boolean enabled) {
		setEnabled(enabled);
		return this;
	}

	public String getErrandClosedEmailSender() {
		return errandClosedEmailSender;
	}

	public void setErrandClosedEmailSender(final String errandClosedEmailSender) {
		this.errandClosedEmailSender = errandClosedEmailSender;
	}

	public EmailWorkerConfigEntity withErrandClosedEmailSender(final String errandClosedEmailSender) {
		setErrandClosedEmailSender(errandClosedEmailSender);
		return this;
	}

	public String getErrandClosedEmailTemplate() {
		return errandClosedEmailTemplate;
	}

	public void setErrandClosedEmailTemplate(final String errandClosedEmailTemplate) {
		this.errandClosedEmailTemplate = errandClosedEmailTemplate;
	}

	public EmailWorkerConfigEntity withErrandClosedEmailTemplate(final String errandClosedEmailTemplate) {
		setErrandClosedEmailTemplate(errandClosedEmailTemplate);
		return this;
	}

	public String getErrandClosedEmailHTMLTemplate() {
		return errandClosedEmailHTMLTemplate;
	}

	public void setErrandClosedEmailHTMLTemplate(final String errandClosedEmailHTMLTemplate) {
		this.errandClosedEmailHTMLTemplate = errandClosedEmailHTMLTemplate;
	}

	public EmailWorkerConfigEntity withErrandClosedEmailHTMLTemplate(final String errandClosedEmailHTMLTemplate) {
		setErrandClosedEmailHTMLTemplate(errandClosedEmailHTMLTemplate);
		return this;
	}

	public String getErrandNewEmailSender() {
		return errandNewEmailSender;
	}

	public void setErrandNewEmailSender(final String errandNewEmailSender) {
		this.errandNewEmailSender = errandNewEmailSender;
	}

	public EmailWorkerConfigEntity withErrandNewEmailSender(final String errandNewEmailSender) {
		setErrandNewEmailSender(errandNewEmailSender);
		return this;
	}

	public String getErrandNewEmailTemplate() {
		return errandNewEmailTemplate;
	}

	public void setErrandNewEmailTemplate(final String errandNewEmailTemplate) {
		this.errandNewEmailTemplate = errandNewEmailTemplate;
	}

	public EmailWorkerConfigEntity withErrandNewEmailTemplate(final String errandNewEmailTemplate) {
		setErrandNewEmailTemplate(errandNewEmailTemplate);
		return this;
	}

	public String getErrandNewEmailHTMLTemplate() {
		return errandNewEmailHTMLTemplate;
	}

	public void setErrandNewEmailHTMLTemplate(final String errandNewEmailHTMLTemplate) {
		this.errandNewEmailHTMLTemplate = errandNewEmailHTMLTemplate;
	}

	public EmailWorkerConfigEntity withErrandNewEmailHTMLTemplate(final String errandNewEmailHTMLTemplate) {
		setErrandNewEmailHTMLTemplate(errandNewEmailHTMLTemplate);
		return this;
	}

	public Integer getDaysOfInactivityBeforeReject() {
		return daysOfInactivityBeforeReject;
	}

	public void setDaysOfInactivityBeforeReject(final Integer daysOfInactivityBeforeReject) {
		this.daysOfInactivityBeforeReject = daysOfInactivityBeforeReject;
	}

	public EmailWorkerConfigEntity withDaysOfInactivityBeforeReject(final Integer daysOfInactivityBeforeReject) {
		setDaysOfInactivityBeforeReject(daysOfInactivityBeforeReject);
		return this;
	}

	public String getStatusForNew() {
		return statusForNew;
	}

	public void setStatusForNew(final String statusForNew) {
		this.statusForNew = statusForNew;
	}

	public EmailWorkerConfigEntity withStatusForNew(final String statusForNew) {
		setStatusForNew(statusForNew);
		return this;
	}

	public String getTriggerStatusChangeOn() {
		return triggerStatusChangeOn;
	}

	public void setTriggerStatusChangeOn(final String triggerStatusChangeOn) {
		this.triggerStatusChangeOn = triggerStatusChangeOn;
	}

	public EmailWorkerConfigEntity withTriggerStatusChangeOn(final String triggerStatusChangeOn) {
		setTriggerStatusChangeOn(triggerStatusChangeOn);
		return this;
	}

	public String getStatusChangeTo() {
		return statusChangeTo;
	}

	public void setStatusChangeTo(final String statusChangeTo) {
		this.statusChangeTo = statusChangeTo;
	}

	public EmailWorkerConfigEntity withStatusChangeTo(final String statusChangeTo) {
		setStatusChangeTo(statusChangeTo);
		return this;
	}

	public String getInactiveStatus() {
		return inactiveStatus;
	}

	public void setInactiveStatus(final String inactiveStatus) {
		this.inactiveStatus = inactiveStatus;
	}

	public EmailWorkerConfigEntity withInactiveStatus(final String inactiveStatus) {
		setInactiveStatus(inactiveStatus);
		return this;
	}

	public boolean isAddSenderAsStakeholder() {
		return addSenderAsStakeholder;
	}

	public void setAddSenderAsStakeholder(final boolean addSenderAsStakeholder) {
		this.addSenderAsStakeholder = addSenderAsStakeholder;
	}

	public EmailWorkerConfigEntity withAddSenderAsStakeholder(final boolean addSenderAsStakeholder) {
		setAddSenderAsStakeholder(addSenderAsStakeholder);
		return this;
	}

	public String getStakeholderRole() {
		return stakeholderRole;
	}

	public void setStakeholderRole(final String stakeholderRole) {
		this.stakeholderRole = stakeholderRole;
	}

	public EmailWorkerConfigEntity withStakeholderRole(final String stakeholderRole) {
		setStakeholderRole(stakeholderRole);
		return this;
	}

	public String getErrandChannel() {
		return errandChannel;
	}

	public void setErrandChannel(final String errandChannel) {
		this.errandChannel = errandChannel;
	}

	public EmailWorkerConfigEntity withErrandChannel(final String errandChannel) {
		setErrandChannel(errandChannel);
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public EmailWorkerConfigEntity withCreated(final OffsetDateTime created) {
		setCreated(created);
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public EmailWorkerConfigEntity withModified(final OffsetDateTime modified) {
		setModified(modified);
		return this;
	}

	@PrePersist
	void onCreate() {
		created = now(systemDefault()).truncatedTo(MILLIS);
	}

	@PreUpdate
	void onUpdate() {
		modified = now(systemDefault()).truncatedTo(MILLIS);
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final EmailWorkerConfigEntity that = (EmailWorkerConfigEntity) o;
		return enabled == that.enabled && addSenderAsStakeholder == that.addSenderAsStakeholder && Objects.equals(id, that.id) && Objects.equals(municipalityId, that.municipalityId) && Objects.equals(namespace, that.namespace)
			&& Objects.equals(errandClosedEmailSender, that.errandClosedEmailSender) && Objects.equals(errandClosedEmailTemplate, that.errandClosedEmailTemplate) && Objects.equals(errandClosedEmailHTMLTemplate,
				that.errandClosedEmailHTMLTemplate) && Objects.equals(errandNewEmailSender, that.errandNewEmailSender) && Objects.equals(errandNewEmailTemplate, that.errandNewEmailTemplate) && Objects.equals(
					errandNewEmailHTMLTemplate, that.errandNewEmailHTMLTemplate) && Objects.equals(daysOfInactivityBeforeReject, that.daysOfInactivityBeforeReject) && Objects.equals(statusForNew, that.statusForNew) && Objects.equals(
						triggerStatusChangeOn, that.triggerStatusChangeOn) && Objects.equals(statusChangeTo, that.statusChangeTo) && Objects.equals(inactiveStatus, that.inactiveStatus) && Objects.equals(stakeholderRole, that.stakeholderRole)
			&& Objects.equals(errandChannel, that.errandChannel) && Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, municipalityId, namespace, enabled, errandClosedEmailSender, errandClosedEmailTemplate, errandClosedEmailHTMLTemplate, errandNewEmailSender, errandNewEmailTemplate, errandNewEmailHTMLTemplate, daysOfInactivityBeforeReject,
			statusForNew, triggerStatusChangeOn, statusChangeTo, inactiveStatus, addSenderAsStakeholder, stakeholderRole, errandChannel, created, modified);
	}

	@Override
	public String toString() {
		return "EmailWorkerConfigEntity{" +
			"id=" + id +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", enabled=" + enabled +
			", errandClosedEmailSender='" + errandClosedEmailSender + '\'' +
			", errandClosedEmailTemplate='" + errandClosedEmailTemplate + '\'' +
			", errandClosedEmailHTMLTemplate='" + errandClosedEmailHTMLTemplate + '\'' +
			", errandNewEmailSender='" + errandNewEmailSender + '\'' +
			", errandNewEmailTemplate='" + errandNewEmailTemplate + '\'' +
			", errandNewEmailHTMLTemplate='" + errandNewEmailHTMLTemplate + '\'' +
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
