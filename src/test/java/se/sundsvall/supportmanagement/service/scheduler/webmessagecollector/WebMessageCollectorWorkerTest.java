package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.Direction.INBOUND;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import generated.se.sundsvall.webmessagecollector.MessageAttachment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.supportmanagement.Constants;
import se.sundsvall.supportmanagement.integration.db.CommunicationRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.webmessagecollector.WebMessageCollectorClient;
import se.sundsvall.supportmanagement.integration.webmessagecollector.configuration.WebMessageCollectorProperties;
import se.sundsvall.supportmanagement.service.EventService;

import generated.se.sundsvall.eventlog.EventType;
import generated.se.sundsvall.webmessagecollector.MessageDTO;

@ExtendWith(MockitoExtension.class)
class WebMessageCollectorWorkerTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
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

	@Mock
	private CommunicationAttachmentDataEntity communicationAttachmentDataEntityMock;

	@InjectMocks
	private WebMessageCollectorWorker webMessageCollectorWorker;

	@Captor
	private ArgumentCaptor<CommunicationEntity> communicationEntityCaptor;

	@Test
	void processMessage() {
		// Arrange
		final var caseId = "123";
		final var messageId = 321;
		final var errandNumber = "KC-2024-0101";
		final var attachmentId = 666;
		final var data = "data".getBytes();
		final var messageAttachment = new MessageAttachment()
			.attachmentId(attachmentId);

		final var messagedto = new MessageDTO()
			.direction(MessageDTO.DirectionEnum.INBOUND)
			.email("email")
			.externalCaseId("123")
			.familyId("234")
			.firstName("firstName")
			.id(messageId)
			.lastName("lastName")
			.message("message")
			.messageId("456")
			.sent(LocalDateTime.now().toString())
			.userId("userId")
			.username("username")
			.attachments(List.of(messageAttachment));

		final var errandEntity = ErrandEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace("namespace")
			.withErrandNumber(errandNumber)
			.withStatus(Constants.ERRAND_STATUS_SOLVED)
			.withTouched(now().minusDays(2));

		// Mock
		when(errandsRepositoryMock.findByExternalTagsValue(any(String.class))).thenReturn(Optional.of(errandEntity));
		when(webMessageCollectorMapperMock.toCommunicationEntity(any(), any())).thenCallRealMethod();
		when(webMessageCollectorClientMock.getAttachment(any(), anyInt())).thenReturn(data);
		when(webMessageCollectorMapperMock.toCommunicationAttachmentDataEntity(any())).thenReturn(communicationAttachmentDataEntityMock);

		// Act
		webMessageCollectorWorker.processMessage(messagedto, MUNICIPALITY_ID);

		// Verify
		verify(errandsRepositoryMock).findByExternalTagsValue(caseId);
		verify(webMessageCollectorMapperMock).toCommunicationEntity(messagedto, errandEntity);
		verify(communicationRepositoryMock, times(2)).saveAndFlush(communicationEntityCaptor.capture());
		verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq("Ärendekommunikation har skapats."), same(errandEntity), isNull(), isNull());
		verify(webMessageCollectorClientMock).getAttachment(MUNICIPALITY_ID, attachmentId);
		verify(webMessageCollectorMapperMock).toCommunicationAttachmentDataEntity(data);
		verify(webMessageCollectorClientMock).deleteMessages(MUNICIPALITY_ID, List.of(messageId));

		assertThat(communicationEntityCaptor.getAllValues()).satisfiesExactly(
			communication -> {
				assertThat(communication).hasNoNullFieldsOrPropertiesExcept("subject", "target", "emailHeaders", "errandAttachments");
				assertThat(communication.getDirection()).isEqualTo(INBOUND);
				assertThat(communication.getExternalCaseID()).isEqualTo(messagedto.getExternalCaseId());
				assertThat(communication.getMessageBody()).isEqualTo(messagedto.getMessage());
				assertThat(communication.getSent()).isCloseTo(now(), within(1, SECONDS));
				assertThat(communication.getErrandNumber()).isEqualTo(errandEntity.getErrandNumber());
			}, communicationWithAttachment -> {
				assertThat(communicationWithAttachment.getAttachments()).hasSize(1);
				assertThat(communicationWithAttachment.getAttachments().getFirst().getAttachmentData()).isSameAs(communicationAttachmentDataEntityMock);
			});

		verifyNoMoreInteractions(webMessageCollectorClientMock, webMessageCollectorPropertiesMock, errandsRepositoryMock, communicationRepositoryMock);
	}

	@Test
	void processMessage_NoErrandFound() {
		// Arrange
		final var externalCaseId = "123";
		final var messagedto = new MessageDTO()
			.externalCaseId(externalCaseId);

		// Mock
		when(errandsRepositoryMock.findByExternalTagsValue(any())).thenReturn(Optional.empty());
		// Act
		webMessageCollectorWorker.processMessage(messagedto, MUNICIPALITY_ID);
		// Verify
		verify(errandsRepositoryMock).findByExternalTagsValue(externalCaseId);
		verifyNoMoreInteractions(webMessageCollectorClientMock, errandsRepositoryMock, communicationRepositoryMock, webMessageCollectorMapperMock, eventServiceMock);
	}

	@Test
	void getWebMessages() {
		// Arrange
		var instance = "instance";
		var familyId = "familyId";
		var list = List.of(new MessageDTO());

		// Mock
		when(webMessageCollectorClientMock.getMessages(any(), any(), any())).thenReturn(list);
		// Act
		var result = webMessageCollectorWorker.getWebMessages(instance, familyId, MUNICIPALITY_ID);
		// Verify
		verify(webMessageCollectorClientMock).getMessages(MUNICIPALITY_ID, familyId, instance);
		assertThat(result).isSameAs(list);
	}
}
