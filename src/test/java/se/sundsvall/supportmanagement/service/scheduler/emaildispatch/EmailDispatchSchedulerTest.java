package se.sundsvall.supportmanagement.service.scheduler.emaildispatch;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.model.EmailDispatchOutboxEntity;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailDispatchSchedulerTest {

	@Mock
	private SubscriberEmailService subscriberEmailServiceMock;

	@InjectMocks
	private EmailDispatchScheduler scheduler;

	@Test
	void processEmailDispatchWithEntries() {
		final var entry1 = EmailDispatchOutboxEntity.create().withId("id-1");
		final var entry2 = EmailDispatchOutboxEntity.create().withId("id-2");

		when(subscriberEmailServiceMock.fetchPending()).thenReturn(List.of(entry1, entry2));

		scheduler.processEmailDispatch();

		verify(subscriberEmailServiceMock).fetchPending();
		verify(subscriberEmailServiceMock).processEntry(entry1);
		verify(subscriberEmailServiceMock).processEntry(entry2);
		verifyNoMoreInteractions(subscriberEmailServiceMock);
	}

	@Test
	void processEmailDispatchWithNoEntries() {
		when(subscriberEmailServiceMock.fetchPending()).thenReturn(List.of());

		scheduler.processEmailDispatch();

		verify(subscriberEmailServiceMock).fetchPending();
		verifyNoMoreInteractions(subscriberEmailServiceMock);
	}
}
