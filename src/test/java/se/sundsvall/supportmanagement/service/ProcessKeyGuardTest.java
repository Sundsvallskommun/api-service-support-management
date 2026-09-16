package se.sundsvall.supportmanagement.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.LabelAttributeEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.service.ProcessErrorLog.CONFIG_ACTIVITY_TYPE;
import static se.sundsvall.supportmanagement.service.ProcessKeyGuard.MOVES_KEY_ERROR_CODE;
import static se.sundsvall.supportmanagement.service.ProcessKeyGuard.TWO_PROCESSES_ERROR_CODE;
import static se.sundsvall.supportmanagement.service.ProcessKeySelector.PROCESS_KEY_ATTRIBUTE;
import static se.sundsvall.supportmanagement.service.ProcessKeySelector.PROCESS_START_MODE_ATTRIBUTE;

@ExtendWith(MockitoExtension.class)
class ProcessKeyGuardTest {

	private static final String ERRAND_ID = "errandId";
	private static final String APPLICATION = "alkt-ansokan";
	private static final String SUPERVISION = "alkt-tillsyn";

	@Mock
	private ErrandProcessRepository processRepositoryMock;

	@Mock
	private MetadataLabelRepository metadataLabelRepositoryMock;

	@Mock
	private ProcessErrorLog errorLogMock;

	// The real resolution rather than a mock of it: the guard is a comparison of two answers, and a stubbed answer
	// would be the very thing under test.
	private final ProcessKeySelector processKeySelector = new ProcessKeySelector();

	private ProcessKeyGuard guard() {
		return new ProcessKeyGuard(processRepositoryMock, metadataLabelRepositoryMock, processKeySelector, errorLogMock);
	}

	// Rule 1 - the labels of an errand may name at most one process

