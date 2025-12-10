package se.sundsvall.supportmanagement.api.model.communication;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

public class CommunicationAttachment {

	@Schema(
		description = "The attachment ID",
		examples = "aGVsbG8gd29ybGQK"

	)
	private String id;

	@Schema(
		description = "The attachment file name",
		examples = "test.txt")
	private String fileName;

	@Schema(description = "The attachment MIME type", examples = "text/plain")
	private String mimeType;

	public static CommunicationAttachment create() {
		return new CommunicationAttachment();

	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public CommunicationAttachment withAttachmentID(final String attachmentID) {
		this.id = attachmentID;
		return this;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	public CommunicationAttachment withFileName(final String fileName) {
		this.fileName = fileName;
		return this;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(final String mimeType) {
		this.mimeType = mimeType;
	}

	public CommunicationAttachment withContentType(final String contentType) {
		this.mimeType = contentType;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final CommunicationAttachment that = (CommunicationAttachment) o;
		return Objects.equals(id, that.id) && Objects.equals(fileName, that.fileName) && Objects.equals(mimeType, that.mimeType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, fileName, mimeType);
	}

	@Override
	public String toString() {
		return "CommunicationAttachment{" +
			"id='" + id + '\'' +
			", fileName='" + fileName + '\'' +
			", mimeType='" + mimeType + '\'' +
			'}';
	}

}
