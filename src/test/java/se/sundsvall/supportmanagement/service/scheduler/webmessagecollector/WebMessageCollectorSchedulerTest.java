package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import generated.se.sundsvall.webmessagecollector.MessageDTO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.supportmanagement.integration.db.WebMessageCollectRepository;
import se.sundsvall.supportmanagement.integration.db.model.WebMessageCollectEntity;

@ExtendWith(MockitoExtension.class)
class WebMessageCollectorSchedulerTest {

	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String INSTANCE = "instance";
	private static final List<String> FAMILY_IDS = List.of("1", "2");

	@Mock
	private WebMessageCollectorWorker webMessageCollectorWorkerMock;

	@Mock
	private WebMessageCollectRepository webMessageCollectRepositoryMock;

	@Mock
	private Dept44HealthUtility healthIndicatorMock;

	@InjectMocks
	private WebMessageCollectorScheduler scheduler;

	@Captor
	private ArgumentCaptor<MessageDTO> messageCaptor;

	@Captor
	private ArgumentCaptor<String> familyIdCaptor;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(scheduler, "jobName", "web-message-collector");
	}

	@Test
	void fetchWebMessages() {
		// Arrange
		final var message1 = new MessageDTO();
		final var message2 = new MessageDTO();

		// Mock
		when(webMessageCollectRepositoryMock.findAll()).thenReturn(List.of(WebMessageCollectEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withInstance(INSTANCE)
			.withFamilyIds(FAMILY_IDS)));
		when(webMessageCollectorWorkerMock.getWebMessages(any(), eq("1"), any())).thenReturn(List.of(message1));
		when(webMessageCollectorWorkerMock.getWebMessages(any(), eq("2"), any())).thenReturn(List.of(message2));

		// Act
		scheduler.fetchWebMessages();

		// Verify
		verify(webMessageCollectRepositoryMock).findAll();
		verify(webMessageCollectorWorkerMock, times(2)).getWebMessages(eq(INSTANCE), familyIdCaptor.capture(), eq(MUNICIPALITY_ID));
		verify(webMessageCollectorWorkerMock, times(2)).processMessage(messageCaptor.capture(), eq(MUNICIPALITY_ID));
		verifyNoMoreInteractions(webMessageCollectorWorkerMock, webMessageCollectRepositoryMock, healthIndicatorMock);

		assertThat(familyIdCaptor.getAllValues()).containsExactly("1", "2");
		assertThat(messageCaptor.getAllValues()).satisfiesExactly(
			first -> assertThat(first).isSameAs(message1),
			second -> assertThat(second).isSameAs(message2));
	}

	@Test
	void fetchWebMessagesErrorFetchingForFamilyId() {
		// Arrange
		final var message2 = new MessageDTO();

		// Mock
		when(webMessageCollectRepositoryMock.findAll()).thenReturn(List.of(WebMessageCollectEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withInstance(INSTANCE)
			.withFamilyIds(FAMILY_IDS)));
		when(webMessageCollectorWorkerMock.getWebMessages(any(), eq("1"), any())).thenThrow(new RuntimeException("ERROR"));
		when(webMessageCollectorWorkerMock.getWebMessages(any(), eq("2"), any())).thenReturn(List.of(message2));

		// Act
		scheduler.fetchWebMessages();

		// Verify
		verify(webMessageCollectRepositoryMock).findAll();
		verify(webMessageCollectorWorkerMock, times(2)).getWebMessages(eq(INSTANCE), familyIdCaptor.capture(), eq(MUNICIPALITY_ID));
		verify(healthIndicatorMock).setHealthIndicatorUnhealthy("web-message-collector", "Error fetching web messages");
		verify(webMessageCollectorWorkerMock, times(1)).processMessage(same(message2), eq(MUNICIPALITY_ID));
		verifyNoMoreInteractions(webMessageCollectorWorkerMock, webMessageCollectRepositoryMock, healthIndicatorMock);

		assertThat(familyIdCaptor.getAllValues()).containsExactly("1", "2");
	}

	@Test
	void fetchWebMessagesErrorProcessingMessage() {
		// Arrange
		final var message1 = new MessageDTO();
		final var message2 = new MessageDTO();

		// Mock
		when(webMessageCollectRepositoryMock.findAll()).thenReturn(List.of(WebMessageCollectEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withInstance(INSTANCE)
			.withFamilyIds(FAMILY_IDS)));
		when(webMessageCollectorWorkerMock.getWebMessages(any(), eq("1"), any())).thenReturn(List.of(message1));
		when(webMessageCollectorWorkerMock.getWebMessages(any(), eq("2"), any())).thenReturn(List.of(message2));
		doThrow(new RuntimeException("ERROR")).when(webMessageCollectorWorkerMock).processMessage(same(message1), any());

		// Act
		scheduler.fetchWebMessages();

		// Verify
		verify(webMessageCollectRepositoryMock).findAll();
		verify(webMessageCollectorWorkerMock, times(2)).getWebMessages(eq(INSTANCE), familyIdCaptor.capture(), eq(MUNICIPALITY_ID));
		verify(webMessageCollectorWorkerMock, times(2)).processMessage(messageCaptor.capture(), eq(MUNICIPALITY_ID));
		verify(healthIndicatorMock).setHealthIndicatorUnhealthy("web-message-collector", "Error processing individual web messages");
		verifyNoMoreInteractions(webMessageCollectorWorkerMock, webMessageCollectRepositoryMock, healthIndicatorMock);
		assertThat(familyIdCaptor.getAllValues()).containsExactly("1", "2");
		assertThat(messageCaptor.getAllValues()).satisfiesExactly(
			first -> assertThat(first).isSameAs(message1),
			second -> assertThat(second).isSameAs(message2));
	}
}
