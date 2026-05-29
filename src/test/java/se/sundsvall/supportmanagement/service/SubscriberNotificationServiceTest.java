package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriberNotificationRepository;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigValueEmbeddable;
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
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.INTEGER;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_NOTIFICATION_TTL_IN_DAYS;

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

	@Mock
	private NamespaceConfigRepository namespaceConfigRepositoryMock;

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

		when(repositoryMock.findActiveByMunicipalityIdAndNamespaceAndIdentifierTypeAndIdentifierValue(
			any(), any(), any(), any(), any(), any()))
			.thenReturn(new PageImpl<>(List.of(entity)));

		final var result = service.getNotifications(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER_TYPE, IDENTIFIER_VALUE, Pageable.unpaged());

		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().getFirst().getId()).isEqualTo(NOTIFICATION_ID);
		assertThat(result.getContent().getFirst().getErrandId()).isEqualTo(ERRAND_ID);
		verify(repositoryMock).findActiveByMunicipalityIdAndNamespaceAndIdentifierTypeAndIdentifierValue(
			any(), any(), any(), any(), any(), any());
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
	void upsert_existingNotification_resetsAcknowledgedAndUpdatesErrandNumber() {
		final var subscriber = buildSubscriber();
		final var existing = SubscriberNotificationEntity.create().withId(NOTIFICATION_ID);
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(buildNamespaceConfig()));
		when(repositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndIdentifierTypeAndIdentifierValue(
			MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, IDENTIFIER_TYPE, IDENTIFIER_VALUE)).thenReturn(Optional.of(existing));

		service.upsert(ERRAND_ID, ERRAND_NUMBER, subscriber);

		assertThat(existing.getErrandNumber()).isEqualTo(ERRAND_NUMBER);
		assertThat(existing.getAcknowledged()).isNull();
		verify(repositoryMock).save(existing);
	}

	@Test
	void upsert_newNotification_setsExpiry() {
		final var subscriber = buildSubscriber();
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(buildNamespaceConfig()));
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

	private NamespaceConfigEntity buildNamespaceConfig() {
		return NamespaceConfigEntity.create()
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withValue(NamespaceConfigValueEmbeddable.create()
				.withKey(PROPERTY_NOTIFICATION_TTL_IN_DAYS)
				.withType(INTEGER)
				.withValue("30"));
	}
}
