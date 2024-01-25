package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "attachment",
	indexes = {
		@Index(name = "idx_attachment_file_name", columnList = "file_name")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_attachment_data_id", columnNames = { "attachment_data_id" })
	})
public class AttachmentEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "file_name")
	private String fileName;

	@Column(name = "mime_type")
	private String mimeType;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "attachment_data_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attachment_data_attachment"))
	private AttachmentDataEntity attachmentData;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "errand_id", nullable = false, foreignKey = @ForeignKey(name = "fk_errand_attachment_errand_id"))
	private ErrandEntity errandEntity;

	public static AttachmentEntity create() {
		return new AttachmentEntity();
	}

	@PrePersist
	void onCreate() {
		created = now(ZoneId.systemDefault()).truncatedTo(MILLIS);
	}

	@PreUpdate
	void onUpdate() {
		modified = now(ZoneId.systemDefault()).truncatedTo(MILLIS);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public AttachmentEntity withId(String id) {
		this.id = id;
		return this;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public AttachmentEntity withFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public AttachmentEntity withMimeType(String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	public AttachmentDataEntity getAttachmentData() {
		return attachmentData;
	}

	public void setAttachmentData(AttachmentDataEntity attachmentData) {
		this.attachmentData = attachmentData;
	}

	public AttachmentEntity withAttachmentData(AttachmentDataEntity attachmentData) {
		this.attachmentData = attachmentData;
		return this;
	}

	public ErrandEntity getErrandEntity() {
		return errandEntity;
	}

	public void setErrandEntity(ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
	}

	public AttachmentEntity withErrandEntity(ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public AttachmentEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public AttachmentEntity withModified(OffsetDateTime modified) {
		this.modified = modified;
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
		AttachmentEntity that = (AttachmentEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(fileName, that.fileName) && Objects.equals(mimeType, that.mimeType) && Objects.equals(attachmentData, that.attachmentData) && Objects.equals(created, that.created) && Objects.equals(modified, that.modified) && Objects.equals(errandEntity, that.errandEntity);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, fileName, mimeType, attachmentData, created, modified, errandEntity);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("AttachmentEntity{");
		sb.append("id='").append(id).append('\'');
		sb.append(", fileName='").append(fileName).append('\'');
		sb.append(", mimeType='").append(mimeType).append('\'');
		sb.append(", attachmentData=").append(attachmentData);
		sb.append(", created=").append(created);
		sb.append(", modified=").append(modified);
		sb.append(", errandEntity=").append(errandEntity);
		sb.append('}');
		return sb.toString();
	}
}
