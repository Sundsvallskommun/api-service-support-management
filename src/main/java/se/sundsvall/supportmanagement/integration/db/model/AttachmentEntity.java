package se.sundsvall.supportmanagement.integration.db.model;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "attachment",
	indexes = {
		@Index(name = "idx_attachment_file_name", columnList = "file_name")
	})
public class AttachmentEntity implements Serializable {

	private static final long serialVersionUID = 2481905635449078631L;

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "file_name")
	private String fileName;

	@Column(name = "mime_type")
	private String mimeType;

	@Column(name = "file", length = LONG32)
	private byte[] file;

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

	public byte[] getFile() {
		return file;
	}

	public void setFile(byte[] file) {
		this.file = file;
	}

	public AttachmentEntity withFile(byte[] file) {
		this.file = file;
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
		if ((o == null) || (getClass() != o.getClass())) {
			return false;
		}
		final var that = (AttachmentEntity) o;
		return Objects.equals(id, that.id) &&
			Objects.equals(fileName, that.fileName) &&
			Objects.equals(mimeType, that.mimeType) &&
			Arrays.equals(file, that.file) &&
			Objects.equals(created, that.created) &&
			Objects.equals(modified, that.modified) &&
			Objects.equals(errandEntity, that.errandEntity);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, fileName, mimeType, Arrays.hashCode(file), created, modified, errandEntity);
	}

	@Override
	public String toString() {
		final var errandId = Optional.ofNullable(errandEntity).map(ErrandEntity::getId).orElse(null);
		return "AttachmentEntity[" +
			"id=" + id +
			", fileName=" + fileName + ", mimeType='" + mimeType + ", file=" + Arrays.toString(file) +
			", created=" + created + ", modified=" + modified + ", errandEntity.id=" + errandId + ']';
	}
}
