package se.sundsvall.supportmanagement.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;
import se.sundsvall.supportmanagement.service.model.ProcessKeySelection;
import se.sundsvall.supportmanagement.service.model.ProcessStartOptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.createErrandProcessEntity;
import static se.sundsvall.supportmanagement.api.model.process.ProcessStartability.AVAILABLE;
import static se.sundsvall.supportmanagement.api.model.process.ProcessStartability.LIVE_INSTANCE;
import static se.sundsvall.supportmanagement.api.model.process.ProcessStartability.NO_PROCESS_ENGINE;
import static se.sundsvall.supportmanagement.api.model.process.ProcessStartability.NO_PROCESS_KEY;
import static se.sundsvall.supportmanagement.api.model.process.ProcessStartability.PROCESS_COMPLETED;
import static se.sundsvall.supportmanagement.api.model.process.ProcessStartability.START_PENDING;
import static se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity.PROCESS_KEY_LENGTH;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.AUTOMATIC;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.MANUAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.WAITING;

class ProcessRulesTest {

	private static final String NAMESPACE = "NAMESPACE";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String APPLICATION = "alkt-ansokan";
	private static final String SUPERVISION = "alkt-tillsyn";
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-21T10:15:30.123456Z"), ZoneId.of("UTC"));

	private static final Supplier<ProcessKeySelection> LABELS_NEVER_ASKED = () -> {
		throw new AssertionError("the labels were read although a process row stood in the way");
	};

	private static final BooleanSupplier OUTBOX_NEVER_ASKED = () -> {
		throw new AssertionError("the outbox was read although no start was available");
	};

	@ParameterizedTest
	@EnumSource(ProcessStatus.class)
	@DisplayName("Verification that an errand has a live process exactly when one of its rows is not terminal")
	void aLiveProcessIsARowThatIsNotTerminal(final ProcessStatus status) {
		assertThat(ProcessRules.hasLiveProcess(List.of(instance(FAILED, APPLICATION), instance(status, APPLICATION)))).isEqualTo(!status.isTerminal());
		assertThat(ProcessRules.hasLiveProcess(List.of())).isFalse();
	}

	@ParameterizedTest
	@EnumSource(ProcessStatus.class)
	@DisplayName("Verification that the process life of an errand is over exactly when one of its rows has completed")
	void theProcessLifeIsOverOnceARowHasCompleted(final ProcessStatus status) {
		assertThat(ProcessRules.hasCompletedProcess(List.of(instance(FAILED, APPLICATION), instance(status, APPLICATION)))).isEqualTo(COMPLETED == status);
		assertThat(ProcessRules.hasCompletedProcess(List.of())).isFalse();
	}

	@Test
	@DisplayName("Verification that an errand without a process whose labels name one may be started with that key, whatever the start mode")
	void anErrandWithoutAProcessMayBeStartedWithTheKeyOfItsLabels() {
		assertThat(ProcessRules.startOptionsOf(true, List.of(), () -> selection(SUPERVISION, MANUAL)))
			.isEqualTo(new ProcessStartOptions(AVAILABLE, List.of(SUPERVISION)));
		assertThat(ProcessRules.startOptionsOf(true, List.of(), () -> selection(SUPERVISION, AUTOMATIC)))
			.isEqualTo(new ProcessStartOptions(AVAILABLE, List.of(SUPERVISION)));
	}

	@Test
	@DisplayName("Verification that an errand whose labels point in two directions offers both keys, so that a person can choose")
	void anAmbiguousErrandOffersEveryKey() {
		assertThat(ProcessRules.startOptionsOf(true, List.of(), () -> ambiguous(APPLICATION, SUPERVISION)))
			.isEqualTo(new ProcessStartOptions(AVAILABLE, List.of(APPLICATION, SUPERVISION)));
	}

	@Test
	@DisplayName("Verification that the namespace, a live instance and a completed one stand in the way in that order, and that none of them reads the labels")
	void theObstaclesAreAnsweredWithoutReadingTheLabels() {
		assertThat(ProcessRules.startOptionsOf(false, List.of(instance(WAITING, APPLICATION)), LABELS_NEVER_ASKED)).isEqualTo(ProcessStartOptions.unavailable(NO_PROCESS_ENGINE));
		assertThat(ProcessRules.startOptionsOf(true, List.of(instance(WAITING, APPLICATION), instance(COMPLETED, APPLICATION)), LABELS_NEVER_ASKED))
			.isEqualTo(ProcessStartOptions.unavailable(LIVE_INSTANCE));
		assertThat(ProcessRules.startOptionsOf(true, List.of(instance(FAILED, APPLICATION), instance(COMPLETED, APPLICATION)), LABELS_NEVER_ASKED))
			.isEqualTo(ProcessStartOptions.unavailable(PROCESS_COMPLETED));
	}

	@Test
	@DisplayName("Verification that an errand without a single label naming a process has nothing to start")
	void anErrandWithoutAProcessKeyHasNothingToStart() {
		assertThat(ProcessRules.startOptionsOf(true, List.of(), () -> ProcessKeySelection.NONE)).isEqualTo(ProcessStartOptions.unavailable(NO_PROCESS_KEY));
	}

