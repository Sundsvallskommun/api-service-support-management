package se.sundsvall.supportmanagement.integration.db;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.MessagingOutboxEntity;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class MessagingOutboxRepositoryTest {

	@Autowired
	private MessagingOutboxRepository repository;

	@Test
	void save() {
		final var entity = MessagingOutboxEntity.create()
			.withMunicipalityId("2281")
			.withMessageType("EMAIL")
			.withPayload("{}");

		final var saved = repository.save(entity);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreated()).isNotNull().isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		assertThat(saved.getMunicipalityId()).isEqualTo("2281");
		assertThat(saved.getMessageType()).isEqualTo("EMAIL");
		assertThat(saved.getPayload()).isEqualTo("{}");
		assertThat(saved.getRetryCount()).isZero();
		assertThat(saved.isDeadLetter()).isFalse();
		assertThat(saved.getNextRetryAt()).isNull();
	}

	@Test
	void findProcessable_returnsEntryWithNullNextRetryAt() {
		repository.save(MessagingOutboxEntity.create()
			.withMunicipalityId("2281")
			.withMessageType("EMAIL")
			.withPayload("{}"));

		final var result = repository.findProcessable(OffsetDateTime.now());

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getNextRetryAt()).isNull();
		assertThat(result.getFirst().isDeadLetter()).isFalse();
	}

	@Test
	void findProcessable_returnsEntryWithPastNextRetryAt() {
		final var entry = MessagingOutboxEntity.create()
			.withMunicipalityId("2281")
			.withMessageType("EMAIL")
			.withPayload("{}");
		entry.setRetryCount(1);
		entry.setNextRetryAt(OffsetDateTime.now().minusMinutes(1));
		repository.save(entry);

		final var result = repository.findProcessable(OffsetDateTime.now());

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getRetryCount()).isEqualTo(1);
	}

	@Test
	void findProcessable_excludesEntryWithFutureNextRetryAt() {
		final var entry = MessagingOutboxEntity.create()
			.withMunicipalityId("2281")
			.withMessageType("EMAIL")
			.withPayload("{}");
		entry.setRetryCount(1);
		entry.setNextRetryAt(OffsetDateTime.now().plusMinutes(5));
		repository.save(entry);

		final var result = repository.findProcessable(OffsetDateTime.now());

		assertThat(result).isEmpty();
	}

	@Test
	void findProcessable_excludesDeadLetterEntries() {
		final var entry = MessagingOutboxEntity.create()
			.withMunicipalityId("2281")
			.withMessageType("EMAIL")
			.withPayload("{}");
		entry.setDeadLetter(true);
		repository.save(entry);

		final var result = repository.findProcessable(OffsetDateTime.now());

		assertThat(result).isEmpty();
	}

	@Test
	void findProcessable_returnsOnlyProcessableEntriesFromMix() {
		// Processable: no nextRetryAt
		repository.save(MessagingOutboxEntity.create()
			.withMunicipalityId("2281")
			.withMessageType("EMAIL")
			.withPayload("{\"order\":1}"));

		// Not processable: nextRetryAt in future
		final var futureRetry = MessagingOutboxEntity.create()
			.withMunicipalityId("2281")
			.withMessageType("EMAIL")
			.withPayload("{\"order\":2}");
		futureRetry.setNextRetryAt(OffsetDateTime.now().plusMinutes(5));
		repository.save(futureRetry);

		// Not processable: dead letter
		final var deadLetter = MessagingOutboxEntity.create()
			.withMunicipalityId("2281")
			.withMessageType("EMAIL")
			.withPayload("{\"order\":3}");
		deadLetter.setDeadLetter(true);
		repository.save(deadLetter);

		// Processable: nextRetryAt in past
		final var pastRetry = MessagingOutboxEntity.create()
			.withMunicipalityId("2281")
			.withMessageType("EMAIL")
			.withPayload("{\"order\":4}");
		pastRetry.setNextRetryAt(OffsetDateTime.now().minusMinutes(1));
		repository.save(pastRetry);

		final var result = repository.findProcessable(OffsetDateTime.now());

		assertThat(result).hasSize(2);
		assertThat(result).allMatch(e -> !e.isDeadLetter());
		assertThat(result).noneMatch(e -> e.getNextRetryAt() != null && e.getNextRetryAt().isAfter(OffsetDateTime.now()));
	}
}
