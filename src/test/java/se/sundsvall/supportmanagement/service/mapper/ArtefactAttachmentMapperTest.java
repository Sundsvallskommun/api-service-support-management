package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachmentPurpose;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentPurposeEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.MeasureAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementAttachmentEntity;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.service.mapper.ArtefactAttachmentMapper.toArtefactAttachment;
import static se.sundsvall.supportmanagement.service.mapper.ArtefactAttachmentMapper.toArtefactAttachments;

class ArtefactAttachmentMapperTest {

	@Test
	void testToArtefactAttachment() {

		// Arrange
		final var created = now();
		final var link = StatementAttachmentEntity.create()
			.withAttachmentEntity(AttachmentEntity.create()
				.withId("attachmentId")
				.withFileName("file.pdf")
				.withMimeType("application/pdf")
				.withFileSize(40960)
				.withPurpose(AttachmentPurposeEntity.create().withId("purposeId").withName("RESPONSE").withDisplayName("Inkommen handling")))
			.withSortOrder(1)
			.withCreated(created)
			.withCreatedBy("jo12doe");

		// Act
		final var result = toArtefactAttachment(link);

		// Assert
		assertThat(result.getAttachmentId()).isEqualTo("attachmentId");
		assertThat(result.getFileName()).isEqualTo("file.pdf");
		assertThat(result.getMimeType()).isEqualTo("application/pdf");
		assertThat(result.getFileSize()).isEqualTo(40960);
		assertThat(result.getPurpose()).isEqualTo(ErrandAttachmentPurpose.create().withId("purposeId").withName("RESPONSE").withDisplayName("Inkommen handling"));
		assertThat(result.getSortOrder()).isEqualTo(1);
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getCreatedBy()).isEqualTo("jo12doe");
	}

	@Test
	void testToArtefactAttachmentWithNull() {
		assertThat(toArtefactAttachment(null)).isNull();
	}

	@Test
	void testToArtefactAttachmentWithoutAttachment() {

		// Act - a link whose attachment has not been loaded says nothing about the attachment
		final var result = toArtefactAttachment(DecisionAttachmentEntity.create());

		// Assert - the purpose is among them now, since it is read from the attachment rather than from the link
		assertThat(result.getAttachmentId()).isNull();
		assertThat(result.getFileName()).isNull();
		assertThat(result.getMimeType()).isNull();
		assertThat(result.getFileSize()).isNull();
		assertThat(result.getPurpose()).isNull();
	}

	@Test
	void testToArtefactAttachmentsIsWrittenOnceForEveryArtefact() {

		// Act - the same mapping serves every link type, which is the point of the interface
		final var result = toArtefactAttachments(List.of(
			MeasureAttachmentEntity.create().withAttachmentEntity(AttachmentEntity.create().withId("one")),
			MeasureAttachmentEntity.create().withAttachmentEntity(AttachmentEntity.create().withId("two"))));

		// Assert
		assertThat(result).extracting("attachmentId").containsExactly("one", "two");
	}

	@Test
	void testToArtefactAttachmentsWithNull() {
		assertThat(toArtefactAttachments(null)).isEmpty();
	}
}
