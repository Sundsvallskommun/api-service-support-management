package se.sundsvall.supportmanagement.service.scheduler.action;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.ErrandActionRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandActionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.service.EventService;
import se.sundsvall.supportmanagement.service.RevisionService;
import se.sundsvall.supportmanagement.service.action.Action;
import se.sundsvall.supportmanagement.service.action.ActionExecutor;
import se.sundsvall.supportmanagement.service.model.RevisionResult;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;
import static se.sundsvall.supportmanagement.service.scheduler.action.ActionWorker.EVENT_LOG_ACTION;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getRequestGroupId;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.shouldNotify;

@ExtendWith(MockitoExtension.class)
class ActionWorkerTest {

	@Mock
	private ErrandActionRepository errandActionRepositoryMock;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private Action actionMock;

	@Mock
	private RevisionService revisionServiceMock;

	@Mock
	private EventService eventServiceMock;

	private ActionWorker actionWorker;

	@BeforeEach
	void setUp() {
		when(actionMock.getName()).thenReturn("testAction");
		// The real executor, so that what the worker records is verified through the services it records with
		actionWorker = new ActionWorker(errandActionRepositoryMock, List.of(actionMock), errandsRepositoryMock, new ActionExecutor(revisionServiceMock, eventServiceMock));
	}

	@Test
	void getExpiredActions() {
		final var expiredAction = ErrandActionEntity.create().withId("action-1");
		when(errandActionRepositoryMock.findAllByExecuteAfterBeforeAndErrandEntityDeletedIsNull(any(OffsetDateTime.class))).thenReturn(List.of(expiredAction));

		final var result = actionWorker.getExpiredActions();

		assertThat(result).containsExactly(expiredAction);
		verify(errandActionRepositoryMock).findAllByExecuteAfterBeforeAndErrandEntityDeletedIsNull(any(OffsetDateTime.class));
		verifyNoMoreInteractions(errandActionRepositoryMock, errandsRepositoryMock);
	}

	@Test
	void processActionWhenFulfilled() {
		final var errand = ErrandEntity.create().withId("errand-id");
		final var config = ActionConfigEntity.create()
			.withName("testAction")
			.withParameters(List.of(ActionConfigParameterEntity.create().withKey("label").withValues(List.of("priority-high"))));
		final var actionEntity = ErrandActionEntity.create()
			.withId("action-id")
			.withErrandEntity(errand)
			.withActionConfigEntity(config);

		when(actionMock.actionFulfilled(errand, Map.of("label", List.of("priority-high")))).thenReturn(true);
		when(errandsRepositoryMock.findWithLockingById(any())).thenReturn(Optional.of(errand));

		actionWorker.processAction(actionEntity);

		verify(errandsRepositoryMock).findWithLockingById("errand-id");
		verify(actionMock).actionFulfilled(errand, Map.of("label", List.of("priority-high")));
		verify(errandActionRepositoryMock).delete(actionEntity);
		verifyNoMoreInteractions(errandActionRepositoryMock, errandsRepositoryMock);
		verifyNoInteractions(eventServiceMock);
	}

	@Test
	void processActionLeavesTheActionWaitingWhenTheErrandWasSoftDeleted() {
		final var errand = ErrandEntity.create().withId("errand-id").withDeleted(OffsetDateTime.now());
		final var config = ActionConfigEntity.create().withName("testAction");
		final var actionEntity = ErrandActionEntity.create()
			.withId("action-id")
			.withErrandEntity(errand)
			.withActionConfigEntity(config);

		when(errandsRepositoryMock.findWithLockingById(any())).thenReturn(Optional.of(errand));

		actionWorker.processAction(actionEntity);

		verify(errandsRepositoryMock).findWithLockingById("errand-id");
		verifyNoMoreInteractions(errandsRepositoryMock);
		verify(actionMock, never()).actionFulfilled(any(), any());
		verifyNoInteractions(errandActionRepositoryMock, revisionServiceMock, eventServiceMock);
	}

