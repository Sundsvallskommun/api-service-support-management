package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.eventlog.EventType;
import java.time.Clock;
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
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity.PROCESS_KEY_LENGTH;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.ERROR;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.MESSAGE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.WAITING;

/**
 * What publication actually writes, held against a real row rather than a captured entity.
 * <p>
 * The start permission is worked out once, at publication, and travels with the event, so what the process engine acts
 * on is the column and nothing else. A completed process ends the process life of an errand and a failed start does
 * not: trying again after a failure is recovery, while a second process after a completed one is a new errand.
 * <p>
 * The other half is what publication refuses to write. A label attribute is free text of unbounded length while the
 * column it feeds is not, and the difference only shows against a database that enforces the width.
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
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String PROCESS_KEY = "alkt-ansokan";

	@Autowired
	private EventService eventService;

	@Autowired
	private ErrandsRepository errandsRepository;

	@Autowired
	private ErrandProcessRepository processRepository;

	@Autowired
	private ProcessEventOutboxRepository outboxRepository;

	@Autowired
	private ErrandProcessActivityRepository activityRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private Clock clock;

	@Test
	@DisplayName("Verification that an errand with no process at all is given the permission, since its label says nothing about the start mode and that reads as automatic")
	void anErrandWithoutAProcessIsAllowedToStart() {
		publishAMessageEvent();

		assertThat(outboxRepository.findAll()).singleElement().extracting(ProcessEventOutboxEntity::isStartAllowed).isEqualTo(true);
	}

	@Test
	@DisplayName("Verification that a process which has run its course is never started over by an ordinary errand change")
	void anErrandWithACompletedInstanceIsRefusedTheStart() {
		givenInstance(COMPLETED);

		publishAMessageEvent();

		assertThat(outboxRepository.findAll()).singleElement().extracting(ProcessEventOutboxEntity::isStartAllowed).isEqualTo(false);
	}

	@Test
	@DisplayName("Verification that an errand whose only instance failed may start again, since trying again after a failed start is recovery")
	void anErrandWhoseOnlyInstanceFailedIsAllowedToStart() {
		givenInstance(FAILED);

		publishAMessageEvent();

		assertThat(outboxRepository.findAll()).singleElement().extracting(ProcessEventOutboxEntity::isStartAllowed).isEqualTo(true);
	}

	@Test
	@DisplayName("Verification that an errand already running a process is refused the start, while the event itself is published all the same")
	void anErrandWithALiveInstanceIsRefusedTheStart() {
		givenInstance(WAITING);

		publishAMessageEvent();

		assertThat(outboxRepository.findAll()).singleElement().extracting(ProcessEventOutboxEntity::isStartAllowed).isEqualTo(false);
	}

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

	private void givenInstance(final ProcessStatus status) {
		final var instance = ErrandProcessEntity.create()
			.withErrandId(ERRAND_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withProcessService("pw-alkt")
			.withProcessKey(PROCESS_KEY)
			.withProcessInstanceId("process-instance-" + status.name().toLowerCase());

		instance.applyStatus(status, clock);

		processRepository.saveAndFlush(instance);
	}

	/**
	 * Written through the event service inside a transaction, which is the shape every way into publication has.
	 */
	private void publishAMessageEvent() {
		new TransactionTemplate(transactionManager).executeWithoutResult(_ -> eventService.createErrandEvent(
			EventType.UPDATE, "Nytt meddelande", errandsRepository.findById(ERRAND_ID).orElseThrow(), null, null, false, MESSAGE));
	}
}
