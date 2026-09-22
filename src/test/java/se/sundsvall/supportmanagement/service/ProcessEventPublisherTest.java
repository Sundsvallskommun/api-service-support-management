package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.eventlog.EventType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties.LoopGuard;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessActivityRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.model.ProcessCommand;
import se.sundsvall.supportmanagement.service.model.ProcessKeySelection;

import static generated.se.sundsvall.eventlog.EventType.CREATE;
import static generated.se.sundsvall.eventlog.EventType.DELETE;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.dept44.support.Identifier.Type.AD_ACCOUNT;
import static se.sundsvall.dept44.support.Identifier.Type.CUSTOM;
import static se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity.MESSAGE_LENGTH;
import static se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity.EXECUTED_BY_LENGTH;
import static se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity.PROCESS_KEY_LENGTH;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.ERROR;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ATTACHMENT;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.DECISION;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.MESSAGE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.PROCESS;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SIGNAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.AUTOMATIC;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.MANUAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.WAITING;
import static se.sundsvall.supportmanagement.service.ProcessErrorLog.CONFIG_ACTIVITY_TYPE;
import static se.sundsvall.supportmanagement.service.ProcessEventPublisher.LOOP_GUARD_ACTIVITY_TYPE;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.clearTriggerProcess;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.setTriggerProcess;

@ExtendWith(MockitoExtension.class)
class ProcessEventPublisherTest {

	private static final String NAMESPACE = "ALKT";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "e1f36b0e-6e2c-4b34-9c2c-6f6b3e1f36b0";
	private static final String PROCESS_SERVICE = "pw-alkt";
	private static final String APPLICATION = "alkt-ansokan";
	private static final String SUPERVISION = "alkt-tillsyn";
	private static final String EXECUTED_BY = "jo12doe";
	private static final String REQUEST_GROUP_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
	private static final String SIGNAL_NAME = "granskning-godkand";

	private static final int THRESHOLD = 20;
	private static final Duration WINDOW = Duration.ofMinutes(10);

	private final Clock clock = Clock.fixed(Instant.parse("2026-09-09T08:00:00Z"), ZoneId.of("UTC"));
	private final ProcessEngineProperties properties = new ProcessEngineProperties(new LoopGuard(THRESHOLD, WINDOW), new ProcessEngineProperties.DirectRun(true, 2, 4, 500));

	@Mock
	private NamespaceConfigService namespaceConfigServiceMock;

	@Mock
	private ProcessEventOutboxRepository outboxRepositoryMock;

	@Mock
	private ErrandProcessRepository processRepositoryMock;

	@Mock
	private ErrandProcessActivityRepository activityRepositoryMock;

	@Mock
	private ProcessKeySelector processKeySelectorMock;

	@Mock
	private ApplicationEventPublisher applicationEventPublisherMock;

	@Captor
	private ArgumentCaptor<ProcessEventOutboxEntity> outboxCaptor;

	@Captor
	private ArgumentCaptor<ErrandProcessActivityEntity> activityCaptor;

	private ProcessEventPublisher publisher;

	/**
	 * Only an exact false, trimmed and whatever its casing, keeps the row from being written. Everything else means wake
	 * the process.
	 */
	private static Stream<Arguments> headerValues() {
		return Stream.of(
			arguments("false", false),
			arguments("FALSE", false),
			arguments("False", false),
			arguments("  false  ", false),
			arguments("true", true),
			arguments("", true),
			arguments("   ", true),
			arguments("nonsense", true),
			arguments(null, true));
	}

	private static Stream<Arguments> commands() {
		return Stream.of(
			arguments(PROCESS, new ProcessCommand(APPLICATION, null)),
			arguments(SIGNAL, new ProcessCommand(null, SIGNAL_NAME)));
	}

	@BeforeEach
	void setUp() {
		publisher = new ProcessEventPublisher(namespaceConfigServiceMock, outboxRepositoryMock, processRepositoryMock, processKeySelectorMock, new ProcessErrorLog(activityRepositoryMock, properties, clock), properties, clock,
			applicationEventPublisherMock);
	}

	@AfterEach
	void tearDown() {
		Identifier.remove();
		clearTriggerProcess();
	}

