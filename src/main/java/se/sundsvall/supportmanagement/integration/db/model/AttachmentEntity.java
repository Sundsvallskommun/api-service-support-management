package se.sundsvall.supportmanagement.integration.db.model;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "attachment",
	indexes = {
		@Index(name = "idx_attachment_file_name", columnList = "file_name")
	})
public class AttachmentEntity implements Serializable {

	private static final long serialVersionUID = 2481905635449078631L;

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(name = "id")
	private String id;

	@Column(name = "file_name")
	private String fileName;

	@Column(name = "mime_type")
	private String mimeType;

	@Lob
	@Column(name = "file")
	private byte[] file;

	@Column(name = "created")
	private OffsetDateTime created;

	@Column(name = "modified")
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

	public String getMimeType() {return mimeType;
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
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		var that = (AttachmentEntity) o;
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
		var errandId = Optional.ofNullable(errandEntity).map(ErrandEntity::getId).orElse(null);
		return "AttachmentEntity[" +
				"id=" + id +
				", fileName=" + fileName + ", mimeType='" + mimeType + ", file=" + Arrays.toString(file) +
				", created=" + created + ", modified=" + modified + ", errandEntity.id=" + errandId + ']';
	}
}
