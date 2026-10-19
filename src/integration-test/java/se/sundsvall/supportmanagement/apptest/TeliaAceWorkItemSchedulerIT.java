package se.sundsvall.supportmanagement.apptest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.TeliaAceContactRepository;
import se.sundsvall.supportmanagement.integration.db.TeliaAceWorkItemRepository;
import se.sundsvall.supportmanagement.service.scheduler.teliaace.TeliaAceWorkItemScheduler;

import static org.assertj.core.api.Assertions.assertThat;

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

	@Autowired
	private TeliaAceContactRepository contactRepository;

	@Test
	void test01_deliversAllQueuedWorkItems() {
		setupCall();

		scheduler.processWorkItems();

		verifyStubs();
		assertThat(workItemRepository.findAll()).isEmpty();

		// errand-1 had no contact yet, so AddWorkItem ran and its response's emailUid must now be stored.
		assertThat(contactRepository.findById("errand-1")).isPresent()
			.get()
			.satisfies(contact -> assertThat(contact.getEmailUid()).isEqualTo("101.CallGuideWorkItem@computer"));

		// errand-2 already had a contact, so UpdateContactData ran against it and the stored emailUid is unchanged.
		assertThat(contactRepository.findById("errand-2")).isPresent()
			.get()
			.satisfies(contact -> assertThat(contact.getEmailUid()).isEqualTo("42.CallGuideWorkItem@computer"));
	}
}
