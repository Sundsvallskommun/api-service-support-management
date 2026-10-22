package se.sundsvall.supportmanagement.apptest;

import java.time.OffsetDateTime;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trips the emergency brake of the loop guard for an errand, for the integration tests that write past a tripped brake.
 */
final class EmergencyBrake {

	private EmergencyBrake() {}

	/**
	 * Writes as many rows delivered to pw-alkt within the window as the configured limit allows for the errand, and
	 * verifies that the count the brake asks for reaches that limit.
	 *
	 * @param outboxRepository the outbox the rows are written to.
	 * @param properties       the settings the limit and the window are read from.
	 * @param municipalityId   the municipality of the errand.
	 * @param namespace        the namespace of the errand.
	 * @param errandId         the errand whose brake is tripped.
	 * @param processKey       the process key the rows carry.
	 */
	static void trip(final ProcessEventOutboxRepository outboxRepository, final ProcessEngineProperties properties, final String municipalityId, final String namespace, final String errandId,
		final String processKey) {
		final var guard = properties.loopGuard();

		for (var i = 0; i < guard.maxEventsPerErrand(); i++) {
			outboxRepository.save(ProcessEventOutboxEntity.create()
				.withMunicipalityId(municipalityId)
				.withNamespace(namespace)
				.withErrandId(errandId)
				.withProcessService("pw-alkt")
				.withProcessKey(processKey)
				.withEventType("UPDATE")
				.withEventSubType("ERRAND")
				.withDeliveredAt(OffsetDateTime.now()));
		}

		assertThat(outboxRepository.countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter(errandId, OffsetDateTime.now().minus(guard.window())))
			.isGreaterThanOrEqualTo(guard.maxEventsPerErrand());
	}
}
