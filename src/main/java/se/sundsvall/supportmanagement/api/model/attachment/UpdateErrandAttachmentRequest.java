package se.sundsvall.supportmanagement.api.model.attachment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.Objects;

/**
 * What can be written about an attachment of the errand once it is there: what it is for.
 * <p>
 * The name, type and content of the attachment are set when it is uploaded, and the operation answers with
 * {@link ErrandAttachment}.
 */
@Schema(description = "Writable properties of an errand attachment")
public class UpdateErrandAttachmentRequest {

	@Schema(description = "What the attachment is for, named by the id of an attachment purpose of the namespace. Left as it is when omitted")
	@Valid
	private ErrandAttachmentPurpose purpose;

	public static UpdateErrandAttachmentRequest create() {
		return new UpdateErrandAttachmentRequest();
	}

	public ErrandAttachmentPurpose getPurpose() {
		return purpose;
	}

	public void setPurpose(final ErrandAttachmentPurpose purpose) {
		this.purpose = purpose;
	}

	public UpdateErrandAttachmentRequest withPurpose(final ErrandAttachmentPurpose purpose) {
		this.purpose = purpose;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(purpose);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final UpdateErrandAttachmentRequest other)) {
			return false;
		}
		return Objects.equals(purpose, other.purpose);
	}

	@Override
	public String toString() {
		return "UpdateErrandAttachmentRequest{" +
			"purpose=" + purpose +
			'}';
	}
}
