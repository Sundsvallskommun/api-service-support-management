package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.eventlog.EventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessActivityRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity.PROCESS_KEY_LENGTH;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.ERROR;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.MESSAGE;

/**
 * Verifies what publication actually writes, read back from a real row of the outbox: the key of a label that has not
 * been read from the database yet, and nothing at all for a process key, taken from a label attribute, that is longer
 * than the column it feeds.
 * <p>
 * The start permission an event carries is verified over the wire, by ProcessStartModeIT and ProcessLoopGuardIT.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles({
	"junit", "dbtest"
})
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-process-event.sql"
})
class ProcessEventPublisherDatabaseTest {

	private static final String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
	private static final String PROCESS_KEY = "alkt-ansokan";
	private static final String PROCESS_LABEL_ID = "5940c8c8-d84a-4144-b650-313356ad1333";
	private static final String ERRAND_WITHOUT_LABELS = "cc236cf1-c00f-4479-8341-ecf5dd90b5b9";

	@Autowired
	private EventService eventService;

	@Autowired
	private ErrandsRepository errandsRepository;

	@Autowired
	private ProcessEventOutboxRepository outboxRepository;

	@Autowired
	private ErrandProcessActivityRepository activityRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	@DisplayName("Verification that a label whose process key cannot fit the column leaves the errand writable, since one mistyped label may not make an errand impossible to write to for ever")
	void anOversizedProcessKeyOnTheLabelLeavesTheErrandWritable() {
		jdbcTemplate.update("update metadata_label_attribute set `value` = ? where `key` = 'processKey'", "a".repeat(PROCESS_KEY_LENGTH + 1));

		assertThatNoException().isThrownBy(this::publishAMessageEvent);

		// No row, and no attempt at one: the insert would have been refused by the column and taken the errand with it
		assertThat(outboxRepository.findAll()).isEmpty();
		assertThat(activityRepository.findByErrandId(ERRAND_ID, Pageable.unpaged()))
			.singleElement()
			.satisfies(entry -> {
				assertThat(entry.getSeverity()).isEqualTo(ERROR);
				assertThat(entry.getErrandProcessId()).isNull();
				assertThat(entry.getMessage()).contains(String.valueOf(PROCESS_KEY_LENGTH));
			});
	}

	@Test
	@DisplayName("Verification that an errand given the process label by a write reaches the process with it, although the label has not been read from the database yet")
	void anErrandGivenTheProcessLabelByAWriteIsPublishedWithItsKey() {
		new TransactionTemplate(transactionManager).executeWithoutResult(_ -> {
			final var errand = errandsRepository.findById(ERRAND_WITHOUT_LABELS).orElseThrow();
			errand.getLabels().add(ErrandLabelEmbeddable.create().withMetadataLabelId(PROCESS_LABEL_ID));

			eventService.createErrandEvent(EventType.UPDATE, "Ärendet har uppdaterats.", errandsRepository.saveAndFlush(errand), null, null, false, ERRAND);
		});

		assertThat(outboxRepository.findAll()).singleElement().satisfies(row -> {
			assertThat(row.getErrandId()).isEqualTo(ERRAND_WITHOUT_LABELS);
			assertThat(row.getProcessKey()).isEqualTo(PROCESS_KEY);
			assertThat(row.isStartAllowed()).isTrue();
		});
	}

	/**
	 * Publishes a message event for the errand through the event service, inside a transaction.
	 */
	private void publishAMessageEvent() {
		new TransactionTemplate(transactionManager).executeWithoutResult(_ -> eventService.createErrandEvent(
			EventType.UPDATE, "Nytt meddelande", errandsRepository.findById(ERRAND_ID).orElseThrow(), null, null, false, MESSAGE));
	}
}
