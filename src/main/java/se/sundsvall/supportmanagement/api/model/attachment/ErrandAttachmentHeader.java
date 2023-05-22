package se.sundsvall.supportmanagement.api.model.attachment;

import io.swagger.v3.oas.annotations.media.Schema;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "ErrandAttachmentHeader model")
public class ErrandAttachmentHeader {

	@Schema(description = "Unique identifier for the attachment", example = "cb20c51f-fcf3-42c0-b613-de563634a8ec", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	protected String id;

	@Schema(description = "Name of the file", example = "my-file.txt")
	@NotBlank(groups = OnCreate.class)
	protected String fileName;

	@Schema(description = "Mime type of the file", accessMode = Schema.AccessMode.READ_ONLY)
	private String mimeType;

	public static ErrandAttachmentHeader create() {
		return new ErrandAttachmentHeader();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ErrandAttachmentHeader withId(String id) {
		this.id = id;
		return this;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public ErrandAttachmentHeader withFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	public String getMimeType() {
		return this.mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public ErrandAttachmentHeader withMimeType(String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, fileName, mimeType);
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
		var other = (ErrandAttachmentHeader) obj;
		return Objects.equals(id, other.id) && Objects.equals(fileName, other.fileName) && Objects.equals(mimeType, other.mimeType);
	}

	@Override
	public String toString() {
		return "ErrandAttachmentHeader [id=" + id + ", fileName=" + fileName + ", mimeType=" + mimeType + "]";
	}
}
