package se.sundsvall.supportmanagement.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.util.MimeTypeUtils.IMAGE_PNG_VALUE;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.dept44.support.Identifier.Type.AD_ACCOUNT;
import static se.sundsvall.dept44.support.Identifier.Type.PARTY_ID;

import generated.se.sundsvall.employee.PortalPersonData;
import generated.se.sundsvall.messaging.ExternalReference;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messagingsettings.SenderInfoResponse;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.communication.Communication;
import se.sundsvall.supportmanagement.api.model.communication.EmailAttachment;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;
import se.sundsvall.supportmanagement.api.model.communication.WebMessageAttachment;
import se.sundsvall.supportmanagement.api.model.communication.WebMessageRequest;
import se.sundsvall.supportmanagement.integration.citizen.CitizenIntegration;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.CommunicationAttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.CommunicationRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.messaging.MessagingClient;
import se.sundsvall.supportmanagement.integration.messagingsettings.MessagingSettingsClient;
import se.sundsvall.supportmanagement.service.mapper.CommunicationMapper;
import se.sundsvall.supportmanagement.service.mapper.MessagingMapper;

@ExtendWith(MockitoExtension.class)
class CommunicationServiceTest {

	private static final Decoder BASE64_DECODER = Base64.getDecoder();
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String HTML_MESSAGE = "<html><h1>message</h1></html>";
	private static final String PLAIN_MESSAGE = "message";
	private static final String RECIPIENT = "recipient";
	private static final String SENDER_EMAIL = "sender@sender.com";
	private static final String SENDER_NAME = "senderName";
	private static final String SUBJECT = "subject";
	private static final String FILE_CONTENT = "iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==";
	private static final String FILE_NAME = "fileName";
	private static final String ERRAND_ID_KEY = "errandId";
	private static final String MESSAGE_ID = "MESSAGE_ID";
	private static final String ATTACHMENT_ID = "attachmentId";

	@Mock
	private Semaphore semaphoreMock;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private CommunicationRepository communicationRepositoryMock;

	@Mock
	private AttachmentRepository attachmentRepositoryMock;

	@Mock
	private CommunicationAttachmentRepository communicationAttachmentRepositoryMock;

	@Mock
	private MessagingClient messagingClientMock;

	@Mock
	private CommunicationMapper communicationMapperMock;

	@Mock
	private ErrandEntity errandEntityMock;

	@Mock
	private CommunicationEntity communicationEntityMock;

	@Mock
	private CommunicationAttachmentEntity communicationAttachmentEntityMock;

	@Mock
	private AttachmentDataEntity attachmentDataEntityMock;

	@Captor
	private ArgumentCaptor<generated.se.sundsvall.messaging.EmailRequest> messagingEmailCaptor;

	@Captor
	private ArgumentCaptor<generated.se.sundsvall.messaging.SmsRequest> messagingSmsCaptor;

	@Mock
	private Blob blobMock;

	@Mock
	private HttpServletResponse servletResponseMock;

	@Mock
	private ServletOutputStream servletOutputStreamMock;

	@Mock
	private ErrandAttachmentService errandAttachmentServiceMock;

	@Mock
	private List<AttachmentEntity> attachmentEntitiesMock;

	@Mock
	private AttachmentEntity attachmentEntityMock;

	@Mock
	private EmployeeService employeeServiceMock;

	@Mock
	private PortalPersonData portalPersonDataMock;

	@Mock
	private CitizenIntegration citizenIntegrationMock;

	@Mock
	private MessagingSettingsClient messagingSettingsClientMock;

	@Captor
	private ArgumentCaptor<generated.se.sundsvall.messaging.MessageRequest> messageRequestCaptor;

	@InjectMocks
	private CommunicationService communicationService;

	private static EmailRequest createEmailRequest() {
		return EmailRequest.create()
			.withAttachments(List.of(EmailAttachment.create()
				.withBase64EncodedString(FILE_CONTENT)
				.withFileName(FILE_NAME)))
			.withHtmlMessage(HTML_MESSAGE)
			.withMessage(PLAIN_MESSAGE)
			.withRecipient(RECIPIENT)
			.withSender(SENDER_EMAIL)
			.withSenderName(SENDER_NAME)
			.withSubject(SUBJECT);
	}

