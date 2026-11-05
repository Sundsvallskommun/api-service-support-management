package se.sundsvall.supportmanagement.service.scheduler.supensions;

import generated.se.sundsvall.eventlog.EventType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.EventService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SUSPENSION;

@ExtendWith(MockitoExtension.class)
class SuspensionWorkerTest {

	private static final String ERRAND_ID = "id";
	private static final String MESSAGE = "Parkering av ärendet har upphört";
	private static final OffsetDateTime SUSPENDED_TO = OffsetDateTime.now().minusHours(1);

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private EventService eventServiceMock;

	@InjectMocks
	private SuspensionWorker suspensionWorker;

	private static ErrandEntity endedSuspension() {
		return ErrandEntity.create()
			.withId(ERRAND_ID)
			.withStatus("SUSPENDED")
			.withSuspendedTo(SUSPENDED_TO);
	}

	@Test
	void getErrandIdsWithEndedSuspension() {
		when(errandsRepositoryMock.findIdsWithUnannouncedSuspensionEndAndDeletedIsNull(eq("SUSPENDED"), any(OffsetDateTime.class))).thenReturn(List.of(ERRAND_ID));

		assertThat(suspensionWorker.getErrandIdsWithEndedSuspension()).containsExactly(ERRAND_ID);
	}

	@Test
	void announceSuspensionEnd() {
		final var errand = endedSuspension();
		when(errandsRepositoryMock.findWithLockingById(ERRAND_ID)).thenReturn(Optional.of(errand));

		suspensionWorker.announceSuspensionEnd(ERRAND_ID);

		final var inOrder = inOrder(eventServiceMock, errandsRepositoryMock);
		inOrder.verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq(MESSAGE), eq(errand), isNull(), isNull(), eq(SUSPENSION));
		inOrder.verify(errandsRepositoryMock).markSuspensionEndNotified(ERRAND_ID, SUSPENDED_TO);
	}

	@Test
	void announceSuspensionEndOfANewSuspension() {
		final var errand = endedSuspension().withSuspensionEndNotifiedFor(SUSPENDED_TO.minusDays(7));
		when(errandsRepositoryMock.findWithLockingById(ERRAND_ID)).thenReturn(Optional.of(errand));

		suspensionWorker.announceSuspensionEnd(ERRAND_ID);

		verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq(MESSAGE), eq(errand), isNull(), isNull(), eq(SUSPENSION));
		verify(errandsRepositoryMock).markSuspensionEndNotified(ERRAND_ID, SUSPENDED_TO);
	}

	@Test
	void announceSuspensionEndDoesNotMarkWhenTheEventFails() {
		final var errand = endedSuspension();
		when(errandsRepositoryMock.findWithLockingById(ERRAND_ID)).thenReturn(Optional.of(errand));
		doThrow(new IllegalStateException("failure")).when(eventServiceMock).createErrandEvent(any(), any(), any(), isNull(), isNull(), eq(SUSPENSION));

		// Left to the scheduler, and the errand is tried again on the next run
		assertThatThrownBy(() -> suspensionWorker.announceSuspensionEnd(ERRAND_ID)).isInstanceOf(IllegalStateException.class);

		verify(errandsRepositoryMock, never()).markSuspensionEndNotified(any(), any());
	}

	@Test
	void announceSuspensionEndForErrandThatIsGone() {
		when(errandsRepositoryMock.findWithLockingById(ERRAND_ID)).thenReturn(Optional.empty());

		suspensionWorker.announceSuspensionEnd(ERRAND_ID);

		verifyNoInteractions(eventServiceMock);
		verify(errandsRepositoryMock, never()).markSuspensionEndNotified(any(), any());
	}

	@ParameterizedTest
	@MethodSource("errandsChangedSinceTheIdWasRead")
	void announceSuspensionEndLeavesAnErrandNoLongerDue(final UnaryOperator<ErrandEntity> change) {
		when(errandsRepositoryMock.findWithLockingById(ERRAND_ID)).thenReturn(Optional.of(change.apply(endedSuspension())));

		suspensionWorker.announceSuspensionEnd(ERRAND_ID);

		verifyNoInteractions(eventServiceMock);
		verify(errandsRepositoryMock, never()).markSuspensionEndNotified(any(), any());
	}

	private static Stream<Arguments> errandsChangedSinceTheIdWasRead() {
		return Stream.of(
			Arguments.of((UnaryOperator<ErrandEntity>) errand -> errand.withStatus("OPEN")),
			Arguments.of((UnaryOperator<ErrandEntity>) errand -> errand.withDeleted(OffsetDateTime.now())),
			Arguments.of((UnaryOperator<ErrandEntity>) errand -> errand.withSuspendedTo(null)),
			Arguments.of((UnaryOperator<ErrandEntity>) errand -> errand.withSuspendedTo(OffsetDateTime.now().plusDays(1))),
			Arguments.of((UnaryOperator<ErrandEntity>) errand -> errand.withSuspensionEndNotifiedFor(SUSPENDED_TO)));
	}
}
