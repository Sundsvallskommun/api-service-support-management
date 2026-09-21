package se.sundsvall.supportmanagement.service.scheduler.notificationdispatch;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.NotificationChannelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.service.SubscriberNotificationService;
import se.sundsvall.supportmanagement.service.scheduler.emaildispatch.SubscriberEmailService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationChannelDispatcherTest {

	private static final String ERRAND_ID = "errand-id";
	private static final String ERRAND_NUMBER = "PRH-2022-000001";

	private static final List<NotificationDispatchEntity> EVENTS = List.of(NotificationDispatchEntity.create()
		.withId("dispatch-id")
		.withEventType("UPDATE")
		.withDescription("Bilaga har skapats")
		.withSubType("ATTACHMENT"));

	@Mock
	private SubscriberNotificationService subscriberNotificationServiceMock;

	@Mock
	private SubscriberEmailService subscriberEmailServiceMock;

	@InjectMocks
	private NotificationChannelDispatcher dispatcher;

	private static SubscriberEntity subscriberWith(final NotificationChannelType type) {
		return subscriberWith(type, null);
	}

	private static SubscriberEntity subscriberWith(final NotificationChannelType type, final String destination) {
		return SubscriberEntity.create()
			.withId("subscriber-id")
			.withChannels(List.of(NotificationChannelEmbeddable.create().withType(type).withDestination(destination)));
	}

	@Test
	void sendInternalChannelCreatesNotification() {

		// Arrange
		final var subscriber = subscriberWith(NotificationChannelType.INTERNAL);

		// Act
		dispatcher.send(ERRAND_ID, ERRAND_NUMBER, subscriber, EVENTS);

		// Assert
		verify(subscriberNotificationServiceMock).create(ERRAND_ID, ERRAND_NUMBER, subscriber, EVENTS);
		verifyNoInteractions(subscriberEmailServiceMock);
	}

	@Test
	void sendSmsChannelIsSkippedUntilImplemented() {

		// Arrange
		final var subscriber = subscriberWith(NotificationChannelType.SMS);

		// Act
		dispatcher.send(ERRAND_ID, ERRAND_NUMBER, subscriber, EVENTS);

		// Assert
		verifyNoInteractions(subscriberNotificationServiceMock, subscriberEmailServiceMock);
	}

	@Test
	void sendEmailChannelEnqueuesOutboxEntry() {

		// Arrange
		final var subscriber = subscriberWith(NotificationChannelType.EMAIL, "test@example.com");
		final var channel = subscriber.getChannels().getFirst();

		// Act
		dispatcher.send(ERRAND_ID, ERRAND_NUMBER, subscriber, EVENTS);

		// Assert
		verify(subscriberEmailServiceMock).enqueue(ERRAND_ID, ERRAND_NUMBER, channel, subscriber, EVENTS);
		verifyNoInteractions(subscriberNotificationServiceMock);
	}

	@Test
	void sendEmailChannelWithoutDestinationEnqueuesOutboxEntry() {

		// Arrange
		final var subscriber = subscriberWith(NotificationChannelType.EMAIL);
		final var channel = subscriber.getChannels().getFirst();

		// Act
		dispatcher.send(ERRAND_ID, ERRAND_NUMBER, subscriber, EVENTS);

		// Assert
		verify(subscriberEmailServiceMock).enqueue(ERRAND_ID, ERRAND_NUMBER, channel, subscriber, EVENTS);
		verifyNoInteractions(subscriberNotificationServiceMock);
	}

	@Test
	void sendPropagatesFailures() {

		// Arrange — failures must reach the worker so the whole group rolls back instead of being partially delivered
		final var subscriber = subscriberWith(NotificationChannelType.INTERNAL);
		doThrow(new RuntimeException("boom")).when(subscriberNotificationServiceMock).create(any(), any(), any(), any());

		// Act + Assert
		assertThatThrownBy(() -> dispatcher.send(ERRAND_ID, ERRAND_NUMBER, subscriber, EVENTS))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("boom");
	}
}
