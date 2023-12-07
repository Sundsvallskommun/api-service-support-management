package se.sundsvall.supportmanagement.api.model.communication;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

public class CommunicationAttachment {

	@Schema(
		description = "The attachment ID",
		example = "aGVsbG8gd29ybGQK"

	)
	private String attachmentID;

	@Schema(
		description = "The attachment filename",
		example = "test.txt"
	)
	private String name;

	@Schema(description = "The attachment content type", example = "text/plain")
	private String contentType;

	public static CommunicationAttachment create() {
		return new CommunicationAttachment();

	}

	public String getAttachmentID() {
		return attachmentID;
	}

	public void setAttachmentID(final String attachmentID) {
		this.attachmentID = attachmentID;
	}

	public CommunicationAttachment withAttachmentID(final String attachmentID) {
		this.attachmentID = attachmentID;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public CommunicationAttachment withName(final String name) {
		this.name = name;
		return this;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(final String contentType) {
		this.contentType = contentType;
	}

	public CommunicationAttachment withContentType(final String contentType) {
		this.contentType = contentType;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final CommunicationAttachment that = (CommunicationAttachment) o;
		return Objects.equals(attachmentID, that.attachmentID) && Objects.equals(name, that.name) && Objects.equals(contentType, that.contentType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(attachmentID, name, contentType);
	}

	@Override
	public String toString() {
		return "CommunicationAttachment{" +
			"attachmentID='" + attachmentID + '\'' +
			", name='" + name + '\'' +
			", contentType='" + contentType + '\'' +
			'}';
	}

}
