package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import se.sundsvall.supportmanagement.integration.db.EmailWorkerConfigRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.EmailWorkerConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.emailreader.EmailReaderClient;
import se.sundsvall.supportmanagement.service.CommunicationService;
import se.sundsvall.supportmanagement.service.ErrandService;

import generated.se.sundsvall.emailreader.Email;

class EmailReaderWorkerTest {

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

	@Mock
	private EmailWorkerConfigRepository emailWorkerConfigRepositoryMock;

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
		email.setSubject("Ärende #PRH-2022-000001 Ansökan om bygglov för fastighet KATARINA 4");
		email.setId("id");

		final var emailConfig = EmailWorkerConfigEntity.create()
			.withEnabled(true)
			.withMunicipalityId("municipalityId")
			.withNamespace("namespace")
			.withErrandClosedEmailSender("errandClosedEmailSender")
			.withErrandClosedEmailTemplate("errandClosedEmailTemplate")
			.withDaysOfInactivityBeforeReject(5)
			.withStatusForNew("NEW")
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("ONGOING")
			.withInactiveStatus("SOLVED");

		final var errandEntity = ErrandEntity.create().withId("id").withStatus("SOLVED").withTouched(OffsetDateTime.now());
		final var communicationEntity = CommunicationEntity.create();


		// MOCK
		when(emailWorkerConfigRepositoryMock.findAll()).thenReturn(List.of(emailConfig));
		when(emailReaderClientMock.getEmails(anyString(), anyString())).thenReturn(List.of(email));
		when(errandRepositoryMock.findByErrandNumber(anyString())).thenReturn(Optional.of(errandEntity));
		when(emailReaderMapperMock.toCommunicationEntity(any())).thenReturn(communicationEntity);

		// ACT
		emailReaderWorker.getAndProcessEmails();

		// VERIFY
		verify(emailWorkerConfigRepositoryMock).findAll();
		verify(emailReaderClientMock).getEmails(emailConfig.getMunicipalityId(), emailConfig.getNamespace());
		verify(errandRepositoryMock).findByErrandNumber("PRH-2022-000001");
		verify(errandServiceMock).updateErrand(errandEntity.getNamespace(), errandEntity.getMunicipalityId(), errandEntity.getId(), Errand.create().withStatus("ONGOING"));
		verify(emailReaderMapperMock).toCommunicationEntity(same(email));
		verify(emailReaderClientMock).deleteEmail(email.getId());
		verify(communicationServiceMock).saveAttachment(same(communicationEntity), same(errandEntity));
		verify(communicationServiceMock).saveCommunication(same(communicationEntity));

