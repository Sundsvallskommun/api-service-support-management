package se.sundsvall.supportmanagement.integration.db.model;

import org.hibernate.annotations.Formula;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
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
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;

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

	@OneToMany(mappedBy = "errandEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
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
	protected void onUpdate() {
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
	public int hashCode() {
		return Objects.hash(assignedGroupId, assignedUserId, attachments, categoryTag, created, stakeholders, description, externalTags, id, modified, municipalityId, namespace, priority, reporterUserId, resolution, statusTag, title, touched, typeTag);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ErrandEntity other = (ErrandEntity) obj;
		return Objects.equals(assignedGroupId, other.assignedGroupId) && Objects.equals(assignedUserId, other.assignedUserId) && Objects.equals(attachments, other.attachments) && Objects.equals(categoryTag, other.categoryTag) && Objects.equals(
			created, other.created) && Objects.equals(stakeholders, other.stakeholders) && Objects.equals(description, other.description) && Objects.equals(externalTags, other.externalTags) && Objects.equals(id, other.id) && Objects.equals(modified,
				other.modified) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(namespace, other.namespace) && Objects.equals(priority, other.priority) && Objects.equals(reporterUserId, other.reporterUserId) && Objects
					.equals(resolution, other.resolution) && Objects.equals(statusTag, other.statusTag) && Objects.equals(title, other.title) && Objects.equals(touched, other.touched) && Objects.equals(typeTag, other.typeTag);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ErrandEntity [id=").append(id).append(", externalTags=").append(externalTags).append(", stakeholders=").append(stakeholders).append(", municipalityId=").append(municipalityId).append(", namespace=").append(namespace).append(
			", title=").append(title).append(", categoryTag=").append(categoryTag).append(", typeTag=").append(typeTag).append(", statusTag=").append(statusTag).append(", resolution=").append(resolution).append(", description=").append(description)
			.append(", priority=").append(priority).append(", reporterUserId=").append(reporterUserId).append(", assignedUserId=").append(assignedUserId).append(", assignedGroupId=").append(assignedGroupId).append(", attachments=").append(
				attachments).append(", created=").append(created).append(", modified=").append(modified).append(", touched=").append(touched).append("]");
		return builder.toString();
	}
}
