package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.supportmanagement.Constants;
import se.sundsvall.supportmanagement.integration.db.CommunicationRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.webmessagecollector.WebMessageCollectorClient;
import se.sundsvall.supportmanagement.integration.webmessagecollector.configuration.WebMessageCollectorProperties;

import generated.se.sundsvall.webmessagecollector.MessageDTO;

@ExtendWith(MockitoExtension.class)
class WebMessageCollectorWorkerTest {

	@Mock
	private WebMessageCollectorClient webMessageCollectorClient;

	@Mock
	private WebMessageCollectorProperties webMessageCollectorProperties;

	@Mock
	private ErrandsRepository errandsRepository;

	@Mock
	private CommunicationRepository communicationRepository;

	@InjectMocks
	private WebMessageCollectorWorker webMessageCollectorWorker;

	@Test
	void fetchWebMessages() {
		// Arrange
		final var messagedto = new MessageDTO()
			.direction(MessageDTO.DirectionEnum.INBOUND)
			.email("email")
			.externalCaseId("123")
			.familyId("234")
			.firstName("firstName")
			.id(123)
			.lastName("lastName")
			.message("message")
			.messageId("456")
			.sent(LocalDateTime.now().toString())
			.userId("userId")
			.username("username");

		final var errandEntity = ErrandEntity.create()
			.withErrandNumber("KC-2024-0101")
			.withStatus(Constants.ERRAND_STATUS_SOLVED)
			.withTouched(OffsetDateTime.now().minusDays(2));
		// Mock
		when(webMessageCollectorProperties.familyIds()).thenReturn(List.of("123"));
		when(webMessageCollectorClient.getMessages(any(String.class))).thenReturn(List.of(messagedto));
		when(errandsRepository.findByExternalTagValue(anyString())).thenReturn(Optional.of(errandEntity));
		// Act
		webMessageCollectorWorker.fetchWebMessages();
		// Verify
		verify(errandsRepository).save(any(ErrandEntity.class));
		verify(communicationRepository).save(any(CommunicationEntity.class));
		verifyNoMoreInteractions(webMessageCollectorClient, webMessageCollectorProperties, errandsRepository, communicationRepository);
	}

	@Test
	void fetchWebMessage_NothingFromWebMessageCollector() {
		// Mock
		when(webMessageCollectorProperties.familyIds()).thenReturn(List.of("123"));
		// Act
		webMessageCollectorWorker.fetchWebMessages();
		// Verify
		verify(webMessageCollectorClient).getMessages(anyString());
		verifyNoMoreInteractions(webMessageCollectorClient, webMessageCollectorProperties, errandsRepository, communicationRepository);

	}

	@Test
	void fetchWebMessage_NoErrandFound() {
		// Arrange
		final var messagedto = new MessageDTO()
			.direction(MessageDTO.DirectionEnum.INBOUND)
			.email("email")
			.externalCaseId("123")
			.familyId("234")
			.firstName("firstName")
			.id(123)
			.lastName("lastName")
			.message("message")
			.messageId("456")
			.sent("sent")
			.userId("userId")
			.username("username");
		// Mock
		when(webMessageCollectorProperties.familyIds()).thenReturn(List.of("123"));
		when(webMessageCollectorClient.getMessages(any(String.class))).thenReturn(List.of(messagedto));
		// Act
		webMessageCollectorWorker.fetchWebMessages();
		// Verify
		verify(errandsRepository).findByExternalTagValue(any(String.class));
		verifyNoMoreInteractions(webMessageCollectorClient, webMessageCollectorProperties, errandsRepository, communicationRepository);

	}


	@Test
	void fetchWebMessage_oldErrand() {
		// Arrange
		final var messagedto = new MessageDTO()
			.direction(MessageDTO.DirectionEnum.INBOUND)
			.email("email")
			.externalCaseId("123")
			.familyId("234")
			.firstName("firstName")
			.id(123)
			.lastName("lastName")
			.message("message")
			.messageId("456")
			.sent(LocalDateTime.now().toString())
			.userId("userId")
			.username("username");

		final var errandEntity = ErrandEntity.create()
			.withErrandNumber("KC-2024-0101")
			.withStatus(Constants.ERRAND_STATUS_SOLVED)
			.withTouched(OffsetDateTime.now().minusDays(5).minusMinutes(1));

		// Mock
		when(webMessageCollectorProperties.familyIds()).thenReturn(List.of("123"));
		when(webMessageCollectorClient.getMessages(any(String.class))).thenReturn(List.of(messagedto));
		when(errandsRepository.findByExternalTagValue(anyString())).thenReturn(Optional.of(errandEntity));

		// Act
		webMessageCollectorWorker.fetchWebMessages();
		// Verify
		verifyNoMoreInteractions(webMessageCollectorClient, webMessageCollectorProperties, errandsRepository, communicationRepository);

	}

	@Test
	void fetchWebMessage_noFamilyId() {
		// Act
		webMessageCollectorWorker.fetchWebMessages();
		// Arrange
		verify(webMessageCollectorProperties).familyIds();
		verifyNoMoreInteractions(webMessageCollectorProperties);
		verifyNoInteractions(webMessageCollectorClient, errandsRepository, communicationRepository);

	}

	@Test
	void fetchWebMessage_noExternalCaseId() {
		// Arrange
		final var messagedto = new MessageDTO()
			.direction(MessageDTO.DirectionEnum.INBOUND)
			.email("email")
			.familyId("234")
			.firstName("firstName")
			.id(123)
			.lastName("lastName")
			.message("message")
			.messageId("456")
			.sent(LocalDateTime.now().toString())
			.userId("userId")
			.username("username");
		// Mock
		when(webMessageCollectorProperties.familyIds()).thenReturn(List.of("123"));
		when(webMessageCollectorClient.getMessages(any(String.class))).thenReturn(List.of(messagedto));
		// Act
		webMessageCollectorWorker.fetchWebMessages();
		//Verify
		verify(errandsRepository).findByExternalTagValue(null);
		verifyNoMoreInteractions(webMessageCollectorProperties, webMessageCollectorClient, errandsRepository);
		verifyNoInteractions(communicationRepository);
	}

}
