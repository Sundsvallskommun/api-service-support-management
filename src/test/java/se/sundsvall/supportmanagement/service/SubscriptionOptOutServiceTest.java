package se.sundsvall.supportmanagement.service;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.SubscriptionOptOutRepository;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.DbSubscriptionTargetType;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionOptOutEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionProfileEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionOptOutServiceTest {

	private static final String PROFILE_ID = "profile-1";
	private static final String AD_ACCOUNT = "adAccount";
	private static final SubscriptionProfileEntity PROFILE = SubscriptionProfileEntity.create().withId(PROFILE_ID);
	private static final IdentifierEmbeddable ANNA = IdentifierEmbeddable.create().withType(AD_ACCOUNT).withValue("anna01");

	@Mock
	private SubscriptionOptOutRepository subscriptionOptOutRepositoryMock;

	@InjectMocks
	private SubscriptionOptOutService service;

	@Captor
	private ArgumentCaptor<SubscriptionOptOutEntity> optOutCaptor;

	@AfterEach
	void verifyNoMore() {
		verifyNoMoreInteractions(subscriptionOptOutRepositoryMock);
	}

	private static SubscriptionEntity subscription(final DbSubscriptionTargetType targetType, final SubscriptionProfileEntity profile) {
		return SubscriptionEntity.create()
			.withSubscriber(SubscriberEntity.create().withId("subscriber-1").withIdentifier(ANNA))
			.withTargetType(targetType)
			.withProfile(profile);
	}

	@Test
	void isProfileMembership() {
		assertThat(SubscriptionOptOutService.isProfileMembership(subscription(DbSubscriptionTargetType.NAMESPACE, PROFILE))).isTrue();
		assertThat(SubscriptionOptOutService.isProfileMembership(subscription(DbSubscriptionTargetType.ERRAND, PROFILE))).isFalse();
		assertThat(SubscriptionOptOutService.isProfileMembership(subscription(DbSubscriptionTargetType.NAMESPACE, null))).isFalse();
	}

	@Test
	void recordOptOutOfMembership() {
		when(subscriptionOptOutRepositoryMock.existsByProfileIdAndIdentifierTypeAndIdentifierValue(PROFILE_ID, AD_ACCOUNT, "anna01")).thenReturn(false);

		service.recordOptOut(subscription(DbSubscriptionTargetType.NAMESPACE, PROFILE));

		verify(subscriptionOptOutRepositoryMock).existsByProfileIdAndIdentifierTypeAndIdentifierValue(PROFILE_ID, AD_ACCOUNT, "anna01");
		verify(subscriptionOptOutRepositoryMock).save(optOutCaptor.capture());
		assertThat(optOutCaptor.getValue().getProfile()).isSameAs(PROFILE);
		// A copy, so the record does not share the subscriber's embeddable
		assertThat(optOutCaptor.getValue().getIdentifier()).isEqualTo(ANNA).isNotSameAs(ANNA);
	}

	@Test
	void recordOptOutAlreadyRecorded() {
		when(subscriptionOptOutRepositoryMock.existsByProfileIdAndIdentifierTypeAndIdentifierValue(PROFILE_ID, AD_ACCOUNT, "anna01")).thenReturn(true);

		service.recordOptOut(subscription(DbSubscriptionTargetType.NAMESPACE, PROFILE));

		verify(subscriptionOptOutRepositoryMock).existsByProfileIdAndIdentifierTypeAndIdentifierValue(PROFILE_ID, AD_ACCOUNT, "anna01");
	}

	@Test
	void recordOptOutOfNoMembershipDoesNothing() {
		service.recordOptOut(subscription(DbSubscriptionTargetType.ERRAND, PROFILE));
		service.recordOptOut(subscription(DbSubscriptionTargetType.NAMESPACE, null));
	}

	@Test
	void clearOptOut() {
		service.clearOptOut(PROFILE, ANNA);

		verify(subscriptionOptOutRepositoryMock).deleteByProfileIdAndIdentifierTypeAndIdentifierValue(PROFILE_ID, AD_ACCOUNT, "anna01");
	}

	@Test
	void findOptedOutKeysIgnoresCase() {
		when(subscriptionOptOutRepositoryMock.findAllByProfileId(PROFILE_ID)).thenReturn(List.of(
			SubscriptionOptOutEntity.create().withIdentifier(IdentifierEmbeddable.create().withType(AD_ACCOUNT).withValue("Anna01"))));

		assertThat(service.findOptedOutKeys(PROFILE_ID)).containsExactly(SubscriptionOptOutService.keyOf("adaccount", "ANNA01"));
		verify(subscriptionOptOutRepositoryMock).findAllByProfileId(PROFILE_ID);
	}
}
