package se.sundsvall.supportmanagement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import java.util.List;

import static java.util.Optional.of;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildAttachmentEntity;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrandAttachment;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrandEntity;

@ExtendWith(MockitoExtension.class)
class ErrandAttachmentServiceTest {

	private static final String ERRAND_ID = "errandId";
	private static final String ATTACHMENT_ID = "attachmentId";
	private static final String FILE = "file";
	private static final String FILE_NAME = "fileName";
	private static final String MIME_TYPE = "mimeType";

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private AttachmentRepository attachmentRepositoryMock;

	@InjectMocks
	private ErrandAttachmentService service;

	@Test
	void createErrandAttachment() {

		// Setup
		final var errandEntity = buildErrandEntity();

		// Mock
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(of(errandEntity));
		when(attachmentRepositoryMock.save(any(AttachmentEntity.class))).thenReturn(buildAttachmentEntity(buildErrandEntity()));

		// Call
		final var result = service.createErrandAttachment(ERRAND_ID, buildErrandAttachment());

		// Assertions and verifications
		assertThat(result).isNotNull().isEqualTo(ATTACHMENT_ID);

		verify(errandsRepositoryMock).findById(ERRAND_ID);
		verify(attachmentRepositoryMock).save(any(AttachmentEntity.class));
	}

	@Test
	void readErrandAttachment() {

		// Mock
		when(errandsRepositoryMock.existsById(ERRAND_ID)).thenReturn(true);
		when(attachmentRepositoryMock.findById(ATTACHMENT_ID)).thenReturn(of(buildAttachmentEntity(buildErrandEntity())));

		// Call
		final var result = service.readErrandAttachment(ERRAND_ID, ATTACHMENT_ID);

		// Assertions and verifications
		assertThat(result).isNotNull();
		assertThat(result.getErrandAttachmentHeader().getId()).isEqualTo(ATTACHMENT_ID);
		assertThat(result.getErrandAttachmentHeader().getFileName()).isEqualTo(FILE_NAME);
		assertThat(result.getErrandAttachmentHeader().getMimeType()).isEqualTo(MIME_TYPE);
		assertThat(result.getBase64EncodedString()).isEqualTo(encodeBase64String(FILE.getBytes()));

		verify(errandsRepositoryMock).existsById(ERRAND_ID);
		verify(attachmentRepositoryMock).findById(ATTACHMENT_ID);
	}

	@Test
	void readErrandAttachmentDontBelongToErrand() {

		// Mock
		when(errandsRepositoryMock.existsById(ERRAND_ID)).thenReturn(true);
		when(attachmentRepositoryMock.findById(ATTACHMENT_ID)).thenReturn(of(buildAttachmentEntity(ErrandEntity.create().withId("otherErrandId"))));

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.readErrandAttachment(ERRAND_ID, ATTACHMENT_ID));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(exception.getTitle()).isEqualTo(BAD_REQUEST.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Bad Request: Attachment with id 'attachmentId' was not found for errand with id 'errandId'");

		verify(errandsRepositoryMock).existsById(ERRAND_ID);
		verify(attachmentRepositoryMock).findById(ATTACHMENT_ID);
	}

	@Test
	void readErrandAttachmentErrandNotFound() {

		// Mock
		when(errandsRepositoryMock.existsById(ERRAND_ID)).thenReturn(false);

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.readErrandAttachment(ERRAND_ID, ATTACHMENT_ID));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found");

		verify(errandsRepositoryMock).existsById(ERRAND_ID);
		verifyNoInteractions(attachmentRepositoryMock);
	}

	@Test
	void readErrandAttachments() {

		// Mock
		when(errandsRepositoryMock.existsById(ERRAND_ID)).thenReturn(true);
		when(attachmentRepositoryMock.findByErrandEntityId(ERRAND_ID)).thenReturn(List.of(buildAttachmentEntity(buildErrandEntity())));

		// Call
		final var result = service.readErrandAttachmentHeaders(ERRAND_ID);

		// Assertions and verifications
		assertThat(result).isNotNull().hasSize(1);
		assertThat(result.get(0).getId()).isEqualTo(ATTACHMENT_ID);
		assertThat(result.get(0).getFileName()).isEqualTo(FILE_NAME);
		assertThat(result.get(0).getMimeType()).isEqualTo(MIME_TYPE);

		verify(errandsRepositoryMock).existsById(ERRAND_ID);
		verify(attachmentRepositoryMock).findByErrandEntityId(ERRAND_ID);
	}

	@Test
	void readErrandAttachmentsErrandNotFound() {

		// Mock
		when(errandsRepositoryMock.existsById(ERRAND_ID)).thenReturn(false);

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.readErrandAttachment(ERRAND_ID, ATTACHMENT_ID));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found");

		verify(errandsRepositoryMock).existsById(ERRAND_ID);
		verifyNoInteractions(attachmentRepositoryMock);
	}

	@Test
	void deleteErrandAttachment() {

		// Mock
		when(errandsRepositoryMock.existsById(ERRAND_ID)).thenReturn(true);

		// Call
		service.deleteErrandAttachment(ERRAND_ID, ATTACHMENT_ID);

		// Assertions and verifications
		verify(errandsRepositoryMock).existsById(ERRAND_ID);
		verify(attachmentRepositoryMock).deleteById(ATTACHMENT_ID);
	}

	@Test
	void deleteErrandAttachmentErrandNotFound() {

		// Mock
		when(errandsRepositoryMock.existsById(ERRAND_ID)).thenReturn(false);

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.deleteErrandAttachment(ERRAND_ID, ATTACHMENT_ID));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found");

		verify(errandsRepositoryMock).existsById(ERRAND_ID);
		verifyNoInteractions(attachmentRepositoryMock);
	}
}
