package se.sundsvall.supportmanagement.integration.db;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;

import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Tests of {@link ProcessEventOutboxRepository} against a real database: which rows a delivery run is allowed to see,
 * and what the emergency brake counts.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql",
	"/db/scripts/testdata-junit-process.sql"
})
class ProcessEventOutboxRepositoryTest {

	@Autowired
	private ProcessEventOutboxRepository processEventOutboxRepository;

	/**
	 * Reads a moment out of the test data the way the entities do: as a wall clock in the default zone of the JVM.
	 */
	private static OffsetDateTime at(final String wallClock) {
		return LocalDateTime.parse(wallClock).atZone(systemDefault()).toOffsetDateTime();
	}

	@Test
	@DisplayName("Verification that a run takes the rows of its own consumer, leaves the delivered ones alone and skips a row too young to be settled")
	void findByProcessServiceAndDeliveredAtIsNullAndCreatedBefore() {
		assertThat(processEventOutboxRepository.findByProcessServiceAndDeliveredAtIsNullAndCreatedBefore("pw-alkt", at("2026-01-01T11:55:00"), PageRequest.of(0, 100)))
			.extracting(ProcessEventOutboxEntity::getId)
			.containsExactly("peo-waiting");
	}

	@Test
	@DisplayName("Verification that the batch limit holds, so one engine being down cannot spend the whole run")
	void findByProcessServiceHonoursTheBatchLimit() {
		assertThat(processEventOutboxRepository.findByProcessServiceAndDeliveredAtIsNullAndCreatedBefore("pw-alkt", at("2026-01-01T12:00:00"), PageRequest.of(0, 100)))
			.hasSize(2);

		assertThat(processEventOutboxRepository.findByProcessServiceAndDeliveredAtIsNullAndCreatedBefore("pw-alkt", at("2026-01-01T12:00:00"), PageRequest.of(0, 1)))
			.hasSize(1);
	}

	@Test
	@DisplayName("Verification that a direct run takes the undelivered rows of its own errand, oldest first, and leaves those that have aged out")
	void findByProcessServiceAndErrandIdAndDeliveredAtIsNullAndCreatedAfter() {
		assertThat(processEventOutboxRepository.findByProcessServiceAndErrandIdAndDeliveredAtIsNullAndCreatedAfterOrderByCreatedAscIdAsc("pw-alkt", "ERRAND_ID-1", at("2026-01-01T11:00:00"), PageRequest.of(0, 100)))
			.extracting(ProcessEventOutboxEntity::getId)
			.containsExactly("peo-waiting", "peo-just-written");

		assertThat(processEventOutboxRepository.findByProcessServiceAndErrandIdAndDeliveredAtIsNullAndCreatedAfterOrderByCreatedAscIdAsc("pw-alkt", "ERRAND_ID-1", at("2026-01-01T11:55:00"), PageRequest.of(0, 100)))
			.extracting(ProcessEventOutboxEntity::getId)
			.containsExactly("peo-just-written");
	}

	@Test
	@DisplayName("Verification that the locking read keeps only the rows still undelivered, whatever ids it is handed")
	void findByIdInAndDeliveredAtIsNull() {
		assertThat(processEventOutboxRepository.findByIdInAndDeliveredAtIsNull(List.of("peo-waiting", "peo-delivered-in-window", "peo-other-consumer", "no-such-row")))
			.extracting(ProcessEventOutboxEntity::getId)
			.containsExactlyInAnyOrder("peo-waiting", "peo-other-consumer");
	}

	@Test
	@DisplayName("Verification that the rows that have aged out are found oldest first, whoever they are addressed to, and never a delivered one")
	void findByDeliveredAtIsNullAndCreatedBefore() {
		assertThat(processEventOutboxRepository.findByDeliveredAtIsNullAndCreatedBeforeOrderByCreatedAscIdAsc(at("2026-01-01T11:55:00"), PageRequest.of(0, 100)))
			.extracting(ProcessEventOutboxEntity::getId)
			.containsExactly("peo-other-consumer", "peo-waiting");

		assertThat(processEventOutboxRepository.findByDeliveredAtIsNullAndCreatedBeforeOrderByCreatedAscIdAsc(at("2026-01-01T11:55:00"), PageRequest.of(0, 1)))
			.extracting(ProcessEventOutboxEntity::getId)
			.containsExactly("peo-other-consumer");
	}

	@Test
	@DisplayName("Verification that the cleanup finds rows by when they were delivered, and never an undelivered one however old it is")
	void findByDeliveredAtBefore() {
		assertThat(processEventOutboxRepository.findByDeliveredAtBefore(at("2026-01-01T11:00:00"), PageRequest.of(0, 100)))
			.extracting(ProcessEventOutboxEntity::getId)
			.containsExactly("peo-delivered-long-ago");

		assertThat(processEventOutboxRepository.findByDeliveredAtBefore(at("2026-01-02T00:00:00"), PageRequest.of(0, 100)))
			.extracting(ProcessEventOutboxEntity::getId)
			.containsExactlyInAnyOrder("peo-delivered-long-ago", "peo-delivered-in-window");
	}

	@Test
	@DisplayName("Verification that an undelivered row addressed to another process consumer is noticed")
	void existsByDeliveredAtIsNullAndProcessServiceNot() {
		assertThat(processEventOutboxRepository.existsByDeliveredAtIsNullAndProcessServiceNot("pw-alkt")).isTrue();

		processEventOutboxRepository.deleteById("peo-other-consumer");

		assertThat(processEventOutboxRepository.existsByDeliveredAtIsNullAndProcessServiceNot("pw-alkt")).isFalse();
	}

	@Test
	@DisplayName("Verification that the emergency brake counts what reached the process engine, not what is queued for it")
	void countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter() {
		assertThat(processEventOutboxRepository.countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter("ERRAND_ID-1", at("2026-01-01T11:50:00"))).isOne();
	}

	@Test
	@DisplayName("Verification that a delivery outage cannot trip the brake by itself, however many rows pile up undelivered")
	void countLeavesUndeliveredRowsOutOfTheWindow() {
		assertThat(processEventOutboxRepository.countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter("ERRAND_ID-1", at("2025-01-01T00:00:00"))).isEqualTo(2);
	}

	@Test
	@DisplayName("Verification that health is read from the oldest undelivered row, which is what makes an age and not a count the condition")
	void findFirstByDeliveredAtIsNullOrderByCreatedAsc() {
		assertThat(processEventOutboxRepository.findFirstByDeliveredAtIsNullOrderByCreatedAsc())
			.get()
			.extracting(ProcessEventOutboxEntity::getId)
			.isEqualTo("peo-other-consumer");
	}
}
