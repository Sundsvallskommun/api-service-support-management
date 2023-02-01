package se.sundsvall.supportmanagement.api.model.attachment;

import io.swagger.v3.oas.annotations.media.Schema;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Null;
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

	@Override
	public int hashCode() {
		return Objects.hash(id, fileName);
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
		return Objects.equals(id, other.id) && Objects.equals(fileName, other.fileName);
	}

	@Override
	public String toString() {
		return "ErrandAttachmentHeader [id=" + id + ", fileName=" + fileName + "]";
	}
}
