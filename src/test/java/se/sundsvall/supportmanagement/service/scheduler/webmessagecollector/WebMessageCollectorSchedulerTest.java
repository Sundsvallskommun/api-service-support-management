package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class WebMessageCollectorSchedulerTest {

	@Mock
	WebMessageCollectorWorker webMessageCollectorWorker;

	@InjectMocks
	WebMessageCollectorScheduler scheduler;

	@Test
	void fetchWebMessages() {
		// Act
		scheduler.fetchWebMessages();
		//Verify
		verify(webMessageCollectorWorker).fetchWebMessages();
		verifyNoMoreInteractions(webMessageCollectorWorker);
	}

}
