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
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
	name = "communication_attachment",
	indexes = {
		@Index(name = "idx_communication_attachment_municipality_id", columnList = "municipality_id"),
		@Index(name = "idx_communication_attachment_namespace", columnList = "namespace")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_communication_attachment_data_id", columnNames = {
			"communication_attachment_data_id"
		})
	})
public class CommunicationAttachmentEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "namespace")
	private String namespace;

	@Column(name = "municipality_id")
	private String municipalityId;

	@Column(name = "name")
	private String name;

	@Column(name = "content_type")
	private String contentType;

	@Transient
	private String foreignId;

	@Column(name = "file_size")
	private Integer fileSize;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "communication_attachment_data_id", nullable = false, foreignKey = @ForeignKey(name = "fk_communication_attachment_data_communication_attachment"))
	private CommunicationAttachmentDataEntity attachmentData;

	@ManyToOne(fetch = FetchType.LAZY)
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

	public CommunicationAttachmentDataEntity getAttachmentData() {
		return attachmentData;
	}

	public void setAttachmentData(final CommunicationAttachmentDataEntity attachmentData) {
		this.attachmentData = attachmentData;
	}

	public CommunicationAttachmentEntity withAttachmentData(final CommunicationAttachmentDataEntity attachmentData) {
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

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public CommunicationAttachmentEntity withName(final String name) {
		this.name = name;
		return this;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(final String contentType) {
		this.contentType = contentType;
	}

	public CommunicationAttachmentEntity withContentType(final String contentType) {
		this.contentType = contentType;
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
		return Objects.equals(id, that.id) && Objects.equals(namespace, that.namespace) && Objects.equals(municipalityId, that.municipalityId) && Objects.equals(name, that.name) && Objects.equals(contentType,
			that.contentType) && Objects.equals(foreignId, that.foreignId) && Objects.equals(fileSize, that.fileSize) && Objects.equals(attachmentData, that.attachmentData) && Objects.equals(communicationEntity,
				that.communicationEntity);
	}

	@Override
	public int hashCode() {
		final var communicationId = Optional.ofNullable(communicationEntity).map(CommunicationEntity::getId).orElse(null);
		return Objects.hash(id, namespace, municipalityId, name, contentType, foreignId, fileSize, attachmentData, communicationId);
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
			", name='" + name + '\'' +
			", contentType='" + contentType + '\'' +
			", foreignId='" + foreignId + '\'' +
			", fileSize=" + fileSize + '\'' +
			'}';
	}

}
