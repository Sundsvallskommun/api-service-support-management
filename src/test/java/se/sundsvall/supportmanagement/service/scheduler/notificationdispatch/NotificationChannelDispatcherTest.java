package se.sundsvall.supportmanagement.service.scheduler.notificationdispatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.NotificationChannelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.service.SubscriberNotificationService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationChannelDispatcherTest {

	private static final String ERRAND_ID = "errand-id";
	private static final String ERRAND_NUMBER = "PRH-2022-000001";

	@Mock
	private SubscriberNotificationService subscriberNotificationServiceMock;

	@InjectMocks
	private NotificationChannelDispatcher dispatcher;

	@Test
	void send_internalChannel_callsUpsert() {
		final var subscriber = SubscriberEntity.create();
		final var channel = NotificationChannelEmbeddable.create().withType(NotificationChannelType.INTERNAL);

		dispatcher.send(ERRAND_ID, ERRAND_NUMBER, subscriber, channel);

		verify(subscriberNotificationServiceMock).upsert(ERRAND_ID, ERRAND_NUMBER, subscriber);
	}

	@Test
	void send_smsChannel_throwsUnsupportedOperationException() {
		final var subscriber = SubscriberEntity.create();
		final var channel = NotificationChannelEmbeddable.create().withType(NotificationChannelType.SMS);

		assertThatThrownBy(() -> dispatcher.send(ERRAND_ID, ERRAND_NUMBER, subscriber, channel))
			.isInstanceOf(UnsupportedOperationException.class)
			.hasMessageContaining("SMS");

		verifyNoInteractions(subscriberNotificationServiceMock);
	}

	@Test
	void send_emailChannel_throwsUnsupportedOperationException() {
		final var subscriber = SubscriberEntity.create();
		final var channel = NotificationChannelEmbeddable.create().withType(NotificationChannelType.EMAIL);

		assertThatThrownBy(() -> dispatcher.send(ERRAND_ID, ERRAND_NUMBER, subscriber, channel))
			.isInstanceOf(UnsupportedOperationException.class)
			.hasMessageContaining("EMAIL");

		verifyNoInteractions(subscriberNotificationServiceMock);
	}
}
