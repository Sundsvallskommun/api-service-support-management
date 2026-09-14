package se.sundsvall.supportmanagement.service;

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
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
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
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.MESSAGE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.PROCESS;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SIGNAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.AUTOMATIC;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.MANUAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.WAITING;
import static se.sundsvall.supportmanagement.service.ProcessEventPublisher.AMBIGUOUS_KEY_ACTIVITY_TYPE;
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
	private final ProcessEngineProperties properties = new ProcessEngineProperties(List.of(PROCESS_SERVICE), new LoopGuard(THRESHOLD, WINDOW));

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

	@Captor
	private ArgumentCaptor<ProcessEventOutboxEntity> outboxCaptor;

	@Captor
	private ArgumentCaptor<ErrandProcessActivityEntity> activityCaptor;

	private ProcessEventPublisher publisher;

	/**
	 * Only an exact false, trimmed and whatever its casing, keeps the row from being written. Everything else means wake
	 * the process, and one row too many is the direction this leans in on purpose: a needless wake is caught by the two
	 * layers below, while a missing one is a process left waiting for ever with nobody noticing.
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

	@BeforeEach
	void setUp() {
		publisher = new ProcessEventPublisher(namespaceConfigServiceMock, outboxRepositoryMock, processRepositoryMock, activityRepositoryMock, processKeySelectorMock, properties, clock);
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

		publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null);

		verifyNoInteractions(outboxRepositoryMock, processRepositoryMock, activityRepositoryMock, processKeySelectorMock);
	}

	@Test
	@DisplayName("Verification that a machine identity asking not to wake the process is obeyed before anything is counted")
	void anOptOutFromAMachineIdentityWritesNothing() {
		givenNamespaceRunsProcess();
		asMachine();
		setTriggerProcess("false");

		publisher.publish(errand(), UPDATE, MESSAGE, PROCESS_SERVICE, REQUEST_GROUP_ID, null);

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

		publisher.publish(errand(), UPDATE, MESSAGE, PROCESS_SERVICE, REQUEST_GROUP_ID, null);

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

		publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null);

		verify(outboxRepositoryMock).save(any());
	}

	@Test
	@DisplayName("Verification that a scheduled job, which has no request to carry a header at all, publishes")
	void aWriteWithoutARequestContextIsPublished() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenLabels(APPLICATION, AUTOMATIC);

		publisher.publish(errand(), UPDATE, MESSAGE, null, null, null);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		assertThat(outboxCaptor.getValue().getExecutedBy()).isNull();
	}

	@Test
	void aSubTypeThatIsNoTriggerOfTheNamespaceWritesNothing() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);

		publisher.publish(errand(), UPDATE, ATTACHMENT, EXECUTED_BY, REQUEST_GROUP_ID, null);

		verify(outboxRepositoryMock, never()).save(any());
		verifyNoInteractions(processRepositoryMock, activityRepositoryMock, processKeySelectorMock);
	}

	@Test
	@DisplayName("Verification that the brake measures what has reached the process, so a pile of undelivered rows cannot trip it")
	void theBrakeCountsDeliveredRowsWithinTheWindow() {
		givenNamespaceRunsProcess();
		givenDeliveredCount(THRESHOLD + 1L);

		publisher.publish(errand(), UPDATE, MESSAGE, PROCESS_SERVICE, REQUEST_GROUP_ID, null);

		verify(outboxRepositoryMock).countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter(ERRAND_ID, OffsetDateTime.now(clock).minus(WINDOW));
		verify(outboxRepositoryMock, never()).save(any());
	}

	@Test
	void theBrakeLetsTheThresholdItselfThrough() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenLabels(APPLICATION, AUTOMATIC);
		givenDeliveredCount(THRESHOLD);

		publisher.publish(errand(), UPDATE, MESSAGE, PROCESS_SERVICE, REQUEST_GROUP_ID, null);

		verify(outboxRepositoryMock).save(any());
	}

	@Test
	@DisplayName("Verification that the entry the brake writes hangs on no process instance, since it fires when there is none")
	void theBrakeWritesAnErrorEntryWithoutAnInstance() {
		givenNamespaceRunsProcess();
		givenDeliveredCount(THRESHOLD + 1L);

		publisher.publish(errand(), UPDATE, MESSAGE, PROCESS_SERVICE, REQUEST_GROUP_ID, null);

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
		when(activityRepositoryMock.existsByErrandIdAndActivityTypeAndSeverityAndCreatedAfter(eq(ERRAND_ID), eq(LOOP_GUARD_ACTIVITY_TYPE), eq(ERROR), any())).thenReturn(true);

		publisher.publish(errand(), UPDATE, MESSAGE, PROCESS_SERVICE, REQUEST_GROUP_ID, null);

		verify(activityRepositoryMock, never()).save(any());
	}

	@Test
	@DisplayName("Verification that labels pointing in two directions stop the publication and say so on the errand, naming both keys")
	void ambiguousLabelsWriteAnErrorEntryAndNoRow() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenNoInstances();
		givenAmbiguousLabels();

		publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null);

		verify(outboxRepositoryMock, never()).save(any());
		verify(activityRepositoryMock).save(activityCaptor.capture());
		assertThat(activityCaptor.getValue()).satisfies(entry -> {
			assertThat(entry.getErrandProcessId()).isNull();
			assertThat(entry.getActivityType()).isEqualTo(AMBIGUOUS_KEY_ACTIVITY_TYPE);
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

		publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null);

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

		publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null);

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

		publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null);

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

		publisher.publish(errand(), UPDATE, MESSAGE, "x".repeat(EXECUTED_BY_LENGTH + 100), REQUEST_GROUP_ID, null);

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
		when(activityRepositoryMock.existsByErrandIdAndActivityTypeAndSeverityAndCreatedAfter(eq(ERRAND_ID), eq(AMBIGUOUS_KEY_ACTIVITY_TYPE), eq(ERROR), any()))
			.thenReturn(false)
			.thenReturn(true);

		for (var i = 0; i < 10; i++) {
			publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null);
		}

		verify(activityRepositoryMock, times(1)).save(any());
	}

	@Test
	@DisplayName("Verification that the key of an errand with a process instance is nailed down by the instance, so a changed label cannot move it")
	void theKeyIsTakenFromTheInstanceRatherThanFromTheLabels() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenLabels(SUPERVISION, AUTOMATIC);
		when(processRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(instance(APPLICATION, WAITING)));

		publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		assertThat(outboxCaptor.getValue().getProcessKey()).isEqualTo(APPLICATION);
	}

	@Test
	@DisplayName("Verification that deleting an errand whose label is gone is published all the same, or the instance is left running for an errand that no longer exists")
	void aDeletionIsPublishedWithoutAKey() {
		givenNamespaceRunsProcess();
		givenTriggers(ERRAND);
		givenNoInstances();

		publisher.publish(errand(), DELETE, ERRAND, EXECUTED_BY, REQUEST_GROUP_ID, null);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		assertThat(outboxCaptor.getValue().getProcessKey()).isNull();
		assertThat(outboxCaptor.getValue().getEventType()).isEqualTo(DELETE.getValue());
	}

	@Test
	@DisplayName("Verification that the labels of a deletion are never read, since the errand and its labels are normally gone by then")
	void aDeletionDoesNotReadTheLabels() {
		givenNamespaceRunsProcess();
		givenTriggers(ERRAND);
		givenNoInstances();

		publisher.publish(errand(), DELETE, ERRAND, EXECUTED_BY, REQUEST_GROUP_ID, null);

		verifyNoInteractions(processKeySelectorMock);
	}

	@Test
	@DisplayName("Verification that nothing is written on the errand of a deletion, whose row an entry would have to hang on")
	void aDeletionWritesNoEntryWhenTheBrakeTrips() {
		givenNamespaceRunsProcess();
		givenDeliveredCount(THRESHOLD + 1L);

		publisher.publish(errand(), DELETE, ERRAND, EXECUTED_BY, REQUEST_GROUP_ID, null);

		verify(outboxRepositoryMock, never()).save(any());
		verify(activityRepositoryMock, never()).save(any());
	}

	@Test
	void anEventOtherThanADeletionIsNotPublishedWithoutAKey() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenNoInstances();
		when(processKeySelectorMock.select(any())).thenReturn(ProcessKeySelection.NONE);

		publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null);

		verify(outboxRepositoryMock, never()).save(any());
		verify(activityRepositoryMock, never()).save(any());
	}

	@Test
	@DisplayName("Verification that the button still works on the errands with the most traffic, which is where the brake would otherwise silence it")
	void aStartCommandIsPublishedThoughTheBrakeHasTripped() {
		givenNamespaceRunsProcess();
		givenNoInstances();
		lenient().when(processKeySelectorMock.select(any())).thenReturn(new ProcessKeySelection(APPLICATION, AUTOMATIC, List.of(APPLICATION)));
		lenient().when(outboxRepositoryMock.countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter(eq(ERRAND_ID), any())).thenReturn(THRESHOLD + 1L);

		publisher.publish(errand(), CREATE, PROCESS, EXECUTED_BY, REQUEST_GROUP_ID, new ProcessCommand(APPLICATION, null));

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		assertThat(outboxCaptor.getValue().getProcessKey()).isEqualTo(APPLICATION);
		assertThat(outboxCaptor.getValue().isStartAllowed()).isTrue();
		verify(outboxRepositoryMock, never()).countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter(any(), any());
	}

	@Test
	@DisplayName("Verification that a command is no errand change, so the process triggers of the namespace have no say over it")
	void aCommandIsPublishedThoughTheNamespaceHasNoTriggers() {
		givenNamespaceRunsProcess();
		givenNoInstances();
		givenLabels(APPLICATION, AUTOMATIC);
		lenient().when(namespaceConfigServiceMock.getProcessTriggers(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Set.of());

		publisher.publish(errand(), UPDATE, SIGNAL, EXECUTED_BY, REQUEST_GROUP_ID, new ProcessCommand(null, SIGNAL_NAME));

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

		publisher.publish(errand(), CREATE, PROCESS, EXECUTED_BY, REQUEST_GROUP_ID, new ProcessCommand(SUPERVISION, null));

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

		publisher.publish(errand(), UPDATE, SIGNAL, EXECUTED_BY, REQUEST_GROUP_ID, new ProcessCommand(null, SIGNAL_NAME));

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		assertThat(outboxCaptor.getValue().isStartAllowed()).isFalse();
	}

	@Test
	void theStartPermissionIsGivenToAnErrandWithNoInstanceAndAnAutomaticLabel() {
		assertThat(startAllowedFor(List.of(), AUTOMATIC)).isTrue();
	}

	@Test
	@DisplayName("Verification that a process which has run its course is never started over by an ordinary errand change")
	void theStartPermissionIsRefusedToAnErrandWithACompletedInstance() {
		assertThat(startAllowedFor(List.of(instance(APPLICATION, COMPLETED)), AUTOMATIC)).isFalse();
	}

	@Test
	@DisplayName("Verification that trying again after a start that failed is recovery rather than a second process")
	void theStartPermissionIsGivenToAnErrandWhoseOnlyInstanceFailed() {
		assertThat(startAllowedFor(List.of(instance(APPLICATION, FAILED)), AUTOMATIC)).isTrue();
	}

	@Test
	void theStartPermissionIsRefusedToAnErrandWithALiveInstance() {
		assertThat(startAllowedFor(List.of(instance(APPLICATION, WAITING)), AUTOMATIC)).isFalse();
	}

	@Test
	@DisplayName("Verification that a label saying MANUAL leaves the permission to the handler, while the event is published all the same")
	void theStartPermissionIsRefusedWhenTheLabelSaysManual() {
		assertThat(startAllowedFor(List.of(), MANUAL)).isFalse();
	}

	@Test
	void theRowCarriesWhereItIsHeadedAndWhoWroteIt() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenLabels(APPLICATION, AUTOMATIC);
		givenNoInstances();

		publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null);

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

		try (var transactionAspect = mockStatic(TransactionAspectSupport.class)) {
			transactionAspect.when(TransactionAspectSupport::currentTransactionStatus).thenReturn(status);
			TransactionSynchronizationManager.setActualTransactionActive(true);

			try {
				assertThatThrownBy(() -> publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null)).isSameAs(failure);
			} finally {
				TransactionSynchronizationManager.setActualTransactionActive(false);
			}
		}

		verify(status).setRollbackOnly();
	}

	@Test
	@DisplayName("Verification that a write with no transaction to roll back is reported rather than allowed to break a call that already saved the errand")
	void aFailureWithoutATransactionIsNotThrown() {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenLabels(APPLICATION, AUTOMATIC);
		givenNoInstances();
		when(outboxRepositoryMock.save(any())).thenThrow(new IllegalStateException("the row could not be written"));

		assertThatNoException().isThrownBy(() -> publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null));
	}

	private boolean startAllowedFor(final List<ErrandProcessEntity> instances, final ProcessStartMode startMode) {
		givenNamespaceRunsProcess();
		givenTriggers(MESSAGE);
		givenLabels(APPLICATION, startMode);
		when(processRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(instances);

		publisher.publish(errand(), UPDATE, MESSAGE, EXECUTED_BY, REQUEST_GROUP_ID, null);

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
		when(processRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of());
	}

	private void givenDeliveredCount(final long delivered) {
		when(outboxRepositoryMock.countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter(eq(ERRAND_ID), any())).thenReturn(delivered);
	}

	private void asMachine() {
		Identifier.set(Identifier.create().withType(CUSTOM).withTypeString("processEngine").withValue(PROCESS_SERVICE));
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
