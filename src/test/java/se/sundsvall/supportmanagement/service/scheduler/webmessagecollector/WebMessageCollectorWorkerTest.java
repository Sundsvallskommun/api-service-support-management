package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.Direction.INBOUND;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import se.sundsvall.supportmanagement.service.EventService;

import generated.se.sundsvall.eventlog.EventType;
import generated.se.sundsvall.webmessagecollector.MessageDTO;

@ExtendWith(MockitoExtension.class)
class WebMessageCollectorWorkerTest {

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	private WebMessageCollectorMapper webMessageCollectorMapperMock;

	@Mock
	private EventService eventServiceMock;

	@Mock
	private WebMessageCollectorClient webMessageCollectorClientMock;

	@Mock
	private WebMessageCollectorProperties webMessageCollectorPropertiesMock;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private CommunicationRepository communicationRepositoryMock;

	@InjectMocks
	private WebMessageCollectorWorker webMessageCollectorWorker;

	@Captor
	private ArgumentCaptor<CommunicationEntity> communicationEntityCaptor;

	@Captor
	private ArgumentCaptor<ErrandEntity> errandEntityCaptor;

	@Test
	void fetchWebMessages() {
		// Arrange
		final var familyId = "123";
		final var instance = "instance";
		final var errandNumber = "KC-2024-0101";
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
			.withErrandNumber(errandNumber)
			.withStatus(Constants.ERRAND_STATUS_SOLVED)
			.withTouched(now().minusDays(2));
		// Mock
		when(webMessageCollectorPropertiesMock.familyIds()).thenReturn(Map.of(instance, List.of(familyId)));
		when(webMessageCollectorClientMock.getMessages(familyId, instance)).thenReturn(List.of(messagedto));
		when(errandsRepositoryMock.findByExternalTagsValue(any(String.class))).thenReturn(Optional.of(errandEntity));


		// Act
		webMessageCollectorWorker.fetchWebMessages();
		// Verify
		verify(webMessageCollectorPropertiesMock).familyIds();
		verify(webMessageCollectorClientMock).getMessages(familyId, instance);
		verify(errandsRepositoryMock, times(1)).findByExternalTagsValue(familyId);

		verify(errandsRepositoryMock).save(errandEntityCaptor.capture());

		assertThat(errandEntityCaptor.getValue()).satisfies(
			errand -> {
				assertThat(errand.getErrandNumber()).isEqualTo(errandNumber);
				assertThat(errand.getStatus()).isEqualTo(Constants.ERRAND_STATUS_ONGOING);
				assertThat(errand.getTouched()).isCloseTo(now().minusDays(2), within(1, SECONDS));
			});

		verify(communicationRepositoryMock).saveAndFlush(communicationEntityCaptor.capture());
		assertThat(communicationEntityCaptor.getValue()).satisfies(
			communication -> {
				assertThat(communication).hasNoNullFieldsOrPropertiesExcept("subject", "target", "emailHeaders");
				assertThat(communication.getDirection()).isEqualTo(INBOUND);
				assertThat(communication.getExternalCaseID()).isEqualTo(messagedto.getExternalCaseId());
				assertThat(communication.getMessageBody()).isEqualTo(messagedto.getMessage());
				assertThat(communication.getSent()).isCloseTo(now(), within(1, SECONDS));
				assertThat(communication.getErrandNumber()).isEqualTo(errandEntity.getErrandNumber());
			});

		verify(webMessageCollectorMapperMock).toCommunicationEntity(messagedto, errandEntity.getErrandNumber());
		verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq("Ärendekommunikation har skapats."), same(errandEntity), isNull(), isNull());
		verifyNoMoreInteractions(webMessageCollectorClientMock, webMessageCollectorPropertiesMock, errandsRepositoryMock, communicationRepositoryMock);
	}

	@Test
	void fetchWebMessage_NothingFromWebMessageCollector() {
		// Arrange
		final var familyId = "123";
		final var instance = "instance";
		when(webMessageCollectorPropertiesMock.familyIds()).thenReturn(Map.of(instance, List.of(familyId)));
		// Act
		webMessageCollectorWorker.fetchWebMessages();
		// Verify
		verify(webMessageCollectorPropertiesMock).familyIds();
		verify(webMessageCollectorClientMock).getMessages(familyId, instance);
		verifyNoMoreInteractions(webMessageCollectorClientMock, webMessageCollectorPropertiesMock, errandsRepositoryMock, communicationRepositoryMock);

	}

	@Test
	void fetchWebMessage_NoErrandFound() {
		// Arrange
		final var familyId = "123";
		final var instance = "instance";
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
		when(webMessageCollectorPropertiesMock.familyIds()).thenReturn(Map.of(instance, List.of(familyId)));
		when(webMessageCollectorClientMock.getMessages(familyId, instance)).thenReturn(List.of(messagedto));
		// Act
		webMessageCollectorWorker.fetchWebMessages();
		// Verify
		verify(webMessageCollectorPropertiesMock).familyIds();
		verify(webMessageCollectorClientMock).getMessages(familyId, instance);
		verify(errandsRepositoryMock).findByExternalTagsValue(any(String.class));
		verifyNoMoreInteractions(webMessageCollectorClientMock, webMessageCollectorPropertiesMock, errandsRepositoryMock, communicationRepositoryMock);

	}


	@Test
	void fetchWebMessage_oldErrand() {
		// Arrange
		final var familyId = "123";
		final var instance = "instance";
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
			.withTouched(now().minusDays(5).minusMinutes(1));

		// Mock
		when(webMessageCollectorPropertiesMock.familyIds()).thenReturn(Map.of(instance, List.of(familyId)));
		when(webMessageCollectorClientMock.getMessages(familyId, instance)).thenReturn(List.of(messagedto));
		when(errandsRepositoryMock.findByExternalTagsValue(any(String.class))).thenReturn(Optional.of(errandEntity));

		// Act
		webMessageCollectorWorker.fetchWebMessages();
		// Verify
		verify(webMessageCollectorPropertiesMock).familyIds();
		verify(webMessageCollectorClientMock).getMessages(familyId, instance);
		verify(errandsRepositoryMock).findByExternalTagsValue(any(String.class));
		verifyNoMoreInteractions(webMessageCollectorClientMock, webMessageCollectorPropertiesMock, errandsRepositoryMock, communicationRepositoryMock);

	}

	@Test
	void fetchWebMessage_noFamilyId() {
		// Act
		webMessageCollectorWorker.fetchWebMessages();
		// Arrange
		verify(webMessageCollectorPropertiesMock).familyIds();
		verifyNoMoreInteractions(webMessageCollectorPropertiesMock);
		verifyNoInteractions(webMessageCollectorClientMock, errandsRepositoryMock, communicationRepositoryMock);

	}

	@Test
	void fetchWebMessage_noExternalCaseId() {
		// Arrange
		final var familyId = "123";
		final var instance = "instance";
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
		when(webMessageCollectorPropertiesMock.familyIds()).thenReturn(Map.of(instance, List.of(familyId)));
		when(webMessageCollectorClientMock.getMessages(familyId, instance)).thenReturn(List.of(messagedto));
		// Act
		webMessageCollectorWorker.fetchWebMessages();
		//Verify
		verify(webMessageCollectorPropertiesMock).familyIds();
		verify(webMessageCollectorClientMock).getMessages(familyId, instance);
		verify(errandsRepositoryMock).findByExternalTagsValue(null);
		verifyNoMoreInteractions(webMessageCollectorPropertiesMock, webMessageCollectorClientMock, errandsRepositoryMock);
		verifyNoInteractions(communicationRepositoryMock);
	}

}
