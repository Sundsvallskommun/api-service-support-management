package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.subscriber.EventFilter;
import se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannelType;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionProfile;
import se.sundsvall.supportmanagement.integration.db.SubscriptionProfileRepository;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionProfileEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class SubscriptionProfileServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final String PROFILE_ID = "profile-1";
	private static final String NAME = "Mejl om nya ärenden";

	@Mock
	private SubscriptionProfileRepository subscriptionProfileRepositoryMock;

	@InjectMocks
	private SubscriptionProfileService service;

	@Captor
	private ArgumentCaptor<SubscriptionProfileEntity> entityCaptor;

	@AfterEach
	void verifyNoMore() {
		verifyNoMoreInteractions(subscriptionProfileRepositoryMock);
	}

	@Test
	void findSubscriptionProfiles() {
		when(subscriptionProfileRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(List.of(SubscriptionProfileEntity.create().withId(PROFILE_ID).withName(NAME)));

		final var result = service.findSubscriptionProfiles(MUNICIPALITY_ID, NAMESPACE);

		assertThat(result).extracting(SubscriptionProfile::getId, SubscriptionProfile::getName)
			.containsExactly(tuple(PROFILE_ID, NAME));
		verify(subscriptionProfileRepositoryMock).findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void findSubscriptionProfile() {
		when(subscriptionProfileRepositoryMock.findByIdAndNamespaceAndMunicipalityId(PROFILE_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(SubscriptionProfileEntity.create().withId(PROFILE_ID).withName(NAME)));

		final var result = service.findSubscriptionProfile(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID);

		assertThat(result.getId()).isEqualTo(PROFILE_ID);
		assertThat(result.getName()).isEqualTo(NAME);
		verify(subscriptionProfileRepositoryMock).findByIdAndNamespaceAndMunicipalityId(PROFILE_ID, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void findSubscriptionProfileNotFound() {
		when(subscriptionProfileRepositoryMock.findByIdAndNamespaceAndMunicipalityId(PROFILE_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findSubscriptionProfile(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(subscriptionProfileRepositoryMock).findByIdAndNamespaceAndMunicipalityId(PROFILE_ID, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void createSubscriptionProfile() {
		final var profile = SubscriptionProfile.create()
			.withName(NAME)
			.withEventFilters(List.of(EventFilter.create().withType("CREATE").withSubtype("ERRAND")))
			.withChannels(List.of(NotificationChannelType.EMAIL));
		when(subscriptionProfileRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, NAME)).thenReturn(false);
		when(subscriptionProfileRepositoryMock.saveAndFlush(any(SubscriptionProfileEntity.class))).thenAnswer(inv -> inv.<SubscriptionProfileEntity>getArgument(0).withId(PROFILE_ID));

		final var result = service.createSubscriptionProfile(MUNICIPALITY_ID, NAMESPACE, profile);

		assertThat(result).isEqualTo(PROFILE_ID);
		verify(subscriptionProfileRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, NAME);
		verify(subscriptionProfileRepositoryMock).saveAndFlush(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(entityCaptor.getValue().getNamespace()).isEqualTo(NAMESPACE);
		assertThat(entityCaptor.getValue().getChannels()).containsExactly(se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.EMAIL);
	}

	@Test
	void createSubscriptionProfileWithExistingName() {
		final var profile = SubscriptionProfile.create().withName(NAME);
		when(subscriptionProfileRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, NAME)).thenReturn(true);

		assertThatThrownBy(() -> service.createSubscriptionProfile(MUNICIPALITY_ID, NAMESPACE, profile))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(CONFLICT);

		verify(subscriptionProfileRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, NAME);
	}

	@Test
	void createSubscriptionProfileLosingRaceOnName() {
		final var profile = SubscriptionProfile.create().withName(NAME);
		when(subscriptionProfileRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, NAME)).thenReturn(false);
		when(subscriptionProfileRepositoryMock.saveAndFlush(any(SubscriptionProfileEntity.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

		assertThatThrownBy(() -> service.createSubscriptionProfile(MUNICIPALITY_ID, NAMESPACE, profile))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(CONFLICT);

		verify(subscriptionProfileRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, NAME);
		verify(subscriptionProfileRepositoryMock).saveAndFlush(any(SubscriptionProfileEntity.class));
	}

	@Test
	void updateSubscriptionProfile() {
		final var entity = SubscriptionProfileEntity.create().withId(PROFILE_ID).withName(NAME).withDescription("Gammal");
		when(subscriptionProfileRepositoryMock.findByIdAndNamespaceAndMunicipalityId(PROFILE_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(entity));
		when(subscriptionProfileRepositoryMock.saveAndFlush(entity)).thenReturn(entity);

		final var result = service.updateSubscriptionProfile(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID, SubscriptionProfile.create().withDescription("Ny"));

		assertThat(result.getDescription()).isEqualTo("Ny");
		assertThat(result.getName()).isEqualTo(NAME);
		verify(subscriptionProfileRepositoryMock).findByIdAndNamespaceAndMunicipalityId(PROFILE_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(subscriptionProfileRepositoryMock).saveAndFlush(entity);
	}

	@Test
	void updateSubscriptionProfileOntoExistingName() {
		final var entity = SubscriptionProfileEntity.create().withId(PROFILE_ID).withName(NAME);
		final var patch = SubscriptionProfile.create().withName("Upptaget namn");
		when(subscriptionProfileRepositoryMock.findByIdAndNamespaceAndMunicipalityId(PROFILE_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(entity));
		when(subscriptionProfileRepositoryMock.saveAndFlush(entity)).thenThrow(new DataIntegrityViolationException("duplicate"));

		assertThatThrownBy(() -> service.updateSubscriptionProfile(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID, patch))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(CONFLICT);

		verify(subscriptionProfileRepositoryMock).findByIdAndNamespaceAndMunicipalityId(PROFILE_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(subscriptionProfileRepositoryMock).saveAndFlush(entity);
	}

	@Test
	void deleteSubscriptionProfile() {
		final var entity = SubscriptionProfileEntity.create().withId(PROFILE_ID);
		when(subscriptionProfileRepositoryMock.findByIdAndNamespaceAndMunicipalityId(PROFILE_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(entity));

		service.deleteSubscriptionProfile(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID);

		verify(subscriptionProfileRepositoryMock).findByIdAndNamespaceAndMunicipalityId(PROFILE_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(subscriptionProfileRepositoryMock).delete(entity);
	}

	@Test
	void deleteSubscriptionProfileNotFound() {
		when(subscriptionProfileRepositoryMock.findByIdAndNamespaceAndMunicipalityId(PROFILE_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteSubscriptionProfile(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(subscriptionProfileRepositoryMock).findByIdAndNamespaceAndMunicipalityId(PROFILE_ID, NAMESPACE, MUNICIPALITY_ID);
	}
}
