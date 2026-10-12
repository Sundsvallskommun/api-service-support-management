package se.sundsvall.supportmanagement.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.LabelAttributeEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.createErrandProcessEntity;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.service.ProcessActivityLog.CONFIG_ACTIVITY_TYPE;
import static se.sundsvall.supportmanagement.service.ProcessKeyGuard.MOVES_KEY_ERROR_CODE;
import static se.sundsvall.supportmanagement.service.ProcessKeyGuard.TWO_PROCESSES_ERROR_CODE;
import static se.sundsvall.supportmanagement.service.ProcessKeySelector.PROCESS_KEY_ATTRIBUTE;
import static se.sundsvall.supportmanagement.service.ProcessKeySelector.PROCESS_START_MODE_ATTRIBUTE;

@ExtendWith(MockitoExtension.class)
class ProcessKeyGuardTest {

	private static final String ERRAND_ID = "errandId";
	private static final String APPLICATION = "alkt-ansokan";
	private static final String SUPERVISION = "alkt-tillsyn";
	private static final String CONSEQUENCE = "The labels were left off.";

	@Mock
	private ErrandProcessRepository processRepositoryMock;

	@Mock
	private ProcessEventOutboxRepository outboxRepositoryMock;

	@Mock
	private MetadataLabelRepository metadataLabelRepositoryMock;

	@Mock
	private ProcessActivityLog activityLogMock;

	private ProcessKeyGuard guard() {
		// The real resolution rather than a mock of it: the guard is a comparison of two answers, and a stubbed answer
		// would be the very thing under test.
		return new ProcessKeyGuard(processRepositoryMock, outboxRepositoryMock, new ProcessKeySelector(metadataLabelRepositoryMock), activityLogMock);
	}

	// Rule 5 - the labels of an errand may name at most one process

