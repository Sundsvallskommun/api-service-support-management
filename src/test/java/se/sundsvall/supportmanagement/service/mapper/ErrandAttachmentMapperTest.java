package se.sundsvall.supportmanagement.service.mapper;

import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildAttachmentEntity;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrandAttachment;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrandEntity;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ErrandAttachmentMapperTest {

	private static final String ATTACHMENT_ID = "attachmentId";
	private static final String FILE_NAME = "fileName";
	private static final String FILE = "file";
	private static final String MIME_TYPE = "mimeType";

	@Test
	void toAttachmentEntity() {

		final var errandEntity = buildErrandEntity().withAttachments(new ArrayList<>());

		final var result = ErrandAttachmentMapper.toAttachmentEntity(errandEntity, buildErrandAttachment());

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(ATTACHMENT_ID);
		assertThat(result.getFileName()).isEqualTo(FILE_NAME);
		assertThat(result.getFile()).isEqualTo(FILE.getBytes());
		assertThat(result.getMimeType()).isEqualTo("text/plain");
		assertThat(result.getErrandEntity()).isSameAs(errandEntity);
		assertThat(errandEntity.getAttachments()).containsExactly(result);
	}

	@Test
	void toAttachmentEntityAllNulls() {

		assertThat(ErrandAttachmentMapper.toAttachmentEntity(null, null)).isNull();
	}

	@Test
	void toErrandAttachment() {

		final var result = ErrandAttachmentMapper.toErrandAttachment(buildAttachmentEntity(buildErrandEntity()));

		assertThat(result).isNotNull();
		assertThat(result.getErrandAttachmentHeader().getId()).isEqualTo(ATTACHMENT_ID);
		assertThat(result.getErrandAttachmentHeader().getFileName()).isEqualTo(FILE_NAME);
		assertThat(result.getErrandAttachmentHeader().getMimeType()).isEqualTo(MIME_TYPE);
		assertThat(result.getBase64EncodedString()).isEqualTo(encodeBase64String(FILE.getBytes()));
	}

	@Test
	void toErrandAttachmentFromNullInput() {

		assertThat(ErrandAttachmentMapper.toErrandAttachment(null))
			.isNull();
	}

	@Test
	void toErrandAttachmentHeaders() {

		final var result = ErrandAttachmentMapper.toErrandAttachmentHeaders(List.of(buildAttachmentEntity(buildErrandEntity())));

		assertThat(result).isNotNull();
		assertThat(result.get(0).getId()).isEqualTo(ATTACHMENT_ID);
		assertThat(result.get(0).getFileName()).isEqualTo(FILE_NAME);
		assertThat(result.get(0).getMimeType()).isEqualTo(MIME_TYPE);
	}

	@Test
	void toErrandAttachmentsFromNullInput() {

		assertThat(ErrandAttachmentMapper.toErrandAttachmentHeaders(null))
			.isNotNull().isEmpty();
	}
}
