package se.sundsvall.supportmanagement.service.scheduler.notificationdispatch;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.NotificationChannelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.service.SubscriberNotificationService;

import static org.assertj.core.api.Assertions.assertThat;
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
	void send_internalChannel_callsUpsertAndReturnsTrue() {
		final var channel = NotificationChannelEmbeddable.create().withType(NotificationChannelType.INTERNAL);
		final var subscriber = SubscriberEntity.create().withChannels(List.of(channel));

		final var result = dispatcher.send(ERRAND_ID, ERRAND_NUMBER, subscriber);

		assertThat(result).isTrue();
		verify(subscriberNotificationServiceMock).upsert(ERRAND_ID, ERRAND_NUMBER, subscriber);
	}

	@Test
	void send_smsChannel_returnsFalse() {
		final var channel = NotificationChannelEmbeddable.create().withType(NotificationChannelType.SMS);
		final var subscriber = SubscriberEntity.create().withChannels(List.of(channel));

		final var result = dispatcher.send(ERRAND_ID, ERRAND_NUMBER, subscriber);

		assertThat(result).isFalse();
		verifyNoInteractions(subscriberNotificationServiceMock);
	}

	@Test
	void send_emailChannel_returnsFalse() {
		final var channel = NotificationChannelEmbeddable.create().withType(NotificationChannelType.EMAIL);
		final var subscriber = SubscriberEntity.create().withChannels(List.of(channel));

		final var result = dispatcher.send(ERRAND_ID, ERRAND_NUMBER, subscriber);

		assertThat(result).isFalse();
		verifyNoInteractions(subscriberNotificationServiceMock);
	}
}
