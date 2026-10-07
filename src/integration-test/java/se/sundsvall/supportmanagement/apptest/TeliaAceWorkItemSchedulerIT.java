package se.sundsvall.supportmanagement.apptest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.TeliaAceWorkItemRepository;
import se.sundsvall.supportmanagement.service.scheduler.teliaace.TeliaAceWorkItemScheduler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the Telia ACE work item queue end to end: two rows are seeded directly (one for a newly created errand,
 * one for an errand updated via email that already has an assigned case worker), the scheduler is triggered, and
 * both are expected to reach Telia ACE with the correct payload - the second carrying {@code predefinedAgentName} -
 * and be removed from the queue once delivered.
 */
@WireMockAppTestSuite(files = "classpath:/TeliaAceWorkItemSchedulerIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it-telia-ace.sql"
})
class TeliaAceWorkItemSchedulerIT extends AbstractAppTest {

	@Autowired
	private TeliaAceWorkItemScheduler scheduler;

	@Autowired
	private TeliaAceWorkItemRepository workItemRepository;

	@Test
	void test01_deliversAllQueuedWorkItems() {
		setupCall();

		scheduler.processWorkItems();

		verifyStubs();
		assertThat(workItemRepository.findAll()).isEmpty();
	}
}
