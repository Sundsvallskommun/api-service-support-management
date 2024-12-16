package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.Direction.INBOUND;

import generated.se.sundsvall.eventlog.EventType;
import generated.se.sundsvall.webmessagecollector.MessageAttachment;
import generated.se.sundsvall.webmessagecollector.MessageDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.supportmanagement.Constants;
import se.sundsvall.supportmanagement.integration.db.CommunicationRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.specification.ErrandSpecification;
import se.sundsvall.supportmanagement.integration.webmessagecollector.WebMessageCollectorClient;
import se.sundsvall.supportmanagement.integration.webmessagecollector.configuration.WebMessageCollectorProperties;
import se.sundsvall.supportmanagement.service.CommunicationService;
import se.sundsvall.supportmanagement.service.EventService;

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
	private CommunicationService communicationServiceMock;

	@Mock
	private CommunicationAttachmentDataEntity communicationAttachmentDataEntityMock;

	@InjectMocks
	private WebMessageCollectorWorker webMessageCollectorWorker;

	@Captor
	private ArgumentCaptor<CommunicationEntity> communicationEntityCaptor;

	@Captor
	private ArgumentCaptor<List<DbExternalTag>> dbExternalTagsArgumentCaptor;

	@Mock
	private Specification<ErrandEntity> specificationMock;

	@Test
	void processMessage() {
		// Arrange
		final var id = 1;
		final var caseId = "caseId";
		final var familyId = "familyId";
		final var messageId = "messageId";
		final var errandNumber = "errandNumber";
		final var attachmentId = 666;
		final var data = "data".getBytes();
		final var messageAttachment = new MessageAttachment()
			.attachmentId(attachmentId);

		final var messagedto = new MessageDTO()
			.direction(MessageDTO.DirectionEnum.INBOUND)
			.email("email")
			.id(id)
			.familyId(familyId)
			.externalCaseId(caseId)
			.messageId(messageId)
			.firstName("firstName")
			.lastName("lastName")
			.message("message")
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
		when(errandsRepositoryMock.findOne(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(Optional.of(errandEntity));
		when(communicationRepositoryMock.existsByErrandNumberAndExternalId(any(), any())).thenReturn(false);
		when(webMessageCollectorMapperMock.toCommunicationEntity(any(), any())).thenCallRealMethod();
		when(webMessageCollectorClientMock.getAttachment(any(), anyInt())).thenReturn(data);
		when(webMessageCollectorMapperMock.toCommunicationAttachmentDataEntity(any())).thenReturn(communicationAttachmentDataEntityMock);

		try (final MockedStatic<ErrandSpecification> specificationMockedStatic = Mockito.mockStatic(ErrandSpecification.class)) {
			specificationMockedStatic.when(() -> ErrandSpecification.hasMatchingTags(any())).thenReturn(specificationMock);

			// Act
			webMessageCollectorWorker.processMessage(messagedto, MUNICIPALITY_ID);

			// Verify static
			specificationMockedStatic.verify(() -> ErrandSpecification.hasMatchingTags(dbExternalTagsArgumentCaptor.capture()));
			assertThat(dbExternalTagsArgumentCaptor.getValue())
				.hasSize(2)
				.extracting(DbExternalTag::getKey, DbExternalTag::getValue)
				.containsExactlyInAnyOrder(
					tuple("caseId", caseId),
					tuple("familyId", familyId));
		}

		// Verify
		verify(errandsRepositoryMock).findOne(same(specificationMock));
		verify(communicationRepositoryMock).existsByErrandNumberAndExternalId(errandNumber, messageId);
		verify(webMessageCollectorMapperMock).toCommunicationEntity(messagedto, errandEntity);
		verify(communicationServiceMock).saveCommunication(communicationEntityCaptor.capture());
		verify(communicationServiceMock).saveAttachment(communicationEntityCaptor.capture(), same(errandEntity));
		verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq("Ärendekommunikation har skapats."), same(errandEntity), isNull(), isNull());
		verify(webMessageCollectorClientMock).getAttachment(MUNICIPALITY_ID, attachmentId);
		verify(webMessageCollectorMapperMock).toCommunicationAttachmentDataEntity(data);
		verify(webMessageCollectorClientMock).deleteMessages(MUNICIPALITY_ID, List.of(id));

		assertThat(communicationEntityCaptor.getAllValues()).hasSize(2);
		assertThat(communicationEntityCaptor.getAllValues().getFirst()).isSameAs(communicationEntityCaptor.getAllValues().getLast());
		assertThat(communicationEntityCaptor.getValue()).satisfies(
			communication -> {
				assertThat(communication).hasNoNullFieldsOrPropertiesExcept("id", "subject", "target", "emailHeaders", "errandAttachments");
				assertThat(communication.getDirection()).isEqualTo(INBOUND);
				assertThat(communication.getExternalId()).isEqualTo(messagedto.getMessageId());
				assertThat(communication.getMessageBody()).isEqualTo(messagedto.getMessage());
				assertThat(communication.getSent()).isCloseTo(now(), within(1, SECONDS));
				assertThat(communication.getErrandNumber()).isEqualTo(errandEntity.getErrandNumber());
				assertThat(communication.getAttachments()).hasSize(1);
				assertThat(communication.getAttachments().getFirst().getAttachmentData()).isSameAs(communicationAttachmentDataEntityMock);
			});

		verifyNoMoreInteractions(webMessageCollectorClientMock, webMessageCollectorPropertiesMock, errandsRepositoryMock, communicationRepositoryMock, communicationServiceMock);
	}

	@Test
	void processMessage_NoErrandFound() {
		// Arrange
		final var externalCaseId = "123";
		final var familyId = "321";
		final var messagedto = new MessageDTO()
			.externalCaseId(externalCaseId)
			.familyId(familyId);

		// Mock
		when(errandsRepositoryMock.findOne(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(Optional.empty());

		try (final MockedStatic<ErrandSpecification> specificationMockedStatic = Mockito.mockStatic(ErrandSpecification.class)) {
			specificationMockedStatic.when(() -> ErrandSpecification.hasMatchingTags(any())).thenReturn(specificationMock);

			// Act
			webMessageCollectorWorker.processMessage(messagedto, MUNICIPALITY_ID);

			// Verify static
			specificationMockedStatic.verify(() -> ErrandSpecification.hasMatchingTags(dbExternalTagsArgumentCaptor.capture()));
			assertThat(dbExternalTagsArgumentCaptor.getValue())
				.hasSize(2)
				.extracting(DbExternalTag::getKey, DbExternalTag::getValue)
				.containsExactlyInAnyOrder(
					tuple("caseId", externalCaseId),
					tuple("familyId", familyId));
		}

		// Verify
		verify(errandsRepositoryMock).findOne(same(specificationMock));
		verifyNoMoreInteractions(webMessageCollectorClientMock, errandsRepositoryMock, communicationRepositoryMock, webMessageCollectorMapperMock, eventServiceMock);
	}

	@Test
	void getWebMessages() {
		// Arrange
		final var instance = "instance";
		final var familyId = "familyId";
		final var list = List.of(new MessageDTO());

		// Mock
		when(webMessageCollectorClientMock.getMessages(any(), any(), any())).thenReturn(list);
		// Act
		final var result = webMessageCollectorWorker.getWebMessages(instance, familyId, MUNICIPALITY_ID);
		// Verify
		verify(webMessageCollectorClientMock).getMessages(MUNICIPALITY_ID, familyId, instance);
		assertThat(result).isSameAs(list);
	}
}
