package se.sundsvall.supportmanagement.service.scheduler.processevent;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import generated.se.sundsvall.pwalkt.ErrandEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties.DirectRun;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties.LoopGuard;
import se.sundsvall.supportmanagement.config.ProcessEventRelayProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessActivityRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.integration.pwalkt.PwAlktIntegration;
import se.sundsvall.supportmanagement.integration.pwalkt.PwAlktUnavailableException;
import se.sundsvall.supportmanagement.service.ProcessActivityLog;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.createErrandProcessEntity;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.ERROR;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.WAITING;
import static se.sundsvall.supportmanagement.service.ProcessActivityLog.DELIVERY_ACTIVITY_TYPE;
import static se.sundsvall.supportmanagement.service.scheduler.processevent.ProcessEventRelay.REJECTION_ERROR_CODE;

@ExtendWith(MockitoExtension.class)
class ProcessEventRelayTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-14T08:00:00.123456Z"), ZoneId.of("UTC"));
	private static final OffsetDateTime NOW = OffsetDateTime.now(CLOCK);
	private static final OffsetDateTime NOW_IN_MILLIS = NOW.truncatedTo(MILLIS);
	private static final String PROCESS_SERVICE = "pw-alkt";
	private static final String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "ALKT";
	private static final String PROCESS_KEY = "alkt-ansokan";
	private static final int BATCH_SIZE = 3;
	private static final Duration WINDOW = Duration.ofMinutes(10);
	private static final Duration TRANSACTION_BUFFER = Duration.ofSeconds(5);
	private static final Duration MAX_AGE = Duration.ofDays(30);
	private static final Duration UNHEALTHY_AFTER = Duration.ofMinutes(15);

	@Mock
	private ProcessEventOutboxRepository outboxRepositoryMock;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private ErrandProcessRepository processRepositoryMock;

	@Mock
	private ErrandProcessActivityRepository activityRepositoryMock;

	@Mock
	private PwAlktIntegration pwAlktIntegrationMock;

	@Mock
	private PlatformTransactionManager transactionManagerMock;

	@Captor
	private ArgumentCaptor<ErrandEvent> eventCaptor;

	@Captor
	private ArgumentCaptor<ErrandProcessActivityEntity> activityCaptor;

	@Captor
	private ArgumentCaptor<TransactionDefinition> definitionCaptor;

	private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

	private ProcessEventRelay relay;

	@BeforeEach
	void setUp() {
		final var properties = new ProcessEngineProperties(new LoopGuard(20, WINDOW), new DirectRun(true, 2, 4, 500));
		relay = new ProcessEventRelay(outboxRepositoryMock, errandsRepositoryMock, processRepositoryMock, pwAlktIntegrationMock, new ProcessActivityLog(activityRepositoryMock, properties, CLOCK),
			new ProcessEventRelayProperties(BATCH_SIZE, TRANSACTION_BUFFER, MAX_AGE, UNHEALTHY_AFTER), transactionManagerMock, CLOCK);

		logAppender.start();
		logger().addAppender(logAppender);
	}

	@AfterEach
	void tearDown() {
		logger().detachAppender(logAppender);
	}

	@Test
	@DisplayName("Verification that a run drops what has aged out first, then takes a batch of the oldest rows and delivers them errand by errand")
	void aRunDeliversErrandByErrandOldestFirst() {
		final var first = row("row-1", "errand-a", NOW.minusMinutes(3));
		final var second = row("row-2", "errand-b", NOW.minusMinutes(2));
		final var third = row("row-3", "errand-a", NOW.minusMinutes(1));
		givenWaiting(first, second, third);
		givenLocked(List.of("row-1", "row-3"), first, third);
		givenLocked(List.of("row-2"), second);
		givenAccepted();

		relay.relay();

		final var inOrder = inOrder(outboxRepositoryMock, pwAlktIntegrationMock);
		inOrder.verify(outboxRepositoryMock).findByDeliveredAtIsNullAndCreatedBeforeOrderByCreatedAscIdAsc(NOW.minus(MAX_AGE), PageRequest.of(0, BATCH_SIZE));
		inOrder.verify(outboxRepositoryMock).findByProcessServiceAndDeliveredAtIsNullAndCreatedBefore(PROCESS_SERVICE, NOW.minus(TRANSACTION_BUFFER), PageRequest.of(0, BATCH_SIZE, Sort.by("created", "id")));
		inOrder.verify(outboxRepositoryMock).findByIdInAndDeliveredAtIsNull(List.of("row-1", "row-3"));
		inOrder.verify(pwAlktIntegrationMock, times(2)).sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any());
		inOrder.verify(outboxRepositoryMock).findByIdInAndDeliveredAtIsNull(List.of("row-2"));
		inOrder.verify(pwAlktIntegrationMock).sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any());
		assertThat(List.of(first, second, third)).extracting(ProcessEventOutboxEntity::getDeliveredAt).containsOnly(NOW_IN_MILLIS);
	}

	@Test
	@DisplayName("Verification that every group and every batch of dropped rows gets a transaction of its own at read committed, which the template has to be told since no proxy tells it")
	void everyTransactionIsOfItsOwnAtReadCommitted() {
		final var row = row("row-1", ERRAND_ID, NOW.minusMinutes(1));
		givenWaiting(row);
		givenLocked(List.of("row-1"), row);
		givenAccepted();

		relay.relay();

		verify(transactionManagerMock, times(2)).getTransaction(definitionCaptor.capture());
		assertThat(definitionCaptor.getAllValues()).allSatisfy(definition -> {
			assertThat(definition.getPropagationBehavior()).isEqualTo(PROPAGATION_REQUIRES_NEW);
			assertThat(definition.getIsolationLevel()).isEqualTo(ISOLATION_READ_COMMITTED);
		});
	}

	@Test
	@DisplayName("Verification that rows that have aged out are dropped a batch at a time, each batch in a transaction of its own, until a batch comes back short")
	void aRunDropsWhatHasAgedOutBatchByBatch() {
		final var firstBatch = List.of(agedRow("row-1"), agedRow("row-2"), agedRow("row-3"));
		final var secondBatch = List.of(agedRow("row-4"), agedRow("row-5"), agedRow("row-6"));
		final var lastBatch = List.of(agedRow("row-7"));
		when(outboxRepositoryMock.findByDeliveredAtIsNullAndCreatedBeforeOrderByCreatedAscIdAsc(NOW.minus(MAX_AGE), PageRequest.of(0, BATCH_SIZE))).thenReturn(firstBatch, secondBatch, lastBatch);
		when(outboxRepositoryMock.findByIdInAndDeliveredAtIsNull(any())).thenReturn(firstBatch, secondBatch, lastBatch);
		givenWaiting();

		relay.relay();

		verify(outboxRepositoryMock, times(3)).deleteAllByIdInBatch(any());
		verify(transactionManagerMock, times(3)).commit(any());
		verifyNoInteractions(pwAlktIntegrationMock);
	}

	@Test
	@DisplayName("Verification that an errand whose delivery fails holds back no one but itself, whether pw-alkt refused it or something unforeseen went wrong")
	void anErrandThatFailsHoldsBackNoOneButItself() {
		final var first = row("row-1", "errand-a", NOW.minusMinutes(3));
		final var second = row("row-2", "errand-b", NOW.minusMinutes(2));
		final var third = row("row-3", "errand-c", NOW.minusMinutes(1));
		givenWaiting(first, second, third);
		givenLocked(List.of("row-1"), first);
		givenLocked(List.of("row-2"), second);
		givenLocked(List.of("row-3"), third);
		when(pwAlktIntegrationMock.sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any()))
			.thenThrow(new PwAlktUnavailableException(false, new IllegalStateException("503")))
			.thenThrow(new IllegalStateException("something unforeseen"))
			.thenReturn(true);

		assertThatNoException().isThrownBy(relay::relay);

		assertThat(first.getDeliveredAt()).isNull();
		assertThat(second.getDeliveredAt()).isNull();
		assertThat(third.getDeliveredAt()).isEqualTo(NOW_IN_MILLIS);
		verify(transactionManagerMock).rollback(any());
	}

	@Test
	@DisplayName("Verification that rows of an errand that never go through cannot fill the batch: the run fetches on past the errand and delivers the rows of the others")
	void aRunFetchesOnPastAnErrandThatFails() {
		final var stuck = row("row-1", "errand-stuck", NOW.minusMinutes(5));
		final var stuckToo = row("row-2", "errand-stuck", NOW.minusMinutes(4));
		final var stuckAsWell = row("row-3", "errand-stuck", NOW.minusMinutes(3));
		final var other = row("row-4", "errand-other", NOW.minusMinutes(2));
		givenWaiting(stuck, stuckToo, stuckAsWell);
		givenLocked(List.of("row-1", "row-2", "row-3"), stuck, stuckToo, stuckAsWell);
		when(outboxRepositoryMock.findByProcessServiceAndDeliveredAtIsNullAndCreatedBeforeAndErrandIdNotIn(PROCESS_SERVICE, NOW.minus(TRANSACTION_BUFFER), Set.of("errand-stuck"),
			PageRequest.of(0, BATCH_SIZE - 1, Sort.by("created", "id")))).thenReturn(List.of(other));
		givenLocked(List.of("row-4"), other);
		when(pwAlktIntegrationMock.sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any()))
			.thenThrow(new PwAlktUnavailableException(false, new IllegalStateException("503")))
			.thenReturn(true);

		relay.relay();

		assertThat(List.of(stuck, stuckToo, stuckAsWell)).extracting(ProcessEventOutboxEntity::getDeliveredAt).containsOnlyNulls();
		assertThat(other.getDeliveredAt()).isEqualTo(NOW_IN_MILLIS);
		verify(pwAlktIntegrationMock, times(2)).sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any());
	}

	@Test
	@DisplayName("Verification that a run stops fetching once a page comes back shorter than asked for, since that page held every row there was")
	void aRunStopsAfterAShortPage() {
		final var stuck = row("row-1", "errand-stuck", NOW.minusMinutes(5));
		givenWaiting(stuck);
		givenLocked(List.of("row-1"), stuck);
		when(pwAlktIntegrationMock.sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenThrow(new PwAlktUnavailableException(false, new IllegalStateException("503")));

		relay.relay();

		verify(outboxRepositoryMock, never()).findByProcessServiceAndDeliveredAtIsNullAndCreatedBeforeAndErrandIdNotIn(any(), any(), any(), any());
	}

	@Test
	@DisplayName("Verification that an open circuit breaker ends the run, since every errand after it would meet the same answer")
	void anOpenCircuitBreakerEndsTheRun() {
		final var first = row("row-1", "errand-a", NOW.minusMinutes(2));
		final var second = row("row-2", "errand-b", NOW.minusMinutes(1));
		givenWaiting(first, second);
		givenLocked(List.of("row-1"), first);
		when(pwAlktIntegrationMock.sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenThrow(new PwAlktUnavailableException(true, new IllegalStateException("open")));

		relay.relay();

		verify(outboxRepositoryMock, never()).findByIdInAndDeliveredAtIsNull(List.of("row-2"));
		assertThat(second.getDeliveredAt()).isNull();
	}

	@Test
	@DisplayName("Verification that a direct run delivers the rows of its errand, held to the age limit but not to the transaction buffer")
	void aDirectRunDeliversTheRowsOfItsErrand() {
		final var first = row("row-1", ERRAND_ID, NOW.minusMinutes(2));
		final var second = row("row-2", ERRAND_ID, NOW.minusMinutes(1));
		givenWaitingFor(ERRAND_ID, first, second);
		givenLocked(List.of("row-1", "row-2"), first, second);
		givenAccepted();

		relay.relayErrand(ERRAND_ID);

		assertThat(List.of(first, second)).extracting(ProcessEventOutboxEntity::getDeliveredAt).containsOnly(NOW_IN_MILLIS);
		verify(outboxRepositoryMock, never()).findByDeliveredAtIsNullAndCreatedBeforeOrderByCreatedAscIdAsc(any(), any());
		verify(outboxRepositoryMock, never()).findByProcessServiceAndDeliveredAtIsNullAndCreatedBefore(any(), any(), any());
	}

	@Test
	@DisplayName("Verification that a direct run that finds nothing, because another run got there first, does nothing")
	void aDirectRunWithNothingWaiting() {
		givenWaitingFor(ERRAND_ID);

		relay.relayErrand(ERRAND_ID);

		verify(outboxRepositoryMock, never()).findByIdInAndDeliveredAtIsNull(any());
		verifyNoInteractions(pwAlktIntegrationMock, transactionManagerMock);
	}

	@Test
	@DisplayName("Verification that a group goes to pw-alkt oldest first, whatever order the locking read returned it in, and is acknowledged in the same transaction")
	void aGroupIsDeliveredOldestFirstAndAcknowledged() {
		final var newest = row("row-3", ERRAND_ID, NOW.minusMinutes(1), "UPDATE");
		final var sameMomentLaterId = row("row-2", ERRAND_ID, NOW.minusMinutes(2), "UPDATE");
		final var oldest = row("row-1", ERRAND_ID, NOW.minusMinutes(2), "CREATE");
		givenWaitingFor(ERRAND_ID, oldest, sameMomentLaterId, newest);
		givenLocked(List.of("row-1", "row-2", "row-3"), newest, sameMomentLaterId, oldest);
		givenAccepted();

		relay.relayErrand(ERRAND_ID);

		verify(pwAlktIntegrationMock, times(3)).sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), eventCaptor.capture());
		assertThat(eventCaptor.getAllValues()).extracting(ErrandEvent::getEventId).containsExactly("row-1", "row-2", "row-3");
		assertThat(List.of(oldest, sameMomentLaterId, newest)).extracting(ProcessEventOutboxEntity::getDeliveredAt).containsOnly(NOW_IN_MILLIS);
		verify(transactionManagerMock).commit(any());
		verifyNoInteractions(errandsRepositoryMock, processRepositoryMock, activityRepositoryMock);
	}

	@Test
	@DisplayName("Verification that a row another run delivered while this one waited for the lock is not delivered again")
	void aRowAnotherRunDeliveredIsLeftAlone() {
		final var alreadyDelivered = row("row-1", ERRAND_ID, NOW.minusMinutes(2));
		final var stillWaiting = row("row-2", ERRAND_ID, NOW.minusMinutes(1));
		givenWaitingFor(ERRAND_ID, alreadyDelivered, stillWaiting);
		givenLocked(List.of("row-1", "row-2"), stillWaiting);
		givenAccepted();

		relay.relayErrand(ERRAND_ID);

		verify(pwAlktIntegrationMock).sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), eventCaptor.capture());
		assertThat(eventCaptor.getValue().getEventId()).isEqualTo("row-2");
	}

	@Test
	@DisplayName("Verification that a row that does not go through ends the group: the rows before it are acknowledged, it and the later ones stay, and the failure goes to the caller once the transaction is committed")
	void aRowThatDoesNotGoThroughEndsTheGroup() {
		final var first = row("row-1", ERRAND_ID, NOW.minusMinutes(3));
		final var second = row("row-2", ERRAND_ID, NOW.minusMinutes(2));
		final var third = row("row-3", ERRAND_ID, NOW.minusMinutes(1));
		final var failure = new PwAlktUnavailableException(false, new IllegalStateException("503"));
		givenWaitingFor(ERRAND_ID, first, second, third);
		givenLocked(List.of("row-1", "row-2", "row-3"), first, second, third);
		when(pwAlktIntegrationMock.sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(true).thenThrow(failure);

		assertThatThrownBy(() -> relay.relayErrand(ERRAND_ID)).isSameAs(failure);

		assertThat(first.getDeliveredAt()).isEqualTo(NOW_IN_MILLIS);
		assertThat(second.getDeliveredAt()).isNull();
		assertThat(third.getDeliveredAt()).isNull();
		verify(pwAlktIntegrationMock, times(2)).sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any());
		final var inOrder = inOrder(transactionManagerMock);
		inOrder.verify(transactionManagerMock).commit(any());
		verify(transactionManagerMock, never()).rollback(any());
		verifyNoInteractions(errandsRepositoryMock, processRepositoryMock, activityRepositoryMock);
	}

	@Test
	@DisplayName("Verification that a refusal for good consumes the row, fails the live instance and writes an error entry on it")
	void aRefusalForGoodFailsTheLiveInstance() {
		final var row = row("row-1", ERRAND_ID, NOW.minusMinutes(1));
		final var instance = liveInstance();
		givenRefused(row);
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(instance));

		relay.relayErrand(ERRAND_ID);

		assertThat(row.getDeliveredAt()).isEqualTo(NOW_IN_MILLIS);
		assertThat(instance.getProcessStatus()).isEqualTo(FAILED);
		assertThat(instance.getActiveMarker()).isNull();
		assertThat(instance.getErrorCode()).isEqualTo(REJECTION_ERROR_CODE);
		assertThat(instance.getErrorMessage()).startsWith("pw-alkt refused an event on this errand for good").contains("'" + PROCESS_KEY + "'");

		verify(activityRepositoryMock).save(activityCaptor.capture());
		assertThat(activityCaptor.getValue()).satisfies(entry -> {
			assertThat(entry.getErrandProcessId()).isEqualTo("process-1");
			assertThat(entry.getErrandId()).isEqualTo(ERRAND_ID);
			assertThat(entry.getActivityType()).isEqualTo(DELIVERY_ACTIVITY_TYPE);
			assertThat(entry.getSeverity()).isEqualTo(ERROR);
			assertThat(entry.getErrorCode()).isEqualTo(REJECTION_ERROR_CODE);
			assertThat(entry.getMessage()).isEqualTo(instance.getErrorMessage());
			assertThat(entry.getOccurredAt()).isEqualTo(NOW_IN_MILLIS);
		});
	}

	@Test
	@DisplayName("Verification that a refusal for good on an errand without a process writes the entry alone, without an instance")
	void aRefusalForGoodWithoutAProcess() {
		final var row = row("row-1", ERRAND_ID, NOW.minusMinutes(1));
		givenRefused(row);
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of());

		relay.relayErrand(ERRAND_ID);

		assertThat(row.getDeliveredAt()).isEqualTo(NOW_IN_MILLIS);
		verify(activityRepositoryMock).save(activityCaptor.capture());
		assertThat(activityCaptor.getValue().getErrandProcessId()).isNull();
		assertThat(activityCaptor.getValue().getMessage()).endsWith("Deploy the process under that key, or correct the processKey attribute of the label");
	}

	@Test
	@DisplayName("Verification that a refusal on an errand that has had a process advises deploying the key alone, since the key of its process rows is what every later event carries")
	void aRefusalOnAnErrandWithAProcessRowAdvisesDeployingTheKey() {
		final var row = row("row-1", ERRAND_ID, NOW.minusMinutes(1));
		final var failed = liveInstance();
		failed.applyStatus(FAILED, CLOCK);
		givenRefused(row);
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(failed));

		relay.relayErrand(ERRAND_ID);

		verify(activityRepositoryMock).save(activityCaptor.capture());
		assertThat(activityCaptor.getValue().getErrandProcessId()).as("a failed instance is not failed again").isNull();
		assertThat(activityCaptor.getValue().getMessage())
			.endsWith("correcting the label does not help: deploy the process under that key")
			.doesNotContain("correct the processKey attribute");
	}

	@Test
	@DisplayName("Verification that a refusal is recorded once every call in the group has been made, so that the lock on the errand is not held across calls")
	void aRefusalIsRecordedAfterTheCalls() {
		final var refused = row("row-1", ERRAND_ID, NOW.minusMinutes(2));
		final var accepted = row("row-2", ERRAND_ID, NOW.minusMinutes(1));
		givenWaitingFor(ERRAND_ID, refused, accepted);
		givenLocked(List.of("row-1", "row-2"), refused, accepted);
		when(pwAlktIntegrationMock.sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(false, true);
		when(errandsRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);

		relay.relayErrand(ERRAND_ID);

		final var inOrder = inOrder(pwAlktIntegrationMock, errandsRepositoryMock);
		inOrder.verify(pwAlktIntegrationMock, times(2)).sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any());
		inOrder.verify(errandsRepositoryMock).existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	@DisplayName("Verification that a refused deletion leaves nothing to write on, and consumes the row all the same")
	void aRefusedDeletionLeavesNothingToWriteOn() {
		final var row = row("row-1", ERRAND_ID, NOW.minusMinutes(1), "DELETE");
		givenWaitingFor(ERRAND_ID, row);
		givenLocked(List.of("row-1"), row);
		when(pwAlktIntegrationMock.sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(false);

		relay.relayErrand(ERRAND_ID);

		assertThat(row.getDeliveredAt()).isEqualTo(NOW_IN_MILLIS);
		verifyNoInteractions(errandsRepositoryMock, processRepositoryMock, activityRepositoryMock);
	}

	@Test
	@DisplayName("Verification that a refusal for an errand that is gone writes nothing, since the entry would have nothing to hang on")
	void aRefusalForAnErrandThatIsGoneWritesNothing() {
		final var row = row("row-1", ERRAND_ID, NOW.minusMinutes(1));
		givenWaitingFor(ERRAND_ID, row);
		givenLocked(List.of("row-1"), row);
		when(pwAlktIntegrationMock.sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(false);
		when(errandsRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(false);

		relay.relayErrand(ERRAND_ID);

		assertThat(row.getDeliveredAt()).isEqualTo(NOW_IN_MILLIS);
		verifyNoInteractions(processRepositoryMock, activityRepositoryMock);
	}

	@Test
	@DisplayName("Verification that the entry is written once per errand and window, while the instance is failed all the same")
	void theEntryIsWrittenOncePerErrandAndWindow() {
		final var row = row("row-1", ERRAND_ID, NOW.minusMinutes(1));
		final var instance = liveInstance();
		givenWaitingFor(ERRAND_ID, row);
		givenLocked(List.of("row-1"), row);
		when(pwAlktIntegrationMock.sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(false);
		when(errandsRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(instance));
		when(activityRepositoryMock.existsByErrandIdAndErrorCodeAndCreatedAfter(ERRAND_ID, REJECTION_ERROR_CODE, NOW_IN_MILLIS.minus(WINDOW))).thenReturn(true);

		relay.relayErrand(ERRAND_ID);

		assertThat(instance.getProcessStatus()).isEqualTo(FAILED);
		verify(activityRepositoryMock, never()).save(any());
	}

	@Test
	@DisplayName("Verification that rows that have aged out are locked by id, dropped undelivered and each logged as an error")
	void rowsThatHaveAgedOutAreDropped() {
		final var aged = row("row-1", ERRAND_ID, NOW.minusDays(31));
		final var agedToo = row("row-2", ERRAND_ID, NOW.minusDays(31).plusSeconds(1));
		givenAgedOut(aged, agedToo);
		givenLocked(List.of("row-1", "row-2"), aged, agedToo);
		givenWaiting();

		relay.relay();

		verify(outboxRepositoryMock).deleteAllByIdInBatch(List.of("row-1", "row-2"));
		assertThat(errorsLogged()).satisfiesExactly(
			message -> assertThat(message).contains("row-1", ERRAND_ID, "undelivered"),
			message -> assertThat(message).contains("row-2", ERRAND_ID, "undelivered"));
		verifyNoInteractions(pwAlktIntegrationMock);
	}

	@Test
	@DisplayName("Verification that with nothing aged out nothing is locked")
	void nothingAgedOut() {
		givenAgedOut();
		givenWaiting();

		relay.relay();

		verify(outboxRepositoryMock, never()).findByIdInAndDeliveredAtIsNull(any());
		verify(outboxRepositoryMock, never()).deleteAllByIdInBatch(any());
	}

	@Test
	@DisplayName("Verification that a row delivered after all, while it was about to be dropped, is left alone")
	void aRowDeliveredMeanwhileIsNotDropped() {
		final var aged = row("row-1", ERRAND_ID, NOW.minusDays(31));
		givenAgedOut(aged);
		givenLocked(List.of("row-1"));
		givenWaiting();

		relay.relay();

		verify(outboxRepositoryMock).deleteAllByIdInBatch(List.of());
		assertThat(errorsLogged()).isEmpty();
	}

	@Test
	@DisplayName("Verification that the relay is healthy with nothing waiting")
	void healthyWithNothingWaiting() {
		givenOldestUndelivered();

		assertThat(relay.findHealthFault()).isEmpty();
	}

	@Test
	@DisplayName("Verification that a row just written leaves the relay healthy, since there is always a row between a publication and the next run")
	void healthyRightAfterAPublication() {
		givenOldestUndelivered(NOW.minusSeconds(1));

		assertThat(relay.findHealthFault()).isEmpty();
	}

	@Test
	@DisplayName("Verification that a row exactly as old as the limit still leaves the relay healthy")
	void healthyAtTheLimit() {
		givenOldestUndelivered(NOW.minus(UNHEALTHY_AFTER));

		assertThat(relay.findHealthFault()).isEmpty();
	}

	@Test
	@DisplayName("Verification that the relay turns unhealthy once the oldest undelivered row has passed the limit")
	void unhealthyPastTheLimit() {
		givenOldestUndelivered(NOW.minus(UNHEALTHY_AFTER).minusNanos(1_000_000));

		assertThat(relay.findHealthFault()).hasValueSatisfying(fault -> assertThat(fault).startsWith("the oldest undelivered process event was written at"));
	}

	@Test
	@DisplayName("Verification that a row addressed to anything but pw-alkt turns the relay unhealthy at once, since no run will ever take it")
	void unhealthyForARowAddressedElsewhere() {
		when(outboxRepositoryMock.existsByDeliveredAtIsNullAndProcessServiceNot(PROCESS_SERVICE)).thenReturn(true);

		assertThat(relay.findHealthFault()).hasValue("undelivered process events are addressed to a process consumer other than 'pw-alkt', and nothing delivers them");
		verify(outboxRepositoryMock, never()).findFirstByDeliveredAtIsNullOrderByCreatedAsc();
	}

	private void givenWaiting(final ProcessEventOutboxEntity... rows) {
		when(outboxRepositoryMock.findByProcessServiceAndDeliveredAtIsNullAndCreatedBefore(PROCESS_SERVICE, NOW.minus(TRANSACTION_BUFFER), PageRequest.of(0, BATCH_SIZE, Sort.by("created", "id"))))
			.thenReturn(List.of(rows));
	}

	private void givenWaitingFor(final String errandId, final ProcessEventOutboxEntity... rows) {
		when(outboxRepositoryMock.findByProcessServiceAndErrandIdAndDeliveredAtIsNullAndCreatedAfterOrderByCreatedAscIdAsc(PROCESS_SERVICE, errandId, NOW.minus(MAX_AGE), PageRequest.of(0, BATCH_SIZE)))
			.thenReturn(List.of(rows));
	}

	private void givenLocked(final List<String> rowIds, final ProcessEventOutboxEntity... rows) {
		when(outboxRepositoryMock.findByIdInAndDeliveredAtIsNull(rowIds)).thenReturn(List.of(rows));
	}

	private void givenAgedOut(final ProcessEventOutboxEntity... rows) {
		when(outboxRepositoryMock.findByDeliveredAtIsNullAndCreatedBeforeOrderByCreatedAscIdAsc(NOW.minus(MAX_AGE), PageRequest.of(0, BATCH_SIZE))).thenReturn(List.of(rows));
	}

	private void givenAccepted() {
		when(pwAlktIntegrationMock.sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(true);
	}

	private void givenRefused(final ProcessEventOutboxEntity row) {
		givenWaitingFor(row.getErrandId(), row);
		givenLocked(List.of(row.getId()), row);
		when(pwAlktIntegrationMock.sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(false);
		when(errandsRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(activityRepositoryMock.existsByErrandIdAndErrorCodeAndCreatedAfter(ERRAND_ID, REJECTION_ERROR_CODE, NOW_IN_MILLIS.minus(WINDOW))).thenReturn(false);
	}

	private void givenOldestUndelivered(final OffsetDateTime... created) {
		when(outboxRepositoryMock.existsByDeliveredAtIsNullAndProcessServiceNot(PROCESS_SERVICE)).thenReturn(false);
		when(outboxRepositoryMock.findFirstByDeliveredAtIsNullOrderByCreatedAsc())
			.thenReturn(Stream.of(created).map(moment -> ProcessEventOutboxEntity.create().withCreated(moment)).findFirst());
	}

	private List<String> errorsLogged() {
		return logAppender.list.stream()
			.filter(event -> Level.ERROR.equals(event.getLevel()))
			.map(ILoggingEvent::getFormattedMessage)
			.toList();
	}

	private static ErrandProcessEntity liveInstance() {
		return createErrandProcessEntity(WAITING, CLOCK, process -> process
			.withId("process-1")
			.withErrandId(ERRAND_ID)
			.withProcessService(PROCESS_SERVICE)
			.withProcessKey(PROCESS_KEY)
			.withProcessInstanceId("pi-1"));
	}

	private static ProcessEventOutboxEntity agedRow(final String id) {
		return row(id, ERRAND_ID, NOW.minusDays(31));
	}

	private static ProcessEventOutboxEntity row(final String id, final String errandId, final OffsetDateTime created) {
		return row(id, errandId, created, "UPDATE");
	}

	private static ProcessEventOutboxEntity row(final String id, final String errandId, final OffsetDateTime created, final String eventType) {
		return ProcessEventOutboxEntity.create()
			.withId(id)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandId(errandId)
			.withProcessService(PROCESS_SERVICE)
			.withProcessKey(PROCESS_KEY)
			.withEventType(eventType)
			.withEventSubType("ERRAND")
			.withCreated(created);
	}

	private static Logger logger() {
		return (Logger) LoggerFactory.getLogger(ProcessEventRelay.class);
	}
}
