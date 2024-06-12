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
import org.hibernate.annotations.TimeZoneStorage;

import java.time.OffsetDateTime;
import java.util.Objects;

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
		@UniqueConstraint(name = "uq_namespace_municipality_id", columnNames = { "namespace", "municipality_id"})
	}
)
public class EmailWorkerConfigEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "municipality_id", nullable = false)
	private String municipalityId;

	@Column(name = "namespace", nullable = false)
	private String namespace;

	@Column(name = "enabled", nullable = false)
	private boolean enabled;

	@Column(name = "errand_closed_email_sender")
	private String errandClosedEmailSender;

	@Column(name = "errand_closed_email_template", length = 5000)
	private String errandClosedEmailTemplate;

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

	public void setId(Long id) {
		this.id = id;
	}

	public EmailWorkerConfigEntity withId(Long id) {
		setId(id);
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public EmailWorkerConfigEntity withMunicipalityId(String municipalityId) {
		setMunicipalityId(municipalityId);
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public EmailWorkerConfigEntity withNamespace(String namespace) {
		setNamespace(namespace);
		return this;
	}

	public boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public EmailWorkerConfigEntity withEnabled(Boolean enabled) {
		setEnabled(enabled);
		return this;
	}

	public String getErrandClosedEmailSender() {
		return errandClosedEmailSender;
	}

	public void setErrandClosedEmailSender(String errandClosedEmailSender) {
		this.errandClosedEmailSender = errandClosedEmailSender;
	}

	public EmailWorkerConfigEntity withErrandClosedEmailSender(String errandClosedEmailSender) {
		setErrandClosedEmailSender(errandClosedEmailSender);
		return this;
	}

	public String getErrandClosedEmailTemplate() {
		return errandClosedEmailTemplate;
	}

	public void setErrandClosedEmailTemplate(String errandClosedEmailTemplate) {
		this.errandClosedEmailTemplate = errandClosedEmailTemplate;
	}

	public EmailWorkerConfigEntity withErrandClosedEmailTemplate(String errandClosedEmailTemplate) {
		setErrandClosedEmailTemplate(errandClosedEmailTemplate);
		return this;
	}

	public Integer getDaysOfInactivityBeforeReject() {
		return daysOfInactivityBeforeReject;
	}

	public void setDaysOfInactivityBeforeReject(Integer daysOfInactivityBeforeReject) {
		this.daysOfInactivityBeforeReject = daysOfInactivityBeforeReject;
	}

	public EmailWorkerConfigEntity withDaysOfInactivityBeforeReject(Integer daysOfInactivityBeforeReject) {
		setDaysOfInactivityBeforeReject(daysOfInactivityBeforeReject);
		return this;
	}

	public String getStatusForNew() {
		return statusForNew;
	}

	public void setStatusForNew(String statusForNew) {
		this.statusForNew = statusForNew;
	}

	public EmailWorkerConfigEntity withStatusForNew(String statusForNew) {
		setStatusForNew(statusForNew);
		return this;
	}

	public String getTriggerStatusChangeOn() {
		return triggerStatusChangeOn;
	}

	public void setTriggerStatusChangeOn(String triggerStatusChangeOn) {
		this.triggerStatusChangeOn = triggerStatusChangeOn;
	}

	public EmailWorkerConfigEntity withTriggerStatusChangeOn(String triggerStatusChangeOn) {
		setTriggerStatusChangeOn(triggerStatusChangeOn);
		return this;
	}

	public String getStatusChangeTo() {
		return statusChangeTo;
	}

	public void setStatusChangeTo(String statusChangeTo) {
		this.statusChangeTo = statusChangeTo;
	}

	public EmailWorkerConfigEntity withStatusChangeTo(String statusChangeTo) {
		setStatusChangeTo(statusChangeTo);
		return this;
	}

	public String getInactiveStatus() {
		return inactiveStatus;
	}

	public void setInactiveStatus(String inactiveStatus) {
		this.inactiveStatus = inactiveStatus;
	}

	public EmailWorkerConfigEntity withInactiveStatus(String inactiveStatus) {
		setInactiveStatus(inactiveStatus);
		return this;
	}

	public boolean isAddSenderAsStakeholder() {
		return addSenderAsStakeholder;
	}

	public void setAddSenderAsStakeholder(boolean addSenderAsStakeholder) {
		this.addSenderAsStakeholder = addSenderAsStakeholder;
	}

	public EmailWorkerConfigEntity withAddSenderAsStakeholder(boolean addSenderAsStakeholder) {
		setAddSenderAsStakeholder(addSenderAsStakeholder);
		return this;
	}

	public String getStakeholderRole() {
		return stakeholderRole;
	}

	public void setStakeholderRole(String stakeholderRole) {
		this.stakeholderRole = stakeholderRole;
	}

	public EmailWorkerConfigEntity withStakeholderRole(String stakeholderRole) {
		setStakeholderRole(stakeholderRole);
		return this;
	}

	public String getErrandChannel() {
		return errandChannel;
	}

	public void setErrandChannel(String errandChannel) {
		this.errandChannel = errandChannel;
	}

	public EmailWorkerConfigEntity withErrandChannel(String errandChannel) {
		setErrandChannel(errandChannel);
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public EmailWorkerConfigEntity withCreated(OffsetDateTime created) {
		setCreated(created);
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public EmailWorkerConfigEntity withModified(OffsetDateTime modified) {
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
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		EmailWorkerConfigEntity that = (EmailWorkerConfigEntity) object;
		return Objects.equals(id, that.id)
			&& Objects.equals(municipalityId, that.municipalityId)
			&& Objects.equals(namespace, that.namespace)
			&& Objects.equals(enabled, that.enabled)
			&& Objects.equals(errandClosedEmailSender, that.errandClosedEmailSender)
			&& Objects.equals(errandClosedEmailTemplate, that.errandClosedEmailTemplate)
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
		return Objects.hash(id, municipalityId, namespace, enabled, errandClosedEmailSender, errandClosedEmailTemplate,
			daysOfInactivityBeforeReject, statusForNew, triggerStatusChangeOn, statusChangeTo, inactiveStatus, addSenderAsStakeholder,
			stakeholderRole, errandChannel, created, modified);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("EmailWorkerConfigEntity{");
		sb.append("id=").append(id);
		sb.append(", municipalityId='").append(municipalityId).append('\'');
		sb.append(", namespace='").append(namespace).append('\'');
		sb.append(", enabled=").append(enabled);
		sb.append(", errandClosedEmailSender='").append(errandClosedEmailSender).append('\'');
		sb.append(", errandClosedEmailTemplate='").append(errandClosedEmailTemplate).append('\'');
		sb.append(", daysOfInactivityBeforeReject=").append(daysOfInactivityBeforeReject);
		sb.append(", statusForNew='").append(statusForNew).append('\'');
		sb.append(", triggerStatusChangeOn='").append(triggerStatusChangeOn).append('\'');
		sb.append(", statusChangeTo='").append(statusChangeTo).append('\'');
		sb.append(", inactiveStatus='").append(inactiveStatus).append('\'');
		sb.append(", addSenderAsStakeholder='").append(addSenderAsStakeholder).append('\'');
		sb.append(", stakeholderRole='").append(stakeholderRole).append('\'');
		sb.append(", errandChannel='").append(errandChannel).append('\'');
		sb.append(", created=").append(created);
		sb.append(", modified=").append(modified);
		sb.append('}');
		return sb.toString();
	}
}
