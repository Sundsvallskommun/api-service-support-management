package se.sundsvall.supportmanagement.api.model.attachment;

import io.swagger.v3.oas.annotations.media.Schema;
import se.sundsvall.dept44.common.validators.annotation.ValidBase64;
import se.sundsvall.supportmanagement.api.validation.ValidFileSize;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;

import javax.validation.constraints.NotNull;
import java.util.Objects;

@Schema(description = "ErrandAttachment model")
public class ErrandAttachment {

	@NotNull(groups = OnCreate.class)
	private ErrandAttachmentHeader errandAttachmentHeader;

	@Schema(description = "Mime type of the file", accessMode = Schema.AccessMode.READ_ONLY)
	private String mimeType;

	@Schema(description = "Base 64 encoded file, max size 10 MB", format = "base64")
	@NotNull
	@ValidBase64
	@ValidFileSize
	private String base64EncodedString;

	public static ErrandAttachment create() {
		return new ErrandAttachment();
	}

	public ErrandAttachmentHeader getErrandAttachmentHeader() {
		return errandAttachmentHeader;
	}

	public void setErrandAttachmentHeader(ErrandAttachmentHeader errandAttachmentHeader) {
		this.errandAttachmentHeader = errandAttachmentHeader;
	}

	public ErrandAttachment withErrandAttachmentHeader(ErrandAttachmentHeader errandAttachmentHeader) {
		this.errandAttachmentHeader = errandAttachmentHeader;
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

	public String getBase64EncodedString() {
		return base64EncodedString;
	}

	public void setBase64EncodedString(String base64EncodedString) {
		this.base64EncodedString = base64EncodedString;
	}

	public ErrandAttachment withBase64EncodedString(String base64EncodedString) {
		this.base64EncodedString = base64EncodedString;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(errandAttachmentHeader, mimeType, base64EncodedString);
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
		ErrandAttachment other = (ErrandAttachment) obj;
		return Objects.equals(errandAttachmentHeader, other.errandAttachmentHeader) && Objects.equals(mimeType, other.mimeType) && Objects.equals(base64EncodedString, other.base64EncodedString);
	}

	@Override
	public String toString() {
		return "ErrandAttachment [errandAttachmentHeader=" + errandAttachmentHeader + ", mimeType=" + mimeType + ", base64EncodedString=" + base64EncodedString + "]";
	}
}
