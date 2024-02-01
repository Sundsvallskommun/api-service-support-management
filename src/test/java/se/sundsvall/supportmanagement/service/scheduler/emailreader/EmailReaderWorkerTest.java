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
	private EmailReaderProperties emailReaderProperties;

	@Mock
	private EmailReaderClient emailReaderClient;

	@Mock
	private ErrandsRepository errandRepository;

	@Mock
	private ErrandService errandService;

	@Mock
	private CommunicationService communicationService;

	@Mock
	private EmailReaderMapper emailReaderMapper;

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
		when(emailReaderProperties.namespace()).thenReturn("namespace");
		when(emailReaderProperties.municipalityId()).thenReturn("municipalityId");
		when(emailReaderClient.getEmails(anyString(), anyString())).thenReturn(List.of(email.subject("Ärende #PRH-2022-000001 Ansökan om bygglov för fastighet KATARINA 4")));
		when(errandRepository.findByErrandNumber(anyString())).thenReturn(Optional.ofNullable(new ErrandEntity().withId("id").withStatus("SOLVED").withTouched(OffsetDateTime.now())));
		when(emailReaderMapper.toAttachments(any())).thenReturn(List.of());
		when(emailReaderMapper.toCommunicationEntity(any())).thenReturn(CommunicationEntity.create());
		when(emailReaderMapper.toErrand(any())).thenReturn(new Errand());

		// ACT
		emailReaderWorker.getAndProcessEmails();

		// VERIFY
		verify(emailReaderProperties, times(1)).namespace();
		verify(emailReaderProperties, times(1)).municipalityId();
		verify(emailReaderClient, times(1)).getEmails(anyString(), anyString());
		verify(errandRepository, times(1)).findByErrandNumber(anyString());
		verify(errandRepository, times(1)).save(any(ErrandEntity.class));
		verify(emailReaderMapper, times(1)).toCommunicationEntity(any(Email.class));
		verify(emailReaderClient, times(1)).deleteEmail(anyString());
		verify(communicationService, times(1)).saveAttachment(any(CommunicationEntity.class), any(ErrandEntity.class));
		verify(communicationService, times(1)).saveCommunication(any(CommunicationEntity.class));

		verifyNoMoreInteractions(emailReaderClient, errandService, errandRepository, emailReaderMapper, communicationService);
	}


	@Test
	void processEmailWithExpiredErrand() {

		// ARRANGE
		final var email = new Email();
		email.setSubject("subject");
		email.setId("id");

		// MOCK
		when(emailReaderProperties.namespace()).thenReturn("namespace");
		when(emailReaderProperties.municipalityId()).thenReturn("municipalityId");
		when(emailReaderClient.getEmails(anyString(), anyString())).thenReturn(List.of(email.subject("Ärende #PRH-2022-000001 Ansökan om bygglov för fastighet KATARINA 4")));
		when(errandRepository.findByErrandNumber(anyString())).thenReturn(Optional.ofNullable(new ErrandEntity().withId("id").withStatus("SOLVED").withTouched(OffsetDateTime.now().minusDays(6))));
		when(emailReaderMapper.toCommunicationEntity(any())).thenReturn(CommunicationEntity.create());
		when(emailReaderProperties.errandClosedEmailSender()).thenReturn("someSender");
		when(emailReaderProperties.errandClosedEmailTemplate()).thenReturn("someTemplate");
		when(emailReaderMapper.createEmailRequest(any(Email.class), any(String.class), any(String.class))).thenReturn(new EmailRequest());


		// ACT
		emailReaderWorker.getAndProcessEmails();

		// VERIFY
		verify(emailReaderClient, times(1)).getEmails(anyString(), anyString());
		verify(errandRepository, times(1)).findByErrandNumber(anyString());
		verify(emailReaderMapper, times(1)).toCommunicationEntity(any(Email.class));
		verify(communicationService, times(1)).saveAttachment(any(CommunicationEntity.class), any(ErrandEntity.class));
		verify(communicationService, times(1)).saveCommunication(any(CommunicationEntity.class));
		verify(emailReaderClient, times(1)).deleteEmail(anyString());
		verify(emailReaderMapper).createEmailRequest(any(Email.class), any(String.class), any(String.class));
		verify(communicationService, times(1)).sendEmail(anyString(), anyString(), anyString(), any(EmailRequest.class));
		verifyNoMoreInteractions(emailReaderClient, errandService, errandRepository, emailReaderMapper, communicationService);

	}


	@Test
	void shouldProcessEmailsWithNoErrandFound() {

		// ARRANGE
		final var email = new Email();
		email.setSubject("subject");
		email.setId("id");

		// MOCK
		when(emailReaderProperties.namespace()).thenReturn("namespace");
		when(emailReaderProperties.municipalityId()).thenReturn("municipalityId");
		when(emailReaderClient.getEmails(anyString(), anyString())).thenReturn(List.of(email.subject("Ärende Ansökan om bygglov för fastighet KATARINA 4")));
		when(errandService.createErrand(anyString(), anyString(), any())).thenReturn("123");
		when(errandRepository.findById(anyString())).thenReturn(Optional.ofNullable(new ErrandEntity().withId("id").withStatus("NEW")));
		when(emailReaderMapper.toAttachments(any())).thenReturn(List.of());
		when(emailReaderMapper.toCommunicationEntity(any())).thenReturn(CommunicationEntity.create());
		when(emailReaderMapper.toErrand(any())).thenReturn(new Errand());


		// ACT
		emailReaderWorker.getAndProcessEmails();

		// VERIFY
		verify(emailReaderProperties, times(2)).namespace();
		verify(emailReaderProperties, times(2)).municipalityId();
		verify(emailReaderClient, times(1)).getEmails(anyString(), anyString());
		verify(errandRepository, times(1)).findById(anyString());
		verify(errandService, times(1)).createErrand(anyString(), anyString(), any());
		verify(emailReaderMapper, times(1)).toErrand(any(Email.class));
		verify(emailReaderMapper, times(1)).toCommunicationEntity(any(Email.class));
		verify(emailReaderClient, times(1)).deleteEmail(anyString());
		verify(communicationService, times(1)).saveAttachment(any(CommunicationEntity.class), any(ErrandEntity.class));
		verify(communicationService, times(1)).saveCommunication(any(CommunicationEntity.class));

		verifyNoMoreInteractions(emailReaderClient, errandService, errandRepository, emailReaderMapper, communicationService);

	}

}
