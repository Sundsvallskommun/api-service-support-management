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
	uniqueConstraints = {@UniqueConstraint(name = "UK_communication_attachment_data_id", columnNames = {"communication_attachment_data_id"})})
public class CommunicationAttachmentEntity {

	@Id
	@Column(name = "attachment_id")
	private String attachmentID;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "communication_id", nullable = false, foreignKey = @ForeignKey(name = "fk_communication_attachment_communication_id"))
	private CommunicationEntity communicationID;

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

	public String getAttachmentID() {
		return attachmentID;
	}

	public void setAttachmentID(final String attachmentID) {
		this.attachmentID = attachmentID;
	}

	public CommunicationAttachmentEntity withAttachmentID(final String attachmentID) {
		this.attachmentID = attachmentID;
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

	public CommunicationEntity getCommunicationID() {
		return communicationID;
	}

	public void setCommunicationID(final CommunicationEntity communicationID) {
		this.communicationID = communicationID;
	}

	public CommunicationAttachmentEntity withCommunicationID(final CommunicationEntity communicationID) {
		this.communicationID = communicationID;
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
		return Objects.equals(attachmentID, that.attachmentID) && Objects.equals(communicationID, that.communicationID) && Objects.equals(attachmentData, that.attachmentData) && Objects.equals(name, that.name) && Objects.equals(contentType, that.contentType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(attachmentID, communicationID, attachmentData, name, contentType);
	}

	@Override
	public String toString() {
		return "CommunicationAttachmentEntity{" +
			"attachmentID='" + attachmentID + '\'' +
			", communicationID='" + communicationID + '\'' +
			", attachmentData=" + attachmentData +
			", name='" + name + '\'' +
			", contentType='" + contentType + '\'' +
			'}';
	}

}