	@Test
	@DisplayName("Verification that labels naming two processes are refused on an errand that runs none, before the errand is asked what it runs")
	void twoLabelsNamingTwoProcessesAreRefusedWithoutAProcess() {
		final var application = label(APPLICATION);
		final var supervision = label(SUPERVISION);

		final var guard = guard();
		final var before = worn(application);
		final var after = worn(application, supervision);

		assertThatThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, before, after))
			.isInstanceOf(ThrowableProblem.class)
			.extracting("status").isEqualTo(BAD_REQUEST);

		verifyNoInteractions(processRepositoryMock, outboxRepositoryMock);
	}

	@Test
	@DisplayName("Verification that the refusal names both of the processes the labels would point at")
	void theAmbiguityRefusalNamesBothProcesses() {
		final var application = label(APPLICATION);
		final var supervision = label(SUPERVISION);

		final var guard = guard();
		final var before = worn(application);
		final var after = worn(application, supervision);

		assertThatThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, before, after))
			.hasMessageContaining(APPLICATION)
			.hasMessageContaining(SUPERVISION);
	}

	@Test
	@DisplayName("Verification that two labels naming the same process are one process rather than an ambiguity")
	void twoLabelsCarryingTheSameKeyAreNotAmbiguous() {
		final var application = label(APPLICATION);
		final var alsoApplication = label(APPLICATION);

		givenNoProcess();

		assertThatNoException().isThrownBy(() -> guard().verifyLabelChange(ERRAND_ID, worn(application), worn(application, alsoApplication)));
	}

	@Test
	void newLabelsNamingTwoProcessesAreRefused() {
		final var guard = guard();
		final var labels = worn(label(APPLICATION), label(SUPERVISION));

		assertThatThrownBy(() -> guard.verifyNewLabels(labels))
			.isInstanceOf(ThrowableProblem.class)
			.extracting("status").isEqualTo(BAD_REQUEST);
	}

	@Test
	void newLabelsNamingOneProcessGoThrough() {
		assertThatNoException().isThrownBy(() -> guard().verifyNewLabels(worn(label(APPLICATION), labelWithoutKey())));
	}

	@Test
	@DisplayName("Verification that an errand created without labels asks nothing of the database")
	void newLabelsThatAreEmptyAreNotLookedUp() {
		assertThatNoException().isThrownBy(() -> guard().verifyNewLabels(emptyList()));

		verifyNoInteractions(metadataLabelRepositoryMock, processRepositoryMock, outboxRepositoryMock);
	}

	// Rule 1 - an errand that has a process keeps the one it has

	@Test
	@DisplayName("Verification that an errand which runs no process, and has no start on its way, may have the label naming a process exchanged for another")
	void anErrandWithoutAProcessIsLeftAlone() {
		givenNoProcess();

		assertThatNoException().isThrownBy(() -> guard().verifyLabelChange(ERRAND_ID, worn(label(APPLICATION)), worn(label(SUPERVISION))));
	}

	@Test
	@DisplayName("Verification that a patch leaving the labels as they were asks nothing of the database at all")
	void labelsThatAreNotTouchedAreNotLookedAt() {
		final var application = label(APPLICATION);

		assertThatNoException().isThrownBy(() -> guard().verifyLabelChange(ERRAND_ID, worn(application), worn(application)));

		verifyNoInteractions(processRepositoryMock, outboxRepositoryMock, metadataLabelRepositoryMock);
	}

	@Test
	@DisplayName("Verification that a label change which says nothing about a process goes through on an errand that runs one")
	void aLabelChangeThatDoesNotTouchTheProcessKeyGoesThrough() {
		final var application = label(APPLICATION);

		givenAProcessRunning(APPLICATION);

		assertThatNoException().isThrownBy(() -> guard().verifyLabelChange(ERRAND_ID, worn(application), worn(application, labelWithoutKey())));
	}

	@Test
	@DisplayName("Verification that exchanging the label for one naming the same process by hand goes through: the start mode names no other process")
	void changingOnlyTheStartModeGoesThrough() {
		final var automatic = label(APPLICATION);
		final var manual = label(APPLICATION).withAttributes(List.of(
			attribute(PROCESS_KEY_ATTRIBUTE, APPLICATION),
			attribute(PROCESS_START_MODE_ATTRIBUTE, "MANUAL")));

		givenAProcessRunning(APPLICATION);

		assertThatNoException().isThrownBy(() -> guard().verifyLabelChange(ERRAND_ID, worn(automatic), worn(manual)));
	}

	@Test
	void exchangingTheLabelForOneNamingAnotherProcessIsRefused() {
		givenAProcessRunning(APPLICATION);

		final var guard = guard();
		final var before = worn(label(APPLICATION));
		final var after = worn(label(SUPERVISION));

		assertThatThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, before, after))
			.isInstanceOf(ThrowableProblem.class)
			.extracting("status").isEqualTo(BAD_REQUEST);
	}

	@Test
	@DisplayName("Verification that the refusal names the errand, the key it runs and the one the change would leave it with")
	void theMoveRefusalNamesBothKeys() {
		givenAProcessRunning(APPLICATION);

		final var guard = guard();
		final var before = worn(label(APPLICATION));
		final var after = worn(label(SUPERVISION));

		assertThatThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, before, after))
			.hasMessageContaining("'" + APPLICATION + "'")
			.hasMessageContaining("'" + SUPERVISION + "'")
			.hasMessageContaining(ERRAND_ID);
	}

	@Test
	@DisplayName("Verification that taking the label naming the process off is refused, since the errand would then name no process at all")
	void takingTheProcessLabelOffIsRefused() {
		givenAProcessRunning(APPLICATION);

		final var guard = guard();
		final var before = worn(label(APPLICATION));
		final var after = worn(labelWithoutKey());

		assertThatThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, before, after))
			.isInstanceOf(ThrowableProblem.class)
			.extracting("status").isEqualTo(BAD_REQUEST);
	}

	@Test
	@DisplayName("Verification that taking off every key of labels that named two processes is refused on an errand with a process, since it would then name none")
	void takingEveryKeyOffAnAmbiguousErrandIsRefused() {
		givenAProcessRunning(APPLICATION);

		final var guard = guard();
		final var before = worn(label(APPLICATION), label(SUPERVISION));
		final var after = worn(labelWithoutKey());

		assertThatThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, before, after))
			.isInstanceOf(ThrowableProblem.class)
			.extracting("status").isEqualTo(BAD_REQUEST);
	}

	@Test
	@DisplayName("Verification that an errand whose labels named two processes may keep the one it runs and drop the other")
	void anAmbiguousErrandMayDropTheProcessItDoesNotRun() {
		final var application = label(APPLICATION);

		givenAProcessRunning(APPLICATION);

		assertThatNoException().isThrownBy(() -> guard().verifyLabelChange(ERRAND_ID, worn(application, label(SUPERVISION)), worn(application)));
	}

	@Test
	@DisplayName("Verification that a process which has run to its end holds the labels just as still as a live one, since the process life of the errand is over")
	void aProcessThatHasRunToItsEndHoldsTheLabelsStill() {
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(completedProcess()));

		final var guard = guard();
		final var before = worn(label(APPLICATION));
		final var after = worn(label(SUPERVISION));

		assertThatThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, before, after))
			.isInstanceOf(ThrowableProblem.class)
			.extracting("status").isEqualTo(BAD_REQUEST);
	}

	@Test
	@DisplayName("Verification that labels which have lost the key may be given back the one the errand actually runs")
	void labelsMayBePointedBackAtTheProcessTheErrandRuns() {
		final var urgent = labelWithoutKey();

		givenAProcessRunning(APPLICATION);

		assertThatNoException().isThrownBy(() -> guard().verifyLabelChange(ERRAND_ID, worn(urgent), worn(urgent, label(APPLICATION))));
	}

	@Test
	@DisplayName("Verification that labels which have lost the key may not be given one naming some other process")
	void labelsThatLostTheKeyMayNotBeGivenAnother() {
		final var urgent = labelWithoutKey();

		givenAProcessRunning(APPLICATION);

		final var guard = guard();
		final var before = worn(urgent);
		final var after = worn(urgent, label(SUPERVISION));

		assertThatThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, before, after))
			.isInstanceOf(ThrowableProblem.class)
			.extracting("status").isEqualTo(BAD_REQUEST);
	}

	// A start on its way counts as the process of the errand

	@Test
	@DisplayName("Verification that an errand with a start on its way and no process row yet keeps the key the start carries")
	void aStartOnItsWayHoldsTheLabelsStill() {
		givenNoProcess();
		when(outboxRepositoryMock.findByErrandIdAndStartAllowedIsTrueAndDeliveredAtIsNull(ERRAND_ID)).thenReturn(List.of(startOnItsWay(APPLICATION)));

		final var guard = guard();
		final var before = worn(label(APPLICATION));
		final var after = worn(label(SUPERVISION));

		assertThatThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, before, after))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("'" + APPLICATION + "'")
			.extracting("status").isEqualTo(BAD_REQUEST);
	}

	@Test
	@DisplayName("Verification that labels may be pointed at the process a start on its way carries")
	void labelsMayBePointedAtTheStartOnItsWay() {
		final var urgent = labelWithoutKey();

		givenNoProcess();
		when(outboxRepositoryMock.findByErrandIdAndStartAllowedIsTrueAndDeliveredAtIsNull(ERRAND_ID)).thenReturn(List.of(startOnItsWay(APPLICATION)));

		assertThatNoException().isThrownBy(() -> guard().verifyLabelChange(ERRAND_ID, worn(urgent), worn(urgent, label(APPLICATION))));
	}

	@Test
	@DisplayName("Verification that the outbox is not asked about a start once the errand has a process row, whose key is the one that holds")
	void theOutboxIsNotAskedWhenTheErrandHasAProcessRow() {
		final var application = label(APPLICATION);

		givenAProcessRunning(APPLICATION);

		assertThatNoException().isThrownBy(() -> guard().verifyLabelChange(ERRAND_ID, worn(application), worn(application, labelWithoutKey())));

		verifyNoInteractions(outboxRepositoryMock);
	}

	// Reading the labels

	@Test
	@DisplayName("Verification that labels carrying their metadata label are read off it, with no lookup")
	void labelsCarryingTheirMetadataLabelAreNotLookedUp() {
		givenAProcessRunning(APPLICATION);

		final var guard = guard();
		final var before = worn(label(APPLICATION));
		final var after = worn(label(SUPERVISION));

		assertThatThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, before, after))
			.isInstanceOf(ThrowableProblem.class);

		verifyNoInteractions(metadataLabelRepositoryMock);
	}

	@Test
	@DisplayName("Verification that a label just put together, without its metadata label, is looked up by id, and only it")
	void aLabelWithoutItsMetadataLabelIsLookedUp() {
		final var application = label(APPLICATION);
		final var supervision = label(SUPERVISION);
		final var added = ErrandLabelEmbeddable.create().withMetadataLabelId(supervision.getId());

		givenAProcessRunning(APPLICATION);
		when(metadataLabelRepositoryMock.findAllById(Set.of(supervision.getId()))).thenReturn(List.of(supervision));

		final var guard = guard();
		final var before = worn(application);
		final var after = List.of(added);

		assertThatThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, before, after))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining(SUPERVISION);
	}

	@Test
	@DisplayName("Verification that a label an errand wears whose metadata label is gone is passed over rather than thrown on")
	void aLabelWithoutAMetadataLabelIsPassedOver() {
		final var application = label(APPLICATION);
		final var missing = ErrandLabelEmbeddable.create().withMetadataLabelId(randomUUID().toString());

		givenAProcessRunning(APPLICATION);
		when(metadataLabelRepositoryMock.findAllById(Set.of(missing.getMetadataLabelId()))).thenReturn(emptyList());

		final var labelsAfter = new ArrayList<>(worn(application));
		labelsAfter.add(missing);

		assertThatNoException().isThrownBy(() -> guard().verifyLabelChange(ERRAND_ID, worn(application), labelsAfter));
	}

	@Test
	@DisplayName("Verification that an errand with no labels before and after is passed over without a lookup")
	void anErrandWithoutLabelsIsLeftAlone() {
		assertThatNoException().isThrownBy(() -> guard().verifyLabelChange(ERRAND_ID, emptyList(), emptyList()));

		verifyNoInteractions(processRepositoryMock, outboxRepositoryMock, metadataLabelRepositoryMock);
	}

	// The writers with no caller to answer

	@Test
	@DisplayName("Verification that a refused change is written on the errand with what the writer did about it, rather than thrown")
	void aRefusedChangeIsWrittenOnTheErrand() {
		givenAProcessRunning(APPLICATION);

		assertThat(guard().refusesLabelChange(ERRAND_ID, worn(label(APPLICATION)), worn(label(SUPERVISION)), CONSEQUENCE)).isTrue();

		verify(activityLogMock).writeOncePerWindow(eq(ERRAND_ID), isNull(), eq(CONFIG_ACTIVITY_TYPE), eq(MOVES_KEY_ERROR_CODE), endsWith(" " + CONSEQUENCE));
	}

	@Test
	@DisplayName("Verification that a change naming a second process is reported under its own code, so that neither fault hides the other")
	void aChangeNamingASecondProcessIsWrittenUnderItsOwnCode() {
		final var application = label(APPLICATION);

		assertThat(guard().refusesLabelChange(ERRAND_ID, worn(application), worn(application, label(SUPERVISION)), CONSEQUENCE)).isTrue();

		verify(activityLogMock).writeOncePerWindow(eq(ERRAND_ID), isNull(), eq(CONFIG_ACTIVITY_TYPE), eq(TWO_PROCESSES_ERROR_CODE), endsWith(" " + CONSEQUENCE));
	}

	@Test
	@DisplayName("Verification that a change which moves nothing is let through and leaves no entry behind")
	void anAllowedChangeLeavesNoEntryBehind() {
		final var application = label(APPLICATION);

		givenAProcessRunning(APPLICATION);

		assertThat(guard().refusesLabelChange(ERRAND_ID, worn(application), worn(application, labelWithoutKey()), CONSEQUENCE)).isFalse();

		verifyNoInteractions(activityLogMock);
	}

	private void givenAProcessRunning(final String processKey) {
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(ErrandProcessEntity.create()
			.withErrandId(ERRAND_ID)
			.withProcessKey(processKey)));
	}

	private void givenNoProcess() {
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(emptyList());
	}

	private static ErrandProcessEntity completedProcess() {
		return createErrandProcessEntity(COMPLETED, Clock.systemUTC(), process -> process.withErrandId(ERRAND_ID).withProcessKey(APPLICATION));
	}

	private static ProcessEventOutboxEntity startOnItsWay(final String processKey) {
		return ProcessEventOutboxEntity.create().withErrandId(ERRAND_ID).withProcessKey(processKey).withStartAllowed(true);
	}

	/**
	 * The labels as an errand wears them once they are settled: each carrying its metadata label.
	 */
	private static Collection<ErrandLabelEmbeddable> worn(final MetadataLabelEntity... labels) {
		return Stream.of(labels)
			.map(label -> ErrandLabelEmbeddable.create().withMetadataLabelId(label.getId()).withMetadataLabel(label))
			.toList();
	}

	private static MetadataLabelEntity label(final String processKey) {
		return MetadataLabelEntity.create()
			.withId(randomUUID().toString())
			.withAttributes(List.of(attribute(PROCESS_KEY_ATTRIBUTE, processKey)));
	}

	private static MetadataLabelEntity labelWithoutKey() {
		return MetadataLabelEntity.create()
			.withId(randomUUID().toString())
			.withAttributes(List.of(attribute("escalationEmail", "escalation@example.com")));
	}

	private static LabelAttributeEmbeddable attribute(final String key, final String value) {
		return LabelAttributeEmbeddable.create().withKey(key).withValue(value);
	}
}