	private static WebMessageRequest createWebMessageRequest() {
		return WebMessageRequest.create()
			.withMessage(PLAIN_MESSAGE)
			.withAttachments(List.of(WebMessageAttachment.create()
				.withBase64EncodedString(FILE_CONTENT)
				.withFileName(FILE_NAME)))
			.withAttachmentIds(List.of(ATTACHMENT_ID))
			.withDispatch(true);
	}

	@Test
	void readMessages() {

		// Parameter values
		final var namespace = "namespace";
		final var municipalityId = "2281";
		final var id = randomUUID().toString();
		final var errandNumber = "errandNumber";

		// Mock
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(any(String.class), any(String.class), any(String.class)))
			.thenReturn(true);
		when(errandsRepositoryMock.findById(any(String.class))).thenReturn(Optional.of(errandEntityMock));
		when(errandEntityMock.getErrandNumber()).thenReturn(errandNumber);
		when(communicationRepositoryMock.findByErrandNumber(any(String.class))).thenReturn(List.of(CommunicationEntity.create()));
		when(communicationMapperMock.toCommunications(anyList())).thenReturn(List.of(Communication.create()));

		// Call
		final var response = communicationService.readCommunications(namespace, municipalityId, id);

		// Verification
		assertThat(response).isNotNull().hasSize(1);

		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(any(String.class), any(String.class), any(String.class));
		verify(errandsRepositoryMock).findById(any(String.class));
		verify(communicationRepositoryMock).findByErrandNumber(any(String.class));
		verify(communicationMapperMock).toCommunications(anyList());

