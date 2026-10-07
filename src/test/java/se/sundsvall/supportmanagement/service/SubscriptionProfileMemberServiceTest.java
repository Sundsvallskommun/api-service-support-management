package se.sundsvall.supportmanagement.service;

import java.util.List;
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
import se.sundsvall.supportmanagement.integration.db.SubscriptionOptOutRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriptionRepository;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionOptOutEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionProfileEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.integration.db.model.subscriber.DbSubscriptionTargetType.NAMESPACE;

@ExtendWith(MockitoExtension.class)
class SubscriptionProfileMemberServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE_NAME = "MY_NAMESPACE";
	private static final String PROFILE_ID = "profile-1";
	private static final String AD_ACCOUNT = "adAccount";

	private static final SubscriptionProfileEntity PROFILE = SubscriptionProfileEntity.create().withId(PROFILE_ID);

	@Mock
	private SubscriptionProfileService subscriptionProfileServiceMock;

	@Mock
	private SubscriberService subscriberServiceMock;

	@Mock
	private SubscriptionRepository subscriptionRepositoryMock;

	@Mock
	private SubscriptionOptOutRepository subscriptionOptOutRepositoryMock;

	@InjectMocks
	private SubscriptionProfileMemberService service;

	@Captor
	private ArgumentCaptor<SubscriptionEntity> subscriptionCaptor;

	@BeforeEach
	void setUpIdentity() {
		se.sundsvall.dept44.support.Identifier.set(se.sundsvall.dept44.support.Identifier.create()
			.withType(se.sundsvall.dept44.support.Identifier.Type.AD_ACCOUNT)
			.withValue("job01"));
	}

	@AfterEach
	void verifyNoMore() {
		se.sundsvall.dept44.support.Identifier.remove();
		verifyNoMoreInteractions(subscriptionProfileServiceMock, subscriberServiceMock, subscriptionRepositoryMock, subscriptionOptOutRepositoryMock);
	}

	private static SubscriberEntity subscriber(final String id, final String value) {
		return SubscriberEntity.create().withId(id).withIdentifier(IdentifierEmbeddable.create().withType(AD_ACCOUNT).withValue(value));
	}

	private static SubscriptionEntity membership(final SubscriberEntity subscriber) {
		return SubscriptionEntity.create().withId("subscription-" + subscriber.getId()).withSubscriber(subscriber).withTargetType(NAMESPACE).withProfile(PROFILE);
	}

	private static Identifier member(final String value) {
		return Identifier.create().withType(AD_ACCOUNT).withValue(value);
	}

	@Test
	void findMembers() {
		when(subscriptionProfileServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE_NAME, PROFILE_ID)).thenReturn(PROFILE);
		when(subscriptionRepositoryMock.findAllByProfileIdAndTargetType(PROFILE_ID, NAMESPACE))
			.thenReturn(List.of(membership(subscriber("s1", "anna01")), membership(subscriber("s2", "bert02"))));

		final var result = service.findMembers(MUNICIPALITY_ID, NAMESPACE_NAME, PROFILE_ID);

		assertThat(result).containsExactly(member("anna01"), member("bert02"));
		verify(subscriptionProfileServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE_NAME, PROFILE_ID);
		verify(subscriptionRepositoryMock).findAllByProfileIdAndTargetType(PROFILE_ID, NAMESPACE);
	}

	@Test
	void findMembersOfUnknownProfile() {
		when(subscriptionProfileServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE_NAME, PROFILE_ID)).thenThrow(Problem.valueOf(NOT_FOUND, "profile missing"));

		assertThatThrownBy(() -> service.findMembers(MUNICIPALITY_ID, NAMESPACE_NAME, PROFILE_ID))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(subscriptionProfileServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE_NAME, PROFILE_ID);
	}

	@Test
	void syncMembersAddsNewAndRemovesDropped() {
		final var kept = subscriber("s1", "anna01");
		final var dropped = subscriber("s2", "bert02");
		final var added = subscriber("s3", "cecil03");
		final var droppedMembership = membership(dropped);

		when(subscriptionProfileServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE_NAME, PROFILE_ID)).thenReturn(PROFILE);
		when(subscriptionRepositoryMock.findAllByProfileIdAndTargetType(PROFILE_ID, NAMESPACE)).thenReturn(List.of(membership(kept), droppedMembership));
		when(subscriptionOptOutRepositoryMock.findAllByProfileId(PROFILE_ID)).thenReturn(List.of());
		when(subscriberServiceMock.findOrCreateSubscriber(MUNICIPALITY_ID, NAMESPACE_NAME, AD_ACCOUNT, "cecil03")).thenReturn(added);

		// The kept member is given in other casing, which still matches
		service.syncMembers(MUNICIPALITY_ID, NAMESPACE_NAME, PROFILE_ID, List.of(member("ANNA01"), member("cecil03")));

		verify(subscriptionProfileServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE_NAME, PROFILE_ID);
		verify(subscriptionRepositoryMock).findAllByProfileIdAndTargetType(PROFILE_ID, NAMESPACE);
		verify(subscriptionOptOutRepositoryMock).findAllByProfileId(PROFILE_ID);
		verify(subscriptionRepositoryMock).delete(droppedMembership);
		verify(subscriberServiceMock).findOrCreateSubscriber(MUNICIPALITY_ID, NAMESPACE_NAME, AD_ACCOUNT, "cecil03");
		verify(subscriptionRepositoryMock).save(subscriptionCaptor.capture());
		final var saved = subscriptionCaptor.getValue();
		assertThat(saved.getSubscriber()).isSameAs(added);
		assertThat(saved.getProfile()).isSameAs(PROFILE);
		assertThat(saved.getTargetType()).isEqualTo(NAMESPACE);
		assertThat(saved.getCreatedBy()).isEqualTo(IdentifierEmbeddable.create().withType(AD_ACCOUNT).withValue("job01"));
	}

	@Test
	void syncMembersSkipsThoseWhoOptedOut() {
		final var optedOut = subscriber("s1", "anna01");

		when(subscriptionProfileServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE_NAME, PROFILE_ID)).thenReturn(PROFILE);
		when(subscriptionRepositoryMock.findAllByProfileIdAndTargetType(PROFILE_ID, NAMESPACE)).thenReturn(List.of());
		when(subscriptionOptOutRepositoryMock.findAllByProfileId(PROFILE_ID))
			.thenReturn(List.of(SubscriptionOptOutEntity.create().withSubscriber(optedOut).withProfile(PROFILE)));
		when(subscriberServiceMock.findOrCreateSubscriber(MUNICIPALITY_ID, NAMESPACE_NAME, AD_ACCOUNT, "anna01")).thenReturn(optedOut);

		service.syncMembers(MUNICIPALITY_ID, NAMESPACE_NAME, PROFILE_ID, List.of(member("anna01")));

		verify(subscriptionProfileServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE_NAME, PROFILE_ID);
		verify(subscriptionRepositoryMock).findAllByProfileIdAndTargetType(PROFILE_ID, NAMESPACE);
		verify(subscriptionOptOutRepositoryMock).findAllByProfileId(PROFILE_ID);
		verify(subscriberServiceMock).findOrCreateSubscriber(MUNICIPALITY_ID, NAMESPACE_NAME, AD_ACCOUNT, "anna01");
	}

	@Test
	void syncMembersWithEmptyListRemovesEveryMember() {
		final var membership = membership(subscriber("s1", "anna01"));

		when(subscriptionProfileServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE_NAME, PROFILE_ID)).thenReturn(PROFILE);
		when(subscriptionRepositoryMock.findAllByProfileIdAndTargetType(PROFILE_ID, NAMESPACE)).thenReturn(List.of(membership));
		when(subscriptionOptOutRepositoryMock.findAllByProfileId(PROFILE_ID)).thenReturn(List.of());

		service.syncMembers(MUNICIPALITY_ID, NAMESPACE_NAME, PROFILE_ID, List.of());

		verify(subscriptionProfileServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE_NAME, PROFILE_ID);
		verify(subscriptionRepositoryMock).findAllByProfileIdAndTargetType(PROFILE_ID, NAMESPACE);
		verify(subscriptionOptOutRepositoryMock).findAllByProfileId(PROFILE_ID);
		verify(subscriptionRepositoryMock).delete(membership);
	}

	@Test
	void syncMembersIgnoresDuplicatesInTheList() {
		final var added = subscriber("s1", "anna01");

		when(subscriptionProfileServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE_NAME, PROFILE_ID)).thenReturn(PROFILE);
		when(subscriptionRepositoryMock.findAllByProfileIdAndTargetType(PROFILE_ID, NAMESPACE)).thenReturn(List.of());
		when(subscriptionOptOutRepositoryMock.findAllByProfileId(PROFILE_ID)).thenReturn(List.of());
		when(subscriberServiceMock.findOrCreateSubscriber(MUNICIPALITY_ID, NAMESPACE_NAME, AD_ACCOUNT, "anna01")).thenReturn(added);

		service.syncMembers(MUNICIPALITY_ID, NAMESPACE_NAME, PROFILE_ID, List.of(member("anna01"), member("Anna01")));

		verify(subscriptionProfileServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE_NAME, PROFILE_ID);
		verify(subscriptionRepositoryMock).findAllByProfileIdAndTargetType(PROFILE_ID, NAMESPACE);
		verify(subscriptionOptOutRepositoryMock).findAllByProfileId(PROFILE_ID);
		verify(subscriberServiceMock).findOrCreateSubscriber(MUNICIPALITY_ID, NAMESPACE_NAME, AD_ACCOUNT, "anna01");
		verify(subscriptionRepositoryMock).save(subscriptionCaptor.capture());
		assertThat(subscriptionCaptor.getValue().getSubscriber()).isSameAs(added);
	}
}
