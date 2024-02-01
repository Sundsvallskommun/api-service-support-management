package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.emailreader.EmailReaderClient;
import se.sundsvall.supportmanagement.integration.emailreader.configuration.EmailReaderProperties;
import se.sundsvall.supportmanagement.service.CommunicationService;
import se.sundsvall.supportmanagement.service.ErrandService;

import generated.se.sundsvall.emailreader.Email;

class EmailReaderWorkerTest {

	@Mock
	private EmailReaderProperties emailReaderPropertiesMock;

	@Mock
	private EmailReaderClient emailReaderClientMock;

	@Mock
	private ErrandsRepository errandRepositoryMock;

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private CommunicationService communicationServiceMock;

	@Mock
	private EmailReaderMapper emailReaderMapperMock;

	@InjectMocks
	private EmailReaderWorker emailReaderWorker;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void shouldProcessEmails() {
		// ARRANGE
		final var email = new Email();
		email.setSubject("subject");
		email.setId("id");

		// MOCK
		when(emailReaderPropertiesMock.namespace()).thenReturn("namespace");
		when(emailReaderPropertiesMock.municipalityId()).thenReturn("municipalityId");
		when(emailReaderClientMock.getEmails(anyString(), anyString())).thenReturn(List.of(email.subject("Ärende #PRH-2022-000001 Ansökan om bygglov för fastighet KATARINA 4")));
		when(errandRepositoryMock.findByErrandNumber(anyString())).thenReturn(Optional.ofNullable(new ErrandEntity().withId("id").withStatus("SOLVED").withTouched(OffsetDateTime.now())));
		when(emailReaderMapperMock.toAttachments(any())).thenReturn(List.of());
		when(emailReaderMapperMock.toCommunicationEntity(any())).thenReturn(CommunicationEntity.create());
		when(emailReaderMapperMock.toErrand(any())).thenReturn(new Errand());

		// ACT
		emailReaderWorker.getAndProcessEmails();

		// VERIFY
		verify(emailReaderPropertiesMock, times(1)).namespace();
		verify(emailReaderPropertiesMock, times(1)).municipalityId();
		verify(emailReaderClientMock, times(1)).getEmails(anyString(), anyString());
		verify(errandRepositoryMock, times(1)).findByErrandNumber(anyString());
		verify(errandRepositoryMock, times(1)).save(any(ErrandEntity.class));
		verify(emailReaderMapperMock, times(1)).toCommunicationEntity(any(Email.class));
		verify(emailReaderClientMock, times(1)).deleteEmail(anyString());
		verify(communicationServiceMock, times(1)).saveAttachment(any(CommunicationEntity.class), any(ErrandEntity.class));
		verify(communicationServiceMock, times(1)).saveCommunication(any(CommunicationEntity.class));

		verifyNoMoreInteractions(emailReaderClientMock, errandServiceMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock);
	}


	@Test
	void processEmailWithExpiredErrand() {

		// ARRANGE
		final var email = new Email();
		email.setSubject("subject");
		email.setId("id");

		// MOCK
		when(emailReaderPropertiesMock.namespace()).thenReturn("namespace");
		when(emailReaderPropertiesMock.municipalityId()).thenReturn("municipalityId");
		when(emailReaderClientMock.getEmails(anyString(), anyString())).thenReturn(List.of(email.subject("Ärende #PRH-2022-000001 Ansökan om bygglov för fastighet KATARINA 4")));
		when(errandRepositoryMock.findByErrandNumber(anyString())).thenReturn(Optional.ofNullable(new ErrandEntity().withId("id").withStatus("SOLVED").withTouched(OffsetDateTime.now().minusDays(6))));
		when(emailReaderMapperMock.toCommunicationEntity(any())).thenReturn(CommunicationEntity.create());
		when(emailReaderPropertiesMock.errandClosedEmailSender()).thenReturn("someSender");
		when(emailReaderPropertiesMock.errandClosedEmailTemplate()).thenReturn("someTemplate");
		when(emailReaderMapperMock.createEmailRequest(any(Email.class), any(String.class), any(String.class))).thenReturn(new EmailRequest());


		// ACT
		emailReaderWorker.getAndProcessEmails();

		// VERIFY
		verify(emailReaderClientMock, times(1)).getEmails(anyString(), anyString());
		verify(errandRepositoryMock, times(1)).findByErrandNumber(anyString());
		verify(emailReaderMapperMock, times(1)).toCommunicationEntity(any(Email.class));
		verify(communicationServiceMock, times(1)).saveAttachment(any(CommunicationEntity.class), any(ErrandEntity.class));
		verify(communicationServiceMock, times(1)).saveCommunication(any(CommunicationEntity.class));
		verify(emailReaderClientMock, times(1)).deleteEmail(anyString());
		verify(emailReaderMapperMock).createEmailRequest(any(Email.class), any(String.class), any(String.class));
		verify(communicationServiceMock, times(1)).sendEmail(anyString(), anyString(), anyString(), any(EmailRequest.class));
		verifyNoMoreInteractions(emailReaderClientMock, errandServiceMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock);

	}


	@Test
	void shouldProcessEmailsWithNoErrandFound() {

		// ARRANGE
		final var email = new Email();
		email.setSubject("subject");
		email.setId("id");

		// MOCK
		when(emailReaderPropertiesMock.namespace()).thenReturn("namespace");
		when(emailReaderPropertiesMock.municipalityId()).thenReturn("municipalityId");
		when(emailReaderClientMock.getEmails(anyString(), anyString())).thenReturn(List.of(email.subject("Ärende Ansökan om bygglov för fastighet KATARINA 4")));
		when(errandServiceMock.createErrand(anyString(), anyString(), any())).thenReturn("123");
		when(errandRepositoryMock.findById(anyString())).thenReturn(Optional.ofNullable(new ErrandEntity().withId("id").withStatus("NEW")));
		when(emailReaderMapperMock.toAttachments(any())).thenReturn(List.of());
		when(emailReaderMapperMock.toCommunicationEntity(any())).thenReturn(CommunicationEntity.create());
		when(emailReaderMapperMock.toErrand(any())).thenReturn(new Errand());


		// ACT
		emailReaderWorker.getAndProcessEmails();

		// VERIFY
		verify(emailReaderPropertiesMock, times(2)).namespace();
		verify(emailReaderPropertiesMock, times(2)).municipalityId();
		verify(emailReaderClientMock, times(1)).getEmails(anyString(), anyString());
		verify(errandRepositoryMock, times(1)).findById(anyString());
		verify(errandServiceMock, times(1)).createErrand(anyString(), anyString(), any());
		verify(emailReaderMapperMock, times(1)).toErrand(any(Email.class));
		verify(emailReaderMapperMock, times(1)).toCommunicationEntity(any(Email.class));
		verify(emailReaderClientMock, times(1)).deleteEmail(anyString());
		verify(communicationServiceMock, times(1)).saveAttachment(any(CommunicationEntity.class), any(ErrandEntity.class));
		verify(communicationServiceMock, times(1)).saveCommunication(any(CommunicationEntity.class));

		verifyNoMoreInteractions(emailReaderClientMock, errandServiceMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock);

	}

}
