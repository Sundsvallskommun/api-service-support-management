package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static jakarta.persistence.FetchType.LAZY;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * Links an attachment of the errand to an investigation.
 * <p>
 * The attachment stays on the errand - the owning relation of the attachment is untouched. This is a relation on top of
 * that ownership, not a change of owner, which is why neither association cascades upwards: the link must never take
 * its attachment or its investigation with it in its fall.
 * <p>
 * The database cascades on both foreign keys, which means the <em>link</em> disappears when either side does, never
 * that the other side follows.
 */
@Entity
@Table(name = "investigation_attachment",
	indexes = {
		@Index(name = "idx_investigation_attachment_investigation_id", columnList = "investigation_id"),
		@Index(name = "idx_investigation_attachment_attachment_id", columnList = "attachment_id")
	},
	uniqueConstraints = @UniqueConstraint(name = "uq_investigation_attachment_investigation_id_attachment_id", columnNames = {
		"investigation_id", "attachment_id"
	}))
public class InvestigationAttachmentEntity implements AttachmentLink {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "investigation_id", nullable = false, foreignKey = @ForeignKey(name = "fk_investigation_attachment_investigation_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private InvestigationEntity investigationEntity;

	/** No cascade here: the attachment is owned by the errand and survives an investigation being removed. */
	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "attachment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_investigation_attachment_attachment_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private AttachmentEntity attachmentEntity;

	/** The order the attachment is shown in under its investigation. */
	@Column(name = "sort_order")
	private Integer sortOrder;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "created_by")
	private String createdBy;

	@PrePersist
	void onCreate() {
		created = now(systemDefault()).truncatedTo(MILLIS);
	}

	public static InvestigationAttachmentEntity create() {
		return new InvestigationAttachmentEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public InvestigationAttachmentEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public InvestigationEntity getInvestigationEntity() {
		return investigationEntity;
	}

	public void setInvestigationEntity(final InvestigationEntity investigationEntity) {
		this.investigationEntity = investigationEntity;
	}

	public InvestigationAttachmentEntity withInvestigationEntity(final InvestigationEntity investigationEntity) {
		this.investigationEntity = investigationEntity;
		return this;
	}

	@Override
	public AttachmentEntity getAttachmentEntity() {
		return attachmentEntity;
	}

	public void setAttachmentEntity(final AttachmentEntity attachmentEntity) {
		this.attachmentEntity = attachmentEntity;
	}

	public InvestigationAttachmentEntity withAttachmentEntity(final AttachmentEntity attachmentEntity) {
		this.attachmentEntity = attachmentEntity;
		return this;
	}

	@Override
	public Integer getSortOrder() {
		return sortOrder;
	}

	@Override
	public void setSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public InvestigationAttachmentEntity withSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
		return this;
	}

	@Override
	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public InvestigationAttachmentEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public InvestigationAttachmentEntity withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, sortOrder, created, createdBy);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final InvestigationAttachmentEntity other)) {
			return false;
		}
		return Objects.equals(id, other.id)
			&& Objects.equals(sortOrder, other.sortOrder)
			&& Objects.equals(created, other.created)
			&& Objects.equals(createdBy, other.createdBy);
	}

	@Override
	public String toString() {
		return "InvestigationAttachmentEntity{" +
			"id='" + id + '\'' +
			", investigationEntity=" + (investigationEntity != null ? investigationEntity.getId() : "null") +
			", attachmentEntity=" + (attachmentEntity != null ? attachmentEntity.getId() : "null") +
			", sortOrder=" + sortOrder +
			", created=" + created +
			", createdBy='" + createdBy + '\'' +
			'}';
	}
}
