package se.sundsvall.supportmanagement.apptest;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.MessagingOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.MessagingOutboxEntity;
import se.sundsvall.supportmanagement.service.scheduler.messagingoutbox.MessagingOutboxWorker;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@WireMockAppTestSuite(files = "classpath:/MessagingOutboxSchedulerIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class MessagingOutboxSchedulerIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String PAYLOAD = """
		{"parties":[{"emailAddress":"test@example.com"}],"subject":"Test Subject","message":"Hello World","sender":{"name":"Test Sender","address":"sender@test.com"}}
		""".strip();

	@Autowired
	private MessagingOutboxWorker worker;

	@Autowired
	private MessagingOutboxRepository repository;

	@Test
	void test01_processesEntrySuccessfully() {
		setupCall();
		final var saved = repository.saveAndFlush(buildEntry());

		worker.process(saved);

		assertThat(repository.findById(saved.getId())).isNotPresent();
	}

	@Test
	void test02_retriesOnSendFailure() {
		setupCall();
		final var saved = repository.saveAndFlush(buildEntry());

		worker.process(saved);

		final var updated = repository.findById(saved.getId()).orElseThrow();
		assertThat(updated.getRetryCount()).isEqualTo(1);
		assertThat(updated.isDeadLetter()).isFalse();
		assertThat(updated.getNextRetryAt())
			.isNotNull()
			.isCloseTo(OffsetDateTime.now().plusMinutes(5), within(10, SECONDS));
	}

	@Test
	void test03_marksAsDeadLetterAfterMaxRetries() {
		setupCall();
		final var entry = buildEntry();
		entry.setRetryCount(4);
		final var saved = repository.saveAndFlush(entry);

		worker.process(saved);

		final var updated = repository.findById(saved.getId()).orElseThrow();
		assertThat(updated.isDeadLetter()).isTrue();
	}

	private static MessagingOutboxEntity buildEntry() {
		final var entry = MessagingOutboxEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withMessageType("EMAIL")
			.withPayload(PAYLOAD);
		// Prevent the background scheduler from processing this entry concurrently
		entry.setNextRetryAt(OffsetDateTime.now().plusHours(1));
		return entry;
	}
}
