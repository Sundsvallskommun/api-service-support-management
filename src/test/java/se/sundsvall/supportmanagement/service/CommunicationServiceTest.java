package se.sundsvall.supportmanagement.service;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.util.MimeTypeUtils.IMAGE_PNG_VALUE;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.api.model.errand.CustomerType.EMPLOYEE;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.ThrowableProblem;

import generated.se.sundsvall.messaging.ExternalReference;
import se.sundsvall.supportmanagement.api.model.communication.EmailAttachment;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;
import se.sundsvall.supportmanagement.api.model.errand.CustomerType;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.EmbeddableCustomer;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.messaging.MessagingClient;

@ExtendWith(MockitoExtension.class)
class CommunicationServiceTest {
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String CUSTOMER_ID = randomUUID().toString();
	private static final String MESSAGE = "message";
	private static final String RECIPIENT = "recipient";
	private static final String SENDER_EMAIL = "sender@sender.com";
	private static final String SENDER_NAME = "senderName";
	private static final String SUBJECT = "subject";
	private static final String FILE_CONTENT = "iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==";
	private static final String FILE_NAME = "fileName";
	private static final String ERRAND_ID_KEY = "errandId";

	@Mock
	private ErrandsRepository repositoryMock;

	@Mock
	private ErrandEntity errandEntityMock;

	@Mock
	private EmbeddableCustomer embeddableCustomerMock;

	@Mock
	private MessagingClient messagingClientMock;

	@InjectMocks
	private CommunicationService service;

	@Captor
	private ArgumentCaptor<generated.se.sundsvall.messaging.EmailRequest> messagingEmailCaptor;

	@Captor
	private ArgumentCaptor<generated.se.sundsvall.messaging.SmsRequest> messagingSmsCaptor;

	@ParameterizedTest
	@EnumSource(value = CustomerType.class)
	void sendEmail(CustomerType type) {
		// Setup
		final var request = createEmailRequest();

		// Mock
		when(repositoryMock.getReferenceById(ERRAND_ID)).thenReturn(errandEntityMock);
		when(errandEntityMock.getId()).thenReturn(ERRAND_ID);
		when(errandEntityMock.getCustomer()).thenReturn(embeddableCustomerMock);
		when(embeddableCustomerMock.getType()).thenReturn(type.name());
		when(embeddableCustomerMock.getId()).thenReturn(CUSTOMER_ID);
		// Call
		service.sendEmail(ERRAND_ID, request);

		// Verifications and assertions
		verify(repositoryMock).getReferenceById(ERRAND_ID);
		verify(messagingClientMock).sendEmail(messagingEmailCaptor.capture());

		final var arguments = messagingEmailCaptor.getValue();
		assertThat(arguments.getEmailAddress()).isEqualTo(RECIPIENT);
		assertThat(arguments.getHeaders()).isNullOrEmpty();
		assertThat(arguments.getHtmlMessage()).isNull();
		assertThat(arguments.getMessage()).isEqualTo(MESSAGE);
		assertThat(arguments.getParty().getPartyId()).isEqualTo(CUSTOMER_ID);
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
	}

	@ParameterizedTest
	@EnumSource(value = CustomerType.class)
	void sendSms(CustomerType type) {
		// Setup
		final var request = createSmsRequest();

		// Mock
		when(repositoryMock.getReferenceById(ERRAND_ID)).thenReturn(errandEntityMock);
		when(errandEntityMock.getId()).thenReturn(ERRAND_ID);
		when(errandEntityMock.getCustomer()).thenReturn(embeddableCustomerMock);
		when(embeddableCustomerMock.getType()).thenReturn(type.name());
		when(embeddableCustomerMock.getId()).thenReturn(CUSTOMER_ID);

		// Call
		service.sendSms(ERRAND_ID, request);

		// Verifications and assertions
		verify(repositoryMock).getReferenceById(ERRAND_ID);
		verify(messagingClientMock).sendSms(messagingSmsCaptor.capture());

		final var arguments = messagingSmsCaptor.getValue();
		assertThat(arguments.getHeaders()).isNullOrEmpty();
		assertThat(arguments.getMessage()).isEqualTo(MESSAGE);
		assertThat(arguments.getMobileNumber()).isEqualTo(RECIPIENT);
		assertThat(arguments.getParty().getPartyId()).isEqualTo(CUSTOMER_ID);
		assertThat(arguments.getParty().getExternalReferences()).isNotEmpty().extracting(
			ExternalReference::getKey,
			ExternalReference::getValue).containsExactly(tuple(ERRAND_ID_KEY, ERRAND_ID));
		assertThat(arguments.getSender().getName()).isEqualTo(SENDER_NAME);
	}

	@Test
	void errandNotFound() {
		// Setup
		final var request = createSmsRequest();

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.sendSms(ERRAND_ID, request));

		// Verifications and assertions
		verify(repositoryMock).getReferenceById(ERRAND_ID);
		verifyNoInteractions(messagingClientMock);

		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id '" + ERRAND_ID + "' could not be found");
	}

	@Test
	void employeWithNonUuid() {
		// Setup
		final var request = createSmsRequest();

		// Mock
		when(repositoryMock.getReferenceById(ERRAND_ID)).thenReturn(errandEntityMock);
		when(errandEntityMock.getCustomer()).thenReturn(embeddableCustomerMock);
		when(embeddableCustomerMock.getType()).thenReturn(EMPLOYEE.name());
		when(embeddableCustomerMock.getId()).thenReturn("non-valid-uuid");

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.sendSms(ERRAND_ID, request));

		// Verifications and assertions
		verify(repositoryMock).getReferenceById(ERRAND_ID);
		verifyNoInteractions(messagingClientMock);

		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(exception.getTitle()).isEqualTo(BAD_REQUEST.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Bad Request: Errand with id '" + ERRAND_ID + "' has an employee customer reference with other identifier than an uuid");
	}

	private SmsRequest createSmsRequest() {
		return SmsRequest.create()
			.withMessage(MESSAGE)
			.withRecipient(RECIPIENT)
			.withSender(SENDER_NAME);
	}

	private static EmailRequest createEmailRequest() {
		return EmailRequest.create()
			.withAttachments(List.of(EmailAttachment.create()
				.withBase64EncodedString(FILE_CONTENT)
				.withName(FILE_NAME)))
			.withMessage(MESSAGE)
			.withRecipient(RECIPIENT)
			.withSender(SENDER_EMAIL)
			.withSenderName(SENDER_NAME)
			.withSubject(SUBJECT);
	}
}
