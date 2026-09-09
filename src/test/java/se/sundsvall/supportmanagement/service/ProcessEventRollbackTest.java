package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.eventlog.EventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.MESSAGE;

/**
 * A publication that cannot write its row has to take the errand change down with it.
 * <p>
 * Every call site of {@code createErrandEvent} catches Exception and logs a warning, so an exception on its own would
 * leave the errand saved and the process none the wiser - exactly the problem the outbox is there to solve. The
 * transaction is therefore marked rollback only, and the errand change cannot be committed whatever the caller does
 * with the exception.
 * <p>
 * Verified by writing to the errand and reading it back afterwards rather than by inspecting a log, since what is at
 * stake is whether the change survived.
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
class ProcessEventRollbackTest {

	private static final String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
	private static final String ORIGINAL_TITLE = "TITLE-1";
	private static final String NEW_TITLE = "a change that must not survive a failed publication";

	@MockitoBean
	private ProcessEventOutboxRepository outboxRepositoryMock;

	@Autowired
	private EventService eventService;

	@Autowired
	private ErrandsRepository errandsRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	@DisplayName("Verification that an errand change is not committed when the publication of its event fails, even though the caller swallows the exception")
	void anErrandChangeIsNotCommittedWhenThePublicationFails() {
		when(outboxRepositoryMock.save(any())).thenThrow(new DataIntegrityViolationException("the row could not be written"));

		assertThatExceptionOfType(UnexpectedRollbackException.class).isThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(_ -> {
			final var errand = errandsRepository.findById(ERRAND_ID).orElseThrow();
			errand.setTitle(NEW_TITLE);
			errandsRepository.saveAndFlush(errand);

			try {
				eventService.createErrandEvent(EventType.UPDATE, "Nytt meddelande", errand, null, null, false, MESSAGE);
			} catch (final Exception swallowed) {
				// Precisely what every call site does today, and the reason the rollback cannot be left to them
			}
		}));

		assertThat(errandsRepository.findById(ERRAND_ID)).get().extracting(ErrandEntity::getTitle).isEqualTo(ORIGINAL_TITLE);
	}
}
