package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;
import se.sundsvall.supportmanagement.service.util.ServiceUtil;

import generated.se.sundsvall.webmessagecollector.MessageDTO;

class WebMessageCollectorMapperTest {


	@Test
	void toCommunicationEntity() {
		// Arrange
		final var direction = MessageDTO.DirectionEnum.INBOUND;

		final var externalCaseId = "123";
		final var familyId = "234";
		final var messageString = "message";
		final var messageId = "456";
		final var sent = LocalDateTime.now().toString();

		final var errandNumber = "KC-2024-010";

		final var messagedto = new MessageDTO()
			.direction(direction)
			.externalCaseId(externalCaseId)
			.familyId(familyId)
			.message(messageString)
			.messageId(messageId)
			.sent(sent);

		// Act
		final var result = WebMessageCollectorMapper.toCommunicationEntity(messagedto, errandNumber);

		// Assert
		assertThat(result).hasNoNullFieldsOrPropertiesExcept("subject", "target", "attachments", "emailHeaders");
		assertThat(result.getId()).isNotNull();
		assertThat(ServiceUtil.isValidUuid(result.getId())).isTrue();
		assertThat(result.getDirection()).isEqualTo(Direction.INBOUND);
		assertThat(result.getExternalCaseID()).isEqualTo(externalCaseId);
		assertThat(result.getMessageBody()).isEqualTo(messageString);
		assertThat(result.getSent()).isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS));


	}

}