	@Test
	@DisplayName("Verification that a namespace running no process pays for nothing beyond the lookup that says so")
	void aNamespaceWithoutAProcessConsumerWritesNothing() {
		when(namespaceConfigServiceMock.getProcessConsumer(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null, false);

		verifyNoInteractions(outboxRepositoryMock, processRepositoryMock, activityRepositoryMock, processKeySelectorMock, applicationEventPublisherMock);
	}

	@Test
	@DisplayName("Verification that a machine identity asking not to wake the process is obeyed before anything is counted")
	void anOptOutFromAMachineIdentityWritesNothing() {
		givenNamespaceRunsProcess();
		asMachine();
		setTriggerProcess("false");

		publisher.publish(errand(), UPDATE, MESSAGE, PROCESS_SERVICE, REQUEST_GROUP_ID, null, false);

		verifyNoInteractions(outboxRepositoryMock, processRepositoryMock, activityRepositoryMock);
	}

	@ParameterizedTest
	@MethodSource("headerValues")
	@DisplayName("Verification that only an exact false silences the row, and that everything else wakes the process")
	void theHeaderIsReadStrictly(final String header, final boolean expectedToPublish) {
		givenNamespaceRunsProcess();
		asMachine();
		setTriggerProcess(header);

		if (expectedToPublish) {
			givenTriggers(MESSAGE);
			givenLabels(APPLICATION, AUTOMATIC);
		}

		publisher.publish(errand(), UPDATE, MESSAGE, PROCESS_SERVICE, REQUEST_GROUP_ID, null, false);

		verify(outboxRepositoryMock, expectedToPublish ? times(1) : never()).save(any());
	}

	@Test
	@DisplayName("Verification that a handler's write wakes the process however the client sets the header, which closes the one hole a freely set header opens")
	void theHeaderIsNotHonouredForAnAdAccount() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenLabels(APPLICATION, AUTOMATIC);
		Identifier.set(Identifier.create().withType(AD_ACCOUNT).withValue(EXECUTED_BY));
		setTriggerProcess("false");

		publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null, false);

