package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType.WEB_MESSAGE;

import generated.se.sundsvall.webmessagecollector.MessageAttachment;
import generated.se.sundsvall.webmessagecollector.MessageDTO;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;
import javax.sql.rowset.serial.SerialBlob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;
import se.sundsvall.supportmanagement.service.util.BlobBuilder;
import se.sundsvall.supportmanagement.service.util.ServiceUtil;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class WebMessageCollectorMapperTest {

	@InjectMocks
	private WebMessageCollectorMapper webMessageCollectorMapper;

	@Mock
	private BlobBuilder blobBuilder;

	@Test
	void toCommunicationEntity() {
		// Arrange
		final var direction = MessageDTO.DirectionEnum.INBOUND;

		final var externalCaseId = "123";
		final var familyId = "234";
		final var messageString = "message";
		final var messageId = "456";
		final var sent = LocalDateTime.now().toString();
		final var firstName = "firstName";
		final var lastName = "lastName";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		final var errand = ErrandEntity.create().withErrandNumber("KC-2024-010").withMunicipalityId(municipalityId).withNamespace(namespace);

		final var attachment = new MessageAttachment()
			.attachmentId(1)
			.mimeType("text/plain")
			.name("attachment.txt");

		final var messagedto = new MessageDTO()
			.direction(direction)
			.firstName(firstName)
			.lastName(lastName)
			.externalCaseId(externalCaseId)
			.familyId(familyId)
			.message(messageString)
			.messageId(messageId)
			.attachments(List.of(attachment))
			.sent(sent);

		// Act
		final var result = webMessageCollectorMapper.toCommunicationEntity(messagedto, errand);

		// Assert
		assertThat(result).hasNoNullFieldsOrPropertiesExcept("subject", "target", "attachments", "emailHeaders", "errandAttachments");
		assertThat(result.getId()).isNotNull();
		assertThat(result.getNamespace()).isEqualTo(namespace);
		assertThat(result.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(result.getType()).isEqualTo(WEB_MESSAGE);
		assertThat(result.getSender()).isEqualTo(firstName + " " + lastName);
		assertThat(ServiceUtil.isValidUuid(result.getId())).isTrue();
		assertThat(result.getDirection()).isEqualTo(Direction.INBOUND);
		assertThat(result.getExternalId()).isEqualTo(messageId);
		assertThat(result.getMessageBody()).isEqualTo(messageString);
		assertThat(result.getSent()).isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS));
		assertThat(result.getAttachments()).hasSize(1);
		assertThat(result.getAttachments().getFirst().getForeignId()).isEqualTo("1");
		assertThat(result.getAttachments().getFirst().getId()).containsPattern(Pattern.compile("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"));
		assertThat(result.getAttachments().getFirst().getContentType()).isEqualTo("text/plain");
		assertThat(result.getAttachments().getFirst().getName()).isEqualTo("attachment.txt");

	}

	@Test
	void toCommunicationAttachmentDataEntity() throws SQLException {

		// Arrange
		final var attachmentData = "attachmentData".getBytes();
		final var blob = new SerialBlob(attachmentData);
		when(blobBuilder.createBlob(attachmentData)).thenReturn(blob);

		// Act
		final var result = webMessageCollectorMapper.toCommunicationAttachmentDataEntity(attachmentData);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getFile()).isNotNull();
		assertThat(result.getFile()).isSameAs(blob);
	}

}
