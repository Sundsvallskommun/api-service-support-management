package se.sundsvall.supportmanagement.apptest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.TeliaAceWorkItemRepository;
import se.sundsvall.supportmanagement.service.scheduler.emailreader.EmailReaderScheduler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the Telia ACE work item queuing against a real database and the real
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} wiring in {@link
 * se.sundsvall.supportmanagement.service.TeliaAceWorkItemService} - not mocks, which is exactly what a unit test
 * cannot prove: that the listener actually fires, and that it only runs once the errand it is about has committed.
 */
@WireMockAppTestSuite(files = "classpath:/EmailReaderTeliaAceNotificationIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it-telia-ace-notification.sql"
})
class EmailReaderTeliaAceNotificationIT extends AbstractAppTest {

	@Autowired
	private EmailReaderScheduler emailReaderScheduler;

	@Autowired
	private ErrandsRepository errandsRepository;

	@Autowired
	private TeliaAceWorkItemRepository teliaAceWorkItemRepository;

	@Test
	void test01_queuesTeliaAceWorkItemForNewErrandAfterItsTransactionCommits() {
		setupCall();

		emailReaderScheduler.getAndProcessEmails();

		verifyStubs();

		// Read back from the database rather than asserted inline against a captured id: the errand only exists at
		// all once processEmail's transaction has committed, and so does the work item that is being proven to come
		// after it - so both must be reachable by a fresh read once the scheduler call has returned.
		final var errand = errandsRepository.findAll();
		assertThat(errand).hasSize(1);

		final var workItems = teliaAceWorkItemRepository.findAll();
		assertThat(workItems).singleElement().satisfies(workItem -> {
			assertThat(workItem.getErrandId()).isEqualTo(errand.getFirst().getId());
			assertThat(workItem.getFromAddress()).isEqualTo("anna.andersson@example.com");
			assertThat(workItem.getContentUrl()).isEqualTo("https://draken.sundsvall.se/namespace-ta/arende/" + errand.getFirst().getErrandNumber());
		});
	}
}