		verifyNoMoreInteractions(errandsRepositoryMock, communicationMapperMock, communicationRepositoryMock);
		verifyNoInteractions(communicationAttachmentRepositoryMock, messagingClientMock);
	}

	@Test
	void readExternalCommunications() {

		// Parameter values
		final var namespace = "namespace";
		final var municipalityId = "2281";
		final var id = randomUUID().toString();
		final var errandNumber = "errandNumber";

		// Mock
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)).thenReturn(true);
		when(errandsRepositoryMock.findById(id)).thenReturn(Optional.of(errandEntityMock));
		when(errandEntityMock.getErrandNumber()).thenReturn(errandNumber);
		when(communicationRepositoryMock.findByErrandNumberAndInternal(errandNumber, false)).thenReturn(List.of(CommunicationEntity.create().withInternal(true)));
		when(communicationMapperMock.toCommunications(anyList())).thenReturn(List.of(Communication.create()));

		// Call
		final var response = communicationService.readExternalCommunications(namespace, municipalityId, id);

		// Verification
		assertThat(response).isNotNull().hasSize(1);
		assertThat(response.getFirst().getViewed()).isNull();

		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
		verify(errandsRepositoryMock).findById(id);
		verify(communicationRepositoryMock).findByErrandNumberAndInternal(errandNumber, false);
		verify(communicationMapperMock).toCommunications(anyList());

		verifyNoMoreInteractions(errandsRepositoryMock, communicationMapperMock, communicationRepositoryMock);
		verifyNoInteractions(communicationAttachmentRepositoryMock, messagingClientMock);
	}

	@Test
	void updateViewedStatus() {

		// Parameter values
		final var namespace = "namespace";
		final var municipalityId = "2281";
		final var id = randomUUID().toString();
		final var messageID = randomUUID().toString();
		final var isViewed = true;

		// Mock
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(any(String.class), any(String.class), any(String.class)))
			.thenReturn(true);
		when(errandsRepositoryMock.findById(any(String.class))).thenReturn(Optional.of(errandEntityMock));
		when(communicationRepositoryMock.findById(any(String.class))).thenReturn(Optional.of(CommunicationEntity.create()));

		// Call
		communicationService.updateViewedStatus(namespace, municipalityId, id, messageID, isViewed);

		// Verification
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(any(String.class), any(String.class), any(String.class));
		verify(errandsRepositoryMock).findById(any(String.class));
		verify(communicationRepositoryMock).findById(any(String.class));
		verify(communicationRepositoryMock).save(any(CommunicationEntity.class));

		verifyNoMoreInteractions(errandsRepositoryMock, communicationRepositoryMock);
		verifyNoInteractions(communicationMapperMock, communicationAttachmentRepositoryMock, messagingClientMock);
	}

	@Test
	void getMessageAttachmentStreamed() throws SQLException, IOException, InterruptedException {
		// Parameter values
		final var attachmentId = "attachmentId";
		final var communicationId = "communicationId";
		final var content = "content";
		final var contentType = "contentType";
		final var fileName = "fileName";
		final var errandNumber = "errandNumber";
		final var inputStream = IOUtils.toInputStream(content, UTF_8);

		// Mock
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(errandEntityMock));
		when(errandEntityMock.getErrandNumber()).thenReturn(errandNumber);
		when(communicationAttachmentRepositoryMock.findByNamespaceAndMunicipalityIdAndCommunicationEntityIdAndId(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(communicationId), any())).thenReturn(Optional.of(communicationAttachmentEntityMock));
		when(communicationAttachmentEntityMock.getMimeType()).thenReturn(contentType);
		when(communicationAttachmentEntityMock.getFileName()).thenReturn(fileName);
		when(communicationAttachmentEntityMock.getAttachmentData()).thenReturn(attachmentDataEntityMock);
		when(attachmentDataEntityMock.getFile()).thenReturn(blobMock);
		when(communicationAttachmentEntityMock.getCommunicationEntity()).thenReturn(communicationEntityMock);
		when(communicationEntityMock.getErrandNumber()).thenReturn(errandNumber);
		when(blobMock.getBinaryStream()).thenReturn(inputStream);
		when(servletResponseMock.getOutputStream()).thenReturn(servletOutputStreamMock);
		when(communicationAttachmentEntityMock.getFileSize()).thenReturn(content.length());
		when(semaphoreMock.tryAcquire(content.length(), 5, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);

		// Call
		communicationService.getMessageAttachmentStreamed(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, communicationId, attachmentId, servletResponseMock);

		// Verification
		verify(errandsRepositoryMock).findById(ERRAND_ID);
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(communicationAttachmentRepositoryMock).findByNamespaceAndMunicipalityIdAndCommunicationEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, communicationId, attachmentId);
		verify(communicationAttachmentEntityMock).getAttachmentData();
		verify(attachmentDataEntityMock).getFile();
		verify(blobMock).getBinaryStream();
		verify(servletResponseMock).addHeader(CONTENT_TYPE, contentType);
		verify(servletResponseMock).addHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
		verify(servletResponseMock).setContentLength(content.length());
		verify(servletResponseMock).getOutputStream();

		verifyNoMoreInteractions(communicationAttachmentRepositoryMock, communicationAttachmentEntityMock, attachmentDataEntityMock, blobMock, servletResponseMock);
		verifyNoInteractions(communicationRepositoryMock, messagingClientMock, communicationMapperMock);
	}

	@Test
	void streamAttachmentDataSuccess() throws IOException, SQLException, InterruptedException {
		final byte[] fileContent = "file content".getBytes();
		final ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent);

		when(servletResponseMock.getOutputStream()).thenReturn(servletOutputStreamMock);
		when(communicationAttachmentEntityMock.getAttachmentData()).thenReturn(attachmentDataEntityMock);
		when(attachmentDataEntityMock.getFile()).thenReturn(blobMock);
		when(blobMock.getBinaryStream()).thenReturn(inputStream);
		when(communicationAttachmentEntityMock.getMimeType()).thenReturn("application/pdf");
		when(communicationAttachmentEntityMock.getFileName()).thenReturn("test.pdf");
		when(communicationAttachmentEntityMock.getFileSize()).thenReturn(fileContent.length);
		when(semaphoreMock.tryAcquire(fileContent.length, 5, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);

		communicationService.streamCommunicationAttachmentData(communicationAttachmentEntityMock, servletResponseMock);

		verify(servletResponseMock).addHeader(CONTENT_TYPE, "application/pdf");
		verify(servletResponseMock).addHeader(CONTENT_DISPOSITION, "attachment; filename=\"test.pdf\"");
		verify(servletResponseMock).setContentLength(fileContent.length);
		verify(servletOutputStreamMock).write(any(byte[].class), eq(0), eq(fileContent.length));
	}

	@Test
	void streamAttachmentDataThrowsSQLException() throws SQLException, InterruptedException {
		final byte[] fileContent = "file content".getBytes();
		when(communicationAttachmentEntityMock.getAttachmentData()).thenReturn(attachmentDataEntityMock);
		when(attachmentDataEntityMock.getFile()).thenReturn(blobMock);
		when(blobMock.getBinaryStream()).thenThrow(new SQLException("Test SQLException"));
		when(communicationAttachmentEntityMock.getFileSize()).thenReturn(fileContent.length);
		when(semaphoreMock.tryAcquire(fileContent.length, 5, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);

		assertThatThrownBy(() -> communicationService.streamCommunicationAttachmentData(communicationAttachmentEntityMock, servletResponseMock))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("SQLException occurred when copying file with attachment id");

		verify(servletResponseMock, never()).addHeader(eq(CONTENT_TYPE), anyString());
	}

	@Test
	void streamAttachmentDataFileSizeNull() {
		when(communicationAttachmentEntityMock.getFileSize()).thenReturn(null);
		when(communicationAttachmentEntityMock.getId()).thenReturn("attachmentId");

		assertThatThrownBy(() -> communicationService.streamCommunicationAttachmentData(communicationAttachmentEntityMock, servletResponseMock))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Attachment with id 'attachmentId' has no data");

		verify(servletResponseMock, never()).addHeader(eq(CONTENT_TYPE), anyString());
	}

	@Test
	void streamAttachmentDataFileSizeZero() {
		when(communicationAttachmentEntityMock.getFileSize()).thenReturn(0);
		when(communicationAttachmentEntityMock.getId()).thenReturn("attachmentId");

		assertThatThrownBy(() -> communicationService.streamCommunicationAttachmentData(communicationAttachmentEntityMock, servletResponseMock))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Attachment with id 'attachmentId' has no data");

		verify(servletResponseMock, never()).addHeader(eq(CONTENT_TYPE), anyString());
	}

	@Test
	void streamAttachmentDataBusy() throws InterruptedException {
		// Arrange
		final byte[] fileContent = "file content".getBytes();
		when(communicationAttachmentEntityMock.getFileSize()).thenReturn(fileContent.length);
		when(semaphoreMock.tryAcquire(fileContent.length, 5, SECONDS)).thenReturn(false);

		// Act and Assert
		assertThatThrownBy(() -> communicationService.streamCommunicationAttachmentData(communicationAttachmentEntityMock, servletResponseMock))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Insufficient Storage: Insufficient storage available to process the request.");
	}

	@Test
	void sendEmail() {
		// Parameter values
		final var request = createEmailRequest();

		// Mock
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(errandEntityMock));
		when(errandEntityMock.getId()).thenReturn(ERRAND_ID);
		when(communicationMapperMock.toCommunicationEntity(NAMESPACE, MUNICIPALITY_ID, request)).thenReturn(CommunicationEntity.create());
		when(communicationMapperMock.toAttachments(any(CommunicationEntity.class))).thenReturn(List.of(AttachmentEntity.create()));

		// Call
		communicationService.sendEmail(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request);

		// Verifications and assertions
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandsRepositoryMock).findById(ERRAND_ID);
		verify(messagingClientMock).sendEmail(eq(MUNICIPALITY_ID), eq(false), messagingEmailCaptor.capture());
		verify(communicationMapperMock).toCommunicationEntity(NAMESPACE, MUNICIPALITY_ID, request);
		verify(communicationRepositoryMock).saveAndFlush(any(CommunicationEntity.class));
		verify(communicationMapperMock).toAttachments(any(CommunicationEntity.class));
		verify(errandAttachmentServiceMock).createErrandAttachment(any(AttachmentEntity.class), any(ErrandEntity.class));

		final var arguments = messagingEmailCaptor.getValue();
		assertThat(arguments.getEmailAddress()).isEqualTo(RECIPIENT);
		assertThat(new String(BASE64_DECODER.decode(arguments.getHtmlMessage()), StandardCharsets.UTF_8)).isEqualTo(HTML_MESSAGE);
		assertThat(arguments.getMessage()).isEqualTo(PLAIN_MESSAGE);
		assertThat(arguments.getParty().getPartyId()).isNull();
		assertThat(arguments.getParty().getExternalReferences()).isNotEmpty().extracting(
			ExternalReference::getKey,
			ExternalReference::getValue).containsExactly(tuple(ERRAND_ID_KEY, ERRAND_ID));
		assertThat(arguments.getSubject()).isEqualTo(SUBJECT);
		assertThat(arguments.getSender().getAddress()).isEqualTo(SENDER_EMAIL);
		assertThat(arguments.getSender().getName()).isEqualTo(SENDER_NAME);
		assertThat(arguments.getAttachments()).isNotNull().hasSize(1).extracting(
			generated.se.sundsvall.messaging.EmailAttachment::getContent,
			generated.se.sundsvall.messaging.EmailAttachment::getContentType,
			generated.se.sundsvall.messaging.EmailAttachment::getName).containsExactly(tuple(FILE_CONTENT, IMAGE_PNG_VALUE, FILE_NAME));
		assertThat(arguments.getHeaders().get(MESSAGE_ID)).isNotNull().hasSize(1).extracting(String::toString).allMatch(s -> s.startsWith("<") && s.contains("@") && s.contains(NAMESPACE) && s.endsWith(">"));

		// Verification
		verifyNoMoreInteractions(errandsRepositoryMock, messagingClientMock, communicationMapperMock, communicationRepositoryMock);
		verifyNoInteractions(communicationAttachmentRepositoryMock);

	}

	@Test
	void sendSms() {
		// Parameter values
		final var request = createSmsRequest();

		// Mock
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(errandEntityMock));
		when(errandEntityMock.getId()).thenReturn(ERRAND_ID);
		when(communicationMapperMock.toCommunicationEntity(NAMESPACE, MUNICIPALITY_ID, request)).thenReturn(CommunicationEntity.create());
		when(communicationMapperMock.toAttachments(any(CommunicationEntity.class))).thenReturn(List.of(AttachmentEntity.create()));

		// Call
		communicationService.sendSms(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request);

		// Verifications and assertions
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandsRepositoryMock).findById(ERRAND_ID);
		verify(messagingClientMock).sendSms(eq(MUNICIPALITY_ID), eq(false), messagingSmsCaptor.capture());
		verify(communicationMapperMock).toCommunicationEntity(NAMESPACE, MUNICIPALITY_ID, request);
		verify(communicationRepositoryMock).saveAndFlush(any(CommunicationEntity.class));
		verify(communicationMapperMock).toAttachments(any(CommunicationEntity.class));
		verify(errandAttachmentServiceMock).createErrandAttachment(any(AttachmentEntity.class), any(ErrandEntity.class));

		final var arguments = messagingSmsCaptor.getValue();
		assertThat(arguments.getMessage()).isEqualTo(PLAIN_MESSAGE);
		assertThat(arguments.getMobileNumber()).isEqualTo(RECIPIENT);
		assertThat(arguments.getParty().getExternalReferences()).isNotEmpty().extracting(
			ExternalReference::getKey,
			ExternalReference::getValue).containsExactly(tuple(ERRAND_ID_KEY, ERRAND_ID));
		assertThat(arguments.getSender()).isEqualTo(SENDER_NAME);

		verifyNoMoreInteractions(communicationRepositoryMock, errandsRepositoryMock, messagingClientMock, communicationMapperMock);
		verifyNoInteractions(communicationAttachmentRepositoryMock);
	}

	@Test
	void sendWebMessage() {
		// Parameter values
		final var request = createWebMessageRequest();
		final var webMessageRequest = new generated.se.sundsvall.messaging.WebMessageRequest();
		final var adUser = "adUser";
		final var fullName = "fullname";

		Identifier.set(Identifier.create().withType(AD_ACCOUNT).withValue("adUser"));

		// Mock
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(errandEntityMock));
		when(errandAttachmentServiceMock.findByNamespaceAndMunicipalityIdAndIdIn(any(), any(), any())).thenReturn(attachmentEntitiesMock);
		when(errandEntityMock.getErrandNumber()).thenReturn(ERRAND_ID_KEY);
		when(communicationMapperMock.toCommunicationEntity(anyString(), anyString(), anyString(), any(), anyString(), anyString())).thenReturn(communicationEntityMock);
		when(communicationEntityMock.withErrandAttachments(any())).thenReturn(communicationEntityMock);
		when(communicationEntityMock.withViewed(true)).thenReturn(communicationEntityMock);
		when(communicationMapperMock.toAttachments(any())).thenReturn(List.of(attachmentEntityMock));
		when(attachmentEntityMock.withErrandEntity(any())).thenReturn(attachmentEntityMock);
		when(employeeServiceMock.getEmployeeByLoginName(MUNICIPALITY_ID, adUser)).thenReturn(portalPersonDataMock);
		when(portalPersonDataMock.getFullname()).thenReturn(fullName);

		try (final MockedStatic<MessagingMapper> messagingMapper = Mockito.mockStatic(MessagingMapper.class)) {
			// Mock static
			messagingMapper.when(() -> MessagingMapper.toWebMessageRequest(any(), any(), any(), anyString())).thenReturn(webMessageRequest);

			// Call
			communicationService.sendWebMessage(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request);

			// Verify static
			messagingMapper.verify(() -> MessagingMapper.toWebMessageRequest(same(errandEntityMock), same(request), same(attachmentEntitiesMock), same(adUser)));
			messagingMapper.verifyNoMoreInteractions();
		}

		// Verifications
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandsRepositoryMock).findById(ERRAND_ID);
		verify(errandAttachmentServiceMock).findByNamespaceAndMunicipalityIdAndIdIn(NAMESPACE, MUNICIPALITY_ID, List.of(ATTACHMENT_ID));
		verify(communicationMapperMock).toCommunicationEntity(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID_KEY), same(request), eq(fullName), eq(adUser));
		verify(communicationEntityMock).withErrandAttachments(same(attachmentEntitiesMock));
		verify(messagingClientMock).sendWebMessage(eq(MUNICIPALITY_ID), eq(false), same(webMessageRequest));
		verify(communicationRepositoryMock).saveAndFlush(same(communicationEntityMock));
		verify(communicationMapperMock).toAttachments(same(communicationEntityMock));
		verify(attachmentEntityMock).withErrandEntity(same(errandEntityMock));
		verify(errandAttachmentServiceMock).createErrandAttachment(same(attachmentEntityMock), same(errandEntityMock));

		verifyNoMoreInteractions(errandsRepositoryMock, messagingClientMock, communicationMapperMock, communicationRepositoryMock, attachmentEntityMock, communicationEntityMock, errandAttachmentServiceMock,
			attachmentEntitiesMock, portalPersonDataMock, employeeServiceMock);
		verifyNoInteractions(communicationAttachmentRepositoryMock);
	}

	/**
	 * Test scenario where the X-Sent-By header is being used, the type is partyId. Dispatch is set to false and messaging
	 * is therefore not called.
	 */
	@Test
	void sendWebMessage_2() {
		final var request = createWebMessageRequest();
		request.setDispatch(false);

		Identifier.set(Identifier.create().withType(PARTY_ID).withValue("e82c8029-7676-467d-8ebb-8638d0abd2b4"));

		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(errandEntityMock));
		when(errandAttachmentServiceMock.findByNamespaceAndMunicipalityIdAndIdIn(any(), any(), any())).thenReturn(attachmentEntitiesMock);
		when(citizenIntegrationMock.getCitizenName(any(), any())).thenReturn("John Doe");
		when(errandEntityMock.getErrandNumber()).thenReturn("123");
		when(communicationMapperMock.toCommunicationEntity(any(), any(), any(), any(), any(), any())).thenReturn(communicationEntityMock);
		when(communicationEntityMock.withViewed(true)).thenReturn(communicationEntityMock);

		communicationService.sendWebMessage(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request);

		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandsRepositoryMock).findById(ERRAND_ID);
		verify(errandAttachmentServiceMock).findByNamespaceAndMunicipalityIdAndIdIn(NAMESPACE, MUNICIPALITY_ID, List.of(ATTACHMENT_ID));
		verify(communicationMapperMock).toCommunicationEntity(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq("123"), same(request), eq("John Doe"), eq("e82c8029-7676-467d-8ebb-8638d0abd2b4"));
		verify(communicationEntityMock).withErrandAttachments(same(attachmentEntitiesMock));
		verify(communicationRepositoryMock).saveAndFlush(any());
		verify(communicationMapperMock).toAttachments(any());
		verify(messagingClientMock, never()).sendWebMessage(any(), anyBoolean(), any());
	}

	/**
	 * Test scenario where the X-Sent-By header is being used, the type is adAccount. Dispatch is set to false and messaging
	 * is therefore not called.
	 */
	@Test
	void sendWebMessage_3() {
		final var request = createWebMessageRequest();
		request.setDispatch(false);

		Identifier.set(Identifier.create().withType(AD_ACCOUNT).withValue("jon01doe"));

		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(errandEntityMock));
		when(errandAttachmentServiceMock.findByNamespaceAndMunicipalityIdAndIdIn(any(), any(), any())).thenReturn(attachmentEntitiesMock);
		when(employeeServiceMock.getEmployeeByLoginName(MUNICIPALITY_ID, "jon01doe")).thenReturn(portalPersonDataMock);
		when(portalPersonDataMock.getFullname()).thenReturn("John Doe");

		when(errandEntityMock.getErrandNumber()).thenReturn("123");
		when(communicationMapperMock.toCommunicationEntity(any(), any(), any(), any(), any(), any())).thenReturn(communicationEntityMock);
		when(communicationEntityMock.withViewed(true)).thenReturn(communicationEntityMock);

		communicationService.sendWebMessage(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request);

		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandsRepositoryMock).findById(ERRAND_ID);
		verify(errandAttachmentServiceMock).findByNamespaceAndMunicipalityIdAndIdIn(NAMESPACE, MUNICIPALITY_ID, List.of(ATTACHMENT_ID));
		verify(communicationMapperMock).toCommunicationEntity(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq("123"), same(request), eq("John Doe"), eq("jon01doe"));
		verify(communicationMapperMock).toAttachments(any());
		verify(communicationEntityMock).withErrandAttachments(same(attachmentEntitiesMock));
		verify(communicationRepositoryMock).saveAndFlush(any());
		verify(messagingClientMock, never()).sendWebMessage(any(), anyBoolean(), any());
	}

	@Test
	void errandNotFound() {
		// Setup
		final var request = createSmsRequest();

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> communicationService.sendSms(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request));

		// Verifications and assertions
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(errandsRepositoryMock);
		verifyNoInteractions(communicationRepositoryMock, communicationAttachmentRepositoryMock, messagingClientMock, communicationMapperMock);

		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id '" + ERRAND_ID + "' could not be found in namespace '" +
			NAMESPACE + "' for municipality with id '" + MUNICIPALITY_ID + "'");
	}

	private SmsRequest createSmsRequest() {
		return SmsRequest.create()
			.withMessage(PLAIN_MESSAGE)
			.withRecipient(RECIPIENT)
			.withSender(SENDER_NAME);
	}

	@Test
	void saveCommunication() {

		communicationService.saveCommunication(CommunicationEntity.create().withErrandNumber("123"));

		verify(communicationRepositoryMock).saveAndFlush(any(CommunicationEntity.class));
		verifyNoMoreInteractions(communicationRepositoryMock);
		verifyNoInteractions(errandsRepositoryMock, communicationAttachmentRepositoryMock, messagingClientMock, communicationMapperMock);
	}

	/**
	 * Test scenario where adUser is null, employee should not be called.
	 */
	@Test
	void getEmployeeName_1() {
		final String adUser = null;

		final var result = communicationService.getEmployeeName(MUNICIPALITY_ID, adUser);

		assertThat(result).isNull();
		verify(employeeServiceMock, never()).getEmployeeByLoginName(MUNICIPALITY_ID, adUser);
	}

	/**
	 * Test scenario where adUser is set and employee is called.
	 */
	@Test
	void getEmployeeName_2() {
		final var adUser = "jon03doe";
		final var portalPersonData = new PortalPersonData();
		portalPersonData.setFullname("John Doe");
		when(employeeServiceMock.getEmployeeByLoginName(MUNICIPALITY_ID, adUser)).thenReturn(portalPersonData);

		final var result = communicationService.getEmployeeName(MUNICIPALITY_ID, adUser);

		assertThat(result).isNotNull().isEqualTo("John Doe");
		verify(employeeServiceMock).getEmployeeByLoginName(MUNICIPALITY_ID, adUser);
	}

	/**
	 * Test scenario where partyId is null. Citizen should not be called.
	 */
	@Test
	void getCitizenName_1() {
		final String partyId = null;
		final var result = communicationService.getCitizenName(MUNICIPALITY_ID, partyId);

		assertThat(result).isNull();
		verify(citizenIntegrationMock, never()).getCitizenName(MUNICIPALITY_ID, partyId);
	}

	/**
	 * Test scenario where partyId is valid. Citizen should be called.
	 */
	@Test
	void getCitizenName_2() {
		final var partyId = UUID.randomUUID().toString();
		when(citizenIntegrationMock.getCitizenName(MUNICIPALITY_ID, partyId)).thenReturn("Johnny Doe");

		final var result = communicationService.getCitizenName(MUNICIPALITY_ID, partyId);

		assertThat(result).isNotNull().isEqualTo("Johnny Doe");
		verify(citizenIntegrationMock).getCitizenName(MUNICIPALITY_ID, partyId);
	}

	@Test
	void sendMessageNotification() {
		// Arrange
		final var errandId = UUID.randomUUID().toString();
		final var messageId = UUID.randomUUID();
		final var errand = ErrandEntity.create()
			.withId(errandId)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE);

		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(errandId, MUNICIPALITY_ID, NAMESPACE)).thenReturn(Optional.of(errand));
		when(messagingSettingsClientMock.getSenderInfo(MUNICIPALITY_ID, NAMESPACE)).thenReturn(new SenderInfoResponse());
		when(messagingClientMock.sendMessage(eq(MUNICIPALITY_ID), any())).thenReturn(
			new MessageResult().messageId(messageId));
		// Act
		communicationService.sendMessageNotification(MUNICIPALITY_ID, NAMESPACE, errandId);

		// Assert
		verify(errandsRepositoryMock).findByIdAndNamespaceAndMunicipalityId(errandId, MUNICIPALITY_ID, NAMESPACE);
		verify(messagingClientMock).sendMessage(eq(MUNICIPALITY_ID), messageRequestCaptor.capture());
		assertThat(messageRequestCaptor.getValue().getMessages()).hasSize(1);
		verifyNoMoreInteractions(errandsRepositoryMock, communicationMapperMock);

	}

	@Test
	void sendMessageNotificationWithNullSenderInfo() {
		// Arrange
		final var errandId = UUID.randomUUID().toString();
		final var errand = ErrandEntity.create()
			.withId(errandId)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE);
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(errandId, MUNICIPALITY_ID, NAMESPACE)).thenReturn(Optional.of(errand));
		when(messagingSettingsClientMock.getSenderInfo(MUNICIPALITY_ID, NAMESPACE)).thenReturn(null);

		// Act & Assert
		assertThatThrownBy(() -> communicationService.sendMessageNotification(MUNICIPALITY_ID, NAMESPACE, errandId))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", Status.INTERNAL_SERVER_ERROR)
			.hasFieldOrPropertyWithValue("message", "Internal Server Error: Failed to retrieve sender information for municipality '" + MUNICIPALITY_ID + "' and namespace '" + NAMESPACE + "'");
		verify(errandsRepositoryMock).findByIdAndNamespaceAndMunicipalityId(errandId, MUNICIPALITY_ID, NAMESPACE);
		verify(messagingSettingsClientMock).getSenderInfo(MUNICIPALITY_ID, NAMESPACE);
		verifyNoInteractions(messagingClientMock, communicationMapperMock);

	}

	@Test
	void sendMessageNotificationWithErrandNotFound() {
		// Arrange
		final var errandId = UUID.randomUUID().toString();
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(errandId, MUNICIPALITY_ID, NAMESPACE)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> communicationService.sendMessageNotification(MUNICIPALITY_ID, NAMESPACE, errandId))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", Status.NOT_FOUND)
			.hasFieldOrPropertyWithValue("message", "Not Found: An errand with id '" + errandId + "' could not be found in namespace '" + NAMESPACE + "' for municipality with id '" + MUNICIPALITY_ID + "'");

		verify(errandsRepositoryMock).findByIdAndNamespaceAndMunicipalityId(errandId, MUNICIPALITY_ID, NAMESPACE);
		verifyNoInteractions(messagingSettingsClientMock, messagingClientMock, communicationMapperMock);

	}

	@Test
	void sendMessageNotificationFailed() {
		// Arrange
		final var errandId = UUID.randomUUID().toString();
		final var errand = ErrandEntity.create()
			.withId(errandId)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE);

		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(errandId, MUNICIPALITY_ID, NAMESPACE)).thenReturn(Optional.of(errand));
		when(messagingSettingsClientMock.getSenderInfo(MUNICIPALITY_ID, NAMESPACE)).thenReturn(new SenderInfoResponse());
		when(messagingClientMock.sendMessage(eq(MUNICIPALITY_ID), any())).thenReturn(null);

		// Act & Assert
		assertThatThrownBy(() -> communicationService.sendMessageNotification(MUNICIPALITY_ID, NAMESPACE, errandId))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", Status.INTERNAL_SERVER_ERROR)
			.hasFieldOrPropertyWithValue("message", "Internal Server Error: Failed to create message notification");

	}
}
