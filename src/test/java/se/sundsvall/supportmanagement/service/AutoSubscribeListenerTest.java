package se.sundsvall.supportmanagement.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AutoSubscribeListenerTest {

	private static final ErrandEntity ERRAND = ErrandEntity.create().withId("errand-1");

	@Mock
	private SubscriptionService subscriptionServiceMock;

	@InjectMocks
	private AutoSubscribeListener listener;

	@AfterEach
	void verifyNoMore() {
		verifyNoMoreInteractions(subscriptionServiceMock);
	}

	@Test
	void changedErrandSubscribesOnlyTheAssignee() {
		listener.handleAutoSubscribeEvent(new AutoSubscribeEvent(ERRAND));

		verify(subscriptionServiceMock).autoSubscribeErrandAssignee(ERRAND);
	}

	@Test
	void createdErrandSubscribesAssigneeAndReporter() {
		listener.handleAutoSubscribeEvent(new AutoSubscribeEvent(ERRAND, true));

		verify(subscriptionServiceMock).autoSubscribeErrandAssignee(ERRAND);
		verify(subscriptionServiceMock).autoSubscribeReporter(ERRAND);
	}

	@Test
	void failingAssigneeDoesNotKeepTheReporterFromBeingSubscribed() {
		doThrow(new RuntimeException("boom")).when(subscriptionServiceMock).autoSubscribeErrandAssignee(ERRAND);

		listener.handleAutoSubscribeEvent(new AutoSubscribeEvent(ERRAND, true));

		verify(subscriptionServiceMock).autoSubscribeErrandAssignee(ERRAND);
		verify(subscriptionServiceMock).autoSubscribeReporter(ERRAND);
	}

	@Test
	void failingReporterIsSwallowed() {
		doThrow(new RuntimeException("boom")).when(subscriptionServiceMock).autoSubscribeReporter(ERRAND);

		listener.handleAutoSubscribeEvent(new AutoSubscribeEvent(ERRAND, true));

		verify(subscriptionServiceMock).autoSubscribeErrandAssignee(ERRAND);
		verify(subscriptionServiceMock).autoSubscribeReporter(ERRAND);
	}
}
