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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.ErrandActionRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
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
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private Action actionMock;

	private ActionWorker actionWorker;

	@BeforeEach
	void setUp() {
		when(actionMock.getName()).thenReturn("testAction");
		actionWorker = new ActionWorker(errandActionRepositoryMock, List.of(actionMock), errandsRepositoryMock);
	}

	@Test
	void getExpiredActions() {
		final var expiredAction = ErrandActionEntity.create().withId("action-1");
		when(errandActionRepositoryMock.findAllByExecuteAfterBefore(any(OffsetDateTime.class))).thenReturn(List.of(expiredAction));

		final var result = actionWorker.getExpiredActions();

		assertThat(result).containsExactly(expiredAction);
		verify(errandActionRepositoryMock).findAllByExecuteAfterBefore(any(OffsetDateTime.class));
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

		actionWorker.processAction(actionEntity);

		verify(actionMock).actionFulfilled(errand, Map.of("label", List.of("priority-high")));
		verify(errandActionRepositoryMock).delete(actionEntity);
		verifyNoMoreInteractions(errandActionRepositoryMock, errandsRepositoryMock);
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
		when(errandsRepositoryMock.findWithLockingById(any())).thenReturn(Optional.of(errand));

		actionWorker.processAction(actionEntity);

		verify(actionMock).actionFulfilled(errand, Map.of("label", List.of("priority-high")));
		verify(errandsRepositoryMock).findWithLockingById("errand-id");
		verify(actionMock).executeAction(errand, config);
		verify(errandActionRepositoryMock).delete(actionEntity);
		verifyNoMoreInteractions(errandActionRepositoryMock, errandsRepositoryMock);
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
