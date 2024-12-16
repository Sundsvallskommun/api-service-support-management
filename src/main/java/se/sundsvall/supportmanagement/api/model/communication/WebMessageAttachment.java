package se.sundsvall.supportmanagement.api.model.communication;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidBase64;
import se.sundsvall.supportmanagement.api.validation.ValidFileSize;

@Schema(description = "WebMessageAttachment model")
public class WebMessageAttachment implements RequestAttachment {

	@NotBlank
	@Schema(description = "The attachment filename", example = "test.txt", requiredMode = REQUIRED)
	private String name;

	@Schema(description = "The attachment (file) content as a BASE64-encoded string, max size 10 MB", format = "base64", example = "aGVsbG8gd29ybGQK", requiredMode = REQUIRED)
	@ValidBase64
	@ValidFileSize
	private String base64EncodedString;

	public static WebMessageAttachment create() {
		return new WebMessageAttachment();
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public WebMessageAttachment withName(final String name) {
		this.name = name;
		return this;
	}

	public String getBase64EncodedString() {
		return base64EncodedString;
	}

	public void setBase64EncodedString(final String base64EncodedString) {
		this.base64EncodedString = base64EncodedString;
	}

	public WebMessageAttachment withBase64EncodedString(final String base64EncodedString) {
		this.base64EncodedString = base64EncodedString;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(base64EncodedString, name);
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
		final var other = (WebMessageAttachment) obj;
		return Objects.equals(base64EncodedString, other.base64EncodedString) && Objects.equals(name, other.name);
	}

	@Override
	public String toString() {
		return "WebMessageAttachment{" +
			"name='" + name + '\'' +
			", base64EncodedString='" + base64EncodedString + '\'' +
			'}';
	}
}
