package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.eventlog.Event;
import jakarta.persistence.EntityManager;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildAttachmentEntity;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrandEntity;

@ExtendWith(MockitoExtension.class)
class ErrandAttachmentServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String ERRAND_ID = "errandId";
	private static final String ATTACHMENT_ID = "attachmentId";
	private static final String FILE = "file";
	private static final String FILE_NAME = "fileName";
	private static final String MIME_TYPE = "mimeType";
	private static final String EVENT_LOG_ADD_ATTACHMENT = "En bilaga har lagts till i ärendet.";
	private static final String EVENT_LOG_REMOVE_ATTACHMENT = "En bilaga har tagits bort från ärendet.";

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private ErrandEntity errandMock;

	@Mock
	private AttachmentEntity attachmentMock;

	@Mock
	private RevisionService revisionServiceMock;

	@Mock
	private Revision currentRevisionMock;

	@Mock
	private Revision previousRevisionMock;

	@Mock
	private EventService eventServiceMock;

	@Mock
	private MultipartFile multipartFileMock;

	@Mock
	private EntityManager entityManagerMock;

	@Mock
	private AttachmentRepository attachmentRepositoryMock;

	@Mock
	private AttachmentDataEntity attachmentDataEntityMock;

	@Mock
	private Blob blobMock;

	@Mock
	private HttpServletResponse httpServletResponseMock;

	@InjectMocks
	private ErrandAttachmentService service;

	@Captor
	private ArgumentCaptor<Event> eventCaptor;

	@Test
	void createErrandAttachment() {
		// Mock
		when(errandsRepositoryMock.findWithLockingById(ERRAND_ID)).thenReturn(of(errandMock));
		when(errandMock.getMunicipalityId()).thenReturn(MUNICIPALITY_ID);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);
		when(revisionServiceMock.createErrandRevision(errandMock)).thenReturn(new RevisionResult(previousRevisionMock, currentRevisionMock));
		when(attachmentRepositoryMock.save(any())).thenReturn(attachmentMock);
		when(attachmentMock.getId()).thenReturn(ATTACHMENT_ID);

		// Call
		try (MockedStatic<ErrandAttachmentMapper> mapper = Mockito.mockStatic(ErrandAttachmentMapper.class)) {
			mapper.when(() -> ErrandAttachmentMapper.toAttachmentEntity(any(), any(), any())).thenReturn(attachmentMock);

			final var result = service.createErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, multipartFileMock);

			// Assertions and verifications
			assertThat(result).isNotNull().isEqualTo(ATTACHMENT_ID);

			mapper.verify(() -> ErrandAttachmentMapper.toAttachmentEntity(same(errandMock), same(multipartFileMock), same(entityManagerMock)));
			verify(errandsRepositoryMock).findWithLockingById(ERRAND_ID);
			verify(attachmentRepositoryMock).save(attachmentMock);
			verify(revisionServiceMock).createErrandRevision(errandMock);
			verify(eventServiceMock).createErrandEvent(UPDATE, EVENT_LOG_ADD_ATTACHMENT, errandMock, currentRevisionMock, previousRevisionMock);
		}

	}

	@Test
	void createErrandAttachmentForEntityNotBelongingToMunicipality() {

		// Setup
		final var errandEntity = buildErrandEntity();

		// Mock
		when(errandsRepositoryMock.findWithLockingById(ERRAND_ID)).thenReturn(of(errandEntity));

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.createErrandAttachment(NAMESPACE, "other_" + MUNICIPALITY_ID, ERRAND_ID, multipartFileMock));

		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found in namespace 'namespace' for municipality with id 'other_municipalityId'");

		verify(errandsRepositoryMock).findWithLockingById(ERRAND_ID);
		verify(errandsRepositoryMock, never()).save(any());
		verifyNoInteractions(revisionServiceMock, eventServiceMock);
	}

	@Test
	void readErrandAttachment() throws IOException, SQLException {


		// Mock
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(attachmentRepositoryMock.findById(ATTACHMENT_ID)).thenReturn(of(attachmentMock));
		when(attachmentMock.getAttachmentData()).thenReturn(attachmentDataEntityMock);
		when(attachmentDataEntityMock.getFile()).thenReturn(blobMock);
		when(attachmentMock.getMimeType()).thenReturn(MIME_TYPE);
		when(attachmentMock.getFileName()).thenReturn(FILE_NAME);
		var outputStreamMock = Mockito.mock(ServletOutputStream.class);
		when(httpServletResponseMock.getOutputStream()).thenReturn(outputStreamMock);
		var inputStreamMock = Mockito.mock(InputStream.class);
		when(blobMock.getBinaryStream()).thenReturn(inputStreamMock);
		when(blobMock.length()).thenReturn(123L);


		// Call
		try (MockedStatic<StreamUtils> streamMock = Mockito.mockStatic(StreamUtils.class)){

			service.readErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID, httpServletResponseMock);

			// Assertions and verifications
			verify(httpServletResponseMock).addHeader(CONTENT_TYPE, MIME_TYPE);
			verify(httpServletResponseMock).addHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + FILE_NAME + "\"");
			verify(httpServletResponseMock).setContentLength(123);
			streamMock.verify(() -> StreamUtils.copy(same(inputStreamMock), same(outputStreamMock)));


			verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
			verifyNoInteractions(revisionServiceMock, eventServiceMock);
		}
	}

	@Test
	void readErrandAttachments() {

		// Mock
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(of(errandMock));
		when(errandMock.getMunicipalityId()).thenReturn(MUNICIPALITY_ID);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);
		when(errandMock.getAttachments()).thenReturn(List.of(buildAttachmentEntity(buildErrandEntity())));

		// Call
		final var result = service.readErrandAttachmentHeaders(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		// Assertions and verifications
		assertThat(result).isNotNull().hasSize(1);
		assertThat(result.get(0).getId()).isEqualTo(ATTACHMENT_ID);
		assertThat(result.get(0).getFileName()).isEqualTo(FILE_NAME);
		assertThat(result.get(0).getMimeType()).isEqualTo(MIME_TYPE);

		verify(errandsRepositoryMock).findById(ERRAND_ID);
		verifyNoInteractions(revisionServiceMock);
	}

	@Test
	void readErrandAttachmentNotFoundOnErrand() {

		// Mock
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.readErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID, httpServletResponseMock));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An attachment with id 'attachmentId' could not be found on errand with id 'errandId'");

		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(revisionServiceMock, eventServiceMock);
	}

	@Test
	void readErrandAttachmentsErrandNotFound() {

		// Mock
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(false);

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.readErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID, httpServletResponseMock));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found in namespace 'namespace' for municipality with id 'municipalityId'");

		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(revisionServiceMock, eventServiceMock);
	}

	@Test
	void deleteErrandAttachment() {

		// Mock
		when(errandsRepositoryMock.findWithLockingById(ERRAND_ID)).thenReturn(of(errandMock));
		when(errandMock.getMunicipalityId()).thenReturn(MUNICIPALITY_ID);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);
		when(errandMock.getAttachments()).thenReturn(new ArrayList<>(List.of(attachmentMock)));
		when(attachmentMock.getId()).thenReturn(ATTACHMENT_ID);
		when(errandsRepositoryMock.save(any(ErrandEntity.class))).thenReturn(errandMock);
		when(revisionServiceMock.createErrandRevision(errandMock)).thenReturn(new RevisionResult(previousRevisionMock,currentRevisionMock));

		// Call
		service.deleteErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID);

		// Assertions and verifications
		assertThat(errandMock.getAttachments()).isEmpty();

		verify(errandsRepositoryMock).findWithLockingById(ERRAND_ID);
		verify(errandsRepositoryMock).save(any(ErrandEntity.class));
		verify(revisionServiceMock).createErrandRevision(errandMock);

		verify(eventServiceMock).createErrandEvent(UPDATE, EVENT_LOG_REMOVE_ATTACHMENT, errandMock, currentRevisionMock, previousRevisionMock);
	}

	@Test
	void deleteErrandAttachmentErrandNotFound() {

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.deleteErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found in namespace 'namespace' for municipality with id 'municipalityId'");

		verify(errandsRepositoryMock).findWithLockingById(ERRAND_ID);
		verify(errandsRepositoryMock, never()).save(any());
		verifyNoInteractions(revisionServiceMock, eventServiceMock);
	}

	@Test
	void deleteErrandAttachmentAttachmentIdNotFound() {

		// Mock
		when(errandsRepositoryMock.findWithLockingById(ERRAND_ID)).thenReturn(of(errandMock));
		when(errandMock.getMunicipalityId()).thenReturn(MUNICIPALITY_ID);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);
		when(errandMock.getAttachments()).thenReturn(new ArrayList<>(List.of(attachmentMock)));
		when(attachmentMock.getId()).thenReturn("other-id");

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.deleteErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An attachment with id 'attachmentId' could not be found on errand with id 'errandId'");

		verify(errandsRepositoryMock).findWithLockingById(ERRAND_ID);
		verify(errandsRepositoryMock, never()).save(any());
		verifyNoInteractions(revisionServiceMock, eventServiceMock);
	}
}
