package se.sundsvall.supportmanagement.integration.db.model.communication;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;

@Entity
@Table(
	name = "communication_attachment",
	indexes = {
		@Index(name = "idx_communication_attachment_municipality_id", columnList = "municipality_id"),
		@Index(name = "idx_communication_attachment_namespace", columnList = "namespace")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_attachment_data_id", columnNames = {
			"attachment_data_id"
		})
	})
public class CommunicationAttachmentEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "namespace", length = 32)
	private String namespace;

	@Column(name = "municipality_id", length = 8)
	private String municipalityId;

	@Column(name = "file_name")
	private String fileName;

	@Column(name = "mime_type")
	private String mimeType;

	@Transient
	private String foreignId;

	@Column(name = "file_size")
	private Integer fileSize;

	@ManyToOne(fetch = LAZY, cascade = ALL)
	@JoinColumn(name = "attachment_data_id", nullable = false, foreignKey = @ForeignKey(name = "fk_communication_attachment_attachment_data"))
	private AttachmentDataEntity attachmentData;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "communication_id", nullable = false, foreignKey = @ForeignKey(name = "fk_communication_attachment_communication_id"))
	private CommunicationEntity communicationEntity;

	public static CommunicationAttachmentEntity create() {
		return new CommunicationAttachmentEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public CommunicationAttachmentEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public CommunicationAttachmentEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public CommunicationAttachmentEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public AttachmentDataEntity getAttachmentData() {
		return attachmentData;
	}

	public void setAttachmentData(final AttachmentDataEntity attachmentData) {
		this.attachmentData = attachmentData;
	}

	public CommunicationAttachmentEntity withAttachmentData(final AttachmentDataEntity attachmentData) {
		this.attachmentData = attachmentData;
		return this;
	}

	public CommunicationEntity getCommunicationEntity() {
		return communicationEntity;
	}

	public void setCommunicationEntity(final CommunicationEntity communicationID) {
		this.communicationEntity = communicationID;
	}

	public CommunicationAttachmentEntity withCommunicationEntity(final CommunicationEntity communicationID) {
		this.communicationEntity = communicationID;
		return this;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	public CommunicationAttachmentEntity withFileName(final String fileName) {
		this.fileName = fileName;
		return this;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(final String mimeType) {
		this.mimeType = mimeType;
	}

	public CommunicationAttachmentEntity withContentType(final String contentType) {
		this.mimeType = contentType;
		return this;
	}

	public String getForeignId() {
		return foreignId;
	}

	public void setForeignId(final String foreignId) {
		this.foreignId = foreignId;
	}

	public CommunicationAttachmentEntity withForeignId(final String foreignId) {
		this.foreignId = foreignId;
		return this;
	}

	public Integer getFileSize() {
		return fileSize;
	}

	public void setFileSize(final Integer fileSize) {
		this.fileSize = fileSize;
	}

	public CommunicationAttachmentEntity withFileSize(final Integer fileSize) {
		this.fileSize = fileSize;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final CommunicationAttachmentEntity that = (CommunicationAttachmentEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(namespace, that.namespace) && Objects.equals(municipalityId, that.municipalityId) && Objects.equals(fileName, that.fileName) && Objects.equals(mimeType,
			that.mimeType) && Objects.equals(foreignId, that.foreignId) && Objects.equals(fileSize, that.fileSize) && Objects.equals(attachmentData, that.attachmentData) && Objects.equals(communicationEntity,
				that.communicationEntity);
	}

	@Override
	public int hashCode() {
		final var communicationId = Optional.ofNullable(communicationEntity).map(CommunicationEntity::getId).orElse(null);
		return Objects.hash(id, namespace, municipalityId, fileName, mimeType, foreignId, fileSize, attachmentData, communicationId);
	}

	@Override
	public String toString() {
		final var communicationId = Optional.ofNullable(communicationEntity).map(CommunicationEntity::getId).orElse(null);
		return "CommunicationAttachmentEntity{" +
			"id='" + id + '\'' +
			", communicationEntity.id=" + communicationId +
			", namespace='" + namespace + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", attachmentData=" + attachmentData +
			", fileName='" + fileName + '\'' +
			", mimeType='" + mimeType + '\'' +
			", foreignId='" + foreignId + '\'' +
			", fileSize=" + fileSize + '\'' +
			'}';
	}

}
