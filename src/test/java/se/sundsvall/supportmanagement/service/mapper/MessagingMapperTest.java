package se.sundsvall.supportmanagement.service.mapper;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.util.MimeTypeUtils.IMAGE_PNG_VALUE;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import generated.se.sundsvall.messaging.ExternalReference;
import generated.se.sundsvall.messaging.Party;
import se.sundsvall.supportmanagement.api.model.communication.EmailAttachment;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;
import se.sundsvall.supportmanagement.integration.db.model.EmbeddableCustomer;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

class MessagingMapperTest {

	private static final String ERRAND_ID = randomUUID().toString();
	private static final String CUSTOMER_ID = randomUUID().toString();
	private static final String USER_ID = "userId";
	private static final String MESSAGE = "message";
	private static final String RECIPIENT = "recipient";
	private static final String SENDER_EMAIL = "sender@sender.com";
	private static final String SENDER_NAME = "senderName";
	private static final String SUBJECT = "subject";
	private static final String FILE_CONTENT = "iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==";
	private static final String FILE_NAME = "fileName";
	private static final String ERRAND_ID_KEY = "errandId";

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void toEmailRequestWithSenderName(boolean hasUuid) {
		final var result = MessagingMapper.toEmailRequest(createErrandEntity(hasUuid), createEmailRequest(true));

		assertThat(result.getEmailAddress()).isEqualTo(RECIPIENT);
		assertThat(result.getHeaders()).isNullOrEmpty();
		assertThat(result.getHtmlMessage()).isNull();
		assertThat(result.getMessage()).isEqualTo(MESSAGE);
		assertThat(result.getSubject()).isEqualTo(SUBJECT);
		assertThat(result.getSender().getAddress()).isEqualTo(SENDER_EMAIL);
		assertThat(result.getSender().getName()).isEqualTo(SENDER_NAME);
		assertThat(result.getSender().getReplyTo()).isNull();
		validateParty(hasUuid, result.getParty());

		assertThat(result.getAttachments()).isNotNull().extracting(
			generated.se.sundsvall.messaging.EmailAttachment::getContent,
			generated.se.sundsvall.messaging.EmailAttachment::getContentType,
			generated.se.sundsvall.messaging.EmailAttachment::getName).containsExactly(tuple(FILE_CONTENT, IMAGE_PNG_VALUE, FILE_NAME));
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void toEmailRequestWithoutSenderName(boolean hasUuid) {
		final var result = MessagingMapper.toEmailRequest(createErrandEntity(hasUuid), createEmailRequest(false));

		assertThat(result.getEmailAddress()).isEqualTo(RECIPIENT);
		assertThat(result.getHeaders()).isNullOrEmpty();
		assertThat(result.getHtmlMessage()).isNull();
		assertThat(result.getMessage()).isEqualTo(MESSAGE);
		assertThat(result.getSubject()).isEqualTo(SUBJECT);
		assertThat(result.getSender().getAddress()).isEqualTo(SENDER_EMAIL);
		assertThat(result.getSender().getName()).isEqualTo(SENDER_EMAIL);
		assertThat(result.getSender().getReplyTo()).isNull();
		validateParty(hasUuid, result.getParty());

		assertThat(result.getAttachments()).isNotNull().extracting(
			generated.se.sundsvall.messaging.EmailAttachment::getContent,
			generated.se.sundsvall.messaging.EmailAttachment::getContentType,
			generated.se.sundsvall.messaging.EmailAttachment::getName).containsExactly(tuple(FILE_CONTENT, IMAGE_PNG_VALUE, FILE_NAME));
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void toSmsRequest(boolean hasUuid) {
		final var result = MessagingMapper.toSmsRequest(createErrandEntity(hasUuid), createSmsRequest());

		assertThat(result.getHeaders()).isNullOrEmpty();
		assertThat(result.getMessage()).isEqualTo(MESSAGE);
		assertThat(result.getMobileNumber()).isEqualTo(RECIPIENT);
		assertThat(result.getSender().getName()).isEqualTo(SENDER_NAME);
		validateParty(hasUuid, result.getParty());
	}

	private static void validateParty(boolean hasUuid, final Party party) {
		if (hasUuid) {
			assertThat(party.getPartyId()).isEqualTo(CUSTOMER_ID);
			assertThat(party.getExternalReferences()).hasSize(1).extracting(
				ExternalReference::getKey,
				ExternalReference::getValue).containsExactly(tuple(ERRAND_ID_KEY, ERRAND_ID));
		} else {
			assertThat(party).isNull();
		}
	}

	private SmsRequest createSmsRequest() {
		return SmsRequest.create()
			.withMessage(MESSAGE)
			.withRecipient(RECIPIENT)
			.withSender(SENDER_NAME);
	}

	private static EmailRequest createEmailRequest(boolean hasSenderName) {
		return EmailRequest.create()
			.withAttachments(List.of(EmailAttachment.create()
				.withBase64EncodedString(FILE_CONTENT)
				.withName(FILE_NAME)))
			.withMessage(MESSAGE)
			.withRecipient(RECIPIENT)
			.withSender(SENDER_EMAIL)
			.withSenderName(hasSenderName ? SENDER_NAME : null)
			.withSubject(SUBJECT);
	}

	private static ErrandEntity createErrandEntity(boolean hasCustomerUuid) {
		return ErrandEntity.create()
			.withId(ERRAND_ID)
			.withCustomer(EmbeddableCustomer.create()
				.withId(hasCustomerUuid ? CUSTOMER_ID : USER_ID));
	}
}
