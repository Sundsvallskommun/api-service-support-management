package se.sundsvall.supportmanagement.api.model.attachment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.time.OffsetDateTime;
import java.util.Objects;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "ErrandAttachment model")
public class ErrandAttachment {

	@Schema(description = "Unique identifier for the attachment", examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	protected String id;

	@Schema(description = "Name of the file", examples = "my-file.txt")
	@NotBlank(groups = OnCreate.class)
	protected String fileName;

	@Schema(description = "Mime type of the file", accessMode = Schema.AccessMode.READ_ONLY)
	private String mimeType;

	@Schema(description = "The attachment created date", examples = "2023-01-01T00:00:00Z")
	private OffsetDateTime created;

	public static ErrandAttachment create() {
		return new ErrandAttachment();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ErrandAttachment withId(final String id) {
		this.id = id;
		return this;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	public ErrandAttachment withFileName(final String fileName) {
		this.fileName = fileName;
		return this;
	}

	public String getMimeType() {
		return this.mimeType;
	}

	public void setMimeType(final String mimeType) {
		this.mimeType = mimeType;
	}

	public ErrandAttachment withMimeType(final String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public ErrandAttachment withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final ErrandAttachment that = (ErrandAttachment) o;
		return Objects.equals(id, that.id) && Objects.equals(fileName, that.fileName) && Objects.equals(mimeType, that.mimeType) && Objects.equals(created, that.created);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, fileName, mimeType, created);
	}

	@Override
	public String toString() {
		return "ErrandAttachment{" +
			"id='" + id + '\'' +
			", fileName='" + fileName + '\'' +
			", mimeType='" + mimeType + '\'' +
			", created=" + created +
			'}';
	}
}
