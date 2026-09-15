package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachmentPurpose;
import se.sundsvall.supportmanagement.api.model.attachment.UpdateErrandAttachmentRequest;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.AttachmentPurposeRepository;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentPurposeEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper;
import se.sundsvall.supportmanagement.service.model.RevisionResult;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildAttachmentEntity;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrandEntity;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ATTACHMENT;

@ExtendWith(MockitoExtension.class)
class ErrandAttachmentServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String ERRAND_ID = "errandId";
	private static final String ATTACHMENT_ID = "attachmentId";
	private static final String PURPOSE_ID = "f6000000-0000-0000-0000-000000000002";
	private static final String FILE_NAME = "fileName";
	private static final String MIME_TYPE = "mimeType";
	private static final String EVENT_LOG_ADD_ATTACHMENT = "En bilaga har lagts till i ärendet.";
	private static final String EVENT_LOG_REMOVE_ATTACHMENT = "En bilaga har tagits bort från ärendet.";
	private static final String EVENT_LOG_UPDATE_ATTACHMENT = "En bilaga i ärendet har uppdaterats.";

	@Mock
	private Semaphore semaphoreMock;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private AccessControlService accessControlServiceMock;

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
	private AttachmentRepository attachmentRepositoryMock;

	@Mock
	private AttachmentDataEntity attachmentDataEntityMock;

	@Mock
	private Blob blobMock;

	@Mock
	private EntityManager entityManagerMock;

	@Mock
	private HttpServletResponse httpServletResponseMock;

	@Mock
	private ServletOutputStream servletOutputStreamMock;

	@Mock
	private AttachmentPurposeRepository attachmentPurposeRepositoryMock;

	@InjectMocks
	private ErrandAttachmentService service;

	@Test
	void createErrandAttachment() throws SQLException {
		// Mock
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandMock);
		when(revisionServiceMock.createErrandRevision(errandMock)).thenReturn(new RevisionResult(previousRevisionMock, currentRevisionMock));
		when(attachmentRepositoryMock.saveAndFlush(any())).thenReturn(attachmentMock);
		when(attachmentMock.getId()).thenReturn(ATTACHMENT_ID);
		when(attachmentMock.getAttachmentData()).thenReturn(attachmentDataEntityMock);
		when(attachmentDataEntityMock.getFile()).thenReturn(blobMock);
		when(blobMock.getBinaryStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));

		// Call
		try (final MockedStatic<ErrandAttachmentMapper> mapper = Mockito.mockStatic(ErrandAttachmentMapper.class)) {
			mapper.when(() -> ErrandAttachmentMapper.toAttachmentEntity(any(), any(MultipartFile.class), nullable(String.class))).thenReturn(attachmentMock);

			final var result = service.createErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, multipartFileMock, null);

			// Assertions and verifications
			assertThat(result).isNotNull().isEqualTo(ATTACHMENT_ID);

			mapper.verify(() -> ErrandAttachmentMapper.toAttachmentEntity(same(errandMock), same(multipartFileMock), nullable(String.class)));
			verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.ATTACHMENT, RW);
			verify(attachmentRepositoryMock).saveAndFlush(attachmentMock);
			verify(entityManagerMock).refresh(attachmentMock);
			verify(attachmentMock).setHash("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
			verify(revisionServiceMock).createErrandRevision(errandMock);
			verify(eventServiceMock).createErrandEvent(UPDATE, EVENT_LOG_ADD_ATTACHMENT, errandMock, currentRevisionMock, previousRevisionMock, ATTACHMENT);
		}

	}

	@Test
	void readErrandAttachment() throws IOException, SQLException, InterruptedException {

		// Mock
		when(attachmentRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID)).thenReturn(of(attachmentMock));
		when(attachmentMock.getAttachmentData()).thenReturn(attachmentDataEntityMock);
		when(attachmentDataEntityMock.getFile()).thenReturn(blobMock);
		when(attachmentMock.getMimeType()).thenReturn(MIME_TYPE);
		when(attachmentMock.getFileName()).thenReturn(FILE_NAME);
		final var outputStreamMock = Mockito.mock(ServletOutputStream.class);
		when(httpServletResponseMock.getOutputStream()).thenReturn(outputStreamMock);
		final var inputStreamMock = Mockito.mock(InputStream.class);
		when(blobMock.getBinaryStream()).thenReturn(inputStreamMock);
		when(attachmentMock.getFileSize()).thenReturn(123);
		when(semaphoreMock.tryAcquire(123, 5, SECONDS)).thenReturn(true);

		// Call
		try (final MockedStatic<StreamUtils> streamMock = Mockito.mockStatic(StreamUtils.class)) {

			service.readErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID, httpServletResponseMock);

			// Assertions and verifications
			verify(httpServletResponseMock).addHeader(CONTENT_TYPE, MIME_TYPE);
			verify(httpServletResponseMock).addHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + FILE_NAME + "\"");
			verify(httpServletResponseMock).setContentLength(123);
			streamMock.verify(() -> StreamUtils.copy(same(inputStreamMock), same(outputStreamMock)));

			verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.ATTACHMENT, LR);
			verifyNoInteractions(revisionServiceMock, eventServiceMock);
		}
	}

	@Test
	void readErrandAttachments() {

		// Mock
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandMock);
		when(errandMock.getAttachments()).thenReturn(List.of(buildAttachmentEntity(buildErrandEntity())));

		// Call
		final var result = service.readErrandAttachments(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		// Assertions and verifications
		assertThat(result).isNotNull().hasSize(1);
		assertThat(result.getFirst().getId()).isEqualTo(ATTACHMENT_ID);
		assertThat(result.getFirst().getFileName()).isEqualTo(FILE_NAME);
		assertThat(result.getFirst().getMimeType()).isEqualTo(MIME_TYPE);

		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ProtectedResource.ATTACHMENT, LR);
		verifyNoInteractions(revisionServiceMock);
	}

	@Test
	void readErrandAttachmentNotFoundOnErrand() {

		// Act/assert
		assertThatException()
			.isThrownBy(() -> service.readErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID, httpServletResponseMock))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(thrownProblem -> {
				assertThat(thrownProblem.getStatus()).isEqualTo(NOT_FOUND);
				assertThat(thrownProblem.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
				assertThat(thrownProblem.getMessage()).isEqualTo("Not Found: An attachment with id 'attachmentId' could not be found on errand with id 'errandId'");
			});

		// Verifications
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.ATTACHMENT, LR);
		verifyNoInteractions(revisionServiceMock, eventServiceMock);
	}

	@Test
	void deleteErrandAttachment() {

		// Mock
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandMock);
		when(errandMock.getAttachments()).thenReturn(new ArrayList<>(List.of(attachmentMock)));
		when(attachmentMock.getId()).thenReturn(ATTACHMENT_ID);
		when(errandsRepositoryMock.save(any(ErrandEntity.class))).thenReturn(errandMock);
		when(revisionServiceMock.createErrandRevision(errandMock)).thenReturn(new RevisionResult(previousRevisionMock, currentRevisionMock));

		// Call
		service.deleteErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID);

		// Assertions and verifications
		assertThat(errandMock.getAttachments()).isEmpty();

		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.ATTACHMENT, RW);
		verify(errandsRepositoryMock).save(any(ErrandEntity.class));
		verify(revisionServiceMock).createErrandRevision(errandMock);

		verify(eventServiceMock).createErrandEvent(UPDATE, EVENT_LOG_REMOVE_ATTACHMENT, errandMock, currentRevisionMock, previousRevisionMock, ATTACHMENT);
	}

	@Test
	void deleteErrandAttachmentAttachmentIdNotFound() {

		// Mock
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandMock);
		when(errandMock.getAttachments()).thenReturn(new ArrayList<>(List.of(attachmentMock)));
		when(attachmentMock.getId()).thenReturn("other-id");

		// Act/assert
		assertThatException()
			.isThrownBy(() -> service.deleteErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(thrownProblem -> {
				assertThat(thrownProblem.getStatus()).isEqualTo(NOT_FOUND);
				assertThat(thrownProblem.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
				assertThat(thrownProblem.getMessage()).isEqualTo("Not Found: An attachment with id 'attachmentId' could not be found on errand with id 'errandId'");
			});

		// Verifications
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.ATTACHMENT, RW);
		verify(errandsRepositoryMock, never()).save(any());
		verifyNoInteractions(revisionServiceMock, eventServiceMock);
	}

	@Test
	void deleteErrandAttachmentThrowException() {
		// Mock
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandMock);
		when(errandMock.getAttachments()).thenReturn(new ArrayList<>(List.of(attachmentMock)));
		when(attachmentMock.getId()).thenReturn(ATTACHMENT_ID);
		when(errandsRepositoryMock.save(any(ErrandEntity.class))).thenThrow(new RuntimeException("Test exception"));

		// Act/assert
		assertThatException()
			.isThrownBy(() -> service.deleteErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(thrownProblem -> {
				assertThat(thrownProblem.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
				assertThat(thrownProblem.getTitle()).isEqualTo(INTERNAL_SERVER_ERROR.getReasonPhrase());
				assertThat(thrownProblem.getMessage()).isEqualTo("Internal Server Error: Failed to delete attachment with id 'attachmentId' from errand with id 'errandId'");
			});

		// Verifications
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.ATTACHMENT, RW);
		verify(errandsRepositoryMock).save(any());
		verifyNoInteractions(revisionServiceMock, eventServiceMock);
	}

	@Test
	void createAttachment() throws SQLException {

		// Mock
		when(revisionServiceMock.createErrandRevision(errandMock)).thenReturn(new RevisionResult(previousRevisionMock, currentRevisionMock));
		when(attachmentMock.getAttachmentData()).thenReturn(attachmentDataEntityMock);
		when(attachmentDataEntityMock.getFile()).thenReturn(blobMock);
		when(blobMock.getBinaryStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));

		// Call
		service.createErrandAttachment(attachmentMock, errandMock);

		// Assertions and verifications
		verify(attachmentRepositoryMock).saveAndFlush(attachmentMock);
		verify(entityManagerMock).refresh(attachmentMock);
		verify(attachmentMock).setHash("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
		verify(revisionServiceMock).createErrandRevision(errandMock);
		verify(eventServiceMock).createErrandEvent(UPDATE, EVENT_LOG_ADD_ATTACHMENT, errandMock, currentRevisionMock, previousRevisionMock, ATTACHMENT);
		verifyNoInteractions(errandsRepositoryMock);
	}

	@Test
	void streamAttachmentDataSuccess() throws IOException, SQLException, InterruptedException {
		final byte[] fileContent = "file content".getBytes();
		final ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent);

		when(httpServletResponseMock.getOutputStream()).thenReturn(servletOutputStreamMock);
		when(attachmentMock.getAttachmentData()).thenReturn(attachmentDataEntityMock);
		when(attachmentDataEntityMock.getFile()).thenReturn(blobMock);
		when(blobMock.getBinaryStream()).thenReturn(inputStream);
		when(attachmentMock.getMimeType()).thenReturn("application/pdf");
		when(attachmentMock.getFileName()).thenReturn("test.pdf");
		when(attachmentMock.getFileSize()).thenReturn(fileContent.length);
		when(semaphoreMock.tryAcquire(fileContent.length, 5, SECONDS)).thenReturn(true);

		service.streamAttachmentData(attachmentMock, httpServletResponseMock);

		verify(httpServletResponseMock).addHeader(CONTENT_TYPE, "application/pdf");
		verify(httpServletResponseMock).addHeader(CONTENT_DISPOSITION, "attachment; filename=\"test.pdf\"");
		verify(httpServletResponseMock).setContentLength(fileContent.length);
		verify(servletOutputStreamMock).write(any(byte[].class), eq(0), eq(fileContent.length));
	}

	@Test
	void streamAttachmentDataThrowsSQLException() throws SQLException, InterruptedException {
		final byte[] fileContent = "file content".getBytes();
		when(attachmentMock.getAttachmentData()).thenReturn(attachmentDataEntityMock);
		when(attachmentDataEntityMock.getFile()).thenReturn(blobMock);
		when(blobMock.getBinaryStream()).thenThrow(new SQLException("Test SQLException"));
		when(attachmentMock.getFileSize()).thenReturn(fileContent.length);
		when(semaphoreMock.tryAcquire(fileContent.length, 5, SECONDS)).thenReturn(true);

		assertThatThrownBy(() -> service.streamAttachmentData(attachmentMock, httpServletResponseMock))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("SQLException occurred when copying file with attachment id");

		verify(httpServletResponseMock, never()).addHeader(eq(CONTENT_TYPE), anyString());
	}

	@Test
	void streamAttachmentDataFileSizeNull() {
		when(attachmentMock.getFileSize()).thenReturn(null);
		when(attachmentMock.getId()).thenReturn("attachmentId");

		assertThatThrownBy(() -> service.streamAttachmentData(attachmentMock, httpServletResponseMock))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Attachment with id 'attachmentId' has no data");

		verify(httpServletResponseMock, never()).addHeader(eq(CONTENT_TYPE), anyString());
	}

	@Test
	void streamAttachmentDataFileSizeZero() {
		when(attachmentMock.getFileSize()).thenReturn(0);
		when(attachmentMock.getId()).thenReturn("attachmentId");

		assertThatThrownBy(() -> service.streamAttachmentData(attachmentMock, httpServletResponseMock))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Attachment with id 'attachmentId' has no data");

		verify(httpServletResponseMock, never()).addHeader(eq(CONTENT_TYPE), anyString());
	}

	@Test
	void streamAttachmentDataBusy() throws InterruptedException {
		// Arrange
		final byte[] fileContent = "file content".getBytes();
		when(attachmentMock.getFileSize()).thenReturn(fileContent.length);
		when(semaphoreMock.tryAcquire(fileContent.length, 5, SECONDS)).thenReturn(false);

		// Act and Assert
		assertThatThrownBy(() -> service.streamAttachmentData(attachmentMock, httpServletResponseMock))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Insufficient Storage: Insufficient storage available to process the request.");
	}

	/**
	 * The only place the purpose is written. It is looked up within the namespace by its id, and the change is recorded
	 * like any other change to the attachments of the errand.
	 */
	@Test
	void updateErrandAttachment() {
		// Arrange
		final var purpose = AttachmentPurposeEntity.create().withId(PURPOSE_ID).withName("RESPONSE").withDisplayName("Inkommen handling");
		final var entity = AttachmentEntity.create().withId(ATTACHMENT_ID);
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.ATTACHMENT, RW)).thenReturn(errandMock);
		when(errandMock.getAttachments()).thenReturn(List.of(entity));
		when(attachmentPurposeRepositoryMock.findByIdAndNamespaceAndMunicipalityId(PURPOSE_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(purpose));
		when(revisionServiceMock.createErrandRevision(errandMock)).thenReturn(new RevisionResult(previousRevisionMock, currentRevisionMock));

		// Act
		final var result = service.updateErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID, purposeRequest());

		// Assert
		assertThat(result.getPurpose()).isEqualTo(ErrandAttachmentPurpose.create().withId(PURPOSE_ID).withName("RESPONSE").withDisplayName("Inkommen handling"));
		assertThat(entity.getPurpose()).isSameAs(purpose);
		final var inOrder = inOrder(attachmentRepositoryMock, revisionServiceMock, eventServiceMock);
		inOrder.verify(attachmentRepositoryMock).flush();
		inOrder.verify(revisionServiceMock).createErrandRevision(errandMock);
		inOrder.verify(eventServiceMock).createErrandEvent(UPDATE, EVENT_LOG_UPDATE_ATTACHMENT, errandMock, currentRevisionMock, previousRevisionMock, ATTACHMENT);
	}

	/**
	 * A patch says nothing about what it omits, so a body without a purpose leaves the stored one standing - and an
	 * errand that did not change gets no revision and no event.
	 */
	@Test
	void updateErrandAttachmentWithoutPurposeLeavesItAlone() {
		// Arrange
		final var purpose = AttachmentPurposeEntity.create().withId(PURPOSE_ID).withName("SUPPORTING");
		final var entity = AttachmentEntity.create().withId(ATTACHMENT_ID).withPurpose(purpose);
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.ATTACHMENT, RW)).thenReturn(errandMock);
		when(errandMock.getAttachments()).thenReturn(List.of(entity));

		// Act
		final var result = service.updateErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID, UpdateErrandAttachmentRequest.create());

		// Assert
		assertThat(result.getPurpose().getName()).isEqualTo("SUPPORTING");
		assertThat(entity.getPurpose()).isSameAs(purpose);
		verify(revisionServiceMock).createErrandRevision(errandMock);
		verifyNoInteractions(attachmentPurposeRepositoryMock, eventServiceMock);
	}

	/**
	 * A purpose of another namespace, or none at all, is refused rather than borrowed.
	 */
	@Test
	void updateErrandAttachmentWithUnknownPurposeGivesBadRequest() {
		// Arrange
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.ATTACHMENT, RW)).thenReturn(errandMock);
		when(attachmentPurposeRepositoryMock.findByIdAndNamespaceAndMunicipalityId(PURPOSE_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		// Act and Assert
		assertThatException()
			.isThrownBy(() -> service.updateErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID, purposeRequest()))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(thrownProblem -> {
				assertThat(thrownProblem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(thrownProblem.getMessage())
					.isEqualTo("Bad Request: '" + PURPOSE_ID + "' is not an attachment purpose of namespace 'namespace' and municipality with id 'municipalityId'");
			});
		verifyNoInteractions(revisionServiceMock, eventServiceMock);
	}

	/**
	 * An event that cannot be written does not undo a change that has been made.
	 */
	@Test
	void updateErrandAttachmentWhenEventFails() {
		// Arrange
		final var purpose = AttachmentPurposeEntity.create().withId(PURPOSE_ID).withName("RESPONSE");
		final var entity = AttachmentEntity.create().withId(ATTACHMENT_ID);
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.ATTACHMENT, RW)).thenReturn(errandMock);
		when(errandMock.getAttachments()).thenReturn(List.of(entity));
		when(attachmentPurposeRepositoryMock.findByIdAndNamespaceAndMunicipalityId(PURPOSE_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(purpose));
		when(revisionServiceMock.createErrandRevision(errandMock)).thenReturn(new RevisionResult(previousRevisionMock, currentRevisionMock));
		doThrow(new RuntimeException("Test exception")).when(eventServiceMock)
			.createErrandEvent(UPDATE, EVENT_LOG_UPDATE_ATTACHMENT, errandMock, currentRevisionMock, previousRevisionMock, ATTACHMENT);

		// Act
		final var result = service.updateErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID, purposeRequest());

		// Assert
		assertThat(result.getPurpose().getName()).isEqualTo("RESPONSE");
	}

	@Test
	void updateErrandAttachmentOfAnotherErrandGivesNotFound() {
		// Arrange
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.ATTACHMENT, RW)).thenReturn(errandMock);
		when(errandMock.getAttachments()).thenReturn(List.of(AttachmentEntity.create().withId("anotherAttachmentId")));

		// Act and Assert
		assertThatException()
			.isThrownBy(() -> service.updateErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID, UpdateErrandAttachmentRequest.create()))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(thrownProblem -> {
				assertThat(thrownProblem.getStatus()).isEqualTo(NOT_FOUND);
				assertThat(thrownProblem.getMessage()).isEqualTo("Not Found: An attachment with id 'attachmentId' could not be found on errand with id 'errandId'");
			});
		verifyNoInteractions(revisionServiceMock, eventServiceMock);
	}

	/**
	 * Clearing the purpose takes the reference away and records the change. The purpose itself is not touched.
	 */
	@Test
	void deleteErrandAttachmentPurpose() {
		// Arrange
		final var entity = AttachmentEntity.create().withId(ATTACHMENT_ID).withPurpose(AttachmentPurposeEntity.create().withId(PURPOSE_ID));
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.ATTACHMENT, RW)).thenReturn(errandMock);
		when(errandMock.getAttachments()).thenReturn(List.of(entity));
		when(revisionServiceMock.createErrandRevision(errandMock)).thenReturn(new RevisionResult(previousRevisionMock, currentRevisionMock));

		// Act
		service.deleteErrandAttachmentPurpose(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID);

		// Assert
		assertThat(entity.getPurpose()).isNull();
		final var inOrder = inOrder(attachmentRepositoryMock, revisionServiceMock, eventServiceMock);
		inOrder.verify(attachmentRepositoryMock).flush();
		inOrder.verify(revisionServiceMock).createErrandRevision(errandMock);
		inOrder.verify(eventServiceMock).createErrandEvent(UPDATE, EVENT_LOG_UPDATE_ATTACHMENT, errandMock, currentRevisionMock, previousRevisionMock, ATTACHMENT);
		verifyNoInteractions(attachmentPurposeRepositoryMock);
	}

	@Test
	void deleteErrandAttachmentPurposeOfAnotherErrandGivesNotFound() {
		// Arrange
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.ATTACHMENT, RW)).thenReturn(errandMock);
		when(errandMock.getAttachments()).thenReturn(List.of(AttachmentEntity.create().withId("anotherAttachmentId")));

		// Act and Assert
		assertThatException()
			.isThrownBy(() -> service.deleteErrandAttachmentPurpose(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(thrownProblem -> assertThat(thrownProblem.getStatus()).isEqualTo(NOT_FOUND));
		verifyNoInteractions(revisionServiceMock, eventServiceMock);
	}

	private static UpdateErrandAttachmentRequest purposeRequest() {
		return UpdateErrandAttachmentRequest.create().withPurpose(ErrandAttachmentPurpose.create().withId(PURPOSE_ID));
	}

	@Test
	void readErrandAttachmentBusy() throws InterruptedException {
		// Arrange
		final byte[] fileContent = "file content".getBytes();
		when(attachmentRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID)).thenReturn(of(attachmentMock));
		when(attachmentMock.getFileSize()).thenReturn(fileContent.length);
		when(semaphoreMock.tryAcquire(fileContent.length, 5, SECONDS)).thenReturn(false);

		// Act and Assert
		assertThatThrownBy(() -> service.readErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID, httpServletResponseMock))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Insufficient Storage: Insufficient storage available to process the request.");
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.ATTACHMENT, LR);
	}
}
