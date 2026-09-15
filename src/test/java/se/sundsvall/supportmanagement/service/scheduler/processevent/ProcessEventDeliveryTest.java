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
import java.util.Optional;
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
import se.sundsvall.supportmanagement.config.ProcessEngineProperties;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties.DirectRun;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties.LoopGuard;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessActivityRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.integration.pwalkt.PwAlktIntegration;
import se.sundsvall.supportmanagement.integration.pwalkt.PwAlktUnavailableException;
import se.sundsvall.supportmanagement.service.ProcessErrorLog;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.ERROR;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.WAITING;
import static se.sundsvall.supportmanagement.service.scheduler.processevent.ProcessEventDelivery.REJECTION_ACTIVITY_TYPE;
import static se.sundsvall.supportmanagement.service.scheduler.processevent.ProcessEventDelivery.REJECTION_ERROR_CODE;

@ExtendWith(MockitoExtension.class)
class ProcessEventDeliveryTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-14T08:00:00.123456Z"), ZoneId.of("UTC"));
	private static final OffsetDateTime NOW = OffsetDateTime.now(CLOCK).truncatedTo(MILLIS);
	private static final Duration WINDOW = Duration.ofMinutes(10);
	private static final String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "ALKT";
	private static final String PROCESS_KEY = "alkt-ansokan";

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

	@Captor
	private ArgumentCaptor<ErrandEvent> eventCaptor;

	@Captor
	private ArgumentCaptor<ErrandProcessActivityEntity> activityCaptor;

	private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

	private ProcessEventDelivery delivery;

	@BeforeEach
	void setUp() {
		final var properties = new ProcessEngineProperties(new LoopGuard(20, WINDOW), new DirectRun(true, 2, 4, 500));
		delivery = new ProcessEventDelivery(outboxRepositoryMock, errandsRepositoryMock, processRepositoryMock, pwAlktIntegrationMock, new ProcessErrorLog(activityRepositoryMock, properties, CLOCK), CLOCK);

		logAppender.start();
		logger().addAppender(logAppender);
	}

	@AfterEach
	void tearDown() {
		logger().detachAppender(logAppender);
	}

	@Test
	@DisplayName("Verification that a group goes to pw-alkt oldest first, whatever order the locking read returned it in, and is acknowledged in the same transaction")
	void aGroupIsDeliveredOldestFirstAndAcknowledged() {
		final var newest = row("row-3", NOW.minusMinutes(1), "UPDATE");
		final var sameMomentLaterId = row("row-2", NOW.minusMinutes(2), "UPDATE");
		final var oldest = row("row-1", NOW.minusMinutes(2), "CREATE");
		when(outboxRepositoryMock.findByIdInAndDeliveredAtIsNull(List.of("row-1", "row-2", "row-3"))).thenReturn(List.of(newest, sameMomentLaterId, oldest));
		when(pwAlktIntegrationMock.sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(true);

		delivery.deliverGroup(List.of("row-1", "row-2", "row-3"));

		verify(pwAlktIntegrationMock, times(3)).sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), eventCaptor.capture());
		assertThat(eventCaptor.getAllValues()).extracting(ErrandEvent::getEventId).containsExactly("row-1", "row-2", "row-3");
		assertThat(List.of(oldest, sameMomentLaterId, newest)).extracting(ProcessEventOutboxEntity::getDeliveredAt).containsOnly(NOW);
		verifyNoInteractions(errandsRepositoryMock, processRepositoryMock, activityRepositoryMock);
	}

	@Test
	@DisplayName("Verification that a row another run delivered while this one waited for the lock is not delivered again")
	void aRowAnotherRunDeliveredIsLeftAlone() {
		final var stillWaiting = row("row-2", NOW.minusMinutes(1), "UPDATE");
		when(outboxRepositoryMock.findByIdInAndDeliveredAtIsNull(List.of("row-1", "row-2"))).thenReturn(List.of(stillWaiting));
		when(pwAlktIntegrationMock.sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(true);

		delivery.deliverGroup(List.of("row-1", "row-2"));

		verify(pwAlktIntegrationMock).sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), eventCaptor.capture());
		assertThat(eventCaptor.getValue().getEventId()).isEqualTo("row-2");
	}

	@Test
	@DisplayName("Verification that a row that does not go through takes the group down, leaving the rest of it unacknowledged")
	void aRowThatDoesNotGoThroughTakesTheGroupDown() {
		final var first = row("row-1", NOW.minusMinutes(2), "UPDATE");
		final var second = row("row-2", NOW.minusMinutes(1), "UPDATE");
		final var failure = new PwAlktUnavailableException(false, new IllegalStateException("503"));
		when(outboxRepositoryMock.findByIdInAndDeliveredAtIsNull(List.of("row-1", "row-2"))).thenReturn(List.of(first, second));
		when(pwAlktIntegrationMock.sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(true).thenThrow(failure);

		assertThatThrownBy(() -> delivery.deliverGroup(List.of("row-1", "row-2"))).isSameAs(failure);

		assertThat(second.getDeliveredAt()).isNull();
		verifyNoInteractions(errandsRepositoryMock, processRepositoryMock, activityRepositoryMock);
	}

	@Test
	@DisplayName("Verification that a refusal for good consumes the row, fails the live instance and writes an error entry on it")
	void aRefusalForGoodFailsTheLiveInstance() {
		final var row = row("row-1", NOW.minusMinutes(1), "UPDATE");
		final var instance = liveInstance();
		givenRefused(row);
		when(processRepositoryMock.findByErrandIdAndActiveMarkerIsNotNull(ERRAND_ID)).thenReturn(Optional.of(instance));

		delivery.deliverGroup(List.of("row-1"));

		assertThat(row.getDeliveredAt()).isEqualTo(NOW);
		assertThat(instance.getProcessStatus()).isEqualTo(FAILED);
		assertThat(instance.getActiveMarker()).isNull();
		assertThat(instance.getErrorCode()).isEqualTo(REJECTION_ERROR_CODE);
		assertThat(instance.getErrorMessage()).startsWith("pw-alkt refused an event on this errand for good").contains("'" + PROCESS_KEY + "'");

		verify(activityRepositoryMock).save(activityCaptor.capture());
		assertThat(activityCaptor.getValue()).satisfies(entry -> {
			assertThat(entry.getErrandProcessId()).isEqualTo("process-1");
			assertThat(entry.getErrandId()).isEqualTo(ERRAND_ID);
			assertThat(entry.getActivityType()).isEqualTo(REJECTION_ACTIVITY_TYPE);
			assertThat(entry.getSeverity()).isEqualTo(ERROR);
			assertThat(entry.getErrorCode()).isEqualTo(REJECTION_ERROR_CODE);
			assertThat(entry.getMessage()).isEqualTo(instance.getErrorMessage());
			assertThat(entry.getOccurredAt()).isEqualTo(NOW);
		});
	}

	@Test
	@DisplayName("Verification that a refusal for good on an errand without a process writes the entry alone, without an instance")
	void aRefusalForGoodWithoutAProcess() {
		final var row = row("row-1", NOW.minusMinutes(1), "UPDATE");
		givenRefused(row);
		when(processRepositoryMock.findByErrandIdAndActiveMarkerIsNotNull(ERRAND_ID)).thenReturn(Optional.empty());

		delivery.deliverGroup(List.of("row-1"));

		assertThat(row.getDeliveredAt()).isEqualTo(NOW);
		verify(activityRepositoryMock).save(activityCaptor.capture());
		assertThat(activityCaptor.getValue().getErrandProcessId()).isNull();
	}

	@Test
	@DisplayName("Verification that a refusal is recorded once every call in the group has been made, so that the lock on the errand is not held across calls")
	void aRefusalIsRecordedAfterTheCalls() {
		final var refused = row("row-1", NOW.minusMinutes(2), "UPDATE");
		final var accepted = row("row-2", NOW.minusMinutes(1), "UPDATE");
		when(outboxRepositoryMock.findByIdInAndDeliveredAtIsNull(List.of("row-1", "row-2"))).thenReturn(List.of(refused, accepted));
		when(pwAlktIntegrationMock.sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(false, true);
		when(errandsRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);

		delivery.deliverGroup(List.of("row-1", "row-2"));

		final var inOrder = inOrder(pwAlktIntegrationMock, errandsRepositoryMock);
		inOrder.verify(pwAlktIntegrationMock, times(2)).sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any());
		inOrder.verify(errandsRepositoryMock).existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	@DisplayName("Verification that a refused deletion leaves nothing to write on, and consumes the row all the same")
	void aRefusedDeletionLeavesNothingToWriteOn() {
		final var row = row("row-1", NOW.minusMinutes(1), "DELETE");
		when(outboxRepositoryMock.findByIdInAndDeliveredAtIsNull(List.of("row-1"))).thenReturn(List.of(row));
		when(pwAlktIntegrationMock.sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(false);

		delivery.deliverGroup(List.of("row-1"));

		assertThat(row.getDeliveredAt()).isEqualTo(NOW);
		verifyNoInteractions(errandsRepositoryMock, processRepositoryMock, activityRepositoryMock);
	}

	@Test
	@DisplayName("Verification that a refusal for an errand that is gone writes nothing, since the entry would have nothing to hang on")
	void aRefusalForAnErrandThatIsGoneWritesNothing() {
		final var row = row("row-1", NOW.minusMinutes(1), "UPDATE");
		when(outboxRepositoryMock.findByIdInAndDeliveredAtIsNull(List.of("row-1"))).thenReturn(List.of(row));
		when(pwAlktIntegrationMock.sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(false);
		when(errandsRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(false);

		delivery.deliverGroup(List.of("row-1"));

		assertThat(row.getDeliveredAt()).isEqualTo(NOW);
		verifyNoInteractions(processRepositoryMock, activityRepositoryMock);
	}

	@Test
	@DisplayName("Verification that the entry is written once per errand and window, while the instance is failed all the same")
	void theEntryIsWrittenOncePerErrandAndWindow() {
		final var row = row("row-1", NOW.minusMinutes(1), "UPDATE");
		final var instance = liveInstance();
		when(outboxRepositoryMock.findByIdInAndDeliveredAtIsNull(List.of("row-1"))).thenReturn(List.of(row));
		when(pwAlktIntegrationMock.sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(false);
		when(errandsRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(processRepositoryMock.findByErrandIdAndActiveMarkerIsNotNull(ERRAND_ID)).thenReturn(Optional.of(instance));
		when(activityRepositoryMock.existsByErrandIdAndErrorCodeAndCreatedAfter(ERRAND_ID, REJECTION_ERROR_CODE, NOW.minus(WINDOW))).thenReturn(true);

		delivery.deliverGroup(List.of("row-1"));

		assertThat(instance.getProcessStatus()).isEqualTo(FAILED);
		verify(activityRepositoryMock, never()).save(any());
	}

	@Test
	@DisplayName("Verification that rows that have aged out are locked by id, dropped undelivered and each logged as an error")
	void rowsThatHaveAgedOutAreDropped() {
		final var createdBefore = NOW.minusDays(30);
		final var aged = row("row-1", NOW.minusDays(31), "UPDATE");
		final var agedToo = row("row-2", NOW.minusDays(31).plusSeconds(1), "UPDATE");
		when(outboxRepositoryMock.findByDeliveredAtIsNullAndCreatedBeforeOrderByCreatedAscIdAsc(createdBefore, PageRequest.of(0, 5))).thenReturn(List.of(aged, agedToo));
		when(outboxRepositoryMock.findByIdInAndDeliveredAtIsNull(List.of("row-1", "row-2"))).thenReturn(List.of(aged, agedToo));

		assertThat(delivery.dropAgedOut(createdBefore, 5)).isEqualTo(2);

		verify(outboxRepositoryMock).deleteAllByIdInBatch(List.of("row-1", "row-2"));
		assertThat(errorsLogged()).satisfiesExactly(
			message -> assertThat(message).contains("row-1", ERRAND_ID, "undelivered"),
			message -> assertThat(message).contains("row-2", ERRAND_ID, "undelivered"));
		verifyNoInteractions(pwAlktIntegrationMock);
	}

	@Test
	@DisplayName("Verification that with nothing aged out nothing is locked")
	void nothingAgedOut() {
		final var createdBefore = NOW.minusDays(30);
		when(outboxRepositoryMock.findByDeliveredAtIsNullAndCreatedBeforeOrderByCreatedAscIdAsc(createdBefore, PageRequest.of(0, 5))).thenReturn(List.of());

		assertThat(delivery.dropAgedOut(createdBefore, 5)).isZero();

		verify(outboxRepositoryMock, never()).findByIdInAndDeliveredAtIsNull(any());
		verify(outboxRepositoryMock, never()).deleteAllByIdInBatch(any());
	}

	@Test
	@DisplayName("Verification that a row delivered after all, while it was about to be dropped, is left alone")
	void aRowDeliveredMeanwhileIsNotDropped() {
		final var createdBefore = NOW.minusDays(30);
		final var aged = row("row-1", NOW.minusDays(31), "UPDATE");
		when(outboxRepositoryMock.findByDeliveredAtIsNullAndCreatedBeforeOrderByCreatedAscIdAsc(createdBefore, PageRequest.of(0, 5))).thenReturn(List.of(aged));
		when(outboxRepositoryMock.findByIdInAndDeliveredAtIsNull(List.of("row-1"))).thenReturn(List.of());

		assertThat(delivery.dropAgedOut(createdBefore, 5)).isZero();

		verify(outboxRepositoryMock).deleteAllByIdInBatch(List.of());
		assertThat(errorsLogged()).isEmpty();
	}

	private void givenRefused(final ProcessEventOutboxEntity row) {
		when(outboxRepositoryMock.findByIdInAndDeliveredAtIsNull(List.of(row.getId()))).thenReturn(List.of(row));
		when(pwAlktIntegrationMock.sendErrandEvent(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(false);
		when(errandsRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(activityRepositoryMock.existsByErrandIdAndErrorCodeAndCreatedAfter(ERRAND_ID, REJECTION_ERROR_CODE, NOW.minus(WINDOW))).thenReturn(false);
	}

	private List<String> errorsLogged() {
		return logAppender.list.stream()
			.filter(event -> Level.ERROR.equals(event.getLevel()))
			.map(ILoggingEvent::getFormattedMessage)
			.toList();
	}

	private static ErrandProcessEntity liveInstance() {
		final var instance = ErrandProcessEntity.create()
			.withId("process-1")
			.withErrandId(ERRAND_ID)
			.withProcessService("pw-alkt")
			.withProcessKey(PROCESS_KEY)
			.withProcessInstanceId("pi-1");
		instance.applyStatus(WAITING, CLOCK);

		return instance;
	}

	private static ProcessEventOutboxEntity row(final String id, final OffsetDateTime created, final String eventType) {
		return ProcessEventOutboxEntity.create()
			.withId(id)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandId(ERRAND_ID)
			.withProcessService("pw-alkt")
			.withProcessKey(PROCESS_KEY)
			.withEventType(eventType)
			.withEventSubType("ERRAND")
			.withCreated(created);
	}

	private static Logger logger() {
		return (Logger) LoggerFactory.getLogger(ProcessEventDelivery.class);
	}
}