	@Test
	@DisplayName("Verification that labels naming two processes are refused on an errand that runs none: it would point at both and belong to neither")
	void twoLabelsNamingTwoProcessesAreRefusedWithoutAProcess() {
		final var application = label(APPLICATION);
		final var supervision = label(SUPERVISION);

		givenLabels(application, supervision);

		final var guard = guard();
		final var before = worn(application);
		final var after = worn(application, supervision);

		assertThatThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, before, after))
			.isInstanceOf(ThrowableProblem.class)
			.extracting("status").isEqualTo(BAD_REQUEST);

		// The count of keys is wrong whatever the errand runs, so nothing is asked about its processes.
		verifyNoInteractions(processRepositoryMock);
	}

	@Test
	@DisplayName("Verification that the refusal names both of the processes the labels would point at")
	void theAmbiguityRefusalNamesBothProcesses() {
		final var application = label(APPLICATION);
		final var supervision = label(SUPERVISION);

		givenLabels(application, supervision);

		final var guard = guard();
		final var before = worn(application);
		final var after = worn(application, supervision);

		assertThatThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, before, after))
			.hasMessageContaining(APPLICATION)
			.hasMessageContaining(SUPERVISION);
	}

	@Test
	@DisplayName("Verification that a second key put beside the first is refused before the errand is ever asked what it runs")
	void puttingASecondProcessKeyBesideTheFirstIsRefused() {
		final var application = label(APPLICATION);
		final var supervision = label(SUPERVISION);

		givenLabels(application, supervision);

		final var guard = guard();
		final var before = worn(application);
		final var after = worn(application, supervision);

		assertThatThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, before, after))
			.isInstanceOf(ThrowableProblem.class);

		verifyNoInteractions(processRepositoryMock);
	}

	@Test
	@DisplayName("Verification that two labels naming the same process are one process rather than an ambiguity")
	void twoLabelsCarryingTheSameKeyAreNotAmbiguous() {
		final var application = label(APPLICATION);
		final var alsoApplication = label(APPLICATION);

		givenLabels(application, alsoApplication);
		givenNoProcess();

		assertThatNoException().isThrownBy(() -> guard().verifyLabelChange(ERRAND_ID, worn(application), worn(application, alsoApplication)));
	}

	@Test
	void newLabelsNamingTwoProcessesAreRefused() {
		final var application = label(APPLICATION);
		final var supervision = label(SUPERVISION);

		givenLabels(application, supervision);

		final var guard = guard();
		final var labels = worn(application, supervision);

		assertThatThrownBy(() -> guard.verifyNewLabels(labels))
			.isInstanceOf(ThrowableProblem.class)
			.extracting("status").isEqualTo(BAD_REQUEST);
	}

	@Test
	void newLabelsNamingOneProcessGoThrough() {
		final var application = label(APPLICATION);
		final var urgent = labelWithoutKey();

		givenLabels(application, urgent);

		assertThatNoException().isThrownBy(() -> guard().verifyNewLabels(worn(application, urgent)));
	}

	@Test
	@DisplayName("Verification that an errand created without labels is not looked up at all, and that no process is asked about either way")
	void newLabelsThatAreEmptyAreNotLookedUp() {
		assertThatNoException().isThrownBy(() -> guard().verifyNewLabels(emptyList()));

		verifyNoInteractions(metadataLabelRepositoryMock, processRepositoryMock);
	}

	// Rule 2 - an errand that has a process keeps the one it has

	@Test
	@DisplayName("Verification that an errand which runs no process may have the label naming a process exchanged for another")
	void anErrandWithoutAProcessIsLeftAlone() {
		final var application = label(APPLICATION);
		final var supervision = label(SUPERVISION);

		givenLabels(application, supervision);
		givenNoProcess();

		assertThatNoException().isThrownBy(() -> guard().verifyLabelChange(ERRAND_ID, worn(application), worn(supervision)));
	}

	@Test
	@DisplayName("Verification that a patch leaving the labels as they were asks nothing of the database at all")
	void labelsThatAreNotTouchedAreNotLookedAt() {
		final var application = label(APPLICATION);

		assertThatNoException().isThrownBy(() -> guard().verifyLabelChange(ERRAND_ID, worn(application), worn(application)));

		verifyNoInteractions(processRepositoryMock, metadataLabelRepositoryMock);
	}

	@Test
	@DisplayName("Verification that a label change which says nothing about a process goes through on an errand that runs one")
	void aLabelChangeThatDoesNotTouchTheProcessKeyGoesThrough() {
		final var application = label(APPLICATION);
		final var urgent = labelWithoutKey();

		givenLabels(application, urgent);
		givenAProcessRunning(APPLICATION);

		assertThatNoException().isThrownBy(() -> guard().verifyLabelChange(ERRAND_ID, worn(application), worn(application, urgent)));
	}

	@Test
	@DisplayName("Verification that exchanging the label for one naming the same process by hand goes through: the start mode names no other process")
	void changingOnlyTheStartModeGoesThrough() {
		final var automatic = label(APPLICATION);
		final var manual = label(APPLICATION).withAttributes(List.of(
			attribute(PROCESS_KEY_ATTRIBUTE, APPLICATION),
			attribute(PROCESS_START_MODE_ATTRIBUTE, "MANUAL")));

		givenLabels(automatic, manual);
		givenAProcessRunning(APPLICATION);

		assertThatNoException().isThrownBy(() -> guard().verifyLabelChange(ERRAND_ID, worn(automatic), worn(manual)));
	}

	@Test
	void exchangingTheLabelForOneNamingAnotherProcessIsRefused() {
		final var application = label(APPLICATION);
		final var supervision = label(SUPERVISION);

		givenLabels(application, supervision);
		givenAProcessRunning(APPLICATION);

		final var guard = guard();
		final var before = worn(application);
		final var after = worn(supervision);

		assertThatThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, before, after))
			.isInstanceOf(ThrowableProblem.class)
			.extracting("status").isEqualTo(BAD_REQUEST);
	}

	@Test
	@DisplayName("Verification that the refusal names the errand, the key it runs and the one the change would leave it with")
	void theMoveRefusalNamesBothKeys() {
		final var application = label(APPLICATION);
		final var supervision = label(SUPERVISION);

		givenLabels(application, supervision);
		givenAProcessRunning(APPLICATION);

		final var guard = guard();
		final var before = worn(application);
		final var after = worn(supervision);

		assertThatThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, before, after))
			.hasMessageContaining("'" + APPLICATION + "'")
			.hasMessageContaining("'" + SUPERVISION + "'")
			.hasMessageContaining(ERRAND_ID);
	}

	@Test
	@DisplayName("Verification that taking the label naming the process off is refused, since the errand would then name no process at all")
	void takingTheProcessLabelOffIsRefused() {
		final var application = label(APPLICATION);
		final var urgent = labelWithoutKey();

		givenLabels(application, urgent);
		givenAProcessRunning(APPLICATION);

		final var guard = guard();
		final var before = worn(application);
		final var after = worn(urgent);

		assertThatThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, before, after))
			.isInstanceOf(ThrowableProblem.class)
			.extracting("status").isEqualTo(BAD_REQUEST);
	}

	@Test
	@DisplayName("Verification that a process which has run to its end holds the labels just as still as a live one, since the process life of the errand is over")
	void aProcessThatHasRunToItsEndHoldsTheLabelsStill() {
		final var application = label(APPLICATION);
		final var supervision = label(SUPERVISION);

		givenLabels(application, supervision);
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(completedProcess()));

		final var guard = guard();
		final var before = worn(application);
		final var after = worn(supervision);

		assertThatThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, before, after))
			.isInstanceOf(ThrowableProblem.class)
			.extracting("status").isEqualTo(BAD_REQUEST);
	}

	@Test
	@DisplayName("Verification that labels which have lost the key may be given back the one the errand actually runs")
	void labelsMayBePointedBackAtTheProcessTheErrandRuns() {
		final var urgent = labelWithoutKey();
		final var application = label(APPLICATION);

		givenLabels(urgent, application);
		givenAProcessRunning(APPLICATION);

		assertThatNoException().isThrownBy(() -> guard().verifyLabelChange(ERRAND_ID, worn(urgent), worn(urgent, application)));
	}

	@Test
	@DisplayName("Verification that labels which have lost the key may not be given one naming some other process")
	void labelsThatLostTheKeyMayNotBeGivenAnother() {
		final var urgent = labelWithoutKey();
		final var supervision = label(SUPERVISION);

		givenLabels(urgent, supervision);
		givenAProcessRunning(APPLICATION);

		final var guard = guard();
		final var before = worn(urgent);
		final var after = worn(urgent, supervision);

		assertThatThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, before, after))
			.isInstanceOf(ThrowableProblem.class)
			.extracting("status").isEqualTo(BAD_REQUEST);
	}

	@Test
	@DisplayName("Verification that a label an errand wears whose metadata label is gone is passed over rather than thrown on")
	void aLabelWithoutAMetadataLabelIsPassedOver() {
		final var application = label(APPLICATION);
		final var missing = ErrandLabelEmbeddable.create().withMetadataLabelId(randomUUID().toString());

		givenLabels(application);
		givenAProcessRunning(APPLICATION);

		final var labelsAfter = new ArrayList<>(worn(application));
		labelsAfter.add(missing);

		assertThatNoException().isThrownBy(() -> guard().verifyLabelChange(ERRAND_ID, worn(application), labelsAfter));
	}

	@Test
	@DisplayName("Verification that an errand with no labels at all before and after is passed over without a lookup")
	void anErrandWithoutLabelsIsLeftAlone() {
		assertThatNoException().isThrownBy(() -> guard().verifyLabelChange(ERRAND_ID, null, null));

		verifyNoInteractions(processRepositoryMock, metadataLabelRepositoryMock);
	}

	@Test
	@DisplayName("Verification that the labels of both sides are read in one lookup rather than one per side")
	void bothSidesAreReadInOneLookup() {
		final var application = label(APPLICATION);
		final var supervision = label(SUPERVISION);

		givenLabels(application, supervision);
		givenAProcessRunning(APPLICATION);

		final var guard = guard();
		final var before = worn(application);
		final var after = worn(supervision);

		assertThatThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, before, after))
			.isInstanceOf(ThrowableProblem.class);

		verify(metadataLabelRepositoryMock, times(1)).findAllById(any());
	}

	// The scheduled writer, which has no caller to answer

	@Test
	@DisplayName("Verification that a refused addition is written on the errand rather than thrown, since a scheduled job has nowhere to send a 400")
	void aRefusedAdditionIsWrittenOnTheErrand() {
		final var application = label(APPLICATION);
		final var supervision = label(SUPERVISION);

		givenLabels(application, supervision);
		givenAProcessRunning(APPLICATION);

		assertThat(guard().refusesLabelChange(ERRAND_ID, worn(application), worn(supervision))).isTrue();

		verify(errorLogMock).writeOncePerWindow(eq(ERRAND_ID), isNull(), eq(CONFIG_ACTIVITY_TYPE), eq(MOVES_KEY_ERROR_CODE), any());
	}

	@Test
	@DisplayName("Verification that an addition naming a second process is reported under its own code, so that neither fault hides the other")
	void anAdditionNamingASecondProcessIsWrittenUnderItsOwnCode() {
		final var application = label(APPLICATION);
		final var supervision = label(SUPERVISION);

		givenLabels(application, supervision);

		assertThat(guard().refusesLabelChange(ERRAND_ID, worn(application), worn(application, supervision))).isTrue();

		verify(errorLogMock).writeOncePerWindow(eq(ERRAND_ID), isNull(), eq(CONFIG_ACTIVITY_TYPE), eq(TWO_PROCESSES_ERROR_CODE), any());
	}

	@Test
	@DisplayName("Verification that an addition which moves nothing is let through and leaves no entry behind")
	void anAllowedAdditionLeavesNoEntryBehind() {
		final var application = label(APPLICATION);
		final var urgent = labelWithoutKey();

		givenLabels(application, urgent);
		givenAProcessRunning(APPLICATION);

		assertThat(guard().refusesLabelChange(ERRAND_ID, worn(application), worn(application, urgent))).isFalse();

		verifyNoInteractions(errorLogMock);
	}

	private void givenLabels(final MetadataLabelEntity... labels) {
		when(metadataLabelRepositoryMock.findAllById(any())).thenReturn(List.of(labels));
	}

	private void givenAProcessRunning(final String processKey) {
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(ErrandProcessEntity.create()
			.withErrandId(ERRAND_ID)
			.withProcessKey(processKey)));
	}

	private void givenNoProcess() {
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(emptyList());
	}

	private ErrandProcessEntity completedProcess() {
		final var entity = ErrandProcessEntity.create()
			.withErrandId(ERRAND_ID)
			.withProcessKey(APPLICATION);

		entity.applyStatus(COMPLETED, Clock.systemUTC());

		return entity;
	}

	private Collection<ErrandLabelEmbeddable> worn(final MetadataLabelEntity... labels) {
		return Stream.of(labels)
			.map(label -> ErrandLabelEmbeddable.create().withMetadataLabelId(label.getId()))
			.toList();
	}

	private MetadataLabelEntity label(final String processKey) {
		return MetadataLabelEntity.create()
			.withId(randomUUID().toString())
			.withAttributes(List.of(attribute(PROCESS_KEY_ATTRIBUTE, processKey)));
	}

	private MetadataLabelEntity labelWithoutKey() {
		return MetadataLabelEntity.create()
			.withId(randomUUID().toString())
			.withAttributes(List.of(attribute("escalationEmail", "escalation@example.com")));
	}

	private LabelAttributeEmbeddable attribute(final String key, final String value) {
		return LabelAttributeEmbeddable.create().withKey(key).withValue(value);
	}
}
