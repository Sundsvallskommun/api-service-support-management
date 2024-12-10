package se.sundsvall.supportmanagement.integration.db.model;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "attachment",
	indexes = {
		@Index(name = "idx_attachment_file_name", columnList = "file_name"),
		@Index(name = "idx_attachment_municipality_id", columnList = "municipality_id"),
		@Index(name = "idx_attachment_namespace", columnList = "namespace")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_attachment_data_id", columnNames = {
			"attachment_data_id"
		})
	})
public class AttachmentEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "namespace")
	private String namespace;

	@Column(name = "municipality_id")
	private String municipalityId;

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

	public void setId(final String id) {
		this.id = id;
	}

	public AttachmentEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public AttachmentEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public AttachmentEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	public AttachmentEntity withFileName(final String fileName) {
		this.fileName = fileName;
		return this;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(final String mimeType) {
		this.mimeType = mimeType;
	}

	public AttachmentEntity withMimeType(final String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	public AttachmentDataEntity getAttachmentData() {
		return attachmentData;
	}

	public void setAttachmentData(final AttachmentDataEntity attachmentData) {
		this.attachmentData = attachmentData;
	}

	public AttachmentEntity withAttachmentData(final AttachmentDataEntity attachmentData) {
		this.attachmentData = attachmentData;
		return this;
	}

	public ErrandEntity getErrandEntity() {
		return errandEntity;
	}

	public void setErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
	}

	public AttachmentEntity withErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public AttachmentEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public AttachmentEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final AttachmentEntity that = (AttachmentEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(namespace, that.namespace) && Objects.equals(municipalityId, that.municipalityId) && Objects.equals(fileName, that.fileName) && Objects.equals(mimeType, that.mimeType) && Objects.equals(
			attachmentData, that.attachmentData) && Objects.equals(created, that.created) && Objects.equals(modified, that.modified) && Objects.equals(errandEntity, that.errandEntity);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, namespace, municipalityId, fileName, mimeType, attachmentData, created, modified, errandEntity);
	}

	@Override
	public String toString() {
		return "AttachmentEntity{" +
			"id='" + id + '\'' +
			", namespace='" + namespace + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", fileName='" + fileName + '\'' +
			", mimeType='" + mimeType + '\'' +
			", attachmentData=" + attachmentData +
			", created=" + created +
			", modified=" + modified +
			'}';
	}

}
