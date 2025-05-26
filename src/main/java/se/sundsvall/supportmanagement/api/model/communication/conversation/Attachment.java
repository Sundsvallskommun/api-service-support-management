package se.sundsvall.supportmanagement.api.model.communication.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;

@Schema(description = "Attachment model")
public class Attachment {

	@Schema(description = "Attachment ID", example = "cb20c51f-fcf3-42c0-b613-de563634a8ec")
	private String id;

	@Schema(description = "Name of the file", example = "my-file.txt")
	private String fileName;

	@Schema(description = "Size of the file in bytes", example = "1024")
	private int fileSize;

	@Schema(description = "Mime type of the file")
	private String mimeType;

	@Schema(description = "The attachment created date", example = "2023-01-01T00:00:00+01:00")
	private OffsetDateTime created;

	public static Attachment create() {
		return new Attachment();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Attachment withId(String id) {
		this.id = id;
		return this;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public Attachment withFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	public int getFileSize() {
		return fileSize;
	}

	public void setFileSize(int fileSize) {
		this.fileSize = fileSize;
	}

	public Attachment withFileSize(int fileSize) {
		this.fileSize = fileSize;
		return this;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public Attachment withMimeType(String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public Attachment withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, fileName, fileSize, id, mimeType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Attachment other = (Attachment) obj;
		return Objects.equals(created, other.created) && Objects.equals(fileName, other.fileName) && fileSize == other.fileSize && Objects.equals(id, other.id) && Objects.equals(mimeType, other.mimeType);
	}

	@Override
	public String toString() {
		return "Attachment [id=" + id + ", fileName=" + fileName + ", fileSize=" + fileSize + ", mimeType=" + mimeType + ", created=" + created + "]";
	}
}
