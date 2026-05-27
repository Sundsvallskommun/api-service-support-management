package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.SubscriberNotificationRepository;
import se.sundsvall.supportmanagement.integration.db.model.SubscriberNotificationEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class SubscriberNotificationServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String NOTIFICATION_ID = "notification-id";
	private static final String ERRAND_ID = "errand-id";
	private static final String ERRAND_NUMBER = "PRH-2022-000001";
	private static final String IDENTIFIER_TYPE = "adAccount";
	private static final String IDENTIFIER_VALUE = "joe01doe";

	@Mock
	private SubscriberNotificationRepository repositoryMock;

	@InjectMocks
	private SubscriberNotificationService service;

	@Test
	void getNotifications() {
		final var entity = SubscriberNotificationEntity.create()
			.withId(NOTIFICATION_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withIdentifierType(IDENTIFIER_TYPE)
			.withIdentifierValue(IDENTIFIER_VALUE)
			.withErrandId(ERRAND_ID)
			.withErrandNumber(ERRAND_NUMBER);

		when(repositoryMock.findAllByMunicipalityIdAndNamespaceAndIdentifierTypeAndIdentifierValue(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER_TYPE, IDENTIFIER_VALUE))
			.thenReturn(List.of(entity));

		final var result = service.getNotifications(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER_TYPE, IDENTIFIER_VALUE);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getId()).isEqualTo(NOTIFICATION_ID);
		assertThat(result.getFirst().getErrandId()).isEqualTo(ERRAND_ID);
		verify(repositoryMock).findAllByMunicipalityIdAndNamespaceAndIdentifierTypeAndIdentifierValue(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER_TYPE, IDENTIFIER_VALUE);
	}

	@Test
	void deleteNotification() {
		final var entity = SubscriberNotificationEntity.create().withId(NOTIFICATION_ID);
		when(repositoryMock.findByIdAndMunicipalityIdAndNamespace(NOTIFICATION_ID, MUNICIPALITY_ID, NAMESPACE)).thenReturn(Optional.of(entity));

		service.deleteNotification(MUNICIPALITY_ID, NAMESPACE, NOTIFICATION_ID);

		verify(repositoryMock).findByIdAndMunicipalityIdAndNamespace(NOTIFICATION_ID, MUNICIPALITY_ID, NAMESPACE);
		verify(repositoryMock).delete(entity);
	}

	@Test
	void deleteNotification_notFound() {
		when(repositoryMock.findByIdAndMunicipalityIdAndNamespace(NOTIFICATION_ID, MUNICIPALITY_ID, NAMESPACE)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteNotification(MUNICIPALITY_ID, NAMESPACE, NOTIFICATION_ID))
			.isInstanceOf(Problem.class)
			.hasMessageContaining(NOT_FOUND.getReasonPhrase());

		verify(repositoryMock, never()).delete(any());
	}

	@Test
	void acknowledgeNotification() {
		final var entity = SubscriberNotificationEntity.create().withId(NOTIFICATION_ID);
		when(repositoryMock.findByIdAndMunicipalityIdAndNamespace(NOTIFICATION_ID, MUNICIPALITY_ID, NAMESPACE)).thenReturn(Optional.of(entity));

		service.acknowledgeNotification(MUNICIPALITY_ID, NAMESPACE, NOTIFICATION_ID);

		assertThat(entity.getAcknowledged()).isNotNull();
		verify(repositoryMock).save(entity);
	}

	@Test
	void acknowledgeNotification_notFound() {
		when(repositoryMock.findByIdAndMunicipalityIdAndNamespace(NOTIFICATION_ID, MUNICIPALITY_ID, NAMESPACE)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.acknowledgeNotification(MUNICIPALITY_ID, NAMESPACE, NOTIFICATION_ID))
			.isInstanceOf(Problem.class)
			.hasMessageContaining(NOT_FOUND.getReasonPhrase());

		verify(repositoryMock, never()).save(any());
	}

	@Test
	void upsert_existingNotification() {
		final var subscriber = buildSubscriber();
		final var existing = SubscriberNotificationEntity.create().withId(NOTIFICATION_ID);
		when(repositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndIdentifierTypeAndIdentifierValue(
			MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, IDENTIFIER_TYPE, IDENTIFIER_VALUE)).thenReturn(Optional.of(existing));

		service.upsert(ERRAND_ID, ERRAND_NUMBER, subscriber);

		verify(repositoryMock).save(existing);
	}

	@Test
	void upsert_newNotification() {
		final var subscriber = buildSubscriber();
		when(repositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndIdentifierTypeAndIdentifierValue(
			MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, IDENTIFIER_TYPE, IDENTIFIER_VALUE)).thenReturn(Optional.empty());

		service.upsert(ERRAND_ID, ERRAND_NUMBER, subscriber);

		verify(repositoryMock).save(any(SubscriberNotificationEntity.class));
	}

	private SubscriberEntity buildSubscriber() {
		return SubscriberEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE));
	}
}
