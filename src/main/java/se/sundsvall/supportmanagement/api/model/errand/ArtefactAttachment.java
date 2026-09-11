package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachmentPurpose;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * An attachment of the errand as it is used by a handling artefact.
 * <p>
 * Only ever an answer. The order it is shown in is written through {@link ArtefactAttachmentLink}, what it is for
 * through the attachment resource of the errand, and the file itself is read - content and all - through
 * {@code GET /{municipalityId}/{namespace}/errands/{errandId}/attachments/{attachmentId}}.
 */
@Schema(description = "Attachment of an errand linked to a handling artefact")
public class ArtefactAttachment {

	@Schema(description = "Attachment ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", accessMode = READ_ONLY)
	private String attachmentId;

	@Schema(description = "Name of the file", examples = "yttrande.pdf", accessMode = READ_ONLY)
	private String fileName;

	@Schema(description = "Mime type of the file", examples = "application/pdf", accessMode = READ_ONLY)
	private String mimeType;

	@Schema(description = "Size of the file in bytes", examples = "40960", accessMode = READ_ONLY)
	private Integer fileSize;

	@Schema(description = "What the attachment is for. Left out for an attachment without a purpose", accessMode = READ_ONLY)
	private ErrandAttachmentPurpose purpose;

	@Schema(description = "Order the attachment is shown in under the artefact", examples = "1", accessMode = READ_ONLY)
	private Integer sortOrder;

	@Schema(description = "Timestamp when the attachment was linked", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "User who linked the attachment", examples = "jo12doe", accessMode = READ_ONLY)
	private String createdBy;

	public static ArtefactAttachment create() {
		return new ArtefactAttachment();
	}

	public String getAttachmentId() {
		return attachmentId;
	}

	public void setAttachmentId(final String attachmentId) {
		this.attachmentId = attachmentId;
	}

	public ArtefactAttachment withAttachmentId(final String attachmentId) {
		this.attachmentId = attachmentId;
		return this;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	public ArtefactAttachment withFileName(final String fileName) {
		this.fileName = fileName;
		return this;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(final String mimeType) {
		this.mimeType = mimeType;
	}

	public ArtefactAttachment withMimeType(final String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	public Integer getFileSize() {
		return fileSize;
	}

	public void setFileSize(final Integer fileSize) {
		this.fileSize = fileSize;
	}

	public ArtefactAttachment withFileSize(final Integer fileSize) {
		this.fileSize = fileSize;
		return this;
	}

	public ErrandAttachmentPurpose getPurpose() {
		return purpose;
	}

	public void setPurpose(final ErrandAttachmentPurpose purpose) {
		this.purpose = purpose;
	}

	public ArtefactAttachment withPurpose(final ErrandAttachmentPurpose purpose) {
		this.purpose = purpose;
		return this;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public ArtefactAttachment withSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public ArtefactAttachment withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public ArtefactAttachment withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(attachmentId, fileName, mimeType, fileSize, purpose, sortOrder, created, createdBy);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final ArtefactAttachment other)) {
			return false;
		}
		return Objects.equals(attachmentId, other.attachmentId)
			&& Objects.equals(fileName, other.fileName)
			&& Objects.equals(mimeType, other.mimeType)
			&& Objects.equals(fileSize, other.fileSize)
			&& Objects.equals(purpose, other.purpose)
			&& Objects.equals(sortOrder, other.sortOrder)
			&& Objects.equals(created, other.created)
			&& Objects.equals(createdBy, other.createdBy);
	}

	@Override
	public String toString() {
		return "ArtefactAttachment{" +
			"attachmentId='" + attachmentId + '\'' +
			", fileName='" + fileName + '\'' +
			", mimeType='" + mimeType + '\'' +
			", fileSize=" + fileSize +
			", purpose=" + purpose +
			", sortOrder=" + sortOrder +
			", created=" + created +
			", createdBy='" + createdBy + '\'' +
			'}';
	}
}
