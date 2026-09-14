package se.sundsvall.supportmanagement.service.scheduler.processevent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.integration.pwalkt.PwAlktUnavailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessEventRelayTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-14T08:00:00Z"), ZoneId.of("UTC"));
	private static final OffsetDateTime NOW = OffsetDateTime.now(CLOCK);
	private static final String PROCESS_SERVICE = "pw-alkt";
	private static final int BATCH_SIZE = 3;
	private static final Duration TRANSACTION_BUFFER = Duration.ofSeconds(5);
	private static final Duration MAX_AGE = Duration.ofDays(30);
	private static final Duration UNHEALTHY_AFTER = Duration.ofMinutes(15);

	@Mock
	private ProcessEventOutboxRepository outboxRepositoryMock;

	@Mock
	private ProcessEventDelivery deliveryMock;

	private ProcessEventRelay relay;

	@BeforeEach
	void setUp() {
		relay = new ProcessEventRelay(outboxRepositoryMock, deliveryMock, CLOCK);

		ReflectionTestUtils.setField(relay, "batchSize", BATCH_SIZE);
		ReflectionTestUtils.setField(relay, "transactionBuffer", TRANSACTION_BUFFER);
		ReflectionTestUtils.setField(relay, "maxAge", MAX_AGE);
		ReflectionTestUtils.setField(relay, "unhealthyAfter", UNHEALTHY_AFTER);
	}

	@Test
	@DisplayName("Verification that a run drops what has aged out first, then takes a batch of the oldest rows and delivers them errand by errand")
	void aRunDeliversErrandByErrandOldestFirst() {
		givenWaiting(row("row-1", "errand-a"), row("row-2", "errand-b"), row("row-3", "errand-a"));

		relay.relay();

		final var inOrder = inOrder(outboxRepositoryMock, deliveryMock);
		inOrder.verify(deliveryMock).dropAgedOut(NOW.minus(MAX_AGE), BATCH_SIZE);
		inOrder.verify(outboxRepositoryMock).findByProcessServiceAndDeliveredAtIsNullAndCreatedBefore(PROCESS_SERVICE, NOW.minus(TRANSACTION_BUFFER), PageRequest.of(0, BATCH_SIZE, Sort.by("created", "id")));
		inOrder.verify(deliveryMock).deliverGroup(List.of("row-1", "row-3"));
		inOrder.verify(deliveryMock).deliverGroup(List.of("row-2"));
		verifyNoMoreInteractions(outboxRepositoryMock, deliveryMock);
	}

	@Test
	@DisplayName("Verification that rows that have aged out are dropped a batch at a time until a batch comes back short")
	void aRunDropsWhatHasAgedOutBatchByBatch() {
		when(deliveryMock.dropAgedOut(NOW.minus(MAX_AGE), BATCH_SIZE)).thenReturn(BATCH_SIZE, BATCH_SIZE, 1);
		givenWaiting();

		relay.relay();

		verify(deliveryMock, times(3)).dropAgedOut(NOW.minus(MAX_AGE), BATCH_SIZE);
		verify(deliveryMock, never()).deliverGroup(any());
	}

	@Test
	@DisplayName("Verification that an errand whose delivery fails holds back no one but itself, whether pw-alkt refused it or something unforeseen went wrong")
	void anErrandThatFailsHoldsBackNoOneButItself() {
		givenWaiting(row("row-1", "errand-a"), row("row-2", "errand-b"), row("row-3", "errand-c"));
		doThrow(new PwAlktUnavailableException(false, new IllegalStateException("503"))).when(deliveryMock).deliverGroup(List.of("row-1"));
		doThrow(new IllegalStateException("something unforeseen")).when(deliveryMock).deliverGroup(List.of("row-2"));

		assertThatNoException().isThrownBy(relay::relay);

		verify(deliveryMock).deliverGroup(List.of("row-3"));
	}

	@Test
	@DisplayName("Verification that an open circuit breaker ends the run, since every errand after it would meet the same answer")
	void anOpenCircuitBreakerEndsTheRun() {
		givenWaiting(row("row-1", "errand-a"), row("row-2", "errand-b"));
		doThrow(new PwAlktUnavailableException(true, new IllegalStateException("open"))).when(deliveryMock).deliverGroup(List.of("row-1"));

		relay.relay();

		verify(deliveryMock, never()).deliverGroup(List.of("row-2"));
	}

	@Test
	@DisplayName("Verification that a direct run delivers the rows of its errand, held to the age limit but not to the transaction buffer")
	void aDirectRunDeliversTheRowsOfItsErrand() {
		givenWaitingFor("errand-a", row("row-1", "errand-a"), row("row-2", "errand-a"));

		relay.relayErrand("errand-a");

		verify(deliveryMock).deliverGroup(List.of("row-1", "row-2"));
		verify(deliveryMock, never()).dropAgedOut(any(), anyInt());
	}

	@Test
	@DisplayName("Verification that a direct run that finds nothing, because another run got there first, does nothing")
	void aDirectRunWithNothingWaiting() {
		givenWaitingFor("errand-a");

		relay.relayErrand("errand-a");

		verifyNoInteractions(deliveryMock);
	}

	@Test
	@DisplayName("Verification that a direct run leaves a failed delivery to its caller")
	void aDirectRunLeavesAFailureToItsCaller() {
		givenWaitingFor("errand-a", row("row-1", "errand-a"));
		final var failure = new PwAlktUnavailableException(false, new IllegalStateException("503"));
		doThrow(failure).when(deliveryMock).deliverGroup(List.of("row-1"));

		assertThatThrownBy(() -> relay.relayErrand("errand-a")).isSameAs(failure);
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
		verify(outboxRepositoryMock, never()).findByDeliveredAtIsNullOrderByCreatedAsc(any());
	}

	private void givenWaiting(final ProcessEventOutboxEntity... rows) {
		when(outboxRepositoryMock.findByProcessServiceAndDeliveredAtIsNullAndCreatedBefore(PROCESS_SERVICE, NOW.minus(TRANSACTION_BUFFER), PageRequest.of(0, BATCH_SIZE, Sort.by("created", "id"))))
			.thenReturn(List.of(rows));
	}

	private void givenWaitingFor(final String errandId, final ProcessEventOutboxEntity... rows) {
		when(outboxRepositoryMock.findByProcessServiceAndErrandIdAndDeliveredAtIsNullAndCreatedAfterOrderByCreatedAscIdAsc(PROCESS_SERVICE, errandId, NOW.minus(MAX_AGE), PageRequest.of(0, BATCH_SIZE)))
			.thenReturn(List.of(rows));
	}

	private void givenOldestUndelivered(final OffsetDateTime... created) {
		when(outboxRepositoryMock.existsByDeliveredAtIsNullAndProcessServiceNot(PROCESS_SERVICE)).thenReturn(false);
		when(outboxRepositoryMock.findByDeliveredAtIsNullOrderByCreatedAsc(PageRequest.of(0, 1)))
			.thenReturn(Stream.of(created).map(moment -> ProcessEventOutboxEntity.create().withCreated(moment)).toList());
	}

	private static ProcessEventOutboxEntity row(final String id, final String errandId) {
		return ProcessEventOutboxEntity.create().withId(id).withErrandId(errandId);
	}
}