	@Test
	void processActionWhenNotFulfilled() {
		final var errand = ErrandEntity.create().withId("errand-id");
		final var config = ActionConfigEntity.create()
			.withName("testAction")
			.withParameters(List.of(ActionConfigParameterEntity.create().withKey("label").withValues(List.of("priority-high"))));
		final var actionEntity = ErrandActionEntity.create()
			.withId("action-id")
			.withErrandEntity(errand)
			.withActionConfigEntity(config);

		when(actionMock.actionFulfilled(errand, Map.of("label", List.of("priority-high")))).thenReturn(false);
		when(actionMock.conditionsFulfilled(errand, config)).thenReturn(true);
		when(errandsRepositoryMock.findWithLockingById(any())).thenReturn(Optional.of(errand));

		actionWorker.processAction(actionEntity);

		verify(actionMock).actionFulfilled(errand, Map.of("label", List.of("priority-high")));
		verify(actionMock).conditionsFulfilled(errand, config);
		verify(errandsRepositoryMock).findWithLockingById("errand-id");
		verify(actionMock).executeAction(errand, config);
		verify(errandActionRepositoryMock).delete(actionEntity);
		verifyNoMoreInteractions(errandActionRepositoryMock, errandsRepositoryMock);
		// The action added no label, so there is none to tell of
		verifyNoInteractions(eventServiceMock);
	}

	@Test
	void processActionLogsTheLabelsTheActionAdded() {
		final var errand = ErrandEntity.create().withId("errand-id")
			.withLabels(new ArrayList<>(List.of(ErrandLabelEmbeddable.create().withMetadataLabelId("existing-label"))));
		final var config = ActionConfigEntity.create()
			.withName("testAction")
			.withParameters(List.of(ActionConfigParameterEntity.create().withKey("label").withValues(List.of("lex-label"))));
		final var actionEntity = ErrandActionEntity.create()
			.withId("action-id")
			.withErrandEntity(errand)
			.withActionConfigEntity(config);

		when(actionMock.actionFulfilled(errand, Map.of("label", List.of("lex-label")))).thenReturn(false);
		when(actionMock.conditionsFulfilled(errand, config)).thenReturn(true);
		when(errandsRepositoryMock.findWithLockingById(any())).thenReturn(Optional.of(errand));
		doAnswer(_ -> errand.getLabels().add(ErrandLabelEmbeddable.create().withMetadataLabelId("lex-label")))
			.when(actionMock).executeAction(errand, config);

		actionWorker.processAction(actionEntity);

		// Only the label the action added is told of, and by nobody
		verify(eventServiceMock).createLabelEvents(UPDATE, errand, null, Set.of("lex-label"), null);
		verify(errandActionRepositoryMock).delete(actionEntity);
	}

	@Test
	void processActionKeepsTheActionWhenTheLabelEventsCannotBeLogged() {
		final var errand = ErrandEntity.create().withId("errand-id").withLabels(new ArrayList<>());
		final var config = ActionConfigEntity.create()
			.withName("testAction")
			.withParameters(List.of(ActionConfigParameterEntity.create().withKey("label").withValues(List.of("lex-label"))));
		final var actionEntity = ErrandActionEntity.create()
			.withId("action-id")
			.withErrandEntity(errand)
			.withActionConfigEntity(config);

		when(actionMock.actionFulfilled(errand, Map.of("label", List.of("lex-label")))).thenReturn(false);
		when(actionMock.conditionsFulfilled(errand, config)).thenReturn(true);
		when(errandsRepositoryMock.findWithLockingById(any())).thenReturn(Optional.of(errand));
		doAnswer(_ -> errand.getLabels().add(ErrandLabelEmbeddable.create().withMetadataLabelId("lex-label")))
			.when(actionMock).executeAction(errand, config);
		doThrow(new RuntimeException("EventLog down")).when(eventServiceMock).createLabelEvents(any(), any(), isNull(), any(), isNull());

		actionWorker.processAction(actionEntity);

		verify(errandActionRepositoryMock).delete(actionEntity);
	}