	@Test
	@DisplayName("Verification that an errand whose only instance failed offers only the key of the process it has run")
	void anErrandWithAFailedStartOffersOnlyItsOwnProcess() {
		assertThat(ProcessRules.startOptionsOf(true, List.of(instance(FAILED, APPLICATION)), () -> ambiguous(APPLICATION, SUPERVISION)))
			.isEqualTo(new ProcessStartOptions(AVAILABLE, List.of(APPLICATION)));
		assertThat(ProcessRules.startOptionsOf(true, List.of(instance(FAILED, APPLICATION)), () -> selection(SUPERVISION, AUTOMATIC)))
			.isEqualTo(ProcessStartOptions.unavailable(NO_PROCESS_KEY));
	}

	@Test
	@DisplayName("Verification that a key longer than a process key may be is never offered, since no start can carry it")
	void anOversizedKeyIsNeverOffered() {
		final var oversized = "k".repeat(PROCESS_KEY_LENGTH + 1);
		final var fitting = "k".repeat(PROCESS_KEY_LENGTH);

		assertThat(ProcessRules.startOptionsOf(true, List.of(), () -> selection(oversized, AUTOMATIC))).isEqualTo(ProcessStartOptions.unavailable(NO_PROCESS_KEY));
		assertThat(ProcessRules.startOptionsOf(true, List.of(), () -> ambiguous(oversized, APPLICATION))).isEqualTo(new ProcessStartOptions(AVAILABLE, List.of(APPLICATION)));
		assertThat(ProcessRules.startOptionsOf(true, List.of(), () -> selection(fitting, AUTOMATIC))).isEqualTo(new ProcessStartOptions(AVAILABLE, List.of(fitting)));
	}

	@Test
	@DisplayName("Verification that the startable field shows a start on its way as START_PENDING, and names no key then")
	void aStartOnItsWayIsShownAsPending() {
		assertThat(ProcessRules.startableOf(true, List.of(), () -> selection(APPLICATION, MANUAL), () -> true)).isEqualTo(ProcessStartOptions.unavailable(START_PENDING));
		assertThat(ProcessRules.startableOf(true, List.of(), () -> selection(APPLICATION, MANUAL), () -> false)).isEqualTo(new ProcessStartOptions(AVAILABLE, List.of(APPLICATION)));
	}

	@Test
	@DisplayName("Verification that the outbox is asked about a start on its way only when a start would otherwise be available")
	void theOutboxIsAskedOnlyWhenAStartIsAvailable() {
		assertThat(ProcessRules.startableOf(false, List.of(), LABELS_NEVER_ASKED, OUTBOX_NEVER_ASKED)).isEqualTo(ProcessStartOptions.unavailable(NO_PROCESS_ENGINE));
		assertThat(ProcessRules.startableOf(true, List.of(instance(WAITING, APPLICATION)), LABELS_NEVER_ASKED, OUTBOX_NEVER_ASKED)).isEqualTo(ProcessStartOptions.unavailable(LIVE_INSTANCE));
		assertThat(ProcessRules.startableOf(true, List.of(instance(COMPLETED, APPLICATION)), LABELS_NEVER_ASKED, OUTBOX_NEVER_ASKED)).isEqualTo(ProcessStartOptions.unavailable(PROCESS_COMPLETED));
		assertThat(ProcessRules.startableOf(true, List.of(), () -> ProcessKeySelection.NONE, OUTBOX_NEVER_ASKED)).isEqualTo(ProcessStartOptions.unavailable(NO_PROCESS_KEY));
	}

	@Test
	@DisplayName("Verification that the process consumer of a namespace is handed back, and that a namespace without one is refused with 400")
	void aNamespaceWithoutAProcessConsumerIsRefused() {
		assertThat(ProcessRules.requireProcessConsumer(Optional.of("pw-alkt"), NAMESPACE, MUNICIPALITY_ID)).isEqualTo("pw-alkt");

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> ProcessRules.requireProcessConsumer(Optional.empty(), NAMESPACE, MUNICIPALITY_ID))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(400);
				assertThat(problem.getDetail()).isEqualTo("The namespace 'NAMESPACE' in municipality '2281' has no process consumer configured and runs no process");
			});
	}

	@Test
	@DisplayName("Verification that a key is oversized exactly when it is longer than the column of a process key")
	void aKeyIsOversizedWhenItDoesNotFit() {
		assertThat(ProcessRules.isOversized("k".repeat(PROCESS_KEY_LENGTH))).isFalse();
		assertThat(ProcessRules.isOversized("k".repeat(PROCESS_KEY_LENGTH + 1))).isTrue();
	}

	private static ErrandProcessEntity instance(final ProcessStatus status, final String processKey) {
		return createErrandProcessEntity(status, CLOCK, process -> process.withProcessKey(processKey));
	}

	private static ProcessKeySelection selection(final String processKey, final ProcessStartMode startMode) {
		return new ProcessKeySelection(processKey, startMode, List.of(processKey));
	}

	private static ProcessKeySelection ambiguous(final String... processKeys) {
		return new ProcessKeySelection(null, null, List.of(processKeys));
	}
}
