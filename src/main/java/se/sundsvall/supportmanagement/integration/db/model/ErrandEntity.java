package se.sundsvall.supportmanagement.integration.db.model;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;

@Entity
@Table(name = "errand",
	indexes = {
		@Index(name = "idx_errand_id", columnList = "id"),
		@Index(name = "idx_errand_customer_id", columnList = "customer_id"),
		@Index(name = "idx_errand_client_id_tag", columnList = "client_id_tag")
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

	@Embedded
	private EmbeddableCustomer customer;

	@Column(name = "client_id_tag")
	private String clientIdTag;

	@Column(name = "title")
	private String title;

	@Column(name = "category_tag")
	private String categoryTag;

	@Column(name = "type_tag")
	private String typeTag;

	@Column(name = "status_tag")
	private String statusTag;

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

	public static ErrandEntity create() {
		return new ErrandEntity();
	}

	@PrePersist
	void onCreate() {
		created = now(ZoneId.systemDefault()).truncatedTo(MILLIS);
	}

	@PreUpdate
	protected void onUpdate() {
		modified = now(ZoneId.systemDefault()).truncatedTo(MILLIS);
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

	public EmbeddableCustomer getCustomer() {
		return customer;
	}

	public void setCustomer(EmbeddableCustomer customer) {
		this.customer = customer;
	}

	public ErrandEntity withCustomer(EmbeddableCustomer customer) {
		this.customer = customer;
		return this;
	}

	public String getClientIdTag() {
		return clientIdTag;
	}

	public void setClientIdTag(String clientIdTag) {
		this.clientIdTag = clientIdTag;
	}

	public ErrandEntity withClientIdTag(String clientIdTag) {
		this.clientIdTag = clientIdTag;
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

	@Override
	public int hashCode() {
		return Objects.hash(id, externalTags, customer, clientIdTag, title, categoryTag, typeTag, statusTag, priority, reporterUserId, assignedUserId, assignedGroupId, attachments, created, modified);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		var that = (ErrandEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(externalTags, that.externalTags) && Objects.equals(customer, that.customer) && Objects.equals(clientIdTag, that.clientIdTag) && Objects
			.equals(title, that.title) && Objects.equals(categoryTag, that.categoryTag) && Objects.equals(typeTag, that.typeTag) && Objects.equals(statusTag, that.statusTag) && Objects.equals(priority, that.priority) && Objects.equals(reporterUserId,
				that.reporterUserId) && Objects.equals(assignedUserId, that.assignedUserId) && Objects.equals(assignedGroupId, that.assignedGroupId) && Objects.equals(attachments, that.attachments) && Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public String toString() {
		var builder = new StringBuilder();
		builder.append("ErrandEntity [id=").append(id).append(", externalTags=").append(externalTags)
			.append(", customer=").append(customer).append(", clientIdTag=")
			.append(clientIdTag).append(", title=").append(title).append(", categoryTag=")
			.append(categoryTag).append(", typeTag=").append(typeTag).append(", statusTag=")
			.append(statusTag).append(", priority=").append(priority).append(", reporterUserId=")
			.append(reporterUserId).append(", assignedUserId=").append(assignedUserId)
			.append(", assignedGroupId=").append(assignedGroupId).append(", attachments=").append(attachments).append(", created=")
			.append(created).append(", modified=").append(modified).append("]");
		return builder.toString();
	}
}
