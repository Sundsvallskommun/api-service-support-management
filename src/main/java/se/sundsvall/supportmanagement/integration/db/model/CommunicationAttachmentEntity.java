package se.sundsvall.supportmanagement.integration.db.model;

import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;


@Entity
@Table(
	name = "communication_attachment",
	uniqueConstraints = {@UniqueConstraint(name = "uq_communication_attachment_data_id", columnNames = {"communication_attachment_data_id"})})
public class CommunicationAttachmentEntity {

	@Id
	@Column(name = "id")
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "communication_id", nullable = false, foreignKey = @ForeignKey(name = "fk_communication_attachment_communication_id"))
	private CommunicationEntity communicationEntity;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "communication_attachment_data_id", nullable = false, foreignKey = @ForeignKey(name = "fk_communication_attachment_data_communication_attachment"))
	private CommunicationAttachmentDataEntity attachmentData;

	@Column(name = "name")
	private String name;

	@Column(name = "content_type")
	private String contentType;

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

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final CommunicationAttachmentEntity that = (CommunicationAttachmentEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(communicationEntity, that.communicationEntity) && Objects.equals(attachmentData, that.attachmentData) && Objects.equals(name, that.name) && Objects.equals(contentType, that.contentType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, communicationEntity, attachmentData, name, contentType);
	}

	@Override
	public String toString() {
		return "CommunicationAttachmentEntity{" +
			"id='" + id + '\'' +
			", communicationEntity=" + communicationEntity +
			", attachmentData=" + attachmentData +
			", name='" + name + '\'' +
			", contentType='" + contentType + '\'' +
			'}';
	}

}
