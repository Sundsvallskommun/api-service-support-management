package se.sundsvall.supportmanagement.service.scheduler.notificationdispatch;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.service.SubscriberNotificationService;
import se.sundsvall.supportmanagement.service.scheduler.emaildispatch.SubscriberEmailService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.EMAIL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.INTERNAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.SMS;

@ExtendWith(MockitoExtension.class)
class NotificationChannelDispatcherTest {

	private static final String ERRAND_ID = "errand-id";
	private static final String ERRAND_NUMBER = "PRH-2022-000001";

	private static final NotificationDispatchEntity ATTACHMENT_EVENT = NotificationDispatchEntity.create()
		.withId("dispatch-id-1")
		.withEventType("UPDATE")
		.withDescription("Bilaga har skapats")
		.withSubType("ATTACHMENT");

	private static final NotificationDispatchEntity MESSAGE_EVENT = NotificationDispatchEntity.create()
		.withId("dispatch-id-2")
		.withEventType("UPDATE")
		.withDescription("Nytt meddelande")
		.withSubType("MESSAGE");

	private static final SubscriberEntity SUBSCRIBER = SubscriberEntity.create().withId("subscriber-id");

	@Mock
	private SubscriberNotificationService subscriberNotificationServiceMock;

	@Mock
	private SubscriberEmailService subscriberEmailServiceMock;

	@InjectMocks
	private NotificationChannelDispatcher dispatcher;

	private static Map<NotificationChannelType, List<NotificationDispatchEntity>> deliveries(final NotificationChannelType type, final List<NotificationDispatchEntity> events) {
		final var deliveries = new EnumMap<NotificationChannelType, List<NotificationDispatchEntity>>(NotificationChannelType.class);
		deliveries.put(type, events);
		return deliveries;
	}

	@Test
	void sendInternalChannelCreatesNotification() {

		// Act
		dispatcher.send(ERRAND_ID, ERRAND_NUMBER, SUBSCRIBER, deliveries(INTERNAL, List.of(ATTACHMENT_EVENT)));

		// Assert
		verify(subscriberNotificationServiceMock).create(ERRAND_ID, ERRAND_NUMBER, SUBSCRIBER, List.of(ATTACHMENT_EVENT));
		verifyNoInteractions(subscriberEmailServiceMock);
	}

	@Test
	void sendSmsChannelIsSkippedUntilImplemented() {

		// Act
		dispatcher.send(ERRAND_ID, ERRAND_NUMBER, SUBSCRIBER, deliveries(SMS, List.of(ATTACHMENT_EVENT)));

		// Assert
		verifyNoInteractions(subscriberNotificationServiceMock, subscriberEmailServiceMock);
	}

	@Test
	void sendEmailChannelEnqueuesOutboxEntry() {

		// Act
		dispatcher.send(ERRAND_ID, ERRAND_NUMBER, SUBSCRIBER, deliveries(EMAIL, List.of(ATTACHMENT_EVENT)));

		// Assert
		verify(subscriberEmailServiceMock).enqueue(ERRAND_ID, ERRAND_NUMBER, SUBSCRIBER, List.of(ATTACHMENT_EVENT));
		verifyNoInteractions(subscriberNotificationServiceMock);
	}

	@Test
	void sendDeliversEachChannelItsOwnEvents() {

		// Arrange — email only carries the message, the internal notification carries both
		final var deliveries = deliveries(EMAIL, List.of(MESSAGE_EVENT));
		deliveries.put(INTERNAL, List.of(ATTACHMENT_EVENT, MESSAGE_EVENT));

		// Act
		dispatcher.send(ERRAND_ID, ERRAND_NUMBER, SUBSCRIBER, deliveries);

		// Assert
		verify(subscriberEmailServiceMock).enqueue(ERRAND_ID, ERRAND_NUMBER, SUBSCRIBER, List.of(MESSAGE_EVENT));
		verify(subscriberNotificationServiceMock).create(ERRAND_ID, ERRAND_NUMBER, SUBSCRIBER, List.of(ATTACHMENT_EVENT, MESSAGE_EVENT));
		verifyNoMoreInteractions(subscriberEmailServiceMock, subscriberNotificationServiceMock);
	}

	@Test
	void sendWithoutDeliveriesDoesNothing() {

		// Act
		dispatcher.send(ERRAND_ID, ERRAND_NUMBER, SUBSCRIBER, Map.of());

		// Assert
		verifyNoInteractions(subscriberNotificationServiceMock, subscriberEmailServiceMock);
	}

	@Test
	void sendPropagatesFailures() {

		// Arrange — failures must reach the worker so the whole group rolls back instead of being partially delivered
		doThrow(new RuntimeException("boom")).when(subscriberNotificationServiceMock).create(any(), any(), any(), any());
		final var deliveries = deliveries(INTERNAL, List.of(ATTACHMENT_EVENT));

		// Act + Assert
		assertThatThrownBy(() -> dispatcher.send(ERRAND_ID, ERRAND_NUMBER, SUBSCRIBER, deliveries))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("boom");
	}
}
