package se.sundsvall.supportmanagement.service.mapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Blob;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Hibernate;
import org.hibernate.LobHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachmentPurpose;
import se.sundsvall.supportmanagement.api.model.attachment.UpdateErrandAttachmentRequest;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentPurposeEntity;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildAttachmentEntity;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrandEntity;

@ExtendWith(MockitoExtension.class)
class ErrandAttachmentMapperTest {

	private static final String ATTACHMENT_ID = "attachmentId";

	private static final String FILE_NAME = "fileName";

	private static final String MIME_TYPE = "mimeType";

	private static final OffsetDateTime CREATED = now().minusWeeks(1);
	private static final OffsetDateTime MODIFIED = now().minusDays(1);
	private static final OffsetDateTime RECEIVED = now().minusWeeks(2);

	@Mock
	private MultipartFile multipartFileMock;

	@Mock
	private LobHelper lobHelperMock;

	@Mock
	private Blob blobMock;

	@Test
	void toAttachmentEntity() throws IOException {

		final var errandEntity = buildErrandEntity().withAttachments(new ArrayList<>());

		when(multipartFileMock.getOriginalFilename()).thenReturn(FILE_NAME);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));

		try (MockedStatic<Hibernate> hibernateMock = Mockito.mockStatic(Hibernate.class)) {
			hibernateMock.when(Hibernate::getLobHelper).thenReturn(lobHelperMock);
			when(lobHelperMock.createBlob(any(), anyLong())).thenReturn(blobMock);

			final var result = ErrandAttachmentMapper.toAttachmentEntity(errandEntity, multipartFileMock, null);

			assertThat(result).isNotNull().hasNoNullFieldsOrPropertiesExcept("id", "created", "modified", "received", "sequenceNumber", "hash", "purpose", "attachmentDataId");
			assertThat(result.getMunicipalityId()).isEqualTo(errandEntity.getMunicipalityId());
			assertThat(result.getNamespace()).isEqualTo(errandEntity.getNamespace());
			assertThat(result.getFileName()).isEqualTo(FILE_NAME);
			assertThat(result.getAttachmentData().getFile()).isSameAs(blobMock);
			assertThat(result.getMimeType()).isEqualTo("text/plain");
			assertThat(result.getChannel()).isEqualTo("WEB_UI");
			assertThat(result.getHash()).isNull();
			assertThat(result.getErrandEntity()).isSameAs(errandEntity);
		}
	}

	@Test
	void toAttachmentEntityWithClientProvidedChannel() throws IOException {
		final var errandEntity = buildErrandEntity().withAttachments(new ArrayList<>());

		when(multipartFileMock.getOriginalFilename()).thenReturn(FILE_NAME);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));

		try (MockedStatic<Hibernate> hibernateMock = Mockito.mockStatic(Hibernate.class)) {
			hibernateMock.when(Hibernate::getLobHelper).thenReturn(lobHelperMock);
			when(lobHelperMock.createBlob(any(), anyLong())).thenReturn(blobMock);

			final var result = ErrandAttachmentMapper.toAttachmentEntity(errandEntity, multipartFileMock, ErrandAttachment.create().withChannel("MY_PAGES").withReceived(RECEIVED));

			assertThat(result.getChannel()).isEqualTo("MY_PAGES");
			assertThat(result.getReceived()).isEqualTo(RECEIVED);
		}
	}

	@Test
	void toAttachmentEntityFromResponseEntity() {
		final var errandEntity = buildErrandEntity().withAttachments(new ArrayList<>());
		final var fileSize = 1024;

		final var file = ResponseEntity.ok()
			.header("Content-Type", "application/octet-stream")
			.body(new InputStreamResource(new ByteArrayInputStream(new byte[0])));

		try (MockedStatic<Hibernate> hibernateMock = Mockito.mockStatic(Hibernate.class)) {
			hibernateMock.when(Hibernate::getLobHelper).thenReturn(lobHelperMock);
			when(lobHelperMock.createBlob(any(), anyLong())).thenReturn(blobMock);

			final var result = ErrandAttachmentMapper.toAttachmentEntity(errandEntity, file, FILE_NAME, fileSize, "MY_PAGES");

			assertThat(result).isNotNull().hasNoNullFieldsOrPropertiesExcept("id", "created", "modified", "received", "sequenceNumber", "hash", "purpose", "attachmentDataId");
			assertThat(result.getMunicipalityId()).isEqualTo(errandEntity.getMunicipalityId());
			assertThat(result.getNamespace()).isEqualTo(errandEntity.getNamespace());
			assertThat(result.getFileName()).isEqualTo(FILE_NAME);
			assertThat(result.getFileSize()).isEqualTo(fileSize);
			assertThat(result.getAttachmentData().getFile()).isSameAs(blobMock);
			assertThat(result.getMimeType()).isEqualTo("application/octet-stream");
			assertThat(result.getChannel()).isEqualTo("MY_PAGES");
			assertThat(result.getErrandEntity()).isSameAs(errandEntity);
		}
	}

	@Test
	void toAttachmentEntityAllNulls() {

		final var multipartFile = (MultipartFile) null;

		assertThat(ErrandAttachmentMapper.toAttachmentEntity(null, multipartFile, null)).isNull();
	}

	@Test
	void toErrandAttachments() {

		final var result = ErrandAttachmentMapper.toErrandAttachments(List.of(buildAttachmentEntity(buildErrandEntity()).withCreated(CREATED).withModified(MODIFIED).withReceived(RECEIVED).withSequenceNumber(3).withChannel("EMAIL")));

		assertThat(result).isNotNull();
		assertThat(result.getFirst().getId()).isEqualTo(ATTACHMENT_ID);
		assertThat(result.getFirst().getFileName()).isEqualTo(FILE_NAME);
		assertThat(result.getFirst().getMimeType()).isEqualTo(MIME_TYPE);
		assertThat(result.getFirst().getChannel()).isEqualTo("EMAIL");
		assertThat(result.getFirst().getCreated()).isCloseTo(CREATED, within(5, SECONDS));
		assertThat(result.getFirst().getModified()).isEqualTo(MODIFIED);
		assertThat(result.getFirst().getReceived()).isEqualTo(RECEIVED);
		assertThat(result.getFirst().getSequenceNumber()).isEqualTo(3);
		assertThat(result.getFirst().getHash()).isNull();
	}

	@Test
	void updateAttachmentEntity() {
		final var purpose = AttachmentPurposeEntity.create().withId("purpose-2");
		final var entity = buildAttachmentEntity(buildErrandEntity()).withPurpose(AttachmentPurposeEntity.create().withId("purpose-1")).withReceived(CREATED);

		final var result = ErrandAttachmentMapper.updateAttachmentEntity(entity, UpdateErrandAttachmentRequest.create().withReceived(RECEIVED), purpose);

		assertThat(result).isSameAs(entity);
		assertThat(result.getPurpose()).isSameAs(purpose);
		assertThat(result.getReceived()).isEqualTo(RECEIVED);
	}

	@Test
	void updateAttachmentEntityLeavesWhatTheRequestLeavesOut() {
		final var purpose = AttachmentPurposeEntity.create().withId("purpose-1");
		final var entity = buildAttachmentEntity(buildErrandEntity()).withPurpose(purpose).withReceived(CREATED);

		final var result = ErrandAttachmentMapper.updateAttachmentEntity(entity, UpdateErrandAttachmentRequest.create(), null);

		assertThat(result.getPurpose()).isSameAs(purpose);
		assertThat(result.getReceived()).isEqualTo(CREATED);
	}

	@Test
	void toErrandAttachmentsFromNullInput() {

		assertThat(ErrandAttachmentMapper.toErrandAttachments(null))
			.isNotNull().isEmpty();
	}

	@Test
	void toErrandAttachmentMapsHash() {
		final var hash = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";
		final var entity = buildAttachmentEntity(buildErrandEntity()).withHash(hash);

		final var result = ErrandAttachmentMapper.toErrandAttachment(entity);

		assertThat(result.getHash()).isEqualTo(hash);
	}

	@Test
	void toErrandAttachmentMapsFileSize() {
		final var entity = buildAttachmentEntity(buildErrandEntity()).withFileSize(40960);

		final var result = ErrandAttachmentMapper.toErrandAttachment(entity);

		assertThat(result.getFileSize()).isEqualTo(40960);
	}

	@Test
	void toErrandAttachmentFromNull() {
		assertThat(ErrandAttachmentMapper.toErrandAttachment(null)).isNull();
	}

	@Test
	void toErrandAttachmentMapsPurpose() {
		final var entity = buildAttachmentEntity(buildErrandEntity())
			.withPurpose(AttachmentPurposeEntity.create().withId("purposeId").withName("RESPONSE").withDisplayName("Inkommen handling").withNamespace("namespace"));

		final var result = ErrandAttachmentMapper.toErrandAttachment(entity);

		assertThat(result.getPurpose()).isEqualTo(ErrandAttachmentPurpose.create().withId("purposeId").withName("RESPONSE").withDisplayName("Inkommen handling"));
	}

	@Test
	void toErrandAttachmentWithoutPurpose() {
		final var result = ErrandAttachmentMapper.toErrandAttachment(buildAttachmentEntity(buildErrandEntity()));

		assertThat(result.getId()).isEqualTo(ATTACHMENT_ID);
		assertThat(result.getPurpose()).isNull();
	}

	@Test
	void toErrandAttachmentPurposeFromNull() {
		assertThat(ErrandAttachmentMapper.toErrandAttachmentPurpose(null)).isNull();
	}

	@Test
	void toAttachmentEntityFromResponseEntityWithNullBody() {
		final var errandEntity = buildErrandEntity();
		final ResponseEntity<InputStreamResource> response = ResponseEntity.ok().body(null);

		assertThat(ErrandAttachmentMapper.toAttachmentEntity(errandEntity, response, "fileName", 0, null)).isNull();
	}

}
