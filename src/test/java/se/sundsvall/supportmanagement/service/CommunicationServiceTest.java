package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
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
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.dept44.support.Identifier.Type.AD_ACCOUNT;
import static se.sundsvall.dept44.support.Identifier.Type.PARTY_ID;

import generated.se.sundsvall.employee.PortalPersonData;
import generated.se.sundsvall.messaging.ExternalReference;
import generated.se.sundsvall.messaging.MessageRequest;
import generated.se.sundsvall.messaging.MessageResult;
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
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ContactChannelEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.messaging.MessagingClient;
import se.sundsvall.supportmanagement.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.supportmanagement.service.mapper.CommunicationMapper;
import se.sundsvall.supportmanagement.service.mapper.MessagingMapper;
import se.sundsvall.supportmanagement.service.model.MessagingSettings;

@ExtendWith(MockitoExtension.class)
class CommunicationServiceTest {
	private static final String DEPARTMENT_NAME = "departmentName";
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
	private AccessControlService accessControlServiceMock;

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
	private MessagingSettingsIntegration messagingSettingsIntegrationMock;

	@Captor
	private ArgumentCaptor<generated.se.sundsvall.messaging.MessageRequest> messageRequestCaptor;

	@Captor
	private ArgumentCaptor<generated.se.sundsvall.messaging.EmailRequest> emailRequestCaptor;

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

	@BeforeEach
	void setup() {
		Identifier.remove();
	}

	@Test
	void readMessages() {

		// Parameter values
		final var namespace = "namespace";
		final var municipalityId = "2281";
		final var id = randomUUID().toString();
		final var errandNumber = "errandNumber";

		// Mock
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandEntityMock);
		when(errandEntityMock.getErrandNumber()).thenReturn(errandNumber);
		when(communicationRepositoryMock.findByErrandNumber(any(String.class))).thenReturn(List.of(CommunicationEntity.create()));
		when(communicationMapperMock.toCommunications(anyList())).thenReturn(List.of(Communication.create()));

		// Call
		final var response = communicationService.readCommunications(namespace, municipalityId, id);

		// Verification
		assertThat(response).isNotNull().hasSize(1);

		verify(accessControlServiceMock).getErrand(namespace, municipalityId, id, false, R, RW);
		verify(communicationRepositoryMock).findByErrandNumber(any(String.class));
		verify(communicationMapperMock).toCommunications(anyList());

