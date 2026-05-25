package se.sundsvall.supportmanagement.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.identifier.Identifier;
import se.sundsvall.supportmanagement.api.model.subscriber.Subscriber;
import se.sundsvall.supportmanagement.integration.db.SubscriberRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriptionRepository;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.dept44.support.Identifier.Type.AD_ACCOUNT;

@ExtendWith(MockitoExtension.class)
class SubscriberServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";

	@Mock
	private SubscriberRepository subscriberRepositoryMock;

	@Mock
	private SubscriptionRepository subscriptionRepositoryMock;

	@InjectMocks
	private SubscriberService service;

	@Captor
	private ArgumentCaptor<SubscriberEntity> entityCaptor;

	@BeforeEach
	void setExecutingUser() {
		se.sundsvall.dept44.support.Identifier.set(
			se.sundsvall.dept44.support.Identifier.create().withType(AD_ACCOUNT).withValue("joe01doe"));
	}

	@AfterEach
	void clearExecutingUser() {
		se.sundsvall.dept44.support.Identifier.remove();
	}

	@Test
	void findSubscribersWithoutFilter() {
		final var entity = SubscriberEntity.create().withId(randomUUID().toString()).withName("a")
			.withIdentifier(IdentifierEmbeddable.create().withType("adAccount").withValue("joe01doe"));
		when(subscriberRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of(entity));
		when(subscriptionRepositoryMock.countBySubscriberId(entity.getId())).thenReturn(3L);

		final var result = service.findSubscribers(MUNICIPALITY_ID, NAMESPACE, null, null);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getId()).isEqualTo(entity.getId());
		assertThat(result.getFirst().getSubscriptionCount()).isEqualTo(3);
		verify(subscriberRepositoryMock).findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verify(subscriptionRepositoryMock).countBySubscriberId(entity.getId());
		verifyNoMoreInteractions(subscriberRepositoryMock, subscriptionRepositoryMock);
	}

	@Test
	void findSubscribersWithIdentifierFilter() {
		when(subscriberRepositoryMock.findAllByNamespaceAndMunicipalityIdAndIdentifierTypeAndIdentifierValue(
			NAMESPACE, MUNICIPALITY_ID, "adAccount", "joe01doe")).thenReturn(List.of());

		final var result = service.findSubscribers(MUNICIPALITY_ID, NAMESPACE, "adAccount", "joe01doe");

		assertThat(result).isEmpty();
		verify(subscriberRepositoryMock).findAllByNamespaceAndMunicipalityIdAndIdentifierTypeAndIdentifierValue(
			NAMESPACE, MUNICIPALITY_ID, "adAccount", "joe01doe");
		verifyNoMoreInteractions(subscriberRepositoryMock);
		verifyNoInteractions(subscriptionRepositoryMock);
	}

	@Test
	void findSubscribersFailsWhenOnlyOneFilterPartGiven() {
		assertThatThrownBy(() -> service.findSubscribers(MUNICIPALITY_ID, NAMESPACE, "adAccount", null))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(BAD_REQUEST);

		assertThatThrownBy(() -> service.findSubscribers(MUNICIPALITY_ID, NAMESPACE, null, "joe01doe"))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(BAD_REQUEST);

		verifyNoInteractions(subscriberRepositoryMock, subscriptionRepositoryMock);
	}

	@Test
	void findSubscriber() {
		final var id = randomUUID().toString();
		final var entity = SubscriberEntity.create().withId(id).withName("a")
			.withIdentifier(IdentifierEmbeddable.create().withType("adAccount").withValue("joe01doe"))
			.withCreated(OffsetDateTime.now());
		when(subscriberRepositoryMock.findByIdAndNamespaceAndMunicipalityId(id, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(entity));
		when(subscriptionRepositoryMock.countBySubscriberId(id)).thenReturn(0L);

		final var result = service.findSubscriber(MUNICIPALITY_ID, NAMESPACE, id);

		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getSubscriptionCount()).isZero();
		verify(subscriberRepositoryMock).findByIdAndNamespaceAndMunicipalityId(id, NAMESPACE, MUNICIPALITY_ID);
		verify(subscriptionRepositoryMock).countBySubscriberId(id);
		verifyNoMoreInteractions(subscriberRepositoryMock, subscriptionRepositoryMock);
	}

	@Test
	void findSubscriberNotFound() {
		final var id = randomUUID().toString();
		when(subscriberRepositoryMock.findByIdAndNamespaceAndMunicipalityId(id, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findSubscriber(MUNICIPALITY_ID, NAMESPACE, id))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(subscriberRepositoryMock).findByIdAndNamespaceAndMunicipalityId(id, NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(subscriberRepositoryMock);
		verifyNoInteractions(subscriptionRepositoryMock);
	}

	@Test
	void createSubscriberHappyPath() {
		final var dto = Subscriber.create()
			.withName("Servicedesk")
			.withIdentifier(Identifier.create().withType("adAccount").withValue("joe01doe"));
		final var newId = randomUUID().toString();
		when(subscriberRepositoryMock.existsByNamespaceAndMunicipalityIdAndIdentifierTypeAndIdentifierValueAndName(
			NAMESPACE, MUNICIPALITY_ID, "adAccount", "joe01doe", "Servicedesk")).thenReturn(false);
		when(subscriberRepositoryMock.save(any(SubscriberEntity.class))).thenAnswer(inv -> inv.<SubscriberEntity>getArgument(0).withId(newId));

		final var result = service.createSubscriber(MUNICIPALITY_ID, NAMESPACE, dto);

		assertThat(result).isEqualTo(newId);
		verify(subscriberRepositoryMock).existsByNamespaceAndMunicipalityIdAndIdentifierTypeAndIdentifierValueAndName(
			NAMESPACE, MUNICIPALITY_ID, "adAccount", "joe01doe", "Servicedesk");
		verify(subscriberRepositoryMock).save(entityCaptor.capture());
		final var saved = entityCaptor.getValue();
		assertThat(saved.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(saved.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(saved.getName()).isEqualTo("Servicedesk");
		assertThat(saved.getIdentifier().getType()).isEqualTo("adAccount");
		assertThat(saved.getIdentifier().getValue()).isEqualTo("joe01doe");
		assertThat(saved.getCreatedBy().getType()).isEqualTo("adAccount");
		assertThat(saved.getCreatedBy().getValue()).isEqualTo("joe01doe");
		verifyNoMoreInteractions(subscriberRepositoryMock);
		verifyNoInteractions(subscriptionRepositoryMock);
	}

	@Test
	void createSubscriberSkipsConflictCheckWhenNameIsNull() {
		final var dto = Subscriber.create()
			.withIdentifier(Identifier.create().withType("adAccount").withValue("joe01doe"));
		when(subscriberRepositoryMock.save(any(SubscriberEntity.class)))
			.thenAnswer(inv -> inv.<SubscriberEntity>getArgument(0).withId(randomUUID().toString()));

		service.createSubscriber(MUNICIPALITY_ID, NAMESPACE, dto);

		verify(subscriberRepositoryMock, never()).existsByNamespaceAndMunicipalityIdAndIdentifierTypeAndIdentifierValueAndName(
			any(), any(), any(), any(), any());
		verify(subscriberRepositoryMock).save(any(SubscriberEntity.class));
		verifyNoMoreInteractions(subscriberRepositoryMock);
		verifyNoInteractions(subscriptionRepositoryMock);
	}

	@Test
	void createSubscriberConflict() {
		final var dto = Subscriber.create()
			.withName("Servicedesk")
			.withIdentifier(Identifier.create().withType("adAccount").withValue("joe01doe"));
		when(subscriberRepositoryMock.existsByNamespaceAndMunicipalityIdAndIdentifierTypeAndIdentifierValueAndName(
			NAMESPACE, MUNICIPALITY_ID, "adAccount", "joe01doe", "Servicedesk")).thenReturn(true);

		assertThatThrownBy(() -> service.createSubscriber(MUNICIPALITY_ID, NAMESPACE, dto))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(CONFLICT);

		verify(subscriberRepositoryMock).existsByNamespaceAndMunicipalityIdAndIdentifierTypeAndIdentifierValueAndName(
			NAMESPACE, MUNICIPALITY_ID, "adAccount", "joe01doe", "Servicedesk");
		verify(subscriberRepositoryMock, never()).save(any());
		verifyNoMoreInteractions(subscriberRepositoryMock);
		verifyNoInteractions(subscriptionRepositoryMock);
	}

	@Test
	void updateSubscriberAppliesPatch() {
		final var id = randomUUID().toString();
		final var existing = SubscriberEntity.create().withId(id).withName("old")
			.withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE)
			.withIdentifier(IdentifierEmbeddable.create().withType("adAccount").withValue("joe01doe"));
		when(subscriberRepositoryMock.findByIdAndNamespaceAndMunicipalityId(id, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(existing));
		when(subscriberRepositoryMock.save(any(SubscriberEntity.class))).thenAnswer(inv -> inv.getArgument(0));
		when(subscriptionRepositoryMock.countBySubscriberId(id)).thenReturn(0L);

		final var patch = Subscriber.create().withName("new");
		final var result = service.updateSubscriber(MUNICIPALITY_ID, NAMESPACE, id, patch);

		assertThat(result.getName()).isEqualTo("new");
		verify(subscriberRepositoryMock).findByIdAndNamespaceAndMunicipalityId(id, NAMESPACE, MUNICIPALITY_ID);
		verify(subscriberRepositoryMock).save(entityCaptor.capture());
		verify(subscriptionRepositoryMock).countBySubscriberId(id);
		assertThat(entityCaptor.getValue().getName()).isEqualTo("new");
		// Identifier untouched
		assertThat(entityCaptor.getValue().getIdentifier().getValue()).isEqualTo("joe01doe");
		verifyNoMoreInteractions(subscriberRepositoryMock, subscriptionRepositoryMock);
	}

	@Test
	void updateSubscriberNotFound() {
		final var id = randomUUID().toString();
		when(subscriberRepositoryMock.findByIdAndNamespaceAndMunicipalityId(id, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.updateSubscriber(MUNICIPALITY_ID, NAMESPACE, id, Subscriber.create()))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(subscriberRepositoryMock).findByIdAndNamespaceAndMunicipalityId(id, NAMESPACE, MUNICIPALITY_ID);
		verify(subscriberRepositoryMock, never()).save(any());
		verifyNoMoreInteractions(subscriberRepositoryMock);
		verifyNoInteractions(subscriptionRepositoryMock);
	}

	@Test
	void deleteSubscriber() {
		final var id = randomUUID().toString();
		final var entity = SubscriberEntity.create().withId(id);
		when(subscriberRepositoryMock.findByIdAndNamespaceAndMunicipalityId(id, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(entity));

		service.deleteSubscriber(MUNICIPALITY_ID, NAMESPACE, id);

		verify(subscriberRepositoryMock).findByIdAndNamespaceAndMunicipalityId(id, NAMESPACE, MUNICIPALITY_ID);
		verify(subscriberRepositoryMock).delete(entity);
		verifyNoMoreInteractions(subscriberRepositoryMock);
		verifyNoInteractions(subscriptionRepositoryMock);
	}

	@Test
	void deleteSubscriberNotFound() {
		final var id = randomUUID().toString();
		when(subscriberRepositoryMock.findByIdAndNamespaceAndMunicipalityId(id, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteSubscriber(MUNICIPALITY_ID, NAMESPACE, id))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(subscriberRepositoryMock).findByIdAndNamespaceAndMunicipalityId(id, NAMESPACE, MUNICIPALITY_ID);
		verify(subscriberRepositoryMock, never()).delete(any(SubscriberEntity.class));
		verifyNoMoreInteractions(subscriberRepositoryMock);
		verifyNoInteractions(subscriptionRepositoryMock);
	}
}
