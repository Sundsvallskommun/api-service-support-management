package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Optional.of;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildAttachmentEntity;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrandAttachment;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrandEntity;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.ThrowableProblem;

import generated.se.sundsvall.eventlog.Event;
import se.sundsvall.supportmanagement.api.filter.ExecutingUserSupplier;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

@ExtendWith(MockitoExtension.class)
class ErrandAttachmentServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String ERRAND_ID = "errandId";
	private static final String ATTACHMENT_ID = "attachmentId";
	private static final String FILE = "file";
	private static final String FILE_NAME = "fileName";
	private static final String MIME_TYPE = "mimeType";
	private static final int CURRENT_REVISION_VERSION = 2;
	private static final int PREVIOUS_REVISION_VERSION = 1;
	private static final String EVENT_LOG_ADD_ATTACHMENT = "En bilaga har lagts till i ärendet.";
	private static final String EVENT_LOG_REMOVE_ATTACHMENT = "En bilaga har tagits bort från ärendet.";
	private static final String EXECUTING_USER = "executingUser";

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
	private ExecutingUserSupplier executingUserSupplierMock;

	@InjectMocks
	private ErrandAttachmentService service;

	@Captor
	private ArgumentCaptor<Event> eventCaptor;

	@Test
	void createErrandAttachment() {
		// Mock
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(of(errandMock));
		when(errandMock.getMunicipalityId()).thenReturn(MUNICIPALITY_ID);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);
		when(errandMock.getAttachments()).thenReturn(new ArrayList<>());
		when(errandsRepositoryMock.save(any(ErrandEntity.class))).thenReturn(errandMock);
		when(revisionServiceMock.createErrandRevision(errandMock)).thenReturn(currentRevisionMock);
		when(currentRevisionMock.getVersion()).thenReturn(CURRENT_REVISION_VERSION);
		when(revisionServiceMock.getErrandRevisionByVersion(ERRAND_ID, PREVIOUS_REVISION_VERSION)).thenReturn(previousRevisionMock);
		when(executingUserSupplierMock.getAdUser()).thenReturn(EXECUTING_USER);

		// Call
		final var result = service.createErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, buildErrandAttachment());

		// Assertions and verifications
		assertThat(result).isNotNull().isEqualTo(ATTACHMENT_ID);

		verify(errandsRepositoryMock).findById(ERRAND_ID);
		verify(errandsRepositoryMock).save(any(ErrandEntity.class));
		verify(revisionServiceMock).createErrandRevision(errandMock);
		verify(revisionServiceMock).getErrandRevisionByVersion(ERRAND_ID, PREVIOUS_REVISION_VERSION);
		verify(eventServiceMock).createEvent(UPDATE, EVENT_LOG_ADD_ATTACHMENT, errandMock, currentRevisionMock, previousRevisionMock, EXECUTING_USER);
	}

	@Test
	void createErrandAttachmentForEntityNotBelongingToMunicipality() {

		// Setup
		final var errandEntity = buildErrandEntity();
		final var errandAttachment = buildErrandAttachment();

		// Mock
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(of(errandEntity));

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.createErrandAttachment(NAMESPACE, "other_" + MUNICIPALITY_ID, ERRAND_ID, errandAttachment));

		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found in namespace 'namespace' for municipality with id 'other_municipalityId'");

		verify(errandsRepositoryMock).findById(ERRAND_ID);
		verify(errandsRepositoryMock, never()).save(any());
		verifyNoInteractions(revisionServiceMock, eventServiceMock);
	}

	@Test
	void readErrandAttachment() {

		// Mock
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(of(errandMock));
		when(errandMock.getMunicipalityId()).thenReturn(MUNICIPALITY_ID);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);
		when(errandMock.getAttachments()).thenReturn(List.of(buildAttachmentEntity(buildErrandEntity())));

		// Call
		final var result = service.readErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID);

		// Assertions and verifications
		assertThat(result).isNotNull();
		assertThat(result.getErrandAttachmentHeader().getId()).isEqualTo(ATTACHMENT_ID);
		assertThat(result.getErrandAttachmentHeader().getFileName()).isEqualTo(FILE_NAME);
		assertThat(result.getErrandAttachmentHeader().getMimeType()).isEqualTo(MIME_TYPE);
		assertThat(result.getBase64EncodedString()).isEqualTo(encodeBase64String(FILE.getBytes()));

		verify(errandsRepositoryMock).findById(ERRAND_ID);
		verifyNoInteractions(revisionServiceMock, eventServiceMock);
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
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(of(errandMock));
		when(errandMock.getMunicipalityId()).thenReturn(MUNICIPALITY_ID);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.readErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An attachment with id 'attachmentId' could not be found on errand with id 'errandId'");

		verify(errandsRepositoryMock).findById(ERRAND_ID);
		verifyNoInteractions(revisionServiceMock, eventServiceMock);
	}

	@Test
	void readErrandAttachmentsErrandNotFound() {

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.readErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found in namespace 'namespace' for municipality with id 'municipalityId'");

		verify(errandsRepositoryMock).findById(ERRAND_ID);
		verifyNoInteractions(revisionServiceMock, eventServiceMock);
	}

	@Test
	void deleteErrandAttachment() {

		// Mock
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(of(errandMock));
		when(errandMock.getMunicipalityId()).thenReturn(MUNICIPALITY_ID);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);
		when(errandMock.getAttachments()).thenReturn(new ArrayList<>(List.of(attachmentMock)));
		when(attachmentMock.getId()).thenReturn(ATTACHMENT_ID);
		when(errandsRepositoryMock.save(any(ErrandEntity.class))).thenReturn(errandMock);
		when(revisionServiceMock.createErrandRevision(errandMock)).thenReturn(currentRevisionMock);
		when(currentRevisionMock.getVersion()).thenReturn(CURRENT_REVISION_VERSION);
		when(revisionServiceMock.getErrandRevisionByVersion(ERRAND_ID, PREVIOUS_REVISION_VERSION)).thenReturn(previousRevisionMock);
		when(executingUserSupplierMock.getAdUser()).thenReturn(EXECUTING_USER);

		// Call
		service.deleteErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID);

		// Assertions and verifications
		assertThat(errandMock.getAttachments()).isEmpty();

		verify(errandsRepositoryMock).findById(ERRAND_ID);
		verify(errandsRepositoryMock).save(any(ErrandEntity.class));
		verify(revisionServiceMock).createErrandRevision(errandMock);

		verify(revisionServiceMock).getErrandRevisionByVersion(ERRAND_ID, PREVIOUS_REVISION_VERSION);
		verify(eventServiceMock).createEvent(UPDATE, EVENT_LOG_REMOVE_ATTACHMENT, errandMock, currentRevisionMock, previousRevisionMock, EXECUTING_USER);
	}

	@Test
	void deleteErrandAttachmentErrandNotFound() {

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.deleteErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found in namespace 'namespace' for municipality with id 'municipalityId'");

		verify(errandsRepositoryMock).findById(ERRAND_ID);
		verify(errandsRepositoryMock, never()).save(any());
		verifyNoInteractions(revisionServiceMock, eventServiceMock);
	}

	@Test
	void deleteErrandAttachmentAttachmentIdNotFound() {

		// Mock
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(of(errandMock));
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

		verify(errandsRepositoryMock).findById(ERRAND_ID);
		verify(errandsRepositoryMock, never()).save(any());
		verifyNoInteractions(revisionServiceMock, eventServiceMock);
	}
}