	@Test
	void processActionWhenConditionsNotFulfilled() {
		final var errand = ErrandEntity.create().withId("errand-id");
		final var config = ActionConfigEntity.create()
			.withName("testAction")
			.withParameters(List.of(ActionConfigParameterEntity.create().withKey("label").withValues(List.of("priority-high"))));
		final var actionEntity = ErrandActionEntity.create()
			.withId("action-id")
			.withErrandEntity(errand)
			.withActionConfigEntity(config);

		when(actionMock.actionFulfilled(errand, Map.of("label", List.of("priority-high")))).thenReturn(false);
		when(actionMock.conditionsFulfilled(errand, config)).thenReturn(false);
		when(errandsRepositoryMock.findWithLockingById(any())).thenReturn(Optional.of(errand));

		actionWorker.processAction(actionEntity);

		verify(actionMock).actionFulfilled(errand, Map.of("label", List.of("priority-high")));
		verify(actionMock).conditionsFulfilled(errand, config);
		verify(errandsRepositoryMock).findWithLockingById("errand-id");
		verify(errandActionRepositoryMock).delete(actionEntity);
		verifyNoMoreInteractions(errandActionRepositoryMock, errandsRepositoryMock, actionMock);
		verifyNoInteractions(revisionServiceMock, eventServiceMock);
	}

	@Test
	@DisplayName("Verification that an action which changed the errand is recorded like a write through the API, so that its history, the event log and its process all learn of it")
	void processActionThatChangedTheErrandLogsTheChange() {
		final var errand = ErrandEntity.create().withId("errand-id");
		final var config = ActionConfigEntity.create().withName("testAction");
		final var actionEntity = ErrandActionEntity.create().withId("action-id").withErrandEntity(errand).withActionConfigEntity(config);
		final var previous = Revision.create().withId("previous-revision");
		final var latest = Revision.create().withId("latest-revision");

		when(errandsRepositoryMock.findWithLockingById("errand-id")).thenReturn(Optional.of(errand));
		when(actionMock.conditionsFulfilled(errand, config)).thenReturn(true);
		when(actionMock.executeAction(errand, config)).thenReturn(true);
		when(revisionServiceMock.createErrandRevision(errand)).thenReturn(new RevisionResult(previous, latest));

		actionWorker.processAction(actionEntity);

		final var inOrder = inOrder(actionMock, errandActionRepositoryMock, revisionServiceMock, eventServiceMock);
		// Taken off the errand first, so the revision recording what it did no longer lists it
		inOrder.verify(errandActionRepositoryMock).delete(actionEntity);
		inOrder.verify(actionMock).executeAction(errand, config);
		inOrder.verify(revisionServiceMock).createErrandRevision(errand);
		inOrder.verify(eventServiceMock).createErrandEvent(UPDATE, EVENT_LOG_ACTION, errand, latest, previous, false, ERRAND, null);
	}

	@Test
	@DisplayName("Verification that an action which left the errand as it was - an email sent - leaves neither a revision nor an event behind")
	void processActionThatChangedNothingLogsNothing() {
		final var errand = ErrandEntity.create().withId("errand-id");
		final var config = ActionConfigEntity.create().withName("testAction");
		final var actionEntity = ErrandActionEntity.create().withId("action-id").withErrandEntity(errand).withActionConfigEntity(config);

		when(errandsRepositoryMock.findWithLockingById("errand-id")).thenReturn(Optional.of(errand));
		when(actionMock.conditionsFulfilled(errand, config)).thenReturn(true);
		when(actionMock.executeAction(errand, config)).thenReturn(false);

		actionWorker.processAction(actionEntity);

		verify(errandActionRepositoryMock).delete(actionEntity);
		verifyNoInteractions(revisionServiceMock, eventServiceMock);
	}

	@Test
	@DisplayName("Verification that a change the last revision already holds writes no event")
	void processActionWhoseChangeLeavesNoRevisionLogsNothing() {
		final var errand = ErrandEntity.create().withId("errand-id");
		final var config = ActionConfigEntity.create().withName("testAction");
		final var actionEntity = ErrandActionEntity.create().withId("action-id").withErrandEntity(errand).withActionConfigEntity(config);

		when(errandsRepositoryMock.findWithLockingById("errand-id")).thenReturn(Optional.of(errand));
		when(actionMock.conditionsFulfilled(errand, config)).thenReturn(true);
		when(actionMock.executeAction(errand, config)).thenReturn(true);

		actionWorker.processAction(actionEntity);

		verify(revisionServiceMock).createErrandRevision(errand);
		verifyNoInteractions(eventServiceMock);
	}