		verifyNoMoreInteractions(errandServiceMock, emailReaderClientMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock, emailWorkerConfigRepositoryMock);
	}


	@Test
	void processEmailWithExpiredErrand() {

		// ARRANGE
		final var email = new Email();
		email.setSubject("Ärende #PRH-2022-000002 Ansökan om bygglov för fastighet KATARINA 4");
		email.setId("id");

		final var emailConfig = EmailWorkerConfigEntity.create()
			.withEnabled(true)
			.withMunicipalityId("municipalityId")
			.withNamespace("namespace")
			.withErrandClosedEmailSender("errandClosedEmailSender")
			.withErrandClosedEmailTemplate("errandClosedEmailTemplate")
			.withDaysOfInactivityBeforeReject(5)
			.withStatusForNew("NEW")
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("ONGOING")
			.withInactiveStatus("SOLVED");

		final var errandEntity = ErrandEntity.create().withId("id").withStatus("SOLVED").withTouched(OffsetDateTime.now().minusDays(6));
		final var communicationEntity = CommunicationEntity.create();
		final var emailRequest = new EmailRequest();

		// MOCK
		when(emailWorkerConfigRepositoryMock.findAll()).thenReturn(List.of(emailConfig));
		when(emailReaderClientMock.getEmails(anyString(), anyString())).thenReturn(List.of(email));
		when(errandRepositoryMock.findByErrandNumber(anyString())).thenReturn(Optional.of(errandEntity));
		when(emailReaderMapperMock.toCommunicationEntity(any())).thenReturn(communicationEntity);
		when(emailReaderMapperMock.createEmailRequest(any(Email.class), any(String.class), any(String.class))).thenReturn(emailRequest);


		// ACT
		emailReaderWorker.getAndProcessEmails();

		// VERIFY
		verify(emailWorkerConfigRepositoryMock).findAll();
		verify(emailReaderClientMock).getEmails(emailConfig.getMunicipalityId(), emailConfig.getNamespace());
		verify(errandRepositoryMock).findByErrandNumber("PRH-2022-000002");
		verify(emailReaderMapperMock).createEmailRequest(same(email), eq(emailConfig.getErrandClosedEmailSender()), eq(emailConfig.getErrandClosedEmailTemplate()));
		verify(communicationServiceMock).sendEmail(eq(emailConfig.getNamespace()), eq(emailConfig.getMunicipalityId()), eq(errandEntity.getId()), same(emailRequest));
		verify(emailReaderMapperMock).toCommunicationEntity(same(email));
		verify(communicationServiceMock).saveAttachment(same(communicationEntity), same(errandEntity));
		verify(communicationServiceMock).saveCommunication(same(communicationEntity));
		verify(emailReaderClientMock).deleteEmail(email.getId());

		verifyNoInteractions(errandServiceMock);
		verifyNoMoreInteractions(emailReaderClientMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock, emailWorkerConfigRepositoryMock);

	}


	@Test
	void shouldProcessEmailsWithNoErrandFound() {

		// ARRANGE
		final var email = new Email();
		email.setSubject("Ansökan om bygglov för fastighet KATARINA 4");
		email.setId("emailId");

		final var emailConfig = EmailWorkerConfigEntity.create()
			.withEnabled(true)
			.withMunicipalityId("municipalityId")
			.withNamespace("namespace")
			.withErrandClosedEmailSender("errandClosedEmailSender")
			.withErrandClosedEmailTemplate("errandClosedEmailTemplate")
			.withDaysOfInactivityBeforeReject(5)
			.withStatusForNew("NEW")
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("ONGOING")
			.withInactiveStatus("SOLVED")
			.withAddSenderAsStakeholder(true)
			.withStakeholderRole("stakeholderRole")
			.withErrandChannel("errandChannel");

		final var errandEntity = new ErrandEntity().withId("errandId").withStatus("NEW");
		final var communicationEntity = CommunicationEntity.create();
		final var errand = new Errand();

		// MOCK
		when(emailWorkerConfigRepositoryMock.findAll()).thenReturn(List.of(emailConfig));
		when(emailReaderClientMock.getEmails(anyString(), anyString())).thenReturn(List.of(email));
		when(errandServiceMock.createErrand(anyString(), anyString(), any())).thenReturn(errandEntity.getId());
		when(errandRepositoryMock.findById(anyString())).thenReturn(Optional.of(errandEntity));
		when(emailReaderMapperMock.toCommunicationEntity(any())).thenReturn(communicationEntity);
		when(emailReaderMapperMock.toErrand(any(), any(), anyBoolean(), any(), any())).thenReturn(errand);

		// ACT
		emailReaderWorker.getAndProcessEmails();

		// VERIFY
		verify(emailWorkerConfigRepositoryMock).findAll();
		verify(emailReaderClientMock).getEmails(emailConfig.getMunicipalityId(), emailConfig.getNamespace());
		verify(errandRepositoryMock).findById(errandEntity.getId());
		verify(emailReaderMapperMock).toErrand(same(email), eq(emailConfig.getStatusForNew()), eq(emailConfig.isAddSenderAsStakeholder()), eq(emailConfig.getStakeholderRole()), eq(emailConfig.getErrandChannel()));
		verify(errandServiceMock).createErrand(eq(emailConfig.getNamespace()), eq(emailConfig.getMunicipalityId()), same(errand));
		verify(emailReaderMapperMock).toCommunicationEntity(same(email));
		verify(emailReaderClientMock).deleteEmail(email.getId());
		verify(communicationServiceMock).saveAttachment(same(communicationEntity), same(errandEntity));
		verify(communicationServiceMock).saveCommunication(same(communicationEntity));

		verifyNoMoreInteractions(emailWorkerConfigRepositoryMock, emailReaderClientMock, errandServiceMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock);

	}

	@Test
	void shouldNotProcessAnyEmails() {
		// ARRANGE
		final var emailConfig = EmailWorkerConfigEntity.create()
			.withEnabled(false);

		// MOCK
		when(emailWorkerConfigRepositoryMock.findAll()).thenReturn(List.of(emailConfig));

		// ACT
		emailReaderWorker.getAndProcessEmails();

		// VERIFY
		verify(emailWorkerConfigRepositoryMock).findAll();
		verifyNoMoreInteractions(emailWorkerConfigRepositoryMock);
		verifyNoInteractions(emailReaderClientMock, errandServiceMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock);
	}
}
