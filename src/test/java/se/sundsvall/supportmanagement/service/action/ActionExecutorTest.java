package se.sundsvall.supportmanagement.service.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.service.EventService;
import se.sundsvall.supportmanagement.service.RevisionService;
import se.sundsvall.supportmanagement.service.model.RevisionResult;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.clearNotify;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.setNotify;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.shouldNotify;

@ExtendWith(MockitoExtension.class)
class ActionExecutorTest {

	private static final String MESSAGE = "Ärendet har uppdaterats av en åtgärd.";

	@Mock
	private RevisionService revisionServiceMock;

	@Mock
	private EventService eventServiceMock;

	@Mock
	private Action actionMock;

	@InjectMocks
	private ActionExecutor actionExecutor;

	@AfterEach
	void tearDown() {
		clearNotify();
	}

	@ParameterizedTest
	@CsvSource({
		"true, true, true",
		"true, false, false",
		"false, true, false",
		"false, false, false"
	})
	@DisplayName("Verification that the action notifies only where both its config and the request it runs in do, and leaves the thread as it found it")
	void executeAndRecordNotifiesWhereBothTheConfigAndTheRequestDo(final boolean configNotifies, final boolean requestNotifies, final boolean expected) {
		final var errand = ErrandEntity.create().withId("errand-id");
		final var config = ActionConfigEntity.create().withNotify(configNotifies);
		final var notifiedDuringAction = new Boolean[1];
		setNotify(String.valueOf(requestNotifies));
		when(actionMock.executeAction(errand, config)).thenAnswer(_ -> {
			notifiedDuringAction[0] = shouldNotify();
			return false;
		});

		actionExecutor.executeAndRecord(errand, actionMock, config, MESSAGE);

		assertThat(notifiedDuringAction[0]).isEqualTo(expected);
		assertThat(shouldNotify()).isEqualTo(requestNotifies);
		verifyNoInteractions(revisionServiceMock, eventServiceMock);
	}

	@Test
	@DisplayName("Verification that what the action did is recorded as a change of its own, made by nobody")
	void executeAndRecordRecordsTheChangeAsMadeByNobody() {
		final var errand = ErrandEntity.create().withId("errand-id").withLabels(new ArrayList<>());
		final var config = ActionConfigEntity.create();
		final var previous = Revision.create().withId("previous-revision");
		final var latest = Revision.create().withId("latest-revision");
		when(actionMock.executeAction(errand, config)).thenAnswer(_ -> {
			errand.getLabels().add(ErrandLabelEmbeddable.create().withMetadataLabelId("label-id"));
			return true;
		});
		when(revisionServiceMock.createErrandRevision(errand)).thenReturn(new RevisionResult(previous, latest));

		actionExecutor.executeAndRecord(errand, actionMock, config, MESSAGE);

		final var inOrder = inOrder(actionMock, revisionServiceMock, eventServiceMock);
		inOrder.verify(actionMock).executeAction(errand, config);
		inOrder.verify(revisionServiceMock).createErrandRevision(errand);
		inOrder.verify(eventServiceMock).createErrandEvent(UPDATE, MESSAGE, errand, latest, previous, false, ERRAND, null);
		inOrder.verify(eventServiceMock).createLabelEvents(UPDATE, errand, null, Set.of("label-id"), null);
	}

	@Test
	@DisplayName("Verification that an action which left the errand as it was leaves neither a revision nor an event behind")
	void executeAndRecordOfAnActionThatChangedNothingRecordsNothing() {
		final var errand = ErrandEntity.create().withId("errand-id").withLabels(List.of());
		final var config = ActionConfigEntity.create();
		when(actionMock.executeAction(errand, config)).thenReturn(false);

		actionExecutor.executeAndRecord(errand, actionMock, config, MESSAGE);

		verifyNoInteractions(revisionServiceMock, eventServiceMock);
	}
}
