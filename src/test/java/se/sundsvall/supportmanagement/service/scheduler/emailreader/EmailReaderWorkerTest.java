package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import generated.se.sundsvall.emailreader.Email;
import generated.se.sundsvall.eventlog.EventType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.integration.db.EmailWorkerConfigRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.EmailWorkerConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.util.ErrandNumberGeneratorService;
import se.sundsvall.supportmanagement.integration.emailreader.EmailReaderClient;
import se.sundsvall.supportmanagement.service.CommunicationService;
import se.sundsvall.supportmanagement.service.ErrandService;
import se.sundsvall.supportmanagement.service.EventService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType.MESSAGE;

class EmailReaderWorkerTest {

	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String NAMESPACE = "namespace";

	@Mock
	private EventService eventServiceMock;

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

	@Mock
	private ErrandNumberGeneratorService errandNumberGeneratorServiceMock;

	@Mock
	private Consumer<String> consumerMock;

	@InjectMocks
	private EmailReaderWorker emailReaderWorker;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void getEnabledEmailConfigs() {
		// ARRANGE
		final var config1 = EmailWorkerConfigEntity.create().withEnabled(true);
		final var config2 = EmailWorkerConfigEntity.create().withEnabled(false);
		// MOCK
		when(emailWorkerConfigRepositoryMock.findAll()).thenReturn(List.of(config1, config2));
		// ACT & VERIFY
		assertThat(emailReaderWorker.getEnabledEmailConfigs()).containsExactly(config1);
	}

	@Test
	void getEmailsFromConfig() {
		// ARRANGE
		final var email = new Email();
		final var config = EmailWorkerConfigEntity.create().withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE);
		// MOCK
		when(emailReaderClientMock.getEmails(any(), any())).thenReturn(List.of(email));
		// ACT
		final var result = emailReaderWorker.getEmailsFromConfig(config);

