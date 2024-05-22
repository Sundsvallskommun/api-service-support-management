package se.sundsvall.supportmanagement.integration.db.model;

import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Formula;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import se.sundsvall.supportmanagement.integration.db.model.listener.ErrandListener;

@Entity
@Table(name = "errand",
	indexes = {
		@Index(name = "idx_errand_id", columnList = "id"),
		@Index(name = "idx_errand_namespace", columnList = "namespace"),
		@Index(name = "idx_errand_municipality_id", columnList = "municipality_id"),
		@Index(name = "idx_errand_status", columnList = "status"),
		@Index(name = "idx_errand_category", columnList = "category"),
		@Index(name = "idx_errand_type", columnList = "type"),
		@Index(name = "idx_errand_assigned_user_id", columnList = "assigned_user_id"),
		@Index(name = "idx_errand_reporter_user_id", columnList = "reporter_user_id"),
		@Index(name = "idx_errand_number", columnList = "errand_number")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_errand_number", columnNames = {"errand_number"})
	})
@EntityListeners(ErrandListener.class)
public class ErrandEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@ElementCollection
	@CollectionTable(name = "external_tag",
		indexes = {
			@Index(name = "idx_external_tag_errand_id", columnList = "errand_id"),
			@Index(name = "idx_external_tag_key", columnList = "key")
		},
		joinColumns = @JoinColumn(name = "errand_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_errand_external_tag_errand_id")),
		uniqueConstraints = @UniqueConstraint(name = "uq_external_tag_errand_id_key", columnNames = {"errand_id", "key"}))
	private List<DbExternalTag> externalTags;

	@OneToMany(mappedBy = "errandEntity", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("externalId")
	private List<StakeholderEntity> stakeholders;

	@ManyToOne
	@JoinColumn(name = "contact_reason_id")
	private ContactReasonEntity contactReasonEntity;

	@Column(name = "business_related")
	private Boolean businessRelated;

	@Column(name = "municipality_id", nullable = false)
	private String municipalityId;

	@Column(name = "namespace", nullable = false)
	private String namespace;

	@Column(name = "title")
	private String title;

	@Column(name = "category")
	private String category;

	@Column(name = "type")
	private String type;

	@Column(name = "status")
	private String status;

	@Column(name = "resolution")
	private String resolution;

	@Column(name = "description", length = LONG32)
	private String description;

	@Column(name = "priority")
	private String priority;

	@Column(name = "reporter_user_id")
	private String reporterUserId;

	@Column(name = "assigned_user_id")
	private String assignedUserId;

	@Column(name = "assigned_group_id")
	private String assignedGroupId;

	@Column(name = "escalation_email")
	private String escalationEmail;

	@OneToMany
	@JoinColumn(name = "errand_id")
	private List<ParameterEntity> parameters;

	@OneToMany(mappedBy = "errandEntity", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("fileName")
	private List<AttachmentEntity> attachments;

	@Column(name = "suspended_to")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime suspendedTo;

	@Column(name = "suspended_from")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime suspendedFrom;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	@Formula("greatest(coalesce(created, 0), coalesce(modified, 0))")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime touched;

	@Column(name = "errand_number", nullable = false)
	private String errandNumber;

	@Transient
	private String previousStatus;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "errand_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_errand_time_measure_errand_id"))
	private List<TimeMeasureEntity> timeMeasures;

	public static ErrandEntity create() {
		return new ErrandEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ErrandEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public List<ParameterEntity> getParameters() {
		return parameters;
	}

	public void setParameters(final List<ParameterEntity> parameters) {
		this.parameters = parameters;
	}

	public ErrandEntity withParameters(final List<ParameterEntity> parameters) {
		this.parameters = parameters;
		return this;
	}

	public List<DbExternalTag> getExternalTags() {
		return externalTags;
	}

	public void setExternalTags(final List<DbExternalTag> externalTags) {
		this.externalTags = externalTags;
	}

	public ErrandEntity withExternalTags(final List<DbExternalTag> externalTags) {
		this.externalTags = externalTags;
		return this;
	}

	public List<StakeholderEntity> getStakeholders() {
		return stakeholders;
	}

	public void setStakeholders(final List<StakeholderEntity> stakeholders) {
		this.stakeholders = stakeholders;
	}

	public ErrandEntity withStakeholders(final List<StakeholderEntity> stakeholders) {
		this.stakeholders = stakeholders;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public ErrandEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public ErrandEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public ErrandEntity withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(final String category) {
		this.category = category;
	}

	public ErrandEntity withCategory(final String category) {
		this.category = category;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public ErrandEntity withType(final String type) {
		this.type = type;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public ErrandEntity withStatus(final String status) {
		this.status = status;
		return this;
	}

	public String getResolution() {
		return resolution;
	}

	public void setResolution(final String resolution) {
		this.resolution = resolution;
	}

	public ErrandEntity withResolution(final String resolution) {
		this.resolution = resolution;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public ErrandEntity withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(final String priority) {
		this.priority = priority;
	}

	public ErrandEntity withPriority(final String priority) {
		this.priority = priority;
		return this;
	}

	public String getReporterUserId() {
		return reporterUserId;
	}

	public void setReporterUserId(final String reporterUserId) {
		this.reporterUserId = reporterUserId;
	}

	public ErrandEntity withReporterUserId(final String reporterUserId) {
		this.reporterUserId = reporterUserId;
		return this;
	}

	public String getAssignedUserId() {
		return assignedUserId;
	}

	public void setAssignedUserId(final String assignedUserId) {
		this.assignedUserId = assignedUserId;
	}

	public ErrandEntity withAssignedUserId(final String assignedUserId) {
		this.assignedUserId = assignedUserId;
		return this;
	}

	public String getAssignedGroupId() {
		return assignedGroupId;
	}

	public void setAssignedGroupId(final String assignedGroupId) {
		this.assignedGroupId = assignedGroupId;
	}

	public ErrandEntity withAssignedGroupId(final String assignedGroupId) {
		this.assignedGroupId = assignedGroupId;
		return this;
	}

	public String getEscalationEmail() {
		return escalationEmail;
	}

	public void setEscalationEmail(final String escalationEmail) {
		this.escalationEmail = escalationEmail;
	}

	public ErrandEntity withEscalationEmail(final String escalationEmail) {
		this.escalationEmail = escalationEmail;
		return this;
	}

	public List<AttachmentEntity> getAttachments() {
		return attachments;
	}

	public void setAttachments(final List<AttachmentEntity> attachments) {
		this.attachments = attachments;
	}

	public ErrandEntity withAttachments(final List<AttachmentEntity> attachments) {
		this.attachments = attachments;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public ErrandEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public ErrandEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public OffsetDateTime getTouched() {
		return touched;
	}

	public void setTouched(final OffsetDateTime touched) {
		this.touched = touched;
	}

	public ErrandEntity withTouched(final OffsetDateTime touched) {
		this.touched = touched;
		return this;
	}

	public String getErrandNumber() {
		return errandNumber;
	}

	public void setErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
	}

	public ErrandEntity withErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
		return this;
	}

	public ContactReasonEntity getContactReason() {
		return contactReasonEntity;
	}

	public void setContactReason(final ContactReasonEntity contactReasonEntity) {
		this.contactReasonEntity = contactReasonEntity;
	}

	public ErrandEntity withContactReason(final ContactReasonEntity contactReasonEntity) {
		this.contactReasonEntity = contactReasonEntity;
		return this;
	}

	public Boolean getBusinessRelated() {
		return businessRelated;
	}

	public void setBusinessRelated(final Boolean businessRelated) {
		this.businessRelated = businessRelated;
	}

	public ErrandEntity withBusinessRelated(final Boolean businessRelated) {
		this.businessRelated = businessRelated;
		return this;
	}

	public OffsetDateTime getSuspendedTo() {
		return suspendedTo;
	}

	public void setSuspendedTo(final OffsetDateTime suspendedTo) {
		this.suspendedTo = suspendedTo;
	}

	public ErrandEntity withSuspendedTo(final OffsetDateTime suspendedTo) {
		this.suspendedTo = suspendedTo;
		return this;
	}

	public OffsetDateTime getSuspendedFrom() {
		return suspendedFrom;
	}

	public void setSuspendedFrom(final OffsetDateTime suspendedFrom) {
		this.suspendedFrom = suspendedFrom;
	}

	public ErrandEntity withSuspendedFrom(final OffsetDateTime suspendedFrom) {
		this.suspendedFrom = suspendedFrom;
		return this;
	}

	public String getPreviousStatus() {
		return previousStatus;
	}

	public void setPreviousStatus(final String previousStatus) {
		this.previousStatus = previousStatus;
	}

	public ErrandEntity withPreviousStatus(final String previousStatus) {
		this.previousStatus = previousStatus;
		return this;
	}

	public List<TimeMeasureEntity> getTimeMeasures() {
		return timeMeasures;
	}

	public void setTimeMeasures(final List<TimeMeasureEntity> timeMeasures) {
		this.timeMeasures = timeMeasures;
	}

	public ErrandEntity withTimeMeasures(final List<TimeMeasureEntity> timeMeasures) {
		this.timeMeasures = timeMeasures;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final ErrandEntity that = (ErrandEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(externalTags, that.externalTags) && Objects.equals(stakeholders, that.stakeholders) && Objects.equals(contactReasonEntity, that.contactReasonEntity) && Objects.equals(businessRelated, that.businessRelated) && Objects.equals(municipalityId, that.municipalityId) && Objects.equals(namespace, that.namespace) && Objects.equals(title, that.title) && Objects.equals(category, that.category) && Objects.equals(type, that.type) && Objects.equals(status, that.status) && Objects.equals(resolution, that.resolution) && Objects.equals(description, that.description) && Objects.equals(priority, that.priority) && Objects.equals(reporterUserId, that.reporterUserId) && Objects.equals(assignedUserId, that.assignedUserId) && Objects.equals(assignedGroupId, that.assignedGroupId) && Objects.equals(escalationEmail, that.escalationEmail) && Objects.equals(parameters, that.parameters) && Objects.equals(attachments, that.attachments) && Objects.equals(suspendedTo, that.suspendedTo) && Objects.equals(suspendedFrom, that.suspendedFrom) && Objects.equals(created, that.created) && Objects.equals(modified, that.modified) && Objects.equals(touched, that.touched) && Objects.equals(errandNumber, that.errandNumber) && Objects.equals(previousStatus, that.previousStatus) && Objects.equals(timeMeasures, that.timeMeasures);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, externalTags, stakeholders, contactReasonEntity, businessRelated, municipalityId, namespace, title, category, type, status, resolution, description, priority, reporterUserId, assignedUserId, assignedGroupId, escalationEmail, parameters, attachments, suspendedTo, suspendedFrom, created, modified, touched, errandNumber, previousStatus, timeMeasures);
	}

	@Override
	public String toString() {
		return "ErrandEntity{" +
			"id='" + id + '\'' +
			", externalTags=" + externalTags +
			", stakeholders=" + stakeholders +
			", contactReasonEntity=" + contactReasonEntity +
			", businessRelated=" + businessRelated +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", title='" + title + '\'' +
			", category='" + category + '\'' +
			", type='" + type + '\'' +
			", status='" + status + '\'' +
			", resolution='" + resolution + '\'' +
			", description='" + description + '\'' +
			", priority='" + priority + '\'' +
			", reporterUserId='" + reporterUserId + '\'' +
			", assignedUserId='" + assignedUserId + '\'' +
			", assignedGroupId='" + assignedGroupId + '\'' +
			", escalationEmail='" + escalationEmail + '\'' +
			", parameters=" + parameters +
			", attachments=" + attachments +
			", suspendedTo=" + suspendedTo +
			", suspendedFrom=" + suspendedFrom +
			", created=" + created +
			", modified=" + modified +
			", touched=" + touched +
			", errandNumber='" + errandNumber + '\'' +
			", previousStatus='" + previousStatus + '\'' +
			", timeMeasures=" + timeMeasures +
			'}';
	}

}
