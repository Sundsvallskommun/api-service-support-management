package se.sundsvall.supportmanagement.api.model.messaging;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.Objects;

import javax.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import se.sundsvall.dept44.common.validators.annotation.ValidBase64;
import se.sundsvall.supportmanagement.api.validation.ValidFileSize;

public class EmailAttachment {

	@NotBlank
	@Schema(description = "The attachment filename", example = "test.txt", requiredMode = REQUIRED)
	private String name;

	@Schema(description = "The attachment (file) content as a BASE64-encoded string, max size 10 MB", format = "base64", example = "aGVsbG8gd29ybGQK", requiredMode = REQUIRED)
	@ValidBase64
	@ValidFileSize
	private String base64EncodedString;

	public static EmailAttachment create() {
		return new EmailAttachment();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public EmailAttachment withName(String name) {
		this.name = name;
		return this;
	}

	public String getBase64EncodedString() {
		return base64EncodedString;
	}

	public void setBase64EncodedString(String base64EncodedString) {
		this.base64EncodedString = base64EncodedString;
	}

	public EmailAttachment withBase64EncodedString(String base64EncodedString) {
		this.base64EncodedString = base64EncodedString;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(base64EncodedString, name);
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
var other = (EmailAttachment) obj;
		return Objects.equals(base64EncodedString, other.base64EncodedString) && Objects.equals(name, other.name);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("EmailAttachment [name=").append(name).append(", base64EncodedString=").append(base64EncodedString).append("]");
		return builder.toString();
	}
}
