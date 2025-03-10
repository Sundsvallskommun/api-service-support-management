package se.sundsvall.supportmanagement.api.model.attachment;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.util.Objects;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;

@Schema(description = "ErrandAttachment model")
public class ErrandAttachment {

	@Schema(description = "Unique identifier for the attachment", example = "cb20c51f-fcf3-42c0-b613-de563634a8ec", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	protected String id;

	@Schema(description = "Name of the file", example = "my-file.txt")
	@NotBlank(groups = OnCreate.class)
	protected String fileName;

	@Schema(description = "Mime type of the file", accessMode = Schema.AccessMode.READ_ONLY)
	private String mimeType;

	public static ErrandAttachment create() {
		return new ErrandAttachment();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ErrandAttachment withId(String id) {
		this.id = id;
		return this;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public ErrandAttachment withFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	public String getMimeType() {
		return this.mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public ErrandAttachment withMimeType(String mimeType) {
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
		var other = (ErrandAttachment) obj;
		return Objects.equals(id, other.id) && Objects.equals(fileName, other.fileName) && Objects.equals(mimeType, other.mimeType);
	}

	@Override
	public String toString() {
		return "ErrandAttachment [id=" + id + ", fileName=" + fileName + ", mimeType=" + mimeType + "]";
	}
}