		// VERIFY
		verify(emailReaderClientMock).getEmails(MUNICIPALITY_ID, NAMESPACE);
		assertThat(result).containsExactly(email);
	}

	@Test
	void shouldProcessEmails() {
		// ARRANGE
		final var email = new Email();
		email.setSubject("Ärende #PRH-2022-000001 Ansökan om bygglov för fastighet KATARINA 4");
		email.setId("id");
		final var bytes = "someFileContet".getBytes();

		final var emailConfig = EmailWorkerConfigEntity.create()
			.withEnabled(true)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandClosedEmailSender("errandClosedEmailSender")
			.withErrandClosedEmailTemplate("errandClosedEmailTemplate")
			.withDaysOfInactivityBeforeReject(5)
			.withStatusForNew("NEW")
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("ONGOING")
			.withInactiveStatus("SOLVED");

		final var errandEntity = ErrandEntity.create().withId("id").withStatus("SOLVED").withCreated(OffsetDateTime.now().minusDays(1)).withModified(OffsetDateTime.now()).withTouched(OffsetDateTime.now());
		final var communicationEntity = CommunicationEntity.create().withAttachments(List.of(CommunicationAttachmentEntity
			.create().withForeignId("2").withMunicipalityId(MUNICIPALITY_ID)));

		// MOCK
		when(errandRepositoryMock.findByErrandNumberAndNamespace(anyString(), anyString())).thenReturn(Optional.of(errandEntity));
		when(emailReaderMapperMock.toCommunicationEntity(any(), any())).thenReturn(communicationEntity);
		when(emailReaderClientMock.getAttachment(MUNICIPALITY_ID, 2)).thenReturn(bytes);

		// ACT
		emailReaderWorker.processEmail(email, emailConfig, consumerMock);

		// VERIFY
		verify(errandRepositoryMock).findByErrandNumberAndNamespace("PRH-2022-000001", NAMESPACE);
		verify(errandRepositoryMock).save(same(errandEntity));
		verify(emailReaderMapperMock).toCommunicationEntity(same(email), same(errandEntity));
		verify(emailReaderClientMock).deleteEmail(MUNICIPALITY_ID, email.getId());
		verify(communicationServiceMock).saveAttachment(same(communicationEntity), same(errandEntity));
		verify(communicationServiceMock).saveCommunication(same(communicationEntity));
		verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq("Nytt meddelande"), same(errandEntity), isNull(), isNull(), eq(MESSAGE));
		verify(emailReaderClientMock).getAttachment(MUNICIPALITY_ID, 2);
		verify(emailReaderMapperMock).toAttachmentDataEntity(bytes);
		verifyNoInteractions(errandServiceMock);
		verifyNoMoreInteractions(emailReaderClientMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock, emailWorkerConfigRepositoryMock, eventServiceMock, consumerMock);
	}

	@Test
	void shouldProcessEmailsWhenNullInAttachmentData() {
		// ARRANGE
		final var email = new Email();
		email.setSubject("Ärende #PRH-2022-000002 Ansökan om bygglov för fastighet KATARINA 3");
		email.setId("id");
		final var emailConfig = EmailWorkerConfigEntity.create()
			.withEnabled(true)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandClosedEmailSender("errandClosedEmailSender")
			.withErrandClosedEmailTemplate("errandClosedEmailTemplate")
			.withDaysOfInactivityBeforeReject(5)
			.withStatusForNew("NEW")
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("ONGOING")
			.withInactiveStatus("SOLVED");

		final var errandEntity = ErrandEntity.create().withId("id").withStatus("SOLVED").withCreated(OffsetDateTime.now().minusDays(1)).withModified(OffsetDateTime.now()).withTouched(OffsetDateTime.now());
		final var communicationEntity = CommunicationEntity.create().withAttachments(List.of(CommunicationAttachmentEntity
			.create().withForeignId("2").withMunicipalityId(MUNICIPALITY_ID)));

		// MOCK
		when(errandRepositoryMock.findByErrandNumberAndNamespace(anyString(), anyString())).thenReturn(Optional.of(errandEntity));
		when(emailReaderMapperMock.toCommunicationEntity(any(), any())).thenReturn(communicationEntity);
		when(emailReaderClientMock.getAttachment(MUNICIPALITY_ID, 2)).thenReturn(null);

		// ACT
		emailReaderWorker.processEmail(email, emailConfig, consumerMock);

		// VERIFY
		verify(errandRepositoryMock).findByErrandNumberAndNamespace("PRH-2022-000002", NAMESPACE);
		verify(errandRepositoryMock).save(same(errandEntity));
		verify(emailReaderMapperMock).toCommunicationEntity(same(email), same(errandEntity));
		verify(emailReaderClientMock).deleteEmail(MUNICIPALITY_ID, email.getId());
		verify(communicationServiceMock).saveAttachment(same(communicationEntity), same(errandEntity));
		verify(communicationServiceMock).saveCommunication(same(communicationEntity));
		verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq("Nytt meddelande"), same(errandEntity), isNull(), isNull(), eq(MESSAGE));
		verify(emailReaderClientMock).getAttachment(MUNICIPALITY_ID, 2);
		verify(emailReaderMapperMock).toAttachmentDataEntity(new byte[0]); // Handle null case by returning an empty byte array
		verifyNoInteractions(errandServiceMock);
		verifyNoMoreInteractions(emailReaderClientMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock, emailWorkerConfigRepositoryMock, eventServiceMock, consumerMock);
	}

	@Test
	void processEmailWithExpiredErrand() {

		// ARRANGE
		final var email = new Email();
		email.setSubject("Ärende #PRH-2022-000002 Ansökan om bygglov för fastighet KATARINA 4");
		email.setId("id");
		email.setSender("user@domain.com");

		final var emailConfig = EmailWorkerConfigEntity.create()
			.withEnabled(true)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandClosedEmailSender("errandClosedEmailSender")
			.withErrandClosedEmailTemplate("errandClosedEmailTemplate")
			.withErrandClosedEmailHTMLTemplate("errandClosedEmailHTMLTemplate")
			.withDaysOfInactivityBeforeReject(5)
			.withStatusForNew("NEW")
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("ONGOING")
			.withInactiveStatus("SOLVED");

		final var errandEntity = ErrandEntity.create().withId("id").withStatus("SOLVED").withCreated(OffsetDateTime.now().minusDays(6)).withTouched(OffsetDateTime.now().minusDays(6));
		final var communicationEntity = CommunicationEntity.create();
		final var emailRequest = new EmailRequest();

		// MOCK
		when(errandRepositoryMock.findByErrandNumberAndNamespace(anyString(), anyString())).thenReturn(Optional.of(errandEntity));
		when(emailReaderMapperMock.toCommunicationEntity(any(), any())).thenReturn(communicationEntity);
		when(emailReaderMapperMock.createEmailRequest(any(Email.class), any(String.class), any(String.class), any(String.class), any(String.class))).thenReturn(emailRequest);

		// ACT
		emailReaderWorker.processEmail(email, emailConfig, consumerMock);

		// VERIFY
		verify(errandRepositoryMock).findByErrandNumberAndNamespace("PRH-2022-000002", NAMESPACE);
		verify(emailReaderMapperMock).createEmailRequest(same(email), eq(emailConfig.getErrandClosedEmailSender()), eq(emailConfig.getErrandClosedEmailTemplate()), eq(emailConfig.getErrandClosedEmailHTMLTemplate()),
			eq("Ärende #PRH-2022-000002 Ansökan om bygglov för fastighet KATARINA 4"));
		verify(communicationServiceMock).sendEmail(eq(errandEntity), same(emailRequest));
		verify(emailReaderMapperMock).toCommunicationEntity(same(email), same(errandEntity));
		verify(communicationServiceMock).saveAttachment(same(communicationEntity), same(errandEntity));
		verify(communicationServiceMock).saveCommunication(same(communicationEntity));
		verify(emailReaderClientMock).deleteEmail(MUNICIPALITY_ID, email.getId());
		verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq("Nytt meddelande"), same(errandEntity), isNull(), isNull(), eq(MESSAGE));
		verifyNoInteractions(errandServiceMock);
		verifyNoMoreInteractions(emailReaderClientMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock, emailWorkerConfigRepositoryMock, eventServiceMock);
	}

	@Test
	void processEmailWithExpiredErrandAndInvalidSenderAddress() {

		// ARRANGE
		final var email = new Email();
		email.setSubject("Ärende #PRH-2022-000002 Ansökan om bygglov för fastighet KATARINA 4");
		email.setId("id");
		email.setSender("invalid");

		final var emailConfig = EmailWorkerConfigEntity.create()
			.withEnabled(true)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandClosedEmailSender("errandClosedEmailSender")
			.withErrandClosedEmailTemplate("errandClosedEmailTemplate")
			.withDaysOfInactivityBeforeReject(5)
			.withStatusForNew("NEW")
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("ONGOING")
			.withInactiveStatus("SOLVED");

		final var errandEntity = ErrandEntity.create().withId("id").withStatus("SOLVED").withCreated(OffsetDateTime.now().minusDays(6)).withTouched(OffsetDateTime.now().minusDays(6));
		final var communicationEntity = CommunicationEntity.create();

		// MOCK
		when(errandRepositoryMock.findByErrandNumberAndNamespace(anyString(), anyString())).thenReturn(Optional.of(errandEntity));
		when(emailReaderMapperMock.toCommunicationEntity(any(), any())).thenReturn(communicationEntity);

		// ACT
		emailReaderWorker.processEmail(email, emailConfig, consumerMock);

		// VERIFY - errand is processed but no confirmation email sent (invalid sender suppressed)
		verify(errandRepositoryMock).findByErrandNumberAndNamespace("PRH-2022-000002", NAMESPACE);
		verify(emailReaderMapperMock, never()).createEmailRequest(any(), any(), any(), any(), any());
		verify(communicationServiceMock, never()).sendEmail(any(ErrandEntity.class), any());
		verify(emailReaderMapperMock).toCommunicationEntity(same(email), same(errandEntity));
		verify(communicationServiceMock).saveAttachment(same(communicationEntity), same(errandEntity));
		verify(communicationServiceMock).saveCommunication(same(communicationEntity));
		verify(emailReaderClientMock).deleteEmail(MUNICIPALITY_ID, email.getId());
		verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq("Nytt meddelande"), same(errandEntity), isNull(), isNull(), eq(MESSAGE));
		verifyNoInteractions(errandServiceMock);
		verifyNoMoreInteractions(emailReaderClientMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock, emailWorkerConfigRepositoryMock, eventServiceMock);
	}

	@Test
	void processEmailWithNewErrand() {

		// ARRANGE
		final var email = new Email();
		email.setSender("user@domain.com");
		email.setSubject("Ansökan om bygglov för fastighet KATARINA 4");
		email.setId("id");

		final var emailConfig = EmailWorkerConfigEntity.create()
			.withEnabled(true)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandClosedEmailSender("errandClosedEmailSender")
			.withErrandClosedEmailTemplate("errandClosedEmailTemplate")
			.withErrandClosedEmailHTMLTemplate("errandClosedEmailHTMLTemplate")
			.withErrandNewEmailSender("errandNewEmailSender")
			.withErrandNewEmailTemplate("errandNewEmailTemplate")
			.withErrandNewEmailHTMLTemplate("errandNewEmailHTMLTemplate")
			.withDaysOfInactivityBeforeReject(5)
			.withStatusForNew("NEW")
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("ONGOING")
			.withInactiveStatus("SOLVED");

		final var errandEntity = ErrandEntity.create().withId("id").withErrandNumber("errandNumber").withStatus("NEW").withCreated(OffsetDateTime.now());
		final var communicationEntity = CommunicationEntity.create();
		final var emailRequest = new EmailRequest().withEmailHeaders(Map.of());

		// MOCK
		when(errandRepositoryMock.findById(anyString())).thenReturn(Optional.of(errandEntity));
		when(errandNumberGeneratorServiceMock.generateErrandNumber(anyString(), anyString())).thenReturn("errandNumber");
		when(errandServiceMock.createErrand(anyString(), anyString(), any())).thenReturn(errandEntity.getId());
		when(emailReaderMapperMock.toErrand(any(), any())).thenReturn(new Errand());
		when(emailReaderMapperMock.toCommunicationEntity(any(), any())).thenReturn(communicationEntity);
		when(emailReaderMapperMock.createEmailRequest(any(Email.class), any(String.class), any(String.class), any(String.class), any(String.class))).thenReturn(emailRequest);

		// ACT
		emailReaderWorker.processEmail(email, emailConfig, consumerMock);

		// VERIFY
		verify(errandRepositoryMock).findById(errandEntity.getId());
		verify(emailReaderMapperMock).createEmailRequest(email.subject("Bekräftelse ärende #errandNumber Ansökan om bygglov för fastighet KATARINA 4"), emailConfig.getErrandNewEmailSender(), emailConfig.getErrandNewEmailTemplate(),
			emailConfig.getErrandNewEmailHTMLTemplate(), "Bekräftelse ärende #errandNumber Ansökan om bygglov för fastighet KATARINA 4");
		verify(emailReaderMapperMock).toErrand(same(email), eq(emailConfig));
		verify(communicationServiceMock).sendEmail(eq(errandEntity), same(emailRequest));
		verify(emailReaderMapperMock).toCommunicationEntity(same(email), same(errandEntity));
		verify(communicationServiceMock).saveAttachment(same(communicationEntity), same(errandEntity));
		verify(communicationServiceMock).saveCommunication(same(communicationEntity));
		verify(emailReaderClientMock).deleteEmail(MUNICIPALITY_ID, email.getId());
		verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq("Nytt meddelande"), same(errandEntity), isNull(), isNull(), eq(MESSAGE));
		verifyNoMoreInteractions(emailReaderClientMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock, emailWorkerConfigRepositoryMock, eventServiceMock);
	}

	@Test
	void processEmailWithNewErrandAndInvalidSenderAddress() {

		// ARRANGE
		final var email = new Email();
		email.setSender("invalid");
		email.setSubject("Ansökan om bygglov för fastighet KATARINA 4");
		email.setId("id");

		final var emailConfig = EmailWorkerConfigEntity.create()
			.withEnabled(true)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandClosedEmailSender("errandClosedEmailSender")
			.withErrandClosedEmailTemplate("errandClosedEmailTemplate")
			.withErrandNewEmailSender("errandNewEmailSender")
			.withErrandNewEmailTemplate("errandNewEmailTemplate")
			.withDaysOfInactivityBeforeReject(5)
			.withStatusForNew("NEW")
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("ONGOING")
			.withInactiveStatus("SOLVED");

		final var errandEntity = ErrandEntity.create().withId("id").withErrandNumber("errandNumber").withStatus("NEW").withCreated(OffsetDateTime.now());
		final var communicationEntity = CommunicationEntity.create();

		// MOCK
		when(errandRepositoryMock.findById(anyString())).thenReturn(Optional.of(errandEntity));
		when(errandNumberGeneratorServiceMock.generateErrandNumber(anyString(), anyString())).thenReturn("errandNumber");
		when(errandServiceMock.createErrand(anyString(), anyString(), any())).thenReturn(errandEntity.getId());
		when(emailReaderMapperMock.toErrand(any(), any())).thenReturn(new Errand());
		when(emailReaderMapperMock.toCommunicationEntity(any(), any())).thenReturn(communicationEntity);

		// ACT
		emailReaderWorker.processEmail(email, emailConfig, consumerMock);

		// VERIFY - errand is created but no confirmation email sent (invalid sender suppressed)
		verify(errandRepositoryMock).findById(errandEntity.getId());
		verify(emailReaderMapperMock, never()).createEmailRequest(any(), any(), any(), any(), any());
		verify(emailReaderMapperMock).toErrand(same(email), same(emailConfig));
		verify(communicationServiceMock, never()).sendEmail(any(ErrandEntity.class), any());
		verify(emailReaderMapperMock).toCommunicationEntity(same(email), same(errandEntity));
		verify(communicationServiceMock).saveAttachment(same(communicationEntity), same(errandEntity));
		verify(communicationServiceMock).saveCommunication(same(communicationEntity));
		verify(emailReaderClientMock).deleteEmail(MUNICIPALITY_ID, email.getId());
		verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq("Nytt meddelande"), same(errandEntity), isNull(), isNull(), eq(MESSAGE));

		verifyNoMoreInteractions(emailReaderClientMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock, emailWorkerConfigRepositoryMock, eventServiceMock);
	}

	@Test
	void processEmailWithNewErrandNotSending() {

		// ARRANGE
		final var email = new Email();
		email.setSubject("Ansökan om bygglov för fastighet KATARINA 4");
		email.setId("id");

		final var emailConfig = EmailWorkerConfigEntity.create()
			.withEnabled(true)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withDaysOfInactivityBeforeReject(5)
			.withStatusForNew("NEW")
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("ONGOING")
			.withInactiveStatus("SOLVED");

		final var errandEntity = ErrandEntity.create().withId("id").withErrandNumber("errandNumber").withStatus("NEW").withCreated(OffsetDateTime.now());
		final var communicationEntity = CommunicationEntity.create();

		// MOCK
		when(errandRepositoryMock.findById(anyString())).thenReturn(Optional.of(errandEntity));
		when(errandNumberGeneratorServiceMock.generateErrandNumber(anyString(), anyString())).thenReturn("errandNumber");
		when(errandServiceMock.createErrand(anyString(), anyString(), any())).thenReturn(errandEntity.getId());
		when(emailReaderMapperMock.toErrand(any(), any())).thenReturn(new Errand());
		when(emailReaderMapperMock.toCommunicationEntity(any(), any())).thenReturn(communicationEntity);

		// ACT
		emailReaderWorker.processEmail(email, emailConfig, consumerMock);

		// VERIFY
		verify(errandRepositoryMock).findById(errandEntity.getId());
		verify(communicationServiceMock).saveAttachment(same(communicationEntity), same(errandEntity));
		verify(communicationServiceMock).saveCommunication(same(communicationEntity));
		verify(emailReaderClientMock).deleteEmail(MUNICIPALITY_ID, email.getId());
		verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq("Nytt meddelande"), same(errandEntity), isNull(), isNull(), eq(MESSAGE));
		verify(emailReaderMapperMock).toErrand(same(email), same(emailConfig));
		verify(emailReaderMapperMock).toCommunicationEntity(same(email), same(errandEntity));
		verifyNoMoreInteractions(emailReaderClientMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock, emailWorkerConfigRepositoryMock, eventServiceMock);

	}

	@Test
	void shouldProcessEmailsWithNoErrandFound() {

		// ARRANGE
		final var email = new Email();
		email.setSubject("Ansökan om bygglov för fastighet KATARINA 4");
		email.setId("emailId");

		final var emailConfig = EmailWorkerConfigEntity.create()
			.withEnabled(true)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
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
		when(errandServiceMock.createErrand(anyString(), anyString(), any())).thenReturn(errandEntity.getId());
		when(errandRepositoryMock.findById(anyString())).thenReturn(Optional.of(errandEntity));
		when(emailReaderMapperMock.toCommunicationEntity(any(), any())).thenReturn(communicationEntity);
		when(emailReaderMapperMock.toErrand(any(), any())).thenReturn(errand);

		// ACT
		emailReaderWorker.processEmail(email, emailConfig, consumerMock);

		// VERIFY
		verify(errandRepositoryMock).findById(errandEntity.getId());
		verify(emailReaderMapperMock).toErrand(same(email), same(emailConfig));
		verify(errandServiceMock).createErrand(eq(emailConfig.getNamespace()), eq(emailConfig.getMunicipalityId()), same(errand));
		verify(emailReaderMapperMock).toCommunicationEntity(same(email), same(errandEntity));
		verify(emailReaderClientMock).deleteEmail(MUNICIPALITY_ID, email.getId());
		verify(communicationServiceMock).saveAttachment(same(communicationEntity), same(errandEntity));
		verify(communicationServiceMock).saveCommunication(same(communicationEntity));
		verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq("Nytt meddelande"), same(errandEntity), isNull(), isNull(), eq(MESSAGE));
		verifyNoMoreInteractions(emailWorkerConfigRepositoryMock, emailReaderClientMock, errandServiceMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock, eventServiceMock);

	}

	@Test
	void shouldProcessEmailsWithNoErrandFoundWithHashInSubject() {

		// ARRANGE
		final var email = new Email();
		email.setSubject("#Ansökan om bygglov för fastighet KATARINA 4");
		email.setId("emailId");

		final var emailConfig = EmailWorkerConfigEntity.create()
			.withEnabled(true)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
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
		when(errandRepositoryMock.findByErrandNumberAndNamespace(any(), any())).thenReturn(Optional.empty());
		when(errandServiceMock.createErrand(anyString(), anyString(), any())).thenReturn(errandEntity.getId());
		when(errandRepositoryMock.findById(anyString())).thenReturn(Optional.of(errandEntity));
		when(emailReaderMapperMock.toCommunicationEntity(any(), any())).thenReturn(communicationEntity);
		when(emailReaderMapperMock.toErrand(any(), any())).thenReturn(errand);

		// ACT
		emailReaderWorker.processEmail(email, emailConfig, consumerMock);

		// VERIFY
		verify(errandRepositoryMock).findByErrandNumberAndNamespace("Ansökan", NAMESPACE);
		verify(errandRepositoryMock).findById(errandEntity.getId());
		verify(emailReaderMapperMock).toErrand(same(email), same(emailConfig));
		verify(errandServiceMock).createErrand(eq(emailConfig.getNamespace()), eq(emailConfig.getMunicipalityId()), same(errand));
		verify(emailReaderMapperMock).toCommunicationEntity(same(email), same(errandEntity));
		verify(emailReaderClientMock).deleteEmail(MUNICIPALITY_ID, email.getId());
		verify(communicationServiceMock).saveAttachment(same(communicationEntity), same(errandEntity));
		verify(communicationServiceMock).saveCommunication(same(communicationEntity));
		verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq("Nytt meddelande"), same(errandEntity), isNull(), isNull(), eq(MESSAGE));
		verifyNoMoreInteractions(emailWorkerConfigRepositoryMock, emailReaderClientMock, errandServiceMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock, eventServiceMock);
	}

	@Test
	void processEmailAutoReplyIgnored() {

		// ARRANGE
		final var email = new Email();
		email.setSubject("Out of office");
		email.setId("id");
		email.setHeaders(Map.of("AUTO_SUBMITTED", List.of("auto-replied")));

		final var emailConfig = EmailWorkerConfigEntity.create()
			.withEnabled(true)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withStatusForNew("NEW")
			.withIgnoreAutoReply(true);

		// ACT
		emailReaderWorker.processEmail(email, emailConfig, consumerMock);

		// VERIFY
		verify(emailReaderClientMock).deleteEmail(MUNICIPALITY_ID, email.getId());
		verifyNoInteractions(errandRepositoryMock, errandServiceMock, communicationServiceMock, eventServiceMock, emailReaderMapperMock);
		verifyNoMoreInteractions(emailReaderClientMock);
	}

	@Test
	void processEmailAutoReplyNotIgnoredWhenConfigDisabled() {

		// ARRANGE
		final var email = new Email();
		email.setSubject("Ärende #PRH-2022-000001 Out of office");
		email.setId("id");
		email.setHeaders(Map.of("AUTO_SUBMITTED", List.of("auto-replied")));

		final var emailConfig = EmailWorkerConfigEntity.create()
			.withEnabled(true)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withStatusForNew("NEW")
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("ONGOING")
			.withIgnoreAutoReply(false);

		final var errandEntity = ErrandEntity.create().withId("id").withStatus("SOLVED").withCreated(OffsetDateTime.now().minusDays(1)).withModified(OffsetDateTime.now()).withTouched(OffsetDateTime.now());
		final var communicationEntity = CommunicationEntity.create();

		// MOCK
		when(errandRepositoryMock.findByErrandNumberAndNamespace(anyString(), anyString())).thenReturn(Optional.of(errandEntity));
		when(emailReaderMapperMock.toCommunicationEntity(any(), any())).thenReturn(communicationEntity);

		// ACT
		emailReaderWorker.processEmail(email, emailConfig, consumerMock);

		// VERIFY - email should be processed normally
		verify(errandRepositoryMock).findByErrandNumberAndNamespace("PRH-2022-000001", NAMESPACE);
		verify(errandRepositoryMock).save(same(errandEntity));
		verify(emailReaderMapperMock).toCommunicationEntity(same(email), same(errandEntity));
		verify(emailReaderClientMock).deleteEmail(MUNICIPALITY_ID, email.getId());
		verify(communicationServiceMock).saveCommunication(same(communicationEntity));
		verify(communicationServiceMock).saveAttachment(same(communicationEntity), same(errandEntity));
		verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq("Nytt meddelande"), same(errandEntity), isNull(), isNull(), eq(MESSAGE));
		verifyNoInteractions(errandServiceMock);
		verifyNoMoreInteractions(emailReaderClientMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock, emailWorkerConfigRepositoryMock, eventServiceMock, consumerMock);
	}

	@Test
	void processEmailDeliveryStatusNotIgnoredEvenWithAutoReplyConfig() {

		// ARRANGE
		final var email = new Email();
		email.setSubject("Ärende #PRH-2022-000001 Delivery Status Notification");
		email.setId("id");
		email.setHeaders(Map.of(
			"AUTO_SUBMITTED", List.of("auto-replied"),
			"CONTENT_TYPE", List.of("multipart/report; report-type=delivery-status")));

		final var emailConfig = EmailWorkerConfigEntity.create()
			.withEnabled(true)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withStatusForNew("NEW")
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("ONGOING")
			.withIgnoreAutoReply(true);

		final var errandEntity = ErrandEntity.create().withId("id").withStatus("SOLVED").withCreated(OffsetDateTime.now().minusDays(1)).withModified(OffsetDateTime.now()).withTouched(OffsetDateTime.now());
		final var communicationEntity = CommunicationEntity.create();

		// MOCK
		when(errandRepositoryMock.findByErrandNumberAndNamespace(anyString(), anyString())).thenReturn(Optional.of(errandEntity));
		when(emailReaderMapperMock.toCommunicationEntity(any(), any())).thenReturn(communicationEntity);

		// ACT
		emailReaderWorker.processEmail(email, emailConfig, consumerMock);

		// VERIFY - delivery-status should be processed despite auto-reply header
		verify(errandRepositoryMock).findByErrandNumberAndNamespace("PRH-2022-000001", NAMESPACE);
		verify(errandRepositoryMock).save(same(errandEntity));
		verify(emailReaderMapperMock).toCommunicationEntity(same(email), same(errandEntity));
		verify(emailReaderClientMock).deleteEmail(MUNICIPALITY_ID, email.getId());
		verify(communicationServiceMock).saveCommunication(same(communicationEntity));
		verify(communicationServiceMock).saveAttachment(same(communicationEntity), same(errandEntity));
		verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq("Nytt meddelande"), same(errandEntity), isNull(), isNull(), eq(MESSAGE));
		verifyNoInteractions(errandServiceMock);
		verifyNoMoreInteractions(emailReaderClientMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock, emailWorkerConfigRepositoryMock, eventServiceMock, consumerMock);
	}

	@Test
	void processEmailWithSystemMessageOnClosedErrand() {

		// ARRANGE
		final var email = new Email();
		email.setSubject("Ärende #PRH-2022-000002 Mailbox almost full");
		email.setId("id");
		email.setSender("postmaster@domain.com");
		email.setHeaders(Map.of("RETURN_PATH", List.of("<>")));

		final var emailConfig = EmailWorkerConfigEntity.create()
			.withEnabled(true)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandClosedEmailSender("errandClosedEmailSender")
			.withErrandClosedEmailTemplate("errandClosedEmailTemplate")
			.withErrandClosedEmailHTMLTemplate("errandClosedEmailHTMLTemplate")
			.withDaysOfInactivityBeforeReject(5)
			.withStatusForNew("NEW")
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("ONGOING")
			.withInactiveStatus("SOLVED");

		final var errandEntity = ErrandEntity.create().withId("id").withStatus("SOLVED").withCreated(OffsetDateTime.now().minusDays(6)).withTouched(OffsetDateTime.now().minusDays(6));
		final var communicationEntity = CommunicationEntity.create();

		// MOCK
		when(errandRepositoryMock.findByErrandNumberAndNamespace(anyString(), anyString())).thenReturn(Optional.of(errandEntity));
		when(emailReaderMapperMock.toCommunicationEntity(any(), any())).thenReturn(communicationEntity);

		// ACT
		emailReaderWorker.processEmail(email, emailConfig, consumerMock);

		// VERIFY - no closed-errand email should be sent for system messages
		verify(errandRepositoryMock).findByErrandNumberAndNamespace("PRH-2022-000002", NAMESPACE);
		verify(emailReaderMapperMock, never()).createEmailRequest(any(), any(), any(), any(), any());
		verify(communicationServiceMock, never()).sendEmail(any(ErrandEntity.class), any());
		verify(emailReaderMapperMock).toCommunicationEntity(same(email), same(errandEntity));
		verify(communicationServiceMock).saveAttachment(same(communicationEntity), same(errandEntity));
		verify(communicationServiceMock).saveCommunication(same(communicationEntity));
		verify(emailReaderClientMock).deleteEmail(MUNICIPALITY_ID, email.getId());
		verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq("Nytt meddelande"), same(errandEntity), isNull(), isNull(), eq(MESSAGE));
		verifyNoInteractions(errandServiceMock);
		verifyNoMoreInteractions(emailReaderClientMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock, emailWorkerConfigRepositoryMock, eventServiceMock);
	}

	@Test
	void processEmailWithSystemMessageOnNewErrand() {

		// ARRANGE
		final var email = new Email();
		email.setSender("postmaster@domain.com");
		email.setSubject("Ansökan om bygglov");
		email.setId("id");
		email.setHeaders(Map.of("RETURN_PATH", List.of("<>")));

		final var emailConfig = EmailWorkerConfigEntity.create()
			.withEnabled(true)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandNewEmailSender("errandNewEmailSender")
			.withErrandNewEmailTemplate("errandNewEmailTemplate")
			.withErrandNewEmailHTMLTemplate("errandNewEmailHTMLTemplate")
			.withDaysOfInactivityBeforeReject(5)
			.withStatusForNew("NEW")
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("ONGOING")
			.withInactiveStatus("SOLVED");

		final var errandEntity = ErrandEntity.create().withId("id").withErrandNumber("errandNumber").withStatus("NEW").withCreated(OffsetDateTime.now());
		final var communicationEntity = CommunicationEntity.create();

		// MOCK
		when(errandRepositoryMock.findById(anyString())).thenReturn(Optional.of(errandEntity));
		when(errandServiceMock.createErrand(anyString(), anyString(), any())).thenReturn(errandEntity.getId());
		when(emailReaderMapperMock.toErrand(any(), any())).thenReturn(new Errand());
		when(emailReaderMapperMock.toCommunicationEntity(any(), any())).thenReturn(communicationEntity);

		// ACT
		emailReaderWorker.processEmail(email, emailConfig, consumerMock);

		// VERIFY - no new-errand email should be sent for system messages
		verify(errandRepositoryMock).findById(errandEntity.getId());
		verify(emailReaderMapperMock, never()).createEmailRequest(any(), any(), any(), any(), any());
		verify(communicationServiceMock, never()).sendEmail(any(ErrandEntity.class), any());
		verify(emailReaderMapperMock).toErrand(same(email), same(emailConfig));
		verify(emailReaderMapperMock).toCommunicationEntity(same(email), same(errandEntity));
		verify(communicationServiceMock).saveAttachment(same(communicationEntity), same(errandEntity));
		verify(communicationServiceMock).saveCommunication(same(communicationEntity));
		verify(emailReaderClientMock).deleteEmail(MUNICIPALITY_ID, email.getId());
		verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq("Nytt meddelande"), same(errandEntity), isNull(), isNull(), eq(MESSAGE));
		verifyNoMoreInteractions(emailReaderClientMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock, emailWorkerConfigRepositoryMock, eventServiceMock);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"no-reply@domain.com", "noreply@domain.com", "NO-reply@otherdomain.com", "noREPLY@OTHERdomain.com"
	})
	void processEmailNoReplyDeletedWhenIgnoreEnabled(final String sender) {

		// ARRANGE
		final var email = new Email();
		email.setSubject("Some notification");
		email.setId("id");
		email.setSender(sender);

		final var emailConfig = EmailWorkerConfigEntity.create()
			.withEnabled(true)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withStatusForNew("NEW")
			.withIgnoreNoReply(true);

		// ACT
		emailReaderWorker.processEmail(email, emailConfig, consumerMock);

		// VERIFY - email is deleted entirely, no errand processing
		verify(emailReaderClientMock).deleteEmail(MUNICIPALITY_ID, email.getId());
		verifyNoInteractions(errandRepositoryMock, errandServiceMock, communicationServiceMock, eventServiceMock, emailReaderMapperMock);
		verifyNoMoreInteractions(emailReaderClientMock);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"no-reply@domain.com", "noreply@domain.com", "NO-reply@otherdomain.com", "noREPLY@OTHERdomain.com"
	})
	void processEmailNoReplyProcessedButSuppressedWhenIgnoreDisabled(final String sender) {

		// ARRANGE
		final var email = new Email();
		email.setSubject("Ärende #PRH-2022-000001 Some notification");
		email.setId("id");
		email.setSender(sender);

		final var emailConfig = EmailWorkerConfigEntity.create()
			.withEnabled(true)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandClosedEmailSender("errandClosedEmailSender")
			.withErrandClosedEmailTemplate("errandClosedEmailTemplate")
			.withErrandClosedEmailHTMLTemplate("errandClosedEmailHTMLTemplate")
			.withDaysOfInactivityBeforeReject(5)
			.withStatusForNew("NEW")
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("ONGOING")
			.withInactiveStatus("SOLVED")
			.withIgnoreNoReply(false);

		final var errandEntity = ErrandEntity.create().withId("id").withStatus("SOLVED").withCreated(OffsetDateTime.now().minusDays(6)).withTouched(OffsetDateTime.now().minusDays(6));
		final var communicationEntity = CommunicationEntity.create();

		// MOCK
		when(errandRepositoryMock.findByErrandNumberAndNamespace(anyString(), anyString())).thenReturn(Optional.of(errandEntity));
		when(emailReaderMapperMock.toCommunicationEntity(any(), any())).thenReturn(communicationEntity);

		// ACT
		emailReaderWorker.processEmail(email, emailConfig, consumerMock);

		// VERIFY - errand is processed but no confirmation email (noreply suppressed)
		verify(errandRepositoryMock).findByErrandNumberAndNamespace("PRH-2022-000001", NAMESPACE);
		verify(emailReaderMapperMock, never()).createEmailRequest(any(), any(), any(), any(), any());
		verify(communicationServiceMock, never()).sendEmail(any(ErrandEntity.class), any());
		verify(emailReaderMapperMock).toCommunicationEntity(same(email), same(errandEntity));
		verify(communicationServiceMock).saveAttachment(same(communicationEntity), same(errandEntity));
		verify(communicationServiceMock).saveCommunication(same(communicationEntity));
		verify(emailReaderClientMock).deleteEmail(MUNICIPALITY_ID, email.getId());
		verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq("Nytt meddelande"), same(errandEntity), isNull(), isNull(), eq(MESSAGE));
		verifyNoInteractions(errandServiceMock);
		verifyNoMoreInteractions(emailReaderClientMock, errandRepositoryMock, emailReaderMapperMock, communicationServiceMock, emailWorkerConfigRepositoryMock, eventServiceMock);
	}

}
