package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachmentPurpose;
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachment;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentPurposeEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.service.mapper.ArtefactAttachmentMapper.toArtefactAttachment;
import static se.sundsvall.supportmanagement.service.mapper.ArtefactAttachmentMapper.toArtefactAttachments;

class ArtefactAttachmentMapperTest {

	@Test
	void testToArtefactAttachment() {

		// Arrange
		final var attachment = AttachmentEntity.create()
			.withId("attachmentId")
			.withFileName("file.pdf")
			.withMimeType("application/pdf")
			.withFileSize(40960)
			.withPurpose(AttachmentPurposeEntity.create().withId("purposeId").withName("RESPONSE").withDisplayName("Inkommen handling"));

		// Act
		final var result = toArtefactAttachment(attachment);

		// Assert
		assertThat(result.getAttachmentId()).isEqualTo("attachmentId");
		assertThat(result.getFileName()).isEqualTo("file.pdf");
		assertThat(result.getMimeType()).isEqualTo("application/pdf");
		assertThat(result.getFileSize()).isEqualTo(40960);
		assertThat(result.getPurpose()).isEqualTo(ErrandAttachmentPurpose.create().withId("purposeId").withName("RESPONSE").withDisplayName("Inkommen handling"));
	}

	@Test
	void testToArtefactAttachmentWithoutPurpose() {

		// Act
		final var result = toArtefactAttachment(AttachmentEntity.create().withId("attachmentId"));

		// Assert
		assertThat(result.getAttachmentId()).isEqualTo("attachmentId");
		assertThat(result.getPurpose()).isNull();
	}

	@Test
	void testToArtefactAttachmentWithNull() {
		assertThat(toArtefactAttachment(null)).isNull();
	}

	@Test
	void testToArtefactAttachments() {

		// Act
		final var result = toArtefactAttachments(List.of(AttachmentEntity.create().withId("one"), AttachmentEntity.create().withId("two")));

		// Assert
		assertThat(result).extracting(ArtefactAttachment::getAttachmentId).containsExactly("one", "two");
	}

	@Test
	void testToArtefactAttachmentsWithNull() {
		assertThat(toArtefactAttachments(null)).isEmpty();
	}
}