	@Test
	@DisplayName("Verification that the event is guarded like at every other call site, so that the action is still done and removed")
	void processActionSurvivesAFailingEvent() {
		final var errand = ErrandEntity.create().withId("errand-id");
		final var config = ActionConfigEntity.create().withName("testAction");
		final var actionEntity = ErrandActionEntity.create().withId("action-id").withErrandEntity(errand).withActionConfigEntity(config);
		final var latest = Revision.create().withId("latest-revision");

		when(errandsRepositoryMock.findWithLockingById("errand-id")).thenReturn(Optional.of(errand));
		when(actionMock.conditionsFulfilled(errand, config)).thenReturn(true);
		when(actionMock.executeAction(errand, config)).thenReturn(true);
		when(revisionServiceMock.createErrandRevision(errand)).thenReturn(new RevisionResult(null, latest));
		doThrow(new IllegalStateException("event log unavailable")).when(eventServiceMock).createErrandEvent(UPDATE, EVENT_LOG_ACTION, errand, latest, null, false, ERRAND, null);

		assertThatNoException().isThrownBy(() -> actionWorker.processAction(actionEntity));

		verify(errandActionRepositoryMock).delete(actionEntity);
	}

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	@DisplayName("Verification that the action runs notifying as its config says, in a request group of its own, and leaves the thread as it found it")
	void processActionNotifiesAsItsConfigSaysInAGroupOfItsOwn(final boolean notify) {
		final var errand = ErrandEntity.create().withId("errand-id");
		final var config = ActionConfigEntity.create().withName("testAction").withNotify(notify);
		final var actionEntity = ErrandActionEntity.create().withId("action-id").withErrandEntity(errand).withActionConfigEntity(config);
		final var notifiedDuringAction = new Boolean[1];
		final var groupDuringAction = new String[1];

		when(errandsRepositoryMock.findWithLockingById("errand-id")).thenReturn(Optional.of(errand));
		when(actionMock.conditionsFulfilled(errand, config)).thenReturn(true);
		when(actionMock.executeAction(errand, config)).thenAnswer(_ -> {
			notifiedDuringAction[0] = shouldNotify();
			groupDuringAction[0] = getRequestGroupId();
			return false;
		});

		actionWorker.processAction(actionEntity);

		assertThat(notifiedDuringAction[0]).isEqualTo(notify);
		assertThat(groupDuringAction[0]).isNotNull();
		assertThat(shouldNotify()).isTrue();
		assertThat(getRequestGroupId()).isNull();
	}

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	@DisplayName("Verification that the action is taken off the errand as well, since the actions of an errand cascade and a deleted one still listed would be written back")
	void processActionTakesTheActionOffTheErrand(final boolean fulfilled) {
		final var config = ActionConfigEntity.create().withName("testAction");
		final var listed = ErrandActionEntity.create().withId("action-id");
		final var other = ErrandActionEntity.create().withId("other-action-id");
		final var errand = ErrandEntity.create().withId("errand-id").withActions(new ArrayList<>(List.of(listed, other)));
		final var actionEntity = ErrandActionEntity.create().withId("action-id").withErrandEntity(errand).withActionConfigEntity(config);

		when(errandsRepositoryMock.findWithLockingById("errand-id")).thenReturn(Optional.of(errand));
		when(actionMock.actionFulfilled(errand, Map.of())).thenReturn(fulfilled);
		lenient().when(actionMock.conditionsFulfilled(errand, config)).thenReturn(true);

		actionWorker.processAction(actionEntity);

		assertThat(errand.getActions()).containsExactly(other);
		verify(errandActionRepositoryMock).delete(actionEntity);
	}

	@Test
	void processActionWhenNoImplementationFound() {
		final var config = ActionConfigEntity.create()
			.withName("UNKNOWN_ACTION");
		final var actionEntity = ErrandActionEntity.create()
			.withId("action-id")
			.withErrandEntity(ErrandEntity.create())
			.withActionConfigEntity(config);

		assertThatThrownBy(() -> actionWorker.processAction(actionEntity))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("No action implementation found for name: UNKNOWN_ACTION");

		verifyNoInteractions(errandActionRepositoryMock, errandsRepositoryMock);
	}

	@Test
	void processActionWithNullConfig() {
		final var actionEntity = ErrandActionEntity.create()
			.withId("action-id")
			.withErrandEntity(ErrandEntity.create().withId("errand-id"));

		assertThatThrownBy(() -> actionWorker.processAction(actionEntity))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("No action config found for errand action with id: action-id");

		verifyNoInteractions(errandActionRepositoryMock, errandsRepositoryMock);
	}
}
