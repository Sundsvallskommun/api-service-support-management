package se.sundsvall.supportmanagement.service.scheduler.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.ErrandActionRepository;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandActionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.action.Action;

@ExtendWith(MockitoExtension.class)
class ActionWorkerTest {

	@Mock
	private ErrandActionRepository errandActionRepositoryMock;

	@Mock
	private Action actionMock;

	private ActionWorker actionWorker;

	@BeforeEach
	void setUp() {
		when(actionMock.getName()).thenReturn("testAction");
		actionWorker = new ActionWorker(errandActionRepositoryMock, List.of(actionMock));
	}

	@Test
	void getExpiredActions() {
		final var expiredAction = ErrandActionEntity.create().withId("action-1");
		when(errandActionRepositoryMock.findAllByExecuteAfterBefore(any(OffsetDateTime.class))).thenReturn(List.of(expiredAction));

		final var result = actionWorker.getExpiredActions();

		assertThat(result).containsExactly(expiredAction);
		verify(errandActionRepositoryMock).findAllByExecuteAfterBefore(any(OffsetDateTime.class));
		verifyNoMoreInteractions(errandActionRepositoryMock);
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

		actionWorker.processAction(actionEntity);

		verify(actionMock).actionFulfilled(errand, Map.of("label", List.of("priority-high")));
		verify(errandActionRepositoryMock).delete(actionEntity);
		verifyNoMoreInteractions(errandActionRepositoryMock);
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

		actionWorker.processAction(actionEntity);

		verify(actionMock).actionFulfilled(errand, Map.of("label", List.of("priority-high")));
		verify(actionMock).executeAction(errand, config);
		verify(errandActionRepositoryMock).delete(actionEntity);
		verifyNoMoreInteractions(errandActionRepositoryMock);
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

		verifyNoInteractions(errandActionRepositoryMock);
	}

	@Test
	void processActionWithNullConfig() {
		final var actionEntity = ErrandActionEntity.create()
			.withId("action-id")
			.withErrandEntity(ErrandEntity.create().withId("errand-id"));

		assertThatThrownBy(() -> actionWorker.processAction(actionEntity))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("No action config found for errand action with id: action-id");

		verifyNoInteractions(errandActionRepositoryMock);
	}
}
