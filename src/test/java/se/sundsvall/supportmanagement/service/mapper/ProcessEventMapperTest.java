package se.sundsvall.supportmanagement.service.mapper;

import generated.se.sundsvall.pwalkt.ErrandEvent.EventTypeEnum;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static se.sundsvall.supportmanagement.service.mapper.ProcessEventMapper.toErrandEvent;

class ProcessEventMapperTest {

	private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-09-14T10:15:30.123+02:00");

	@Test
	@DisplayName("Verification that the event is the row as it stands, the start permission and the signal name included")
	void everythingIsCopiedFromTheRow() {
		final var row = ProcessEventOutboxEntity.create()
			.withId("3f2b91c4-7d5e-4a10-9c33-8e6b2f0a1d77")
			.withMunicipalityId("2281")
			.withNamespace("ALKT")
			.withErrandId("f0882f1d-06bc-47fd-b017-1d8307f5ce95")
			.withProcessService("pw-alkt")
			.withProcessKey("alkt-ansokan")
			.withEventType("UPDATE")
			.withEventSubType("SIGNAL")
			.withStartAllowed(true)
			.withSignalName("granskning-godkand")
			.withExecutedBy("joe01doe")
			.withCreated(CREATED);

		assertThat(toErrandEvent(row)).satisfies(event -> {
			assertThat(event.getEventId()).isEqualTo("3f2b91c4-7d5e-4a10-9c33-8e6b2f0a1d77");
			assertThat(event.getEventType()).isEqualTo(EventTypeEnum.UPDATE);
			assertThat(event.getEventSubType()).isEqualTo("SIGNAL");
			assertThat(event.getErrandId()).isEqualTo("f0882f1d-06bc-47fd-b017-1d8307f5ce95");
			assertThat(event.getProcessKey()).isEqualTo("alkt-ansokan");
			assertThat(event.getStartAllowed()).isTrue();
			assertThat(event.getSignalName()).isEqualTo("granskning-godkand");
			assertThat(event.getOccurredAt()).isEqualTo(CREATED);
		});
	}

	@Test
	@DisplayName("Verification that a deletion without a key goes out without one, and that a refused start permission is a false rather than an absence")
	void aDeletionWithoutAKey() {
		final var row = ProcessEventOutboxEntity.create()
			.withId("row-1")
			.withErrandId("errand-1")
			.withEventType("DELETE")
			.withEventSubType("ERRAND")
			.withStartAllowed(false)
			.withCreated(CREATED);

		assertThat(toErrandEvent(row)).satisfies(event -> {
			assertThat(event.getEventType()).isEqualTo(EventTypeEnum.DELETE);
			assertThat(event.getProcessKey()).isNull();
			assertThat(event.getSignalName()).isNull();
			assertThat(event.getStartAllowed()).isFalse();
		});
	}

	@ParameterizedTest
	@EnumSource(EventTypeEnum.class)
	@DisplayName("Verification that every event type pw-alkt knows is carried over by name")
	void everyEventTypeIsCarriedOver(final EventTypeEnum eventType) {
		final var row = ProcessEventOutboxEntity.create().withEventType(eventType.getValue());

		assertThat(toErrandEvent(row).getEventType()).isEqualTo(eventType);
	}

	@Test
	@DisplayName("Verification that an event type pw-alkt does not know is refused rather than sent as something else")
	void anUnknownEventTypeIsRefused() {
		final var row = ProcessEventOutboxEntity.create().withEventType("EXECUTE");

		assertThatIllegalArgumentException().isThrownBy(() -> toErrandEvent(row));
	}
}
