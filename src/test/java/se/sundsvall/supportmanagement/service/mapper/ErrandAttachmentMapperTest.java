package se.sundsvall.supportmanagement.service.mapper;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildAttachmentEntity;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrandEntity;

import jakarta.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Blob;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.LobHelper;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ErrandAttachmentMapperTest {

	private static final String ATTACHMENT_ID = "attachmentId";

	private static final String FILE_NAME = "fileName";

	private static final String MIME_TYPE = "mimeType";

	private static final OffsetDateTime CREATED = now().minusWeeks(1);

	@Mock
	private EntityManager entityManagerMock;

	@Mock
	private MultipartFile multipartFileMock;

	@Mock
	private Session sessionMock;

	@Mock
	private LobHelper lobHelperMock;

	@Mock
	private Blob blobMock;

	@Test
	void toAttachmentEntity() throws IOException {

		final var errandEntity = buildErrandEntity().withAttachments(new ArrayList<>());

		when(entityManagerMock.unwrap(Session.class)).thenReturn(sessionMock);
		when(sessionMock.getLobHelper()).thenReturn(lobHelperMock);
		when(lobHelperMock.createBlob(any(), anyLong())).thenReturn(blobMock);
		when(multipartFileMock.getOriginalFilename()).thenReturn(FILE_NAME);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));

		final var result = ErrandAttachmentMapper.toAttachmentEntity(errandEntity, multipartFileMock, entityManagerMock);

		assertThat(result).isNotNull().hasNoNullFieldsOrPropertiesExcept("id", "created", "modified");
		assertThat(result.getMunicipalityId()).isEqualTo(errandEntity.getMunicipalityId());
		assertThat(result.getNamespace()).isEqualTo(errandEntity.getNamespace());
		assertThat(result.getFileName()).isEqualTo(FILE_NAME);
		assertThat(result.getAttachmentData().getFile()).isSameAs(blobMock);
		assertThat(result.getMimeType()).isEqualTo("text/plain");
		assertThat(result.getErrandEntity()).isSameAs(errandEntity);
	}

	@Test
	void toAttachmentEntityAllNulls() {

		assertThat(ErrandAttachmentMapper.toAttachmentEntity(null, null, null)).isNull();
	}

	@Test
	void toErrandAttachments() {

		final var result = ErrandAttachmentMapper.toErrandAttachments(List.of(buildAttachmentEntity(buildErrandEntity()).withCreated(CREATED)));

		assertThat(result).isNotNull();
		assertThat(result.getFirst().getId()).isEqualTo(ATTACHMENT_ID);
		assertThat(result.getFirst().getFileName()).isEqualTo(FILE_NAME);
		assertThat(result.getFirst().getMimeType()).isEqualTo(MIME_TYPE);
		assertThat(result.getFirst().getCreated()).isCloseTo(CREATED, within(5, SECONDS));
	}

	@Test
	void toErrandAttachmentsFromNullInput() {

		assertThat(ErrandAttachmentMapper.toErrandAttachments(null))
			.isNotNull().isEmpty();
	}

}
