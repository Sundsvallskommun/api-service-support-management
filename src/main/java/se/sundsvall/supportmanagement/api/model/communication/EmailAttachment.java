package se.sundsvall.supportmanagement.api.model.communication;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidBase64;
import se.sundsvall.supportmanagement.api.validation.ValidFileSize;

@Schema(description = "EmailAttachment model")
public class EmailAttachment implements RequestAttachment {

	@NotBlank
	@Schema(description = "The attachment file name", example = "test.txt", requiredMode = REQUIRED)
	private String fileName;

	@Schema(description = "The attachment (file) content as a BASE64-encoded string, max size 50 MB", format = "base64", example = "aGVsbG8gd29ybGQK", requiredMode = REQUIRED)
	@ValidBase64
	@ValidFileSize
	private String base64EncodedString;

	public static EmailAttachment create() {
		return new EmailAttachment();
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	public EmailAttachment withFileName(final String fileName) {
		this.fileName = fileName;
		return this;
	}

	public String getBase64EncodedString() {
		return base64EncodedString;
	}

	public void setBase64EncodedString(final String base64EncodedString) {
		this.base64EncodedString = base64EncodedString;
	}

	public EmailAttachment withBase64EncodedString(final String base64EncodedString) {
		this.base64EncodedString = base64EncodedString;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(base64EncodedString, fileName);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final var other = (EmailAttachment) obj;
		return Objects.equals(base64EncodedString, other.base64EncodedString) && Objects.equals(fileName, other.fileName);
	}

	@Override
	public String toString() {
		return "EmailAttachment{" +
			"fileName='" + fileName + '\'' +
			", base64EncodedString='" + base64EncodedString + '\'' +
			'}';
	}
}
