package se.sundsvall.supportmanagement.api.model.message;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.Objects;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

public class MessageAttachment {

	@NotBlank
	@Schema(
		description = "The attachment ID",
		example = "aGVsbG8gd29ybGQK",
		requiredMode = REQUIRED
	)
	private String attachmentID;

	@NotBlank
	@Schema(
		description = "The attachment filename",
		example = "test.txt",
		requiredMode = REQUIRED
	)
	private String name;

	@Schema(description = "The attachment content type", example = "text/plain")
	private String contentType;

	public static MessageAttachment create() {
		return new MessageAttachment();

	}

	public String getAttachmentID() {
		return attachmentID;
	}

	public void setAttachmentID(final String attachmentID) {
		this.attachmentID = attachmentID;
	}

	public MessageAttachment withAttachmentID(final String attachmentID) {
		this.attachmentID = attachmentID;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public MessageAttachment withName(final String name) {
		this.name = name;
		return this;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(final String contentType) {
		this.contentType = contentType;
	}

	public MessageAttachment withContentType(final String contentType) {
		this.contentType = contentType;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final MessageAttachment that = (MessageAttachment) o;
		return Objects.equals(attachmentID, that.attachmentID) && Objects.equals(name, that.name) && Objects.equals(contentType, that.contentType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(attachmentID, name, contentType);
	}

	@Override
	public String toString() {
		return "MessageAttachment{" +
			"attachmentID='" + attachmentID + '\'' +
			", name='" + name + '\'' +
			", contentType='" + contentType + '\'' +
			'}';
	}

}