		verifyNoMoreInteractions(accessControlServiceMock, communicationMapperMock, communicationRepositoryMock);
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
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandEntityMock);
		when(errandEntityMock.getErrandNumber()).thenReturn(errandNumber);
		when(communicationRepositoryMock.findByErrandNumberAndInternal(errandNumber, false)).thenReturn(List.of(CommunicationEntity.create().withInternal(true)));
		when(communicationMapperMock.toCommunications(anyList())).thenReturn(List.of(Communication.create()));

		// Call
		final var response = communicationService.readExternalCommunications(namespace, municipalityId, id);

		// Verification
		assertThat(response).isNotNull().hasSize(1);
		assertThat(response.getFirst().getViewed()).isNull();

		verify(accessControlServiceMock).getErrand(namespace, municipalityId, id, false, R, RW);
		verify(communicationRepositoryMock).findByErrandNumberAndInternal(errandNumber, false);
		verify(communicationMapperMock).toCommunications(anyList());

		verifyNoMoreInteractions(accessControlServiceMock, communicationMapperMock, communicationRepositoryMock);
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
		when(communicationRepositoryMock.findById(any(String.class))).thenReturn(Optional.of(CommunicationEntity.create()));

		// Call
		communicationService.updateViewedStatus(namespace, municipalityId, id, messageID, isViewed);

		// Verification
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(namespace, municipalityId, id, RW);
		verify(communicationRepositoryMock).findById(any(String.class));
		verify(communicationRepositoryMock).save(any(CommunicationEntity.class));

		verifyNoMoreInteractions(accessControlServiceMock, communicationRepositoryMock);
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
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandEntityMock);
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
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, R, RW);
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
		final var fileContent = "file content".getBytes();
		final var inputStream = new ByteArrayInputStream(fileContent);

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
		final var fileContent = "file content".getBytes();
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
		final var fileContent = "file content".getBytes();
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
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any())).thenReturn(errandEntityMock);
		when(errandEntityMock.getId()).thenReturn(ERRAND_ID);
		when(errandEntityMock.getNamespace()).thenReturn(NAMESPACE);
		when(errandEntityMock.getMunicipalityId()).thenReturn(MUNICIPALITY_ID);
		when(communicationMapperMock.toCommunicationEntity(NAMESPACE, MUNICIPALITY_ID, request)).thenReturn(CommunicationEntity.create());
		when(communicationMapperMock.toAttachments(any(CommunicationEntity.class))).thenReturn(List.of(AttachmentEntity.create()));

		// Call
		communicationService.sendEmail(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request);

		// Verifications and assertions
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, RW);
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
		verifyNoMoreInteractions(accessControlServiceMock, messagingClientMock, communicationMapperMock, communicationRepositoryMock);
		verifyNoInteractions(communicationAttachmentRepositoryMock);

	}

	@Test
	void sendSms() {
		// Parameter values
		final var request = createSmsRequest();

		// Mock
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any())).thenReturn(errandEntityMock);
		when(errandEntityMock.getId()).thenReturn(ERRAND_ID);
		when(communicationMapperMock.toCommunicationEntity(NAMESPACE, MUNICIPALITY_ID, request)).thenReturn(CommunicationEntity.create());
		when(communicationMapperMock.toAttachments(any(CommunicationEntity.class))).thenReturn(List.of(AttachmentEntity.create()));

		// Call
		communicationService.sendSms(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request);

		// Verifications and assertions
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, RW);
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

		verifyNoMoreInteractions(communicationRepositoryMock, accessControlServiceMock, messagingClientMock, communicationMapperMock);
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
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any())).thenReturn(errandEntityMock);
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
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, RW);
		verify(errandAttachmentServiceMock).findByNamespaceAndMunicipalityIdAndIdIn(NAMESPACE, MUNICIPALITY_ID, List.of(ATTACHMENT_ID));
		verify(communicationMapperMock).toCommunicationEntity(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID_KEY), same(request), eq(fullName), eq(adUser));
		verify(communicationEntityMock).withErrandAttachments(same(attachmentEntitiesMock));
		verify(messagingClientMock).sendWebMessage(eq(MUNICIPALITY_ID), eq(false), same(webMessageRequest));
		verify(communicationRepositoryMock).saveAndFlush(same(communicationEntityMock));
		verify(communicationMapperMock).toAttachments(same(communicationEntityMock));
		verify(attachmentEntityMock).withErrandEntity(same(errandEntityMock));
		verify(errandAttachmentServiceMock).createErrandAttachment(same(attachmentEntityMock), same(errandEntityMock));

		verifyNoMoreInteractions(accessControlServiceMock, messagingClientMock, communicationMapperMock, communicationRepositoryMock, attachmentEntityMock, communicationEntityMock, errandAttachmentServiceMock,
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

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any())).thenReturn(errandEntityMock);
		when(errandAttachmentServiceMock.findByNamespaceAndMunicipalityIdAndIdIn(any(), any(), any())).thenReturn(attachmentEntitiesMock);
		when(citizenIntegrationMock.getCitizenName(any(), any())).thenReturn("John Doe");
		when(errandEntityMock.getErrandNumber()).thenReturn("123");
		when(communicationMapperMock.toCommunicationEntity(any(), any(), any(), any(), any(), any())).thenReturn(communicationEntityMock);
		when(communicationEntityMock.withViewed(true)).thenReturn(communicationEntityMock);

		communicationService.sendWebMessage(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request);

		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, RW);
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

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any())).thenReturn(errandEntityMock);
		when(errandAttachmentServiceMock.findByNamespaceAndMunicipalityIdAndIdIn(any(), any(), any())).thenReturn(attachmentEntitiesMock);
		when(employeeServiceMock.getEmployeeByLoginName(MUNICIPALITY_ID, "jon01doe")).thenReturn(portalPersonDataMock);
		when(portalPersonDataMock.getFullname()).thenReturn("John Doe");

		when(errandEntityMock.getErrandNumber()).thenReturn("123");
		when(communicationMapperMock.toCommunicationEntity(any(), any(), any(), any(), any(), any())).thenReturn(communicationEntityMock);
		when(communicationEntityMock.withViewed(true)).thenReturn(communicationEntityMock);

		communicationService.sendWebMessage(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request);

		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, RW);
		verify(errandAttachmentServiceMock).findByNamespaceAndMunicipalityIdAndIdIn(NAMESPACE, MUNICIPALITY_ID, List.of(ATTACHMENT_ID));
		verify(communicationMapperMock).toCommunicationEntity(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq("123"), same(request), eq("John Doe"), eq("jon01doe"));
		verify(communicationMapperMock).toAttachments(any());
		verify(communicationEntityMock).withErrandAttachments(same(attachmentEntitiesMock));
		verify(communicationRepositoryMock).saveAndFlush(any());
		verify(messagingClientMock, never()).sendWebMessage(any(), anyBoolean(), any());
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
		verifyNoInteractions(accessControlServiceMock, communicationAttachmentRepositoryMock, messagingClientMock, communicationMapperMock);
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
		Identifier.set(Identifier.create().withType(PARTY_ID).withValue("xx"));
		final var errandId = UUID.randomUUID().toString();
		final var messageId = UUID.randomUUID();
		final var errand = ErrandEntity.create()
			.withId(errandId)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE);

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any())).thenReturn(errand);
		when(messagingSettingsIntegrationMock.getMessagingsettings(MUNICIPALITY_ID, NAMESPACE, DEPARTMENT_NAME)).thenReturn(new MessagingSettings(null, null, null, null, null, null, null));
		when(messagingClientMock.sendMessage(eq(MUNICIPALITY_ID), any())).thenReturn(
			new MessageResult().messageId(messageId));
		// Act
		communicationService.sendMessageNotification(MUNICIPALITY_ID, NAMESPACE, errandId, DEPARTMENT_NAME);

		// Assert
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, errandId, false, RW);
		verify(messagingClientMock).sendMessage(eq(MUNICIPALITY_ID), messageRequestCaptor.capture());
		assertThat(messageRequestCaptor.getValue().getMessages()).hasSize(1);
		verifyNoMoreInteractions(accessControlServiceMock, communicationMapperMock);

	}

	@Test
	void sendMessageNotificationWithNullSenderInfo() {
		// Arrange
		final var exception = Problem.valueOf(INTERNAL_SERVER_ERROR, "No messagingsettings found");
		final var errandId = UUID.randomUUID().toString();
		final var errand = ErrandEntity.create()
			.withId(errandId)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE);
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any())).thenReturn(errand);
		when(messagingSettingsIntegrationMock.getMessagingsettings(MUNICIPALITY_ID, NAMESPACE, DEPARTMENT_NAME)).thenThrow(exception);

		// Act & Assert
		assertThatThrownBy(() -> communicationService.sendMessageNotification(MUNICIPALITY_ID, NAMESPACE, errandId, DEPARTMENT_NAME))
			.isInstanceOf(ThrowableProblem.class)
			.isSameAs(exception);
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, errandId, false, RW);
		verify(messagingSettingsIntegrationMock).getMessagingsettings(MUNICIPALITY_ID, NAMESPACE, DEPARTMENT_NAME);
		verifyNoInteractions(messagingClientMock, communicationMapperMock);

	}

	@Test
	void sendMessageNotificationFailed() {
		// Arrange
		final var errandId = UUID.randomUUID().toString();
		final var errand = ErrandEntity.create()
			.withId(errandId)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE);

		Identifier.set(Identifier.create().withType(PARTY_ID).withValue("e82c8029-7676-467d-8ebb-8638d0abd2b4"));

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any())).thenReturn(errand);
		when(messagingSettingsIntegrationMock.getMessagingsettings(MUNICIPALITY_ID, NAMESPACE, DEPARTMENT_NAME)).thenReturn(new MessagingSettings(null, null, null, null, null, null, null));
		when(messagingClientMock.sendMessage(eq(MUNICIPALITY_ID), any())).thenReturn(null);

		// Act & Assert
		assertThatThrownBy(() -> communicationService.sendMessageNotification(MUNICIPALITY_ID, NAMESPACE, errandId, DEPARTMENT_NAME))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", Status.INTERNAL_SERVER_ERROR)
			.hasFieldOrPropertyWithValue("message", "Internal Server Error: Failed to create message notification");

		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, errandId, false, RW);
		verify(messagingSettingsIntegrationMock).getMessagingsettings(MUNICIPALITY_ID, NAMESPACE, DEPARTMENT_NAME);
		verify(messagingClientMock).sendMessage(eq(MUNICIPALITY_ID), any(MessageRequest.class));
	}

	@Test
	void sendEmailNotification() {
		Identifier.set(Identifier.parse("bcd234; type=adAccount"));

		final var title = "title";
		final var errandNumber = "errandNumber";
		final var reporterSupportText = "reporterSupportText";
		final var katlaUrl = "katlaUrl";
		final var contactInformationEmail = "contactInformationEmail";
		final var contactInformationName = "contactInformationEmailName";
		final var recieverEmail = "abc123@noreply.com";
		final var messagingsettings = new MessagingSettings(null, reporterSupportText, null, katlaUrl, null, contactInformationEmail, contactInformationName);

		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, RW)).thenReturn(errandEntityMock);
		when(errandEntityMock.getMunicipalityId()).thenReturn(MUNICIPALITY_ID);
		when(errandEntityMock.getNamespace()).thenReturn(NAMESPACE);
		when(errandEntityMock.getTitle()).thenReturn(title);
		when(errandEntityMock.getErrandNumber()).thenReturn(errandNumber);
		when(errandEntityMock.getStakeholders()).thenReturn(List.of(StakeholderEntity.create()
			.withRole("REPORTER")
			.withContactChannels(List.of(ContactChannelEntity.create()
				.withType("email")
				.withValue(recieverEmail)))
			.withParameters(List.of(StakeholderParameterEntity.create()
				.withKey("username")
				.withValues(List.of("abc123"))))));
		when(messagingSettingsIntegrationMock.getMessagingsettings(MUNICIPALITY_ID, NAMESPACE, DEPARTMENT_NAME)).thenReturn(messagingsettings);
		when(errandAttachmentServiceMock.findByNamespaceAndMunicipalityIdAndIdIn(any(), any(), any())).thenReturn(attachmentEntitiesMock);
		when(communicationMapperMock.toCommunicationEntity(eq(NAMESPACE), eq(MUNICIPALITY_ID), any(EmailRequest.class))).thenReturn(CommunicationEntity.create());

		communicationService.sendEmailNotificationToReporter(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, DEPARTMENT_NAME);

		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, RW);
		verify(errandEntityMock).getStakeholders();
		verify(messagingSettingsIntegrationMock).getMessagingsettings(MUNICIPALITY_ID, NAMESPACE, DEPARTMENT_NAME);
		verify(errandAttachmentServiceMock).findByNamespaceAndMunicipalityIdAndIdIn(eq(NAMESPACE), eq(MUNICIPALITY_ID), any());
		verify(communicationMapperMock).toCommunicationEntity(eq(NAMESPACE), eq(MUNICIPALITY_ID), any(EmailRequest.class));
		verify(messagingClientMock).sendEmail(eq(MUNICIPALITY_ID), eq(false), emailRequestCaptor.capture());
		verifyNoMoreInteractions(accessControlServiceMock, messagingSettingsIntegrationMock, messagingClientMock);

		assertThat(emailRequestCaptor.getValue()).satisfies(emailRequest -> {
			assertThat(emailRequest.getSubject()).isEqualTo("Nytt meddelande kopplat till ärendet %s %s".formatted(title, errandNumber));
			assertThat(emailRequest.getMessage()).isEqualToNormalizingUnicode(reporterSupportText);
			assertThat(emailRequest.getEmailAddress()).isEqualTo(recieverEmail);
			assertThat(emailRequest.getHtmlMessage()).isNull();
			assertThat(emailRequest.getSender().getAddress()).isEqualTo(contactInformationEmail);
			assertThat(emailRequest.getSender().getName()).isEqualTo(contactInformationName);
		});
	}

	@Test
	void sendEmailNotificationWhenNoReporterStakeholder() {
		Identifier.set(Identifier.parse("bcd234; type=adAccount"));

		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, RW)).thenReturn(errandEntityMock);
		when(errandEntityMock.getStakeholders()).thenReturn(List.of(StakeholderEntity.create()
			.withRole("APPLICANT")));

		communicationService.sendEmailNotificationToReporter(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, DEPARTMENT_NAME);

		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, RW);
		verify(errandEntityMock).getStakeholders();
		verifyNoMoreInteractions(accessControlServiceMock, errandEntityMock);
		verifyNoInteractions(messagingSettingsIntegrationMock, messagingClientMock);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("stakeholderEligibleForEmailNotificationArgumentProvider")
	void sendEmailNotificationWhenStakeholderNotEligibleForEmailNotification(String description, String identifierValue, StakeholderEntity stakeholderEntity, boolean eligible) {
		if (nonNull(identifierValue)) {
			Identifier.set(Identifier.parse(identifierValue));
		}

		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, RW)).thenReturn(errandEntityMock);
		when(errandEntityMock.getStakeholders()).thenReturn(isNull(stakeholderEntity) ? null : List.of(stakeholderEntity));

		communicationService.sendEmailNotificationToReporter(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, DEPARTMENT_NAME);

		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, RW);
		verify(errandEntityMock).getStakeholders();
		verifyNoMoreInteractions(accessControlServiceMock, errandEntityMock);
		verifyNoInteractions(messagingSettingsIntegrationMock, messagingClientMock);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("stakeholderWithNoEmailProvider")
	void sendEmailNotificationWhenStakeholderHasNoEmail(String description, List<ContactChannelEntity> channels) {
		Identifier.set(Identifier.parse("bcd234; type=adAccount"));

		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, RW)).thenReturn(errandEntityMock);
		when(errandEntityMock.getStakeholders()).thenReturn(List.of(StakeholderEntity.create()
			.withRole("REPORTER")
			.withParameters(List.of(StakeholderParameterEntity.create()
				.withKey("username")
				.withValues(List.of("abc123"))))
			.withContactChannels(channels)));

		communicationService.sendEmailNotificationToReporter(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, DEPARTMENT_NAME);

		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, RW);
		verify(errandEntityMock).getStakeholders();
		verifyNoMoreInteractions(accessControlServiceMock, errandEntityMock);
		verifyNoInteractions(messagingSettingsIntegrationMock, messagingClientMock);
	}

	private static Stream<Arguments> stakeholderWithNoEmailProvider() {
		return Stream.of(
			Arguments.of("Stakeholder with contact channels equal to null", null),
			Arguments.of("Stakeholder with contact channels equal to empty list", emptyList()),
			Arguments.of("Stakeholder has contact channels but no entry matching email", List.of(ContactChannelEntity.create().withType("notEmail").withValue("not-email-value"))));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("stakeholderEligibleForEmailNotificationArgumentProvider")
	void isStakeholderEligibleForEmailNotification(String description, String identifierValue, StakeholderEntity stakeholderEntity, boolean expectedOutcome) {
		if (nonNull(identifierValue)) {
			Identifier.set(Identifier.parse(identifierValue));
		}

		assertThat(communicationService.isStakeholderEligibleForEmailNotification(stakeholderEntity)).isEqualTo(expectedOutcome);
	}

	private static Stream<Arguments> stakeholderEligibleForEmailNotificationArgumentProvider() {
		final var stakeholder = StakeholderEntity.create()
			.withRole("REPORTER")
			.withParameters(List.of(StakeholderParameterEntity.create()
				.withKey("username")
				.withValues(List.of("abc123"))));

		return Stream.of(
			Arguments.of("Identifier is null", null, stakeholder, true),
			Arguments.of("Identifier with custom type", "any_identifier; type=custom", stakeholder, true),
			Arguments.of("Identifier with party id type", "054c2673-af4e-461b-9afa-c5c813303bc7; type=partyId", stakeholder, true),
			Arguments.of("Identifier with ad account type but different value as provided stakeholders ad account", "bcd234; type=adAccount", stakeholder, true),
			Arguments.of("Identifier with ad account and stakeholder is null", "abc123; type=adAccount", null, false),
			Arguments.of("Identifier with ad account and stakeholder without parameters", "ABC123; type=adAccount", StakeholderEntity.create(), false),
			Arguments.of("Identifier with ad account and stakeholder empty parameters", "abc123; type=adAccount", StakeholderEntity.create().withParameters(emptyList()), false),
			Arguments.of("Identifier with ad account type and same value as provided stakeholders ad account", "abc123; type=adAccount", stakeholder, false));
	}

	@Test
	void deleteAllCommunicationsByErrandNumber() {
		// Arrange
		final var errandNumber = "KC-23090001";
		when(communicationRepositoryMock.findByErrandNumber(errandNumber)).thenReturn(List.of(communicationEntityMock));

		// Act
		communicationService.deleteAllCommunicationsByErrandNumber(errandNumber);

		// Assert
		verify(communicationRepositoryMock).findByErrandNumber(errandNumber);
		verify(communicationRepositoryMock).deleteAll(List.of(communicationEntityMock));
		verifyNoMoreInteractions(communicationRepositoryMock);
		verifyNoInteractions(accessControlServiceMock, communicationAttachmentRepositoryMock, messagingClientMock, communicationMapperMock);
	}
}