		verify(outboxRepositoryMock).save(any());
	}

	@Test
	@DisplayName("Verification that a scheduled job, which has no request to carry a header at all, publishes")
	void aWriteWithoutARequestContextIsPublished() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenLabels(APPLICATION, AUTOMATIC);

		publisher.publish(errand(), UPDATE, MESSAGE, null, null, null, false);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		assertThat(outboxCaptor.getValue().getExecutedBy()).isNull();
	}

	@Test
	void aSubTypeThatIsNoTriggerOfTheNamespaceWritesNothing() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);

		publisher.publish(errand(), UPDATE, ATTACHMENT, EXECUTED_BY, REQUEST_GROUP_ID, null, false);

		verify(outboxRepositoryMock, never()).save(any());
		verifyNoInteractions(processRepositoryMock, activityRepositoryMock, processKeySelectorMock);
	}

	@Test
	@DisplayName("Verification that the brake measures what has reached the process, so a pile of undelivered rows cannot trip it")
	void theBrakeCountsDeliveredRowsWithinTheWindow() {
		givenNamespaceRunsProcess();
		givenDeliveredCount(THRESHOLD + 1L);

		publisher.publish(errand(), UPDATE, MESSAGE, PROCESS_SERVICE, REQUEST_GROUP_ID, null, false);

		verify(outboxRepositoryMock).countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter(ERRAND_ID, OffsetDateTime.now(clock).minus(WINDOW));
		verify(outboxRepositoryMock, never()).save(any());
	}

	@Test
	@DisplayName("Verification that the maximum is the most that reaches the process: with that many delivered, the next event is dropped")
	void theBrakeDropsTheEventThatWouldGoPastTheMaximum() {
		givenNamespaceRunsProcess();
		givenDeliveredCount(THRESHOLD);

		publisher.publish(errand(), UPDATE, MESSAGE, PROCESS_SERVICE, REQUEST_GROUP_ID, null, false);

		verify(outboxRepositoryMock, never()).save(any());
	}

	@Test
	@DisplayName("Verification that an errand one event short of the maximum still gets its event through")
	void theBrakeLetsTheEventThatReachesTheMaximumThrough() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenLabels(APPLICATION, AUTOMATIC);
		givenDeliveredCount(THRESHOLD - 1L);

		publisher.publish(errand(), UPDATE, MESSAGE, PROCESS_SERVICE, REQUEST_GROUP_ID, null, false);

		verify(outboxRepositoryMock).save(any());
	}

	@Test
	@DisplayName("Verification that the entry the brake writes hangs on no process instance, since it fires when there is none")
	void theBrakeWritesAnErrorEntryWithoutAnInstance() {
		givenNamespaceRunsProcess();
		givenDeliveredCount(THRESHOLD + 1L);

		publisher.publish(errand(), UPDATE, MESSAGE, PROCESS_SERVICE, REQUEST_GROUP_ID, null, false);

		verify(activityRepositoryMock).save(activityCaptor.capture());
		assertThat(activityCaptor.getValue()).satisfies(entry -> {
			assertThat(entry.getErrandProcessId()).isNull();
			assertThat(entry.getErrandId()).isEqualTo(ERRAND_ID);
			assertThat(entry.getActivityType()).isEqualTo(LOOP_GUARD_ACTIVITY_TYPE);
			assertThat(entry.getSeverity()).isEqualTo(ERROR);
			assertThat(entry.getMessage()).contains("emergency brake tripped");
		});
	}

	@Test
	@DisplayName("Verification that a loop producing event after event leaves one entry per window behind, not one per event")
	void theBrakeEntryIsWrittenOncePerErrandAndWindow() {
		givenNamespaceRunsProcess();
		givenDeliveredCount(THRESHOLD + 1L);
		when(activityRepositoryMock.existsByErrandIdAndErrorCodeAndCreatedAfter(eq(ERRAND_ID), eq("EVENT_RATE_EXCEEDED"), any())).thenReturn(true);

		publisher.publish(errand(), UPDATE, MESSAGE, PROCESS_SERVICE, REQUEST_GROUP_ID, null, false);

		verify(activityRepositoryMock, never()).save(any());
	}

	@Test
	@DisplayName("Verification that a decision concluded by a handler is published past a tripped brake, which is then neither asked nor reported")
	void aConcludedDecisionPassesTheBrake() {
		givenNamespaceRunsProcess();
		givenTriggers(DECISION);
		Identifier.set(Identifier.create().withType(AD_ACCOUNT).withValue(EXECUTED_BY));
		givenLabels(APPLICATION, AUTOMATIC);
		lenient().when(outboxRepositoryMock.countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter(eq(ERRAND_ID), any())).thenReturn(THRESHOLD + 1L);

		publisher.publish(errand(), UPDATE, DECISION, EXECUTED_BY, REQUEST_GROUP_ID, null, true);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("UPDATE");
		assertThat(outboxCaptor.getValue().getEventSubType()).isEqualTo("DECISION");
		verify(outboxRepositoryMock, never()).countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter(any(), any());
		verifyNoInteractions(activityRepositoryMock);
	}

	@Test
	@DisplayName("Verification that a decision concluded by the process itself is held back by a tripped brake")
	void aDecisionConcludedByTheProcessIsHeldBackByTheBrake() {
		givenNamespaceRunsProcess();
		givenDeliveredCount(THRESHOLD);
		asMachine();

		publisher.publish(errand(), UPDATE, DECISION, PROCESS_SERVICE, REQUEST_GROUP_ID, null, true);

		verify(outboxRepositoryMock, never()).save(any());
		verify(activityRepositoryMock).save(activityCaptor.capture());
		assertThat(activityCaptor.getValue().getActivityType()).isEqualTo(LOOP_GUARD_ACTIVITY_TYPE);
	}

	@Test
	@DisplayName("Verification that an ordinary change to a decision is held back by a tripped brake like any other event")
	void anOrdinaryDecisionChangeIsHeldBackByTheBrake() {
		givenNamespaceRunsProcess();
		givenDeliveredCount(THRESHOLD);

		publisher.publish(errand(), UPDATE, DECISION, EXECUTED_BY, REQUEST_GROUP_ID, null, false);

		verify(outboxRepositoryMock, never()).save(any());
	}

	@Test
	@DisplayName("Verification that a process concluding its own decision is not woken by it, since passing the brake is all a concluded decision is given")
	void aConcludedDecisionStillObeysTheOptOut() {
		givenNamespaceRunsProcess();
		asMachine();
		setTriggerProcess("false");

		publisher.publish(errand(), UPDATE, DECISION, PROCESS_SERVICE, REQUEST_GROUP_ID, null, true);

		verifyNoInteractions(outboxRepositoryMock, processRepositoryMock, activityRepositoryMock);
	}

	@Test
	@DisplayName("Verification that a concluded decision is not published by a namespace that does not trigger on decisions")
	void aConcludedDecisionStillNeedsItsTrigger() {
		givenNamespaceRunsProcess();
		givenTriggers(ERRAND);

		publisher.publish(errand(), UPDATE, DECISION, EXECUTED_BY, REQUEST_GROUP_ID, null, true);

		verify(outboxRepositoryMock, never()).save(any());
		verifyNoInteractions(processRepositoryMock, processKeySelectorMock);
	}

	@Test
	@DisplayName("Verification that labels pointing in two directions stop the publication and say so on the errand, naming both keys")
	void ambiguousLabelsWriteAnErrorEntryAndNoRow() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenNoInstances();
		givenAmbiguousLabels();

		publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null, false);

		verify(outboxRepositoryMock, never()).save(any());
		verify(activityRepositoryMock).save(activityCaptor.capture());
		assertThat(activityCaptor.getValue()).satisfies(entry -> {
			assertThat(entry.getErrandProcessId()).isNull();
			assertThat(entry.getActivityType()).isEqualTo(CONFIG_ACTIVITY_TYPE);
			assertThat(entry.getSeverity()).isEqualTo(ERROR);
			assertThat(entry.getMessage()).contains(APPLICATION, SUPERVISION);
		});
	}

	@Test
	@DisplayName("Verification that a key too long for its column is reported as the configuration fault it is, rather than left to fail the insert and take the errand change with it")
	void aProcessKeyLongerThanItsColumnWritesAnErrorEntryAndNoRow() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenNoInstances();
		final var oversized = "a".repeat(PROCESS_KEY_LENGTH + 1);
		when(processKeySelectorMock.select(any())).thenReturn(new ProcessKeySelection(oversized, AUTOMATIC, List.of(oversized)));

		publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null, false);

		verify(outboxRepositoryMock, never()).save(any());
		verify(activityRepositoryMock).save(activityCaptor.capture());
		assertThat(activityCaptor.getValue()).satisfies(entry -> {
			assertThat(entry.getErrandProcessId()).isNull();
			assertThat(entry.getSeverity()).isEqualTo(ERROR);
			assertThat(entry.getMessage()).contains(String.valueOf(PROCESS_KEY_LENGTH + 1), String.valueOf(PROCESS_KEY_LENGTH));
			assertThat(entry.getMessage()).hasSizeLessThanOrEqualTo(MESSAGE_LENGTH);
		});
	}

	@Test
	@DisplayName("Verification that a key of exactly the width of its column is published, so the check refuses nothing it should not")
	void aProcessKeyOfExactlyTheColumnWidthIsPublished() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenNoInstances();
		final var exact = "a".repeat(PROCESS_KEY_LENGTH);
		when(processKeySelectorMock.select(any())).thenReturn(new ProcessKeySelection(exact, AUTOMATIC, List.of(exact)));

		publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null, false);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		assertThat(outboxCaptor.getValue().getProcessKey()).isEqualTo(exact);
	}

	@Test
	@DisplayName("Verification that an entry naming two enormous keys still fits the column it is written to")
	void theAmbiguityEntryFitsItsColumnHoweverLongTheKeysAre() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenNoInstances();
		when(processKeySelectorMock.select(any())).thenReturn(new ProcessKeySelection(null, null, List.of("b".repeat(40_000), "c".repeat(40_000))));

		publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null, false);

		verify(activityRepositoryMock).save(activityCaptor.capture());
		assertThat(activityCaptor.getValue().getMessage())
			.hasSizeLessThanOrEqualTo(MESSAGE_LENGTH)
			.contains("start the handling by hand");
	}

	@Test
	@DisplayName("Verification that an identity too long for its column is cut rather than allowed to fail a write it only traces")
	void anOversizedIdentityIsCutToFit() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenLabels(APPLICATION, AUTOMATIC);
		givenNoInstances();

		publisher.publish(errand(), UPDATE, MESSAGE, "x".repeat(EXECUTED_BY_LENGTH + 100), REQUEST_GROUP_ID, null, false);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		assertThat(outboxCaptor.getValue().getExecutedBy()).hasSize(EXECUTED_BY_LENGTH);
	}

	@Test
	@DisplayName("Verification that ten events on an ambiguous errand leave one entry behind, not ten")
	void theAmbiguityEntryIsWrittenOncePerErrandAndWindow() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenNoInstances();
		givenAmbiguousLabels();
		when(activityRepositoryMock.existsByErrandIdAndErrorCodeAndCreatedAfter(eq(ERRAND_ID), eq("AMBIGUOUS_PROCESS_KEY"), any()))
			.thenReturn(false)
			.thenReturn(true);

		for (var i = 0; i < 10; i++) {
			publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null, false);
		}

		verify(activityRepositoryMock, times(1)).save(any());
	}

	@Test
	@DisplayName("Verification that the key of an errand with a process instance is nailed down by the instance, so a changed label cannot move it")
	void theKeyIsTakenFromTheInstanceRatherThanFromTheLabels() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenLabels(SUPERVISION, AUTOMATIC);
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(instance(APPLICATION, WAITING)));

		publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null, false);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		assertThat(outboxCaptor.getValue().getProcessKey()).isEqualTo(APPLICATION);
	}

	@Test
	@DisplayName("Verification that deleting an errand whose label is gone is published all the same, or the instance is left running for an errand that no longer exists")
	void aDeletionIsPublishedWithoutAKey() {
		givenNamespaceRunsProcess();
		givenNoInstances();

		publisher.publish(errand(), DELETE, ERRAND, EXECUTED_BY, REQUEST_GROUP_ID, null, false);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		assertThat(outboxCaptor.getValue().getProcessKey()).isNull();
		assertThat(outboxCaptor.getValue().getEventType()).isEqualTo(DELETE.getValue());
	}

	@Test
	@DisplayName("Verification that the labels of a deletion are never read, since the errand and its labels are normally gone by then")
	void aDeletionDoesNotReadTheLabels() {
		givenNamespaceRunsProcess();
		givenNoInstances();

		publisher.publish(errand(), DELETE, ERRAND, EXECUTED_BY, REQUEST_GROUP_ID, null, false);

		verifyNoInteractions(processKeySelectorMock);
	}

	@Test
	@DisplayName("Verification that a deletion passes all three layers of the loop guard, since it cannot loop and holding it back would leave the instance running")
	void aDeletionPassesTheLoopGuard() {
		givenNamespaceRunsProcess();
		givenNoInstances();
		asMachine();
		setTriggerProcess("false");

		publisher.publish(errand(), DELETE, ERRAND, PROCESS_SERVICE, REQUEST_GROUP_ID, null, false);

		verify(outboxRepositoryMock).save(any());
		verify(outboxRepositoryMock, never()).countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter(any(), any());
		verify(namespaceConfigServiceMock, never()).getProcessTriggers(any(), any());
		verifyNoInteractions(activityRepositoryMock);
	}

	@ParameterizedTest
	@MethodSource("commands")
	@DisplayName("Verification that a command asking not to wake the process from a machine identity is published all the same, since layer 1 is waived for commands of their own accord")
	void aCommandPassesLayerOneFromAMachineIdentity(final EventSubType subType, final ProcessCommand command) {
		givenNamespaceRunsProcess();
		givenNoInstances();
		givenLabels(APPLICATION, AUTOMATIC);
		asMachine();
		setTriggerProcess("false");

		publisher.publish(errand(), UPDATE, subType, PROCESS_SERVICE, REQUEST_GROUP_ID, command, false);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		assertThat(outboxCaptor.getValue().getEventSubType()).isEqualTo(subType.getValue());
		assertThat(outboxCaptor.getValue().getProcessKey()).isEqualTo(APPLICATION);
		assertThat(outboxCaptor.getValue().getSignalName()).isEqualTo(command.signalName());
	}

	@Test
	void anEventOtherThanADeletionIsNotPublishedWithoutAKey() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenNoInstances();
		when(processKeySelectorMock.select(any())).thenReturn(ProcessKeySelection.NONE);

		publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null, false);

		verify(outboxRepositoryMock, never()).save(any());
		verify(activityRepositoryMock, never()).save(any());
	}

	@ParameterizedTest
	@MethodSource("commands")
	@DisplayName("Verification that a command is published though the brake has tripped, carrying what it carries")
	void aCommandIsPublishedThoughTheBrakeHasTripped(final EventSubType subType, final ProcessCommand command) {
		givenNamespaceRunsProcess();
		givenNoInstances();
		givenLabels(APPLICATION, AUTOMATIC);
		lenient().when(outboxRepositoryMock.countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter(eq(ERRAND_ID), any())).thenReturn(THRESHOLD + 1L);

		publisher.publish(errand(), UPDATE, subType, EXECUTED_BY, REQUEST_GROUP_ID, command, false);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		assertThat(outboxCaptor.getValue().getEventSubType()).isEqualTo(subType.getValue());
		assertThat(outboxCaptor.getValue().getProcessKey()).isEqualTo(APPLICATION);
		assertThat(outboxCaptor.getValue().getSignalName()).isEqualTo(command.signalName());
		assertThat(outboxCaptor.getValue().isStartAllowed()).isEqualTo(PROCESS == subType);
		verify(outboxRepositoryMock, never()).countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter(any(), any());
		verify(activityRepositoryMock, never()).save(any());
	}

	@Test
	@DisplayName("Verification that a command is no errand change, so the process triggers of the namespace have no say over it")
	void aCommandIsPublishedThoughTheNamespaceHasNoTriggers() {
		givenNamespaceRunsProcess();
		givenNoInstances();
		givenLabels(APPLICATION, AUTOMATIC);
		lenient().when(namespaceConfigServiceMock.getProcessTriggers(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Set.of());

		publisher.publish(errand(), UPDATE, SIGNAL, EXECUTED_BY, REQUEST_GROUP_ID, new ProcessCommand(null, SIGNAL_NAME), false);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		assertThat(outboxCaptor.getValue().getSignalName()).isEqualTo(SIGNAL_NAME);
		verify(namespaceConfigServiceMock, never()).getProcessTriggers(any(), any());
	}

	@Test
	@DisplayName("Verification that the key a handler chose is not resolved again, which would drop the command in the very case it exists for")
	void aStartCommandKeepsItsOwnKeyThoughTheLabelsAreAmbiguous() {
		givenNamespaceRunsProcess();
		givenNoInstances();
		givenAmbiguousLabels();

		publisher.publish(errand(), CREATE, PROCESS, EXECUTED_BY, REQUEST_GROUP_ID, new ProcessCommand(SUPERVISION, null), false);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		assertThat(outboxCaptor.getValue().getProcessKey()).isEqualTo(SUPERVISION);
		verify(activityRepositoryMock, never()).save(any());
	}

	@Test
	@DisplayName("Verification that a signal is aimed at a process that already runs and therefore never carries the permission to start one")
	void aSignalAsksForNoStart() {
		givenNamespaceRunsProcess();
		givenNoInstances();
		givenLabels(APPLICATION, AUTOMATIC);

		publisher.publish(errand(), UPDATE, SIGNAL, EXECUTED_BY, REQUEST_GROUP_ID, new ProcessCommand(null, SIGNAL_NAME), false);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		assertThat(outboxCaptor.getValue().isStartAllowed()).isFalse();
	}

	@Test
	void theStartPermissionIsGivenToAnErrandWithNoInstanceAndAnAutomaticLabel() {
		assertThat(startAllowedFor(List.of(), APPLICATION, AUTOMATIC)).isTrue();
	}

	@Test
	@DisplayName("Verification that a process which has run its course is never started over by an ordinary errand change")
	void theStartPermissionIsRefusedToAnErrandWithACompletedInstance() {
		assertThat(startAllowedFor(List.of(instance(APPLICATION, COMPLETED)), APPLICATION, AUTOMATIC)).isFalse();
	}

	@Test
	@DisplayName("Verification that trying again after a start that failed is recovery rather than a second process")
	void theStartPermissionIsGivenToAnErrandWhoseOnlyInstanceFailed() {
		assertThat(startAllowedFor(List.of(instance(APPLICATION, FAILED)), APPLICATION, AUTOMATIC)).isTrue();
	}

	@Test
	void theStartPermissionIsRefusedToAnErrandWithALiveInstance() {
		assertThat(startAllowedFor(List.of(instance(APPLICATION, WAITING)), APPLICATION, AUTOMATIC)).isFalse();
	}

	@Test
	@DisplayName("Verification that a label saying MANUAL leaves the permission to the handler, while the event is published all the same")
	void theStartPermissionIsRefusedWhenTheLabelSaysManual() {
		assertThat(startAllowedFor(List.of(), APPLICATION, MANUAL)).isFalse();
	}

	@Test
	@DisplayName("Verification that the start mode is read only off a label naming the key the row carries, never off a label pointing at another process")
	void theStartPermissionIsRefusedWhenTheLabelsNameAnotherProcessThanTheInstance() {
		assertThat(startAllowedFor(List.of(instance(APPLICATION, FAILED)), SUPERVISION, AUTOMATIC)).isFalse();
	}

	@Test
	void theRowCarriesWhereItIsHeadedAndWhoWroteIt() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenLabels(APPLICATION, AUTOMATIC);
		givenNoInstances();

		publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null, false);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		assertThat(outboxCaptor.getValue()).satisfies(row -> {
			assertThat(row.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
			assertThat(row.getNamespace()).isEqualTo(NAMESPACE);
			assertThat(row.getErrandId()).isEqualTo(ERRAND_ID);
			assertThat(row.getProcessService()).isEqualTo(PROCESS_SERVICE);
			assertThat(row.getProcessKey()).isEqualTo(APPLICATION);
			assertThat(row.getEventType()).isEqualTo(UPDATE.getValue());
			assertThat(row.getEventSubType()).isEqualTo(MESSAGE.getValue());
			assertThat(row.getExecutedBy()).isEqualTo(EXECUTED_BY);
			assertThat(row.getRequestGroupId()).isEqualTo(REQUEST_GROUP_ID);
			assertThat(row.getSignalName()).isNull();
		});
		verify(applicationEventPublisherMock).publishEvent(new ProcessEventWritten(ERRAND_ID));
	}

	@Test
	@DisplayName("Verification that no signal goes out for a row that was never written, since there would be nothing for the relay to deliver")
	void noSignalWithoutARow() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);

		publisher.publish(errand(), UPDATE, ATTACHMENT, EXECUTED_BY, REQUEST_GROUP_ID, null, false);

		verifyNoInteractions(applicationEventPublisherMock);
	}

	@Test
	@DisplayName("Verification that a publication which cannot be written takes the errand change down with it, whatever the caller does with the exception")
	void aFailureWithATransactionMarksItRollbackOnly() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenLabels(APPLICATION, AUTOMATIC);
		givenNoInstances();

		final var failure = new IllegalStateException("the row could not be written");
		when(outboxRepositoryMock.save(any())).thenThrow(failure);

		final var status = mock(TransactionStatus.class);

		inTransaction(status, () -> assertThatThrownBy(() -> publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null, false)).isSameAs(failure));

		verify(status).setRollbackOnly();
		verifyNoInteractions(applicationEventPublisherMock);
	}

	@Test
	@DisplayName("Verification that a write with no transaction to roll back is reported rather than allowed to break a call that already saved the errand")
	void aFailureWithoutATransactionIsNotThrown() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenLabels(APPLICATION, AUTOMATIC);
		givenNoInstances();
		when(outboxRepositoryMock.save(any())).thenThrow(new IllegalStateException("the row could not be written"));

		assertThatNoException().isThrownBy(() -> publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null, false));
	}

	@ParameterizedTest
	@EnumSource(value = EventType.class, names = {
		"CREATE", "UPDATE", "DELETE"
	})
	@DisplayName("Verification that the event types a process knows are written as they are, a deletion reading neither the triggers nor the labels")
	void theEventTypesAProcessKnowsAreWrittenAsTheyAre(final EventType eventType) {
		givenNamespaceRunsProcess();
		lenient().when(namespaceConfigServiceMock.getProcessTriggers(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Set.of(ERRAND));
		lenient().when(processKeySelectorMock.select(any())).thenReturn(new ProcessKeySelection(APPLICATION, AUTOMATIC, List.of(APPLICATION)));
		givenNoInstances();

		publisher.publish(errand(), eventType, ERRAND, EXECUTED_BY, REQUEST_GROUP_ID, null, false);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		assertThat(outboxCaptor.getValue().getEventType()).isEqualTo(eventType.getValue());
	}

	@ParameterizedTest
	@EnumSource(value = EventType.class, names = {
		"CREATE", "UPDATE", "DELETE"
	}, mode = EXCLUDE)
	@DisplayName("Verification that an event type no process knows fails the publication and takes the errand change down, rather than leaving a row behind that can never be delivered")
	void anEventTypeNoProcessKnowsFailsThePublication(final EventType eventType) {
		givenNamespaceRunsProcess();
		final var status = mock(TransactionStatus.class);

		inTransaction(status, () -> assertThatIllegalArgumentException()
			.isThrownBy(() -> publisher.publish(errand(), eventType, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null, false))
			.withMessageContaining(eventType.getValue()));

		verify(status).setRollbackOnly();
		verify(namespaceConfigServiceMock, never()).getProcessTriggers(any(), any());
		verifyNoInteractions(outboxRepositoryMock, processRepositoryMock, activityRepositoryMock, processKeySelectorMock, applicationEventPublisherMock);
	}

	@ParameterizedTest
	@EnumSource(EventType.class)
	@DisplayName("Verification that a namespace running no process never gets as far as the event type, so no event type can fail a write to its errands")
	void aNamespaceWithoutAProcessConsumerTakesEveryEventType(final EventType eventType) {
		when(namespaceConfigServiceMock.getProcessConsumer(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());
		final var status = mock(TransactionStatus.class);

		inTransaction(status, () -> assertThatNoException().isThrownBy(() -> publisher.publish(errand(), eventType, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null, false)));

		verifyNoInteractions(status, outboxRepositoryMock);
	}

	private boolean startAllowedFor(final List<ErrandProcessEntity> instances, final String labelKey, final ProcessStartMode startMode) {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenLabels(labelKey, startMode);
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(instances);

		publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null, false);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		return outboxCaptor.getValue().isStartAllowed();
	}

	private void givenNamespaceRunsProcess() {
		when(namespaceConfigServiceMock.getProcessConsumer(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(PROCESS_SERVICE));
	}

	private void givenTriggers(final EventSubType... triggers) {
		when(namespaceConfigServiceMock.getProcessTriggers(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Set.of(triggers));
	}

	private void givenLabels(final String processKey, final ProcessStartMode startMode) {
		when(processKeySelectorMock.select(any())).thenReturn(new ProcessKeySelection(processKey, startMode, List.of(processKey)));
	}

	private void givenAmbiguousLabels() {
		when(processKeySelectorMock.select(any())).thenReturn(new ProcessKeySelection(null, null, List.of(APPLICATION, SUPERVISION)));
	}

	private void givenNoInstances() {
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of());
	}

	private void givenDeliveredCount(final long delivered) {
		when(outboxRepositoryMock.countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter(eq(ERRAND_ID), any())).thenReturn(delivered);
	}

	private void asMachine() {
		Identifier.set(Identifier.create().withType(CUSTOM).withTypeString("processEngine").withValue(PROCESS_SERVICE));
	}

	private static void inTransaction(final TransactionStatus status, final Runnable call) {
		try (var transactionAspect = mockStatic(TransactionAspectSupport.class)) {
			transactionAspect.when(TransactionAspectSupport::currentTransactionStatus).thenReturn(status);
			TransactionSynchronizationManager.setActualTransactionActive(true);

			try {
				call.run();
			} finally {
				TransactionSynchronizationManager.setActualTransactionActive(false);
			}
		}
	}

	private ErrandEntity errand() {
		return ErrandEntity.create()
			.withId(ERRAND_ID)
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID);
	}

	private ErrandProcessEntity instance(final String processKey, final ProcessStatus status) {
		final var instance = ErrandProcessEntity.create()
			.withId(randomUUID().toString())
			.withErrandId(ERRAND_ID)
			.withProcessKey(processKey);

		instance.applyStatus(status, clock);

		return instance;
	}
}
