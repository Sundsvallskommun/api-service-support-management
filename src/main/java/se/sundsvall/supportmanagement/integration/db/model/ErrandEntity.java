package se.sundsvall.supportmanagement.integration.db.model;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Formula;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "errand",
	indexes = {
		@Index(name = "idx_errand_id", columnList = "id"),
		@Index(name = "idx_errand_namespace", columnList = "namespace"),
		@Index(name = "idx_errand_municipality_id", columnList = "municipality_id")
	})
public class ErrandEntity implements Serializable {

	private static final long serialVersionUID = -4433880592443933243L;

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(name = "id")
	private String id;

	@ElementCollection
	@CollectionTable(name = "external_tag",
		indexes = {
			@Index(name = "idx_external_tag_errand_id", columnList = "errand_id"),
			@Index(name = "idx_external_tag_key", columnList = "key")
		},
		joinColumns = @JoinColumn(name = "errand_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_errand_external_tag_errand_id")),
		uniqueConstraints = @UniqueConstraint(name = "uq_external_tag_errand_id_key", columnNames = { "errand_id", "key" }))
	private List<DbExternalTag> externalTags;

	@OneToMany(mappedBy = "errandEntity", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<StakeholderEntity> stakeholders;

	@Column(name = "municipality_id", nullable = false)
	private String municipalityId;

	@Column(name = "namespace", nullable = false)
	private String namespace;

	@Column(name = "title")
	private String title;

	@Column(name = "category_tag")
	private String categoryTag;

	@Column(name = "type_tag")
	private String typeTag;

	@Column(name = "status_tag")
	private String statusTag;

	@Column(name = "resolution")
	private String resolution;

	@Column(name = "description")
	@Lob
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

	@OneToMany(mappedBy = "errandEntity", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<AttachmentEntity> attachments;

	@Column(name = "created")
	private OffsetDateTime created;

	@Column(name = "modified")
	private OffsetDateTime modified;

	@Formula("greatest(coalesce(created, 0), coalesce(modified, 0))")
	private OffsetDateTime touched;

	public static ErrandEntity create() {
		return new ErrandEntity();
	}

	@PrePersist
	void onCreate() {
		created = now(systemDefault()).truncatedTo(MILLIS);
		Optional.ofNullable(stakeholders).ifPresent(st -> st
				.forEach(s -> s.setErrandEntity(this)));
	}

	@PreUpdate
	void onUpdate() {
		modified = now(systemDefault()).truncatedTo(MILLIS);
		Optional.ofNullable(stakeholders).ifPresent(st -> st
				.forEach(s -> s.setErrandEntity(this)));
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ErrandEntity withId(String id) {
		this.id = id;
		return this;
	}

	public List<DbExternalTag> getExternalTags() {
		return externalTags;
	}

	public void setExternalTags(List<DbExternalTag> externalTags) {
		this.externalTags = externalTags;
	}

	public ErrandEntity withExternalTags(List<DbExternalTag> externalTags) {
		this.externalTags = externalTags;
		return this;
	}

	public List<StakeholderEntity> getStakeholders() {
		return stakeholders;
	}

	public void setStakeholders(List<StakeholderEntity> stakeholders) {
		this.stakeholders = stakeholders;
	}

	public ErrandEntity withStakeholders(List<StakeholderEntity> stakeholders) {
		this.stakeholders = stakeholders;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public ErrandEntity withMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public ErrandEntity withNamespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public ErrandEntity withTitle(String title) {
		this.title = title;
		return this;
	}

	public String getCategoryTag() {
		return categoryTag;
	}

	public void setCategoryTag(String categoryTag) {
		this.categoryTag = categoryTag;
	}

	public ErrandEntity withCategoryTag(String categoryTag) {
		this.categoryTag = categoryTag;
		return this;
	}

	public String getTypeTag() {
		return typeTag;
	}

	public void setTypeTag(String typeTag) {
		this.typeTag = typeTag;
	}

	public ErrandEntity withTypeTag(String typeTag) {
		this.typeTag = typeTag;
		return this;
	}

	public String getStatusTag() {
		return statusTag;
	}

	public void setStatusTag(String statusTag) {
		this.statusTag = statusTag;
	}

	public ErrandEntity withStatusTag(String statusTag) {
		this.statusTag = statusTag;
		return this;
	}

	public String getResolution() {
		return resolution;
	}

	public void setResolution(String resolution) {
		this.resolution = resolution;
	}

	public ErrandEntity withResolution(String resolution) {
		this.resolution = resolution;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ErrandEntity withDescription(String description) {
		this.description = description;
		return this;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public ErrandEntity withPriority(String priority) {
		this.priority = priority;
		return this;
	}

	public String getReporterUserId() {
		return reporterUserId;
	}

	public void setReporterUserId(String reporterUserId) {
		this.reporterUserId = reporterUserId;
	}

	public ErrandEntity withReporterUserId(String reporterUserId) {
		this.reporterUserId = reporterUserId;
		return this;
	}

	public String getAssignedUserId() {
		return assignedUserId;
	}

	public void setAssignedUserId(String assignedUserId) {
		this.assignedUserId = assignedUserId;
	}

	public ErrandEntity withAssignedUserId(String assignedUserId) {
		this.assignedUserId = assignedUserId;
		return this;
	}

	public String getAssignedGroupId() {
		return assignedGroupId;
	}

	public void setAssignedGroupId(String assignedGroupId) {
		this.assignedGroupId = assignedGroupId;
	}

	public ErrandEntity withAssignedGroupId(String assignedGroupId) {
		this.assignedGroupId = assignedGroupId;
		return this;
	}

	public String getEscalationEmail() {
		return escalationEmail;
	}

	public void setEscalationEmail(String escalationEmail) {
		this.escalationEmail = escalationEmail;
	}

	public ErrandEntity withEscalationEmail(String escalationEmail) {
		this.escalationEmail = escalationEmail;
		return this;
	}

	public List<AttachmentEntity> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<AttachmentEntity> attachments) {
		this.attachments = attachments;
	}

	public ErrandEntity withAttachments(List<AttachmentEntity> attachments) {
		this.attachments = attachments;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public ErrandEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public ErrandEntity withModified(OffsetDateTime modified) {
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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ErrandEntity that = (ErrandEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(externalTags, that.externalTags) && Objects.equals(stakeholders, that.stakeholders) && Objects.equals(municipalityId, that.municipalityId) && Objects.equals(namespace, that.namespace) && Objects.equals(title, that.title) && Objects.equals(categoryTag, that.categoryTag) && Objects.equals(typeTag, that.typeTag) && Objects.equals(statusTag, that.statusTag) && Objects.equals(resolution, that.resolution) && Objects.equals(description, that.description) && Objects.equals(priority, that.priority) && Objects.equals(reporterUserId, that.reporterUserId) && Objects.equals(assignedUserId, that.assignedUserId) && Objects.equals(assignedGroupId, that.assignedGroupId) && Objects.equals(escalationEmail, that.escalationEmail) && Objects.equals(attachments, that.attachments) && Objects.equals(created, that.created) && Objects.equals(modified, that.modified) && Objects.equals(touched, that.touched);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, externalTags, stakeholders, municipalityId, namespace, title, categoryTag, typeTag, statusTag, resolution, description, priority, reporterUserId, assignedUserId, assignedGroupId, escalationEmail, attachments, created, modified, touched);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ErrandEntity{");
		sb.append("id='").append(id).append('\'');
		sb.append(", externalTags=").append(externalTags);
		sb.append(", stakeholders=").append(stakeholders);
		sb.append(", municipalityId='").append(municipalityId).append('\'');
		sb.append(", namespace='").append(namespace).append('\'');
		sb.append(", title='").append(title).append('\'');
		sb.append(", categoryTag='").append(categoryTag).append('\'');
		sb.append(", typeTag='").append(typeTag).append('\'');
		sb.append(", statusTag='").append(statusTag).append('\'');
		sb.append(", resolution='").append(resolution).append('\'');
		sb.append(", description='").append(description).append('\'');
		sb.append(", priority='").append(priority).append('\'');
		sb.append(", reporterUserId='").append(reporterUserId).append('\'');
		sb.append(", assignedUserId='").append(assignedUserId).append('\'');
		sb.append(", assignedGroupId='").append(assignedGroupId).append('\'');
		sb.append(", escalationEmail='").append(escalationEmail).append('\'');
		sb.append(", attachments=").append(attachments);
		sb.append(", created=").append(created);
		sb.append(", modified=").append(modified);
		sb.append(", touched=").append(touched);
		sb.append('}');
		return sb.toString();
	}
}
