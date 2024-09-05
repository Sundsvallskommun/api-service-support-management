package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentEntity;


@ExtendWith(MockitoExtension.class)
class WebMessageCollectorSchedulerTest {

	@Mock
	private WebMessageCollectorWorker webMessageCollectorWorkerMock;

	@InjectMocks
	private WebMessageCollectorScheduler scheduler;

	@Captor
	private ArgumentCaptor<CommunicationAttachmentEntity> communicationAttachmentEntityCaptor;

	@Test
	void fetchWebMessages() {
		// Arrange
		final var entity = new CommunicationAttachmentEntity();
		when(webMessageCollectorWorkerMock.fetchWebMessages()).thenReturn(Map.of("2281", List.of(entity)));

		// Act
		scheduler.fetchWebMessages();
		//Verify
		verify(webMessageCollectorWorkerMock).fetchWebMessages();
		verify(webMessageCollectorWorkerMock).processAttachments(communicationAttachmentEntityCaptor.capture(), eq("2281"));
		assertThat(communicationAttachmentEntityCaptor.getValue()).isSameAs(entity);
		verifyNoMoreInteractions(webMessageCollectorWorkerMock);
	}

}
