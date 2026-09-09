package se.sundsvall.supportmanagement.apptest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.service.scheduler.emailreader.EmailReaderScheduler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The email intake writes no revision and reaches the errand through neither the API nor the errand service, which is
 * the whole reason publication hangs off the event service rather than off a comparison of revisions. Dropped there, it
 * would show up in no other test - the errand would simply be updated and the process never told.
 */
@WireMockAppTestSuite(files = "classpath:/ProcessEventPublicationIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-process-event.sql"
})
class ProcessEventPublicationIT extends AbstractAppTest {

	private static final String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";

	@Autowired
	private EmailReaderScheduler emailReaderScheduler;

	@Autowired
	private ProcessEventOutboxRepository outboxRepository;

	@Test
	@DisplayName("Verification that an email arriving on an errand reaches the process of that errand")
	void test01_anEmailIntakeGivesAnOutboxRow() {
		setupCall();

		emailReaderScheduler.getAndProcessEmails();

		assertThat(outboxRepository.findAll())
			.singleElement()
			.satisfies(row -> {
				assertThat(row.getErrandId()).isEqualTo(ERRAND_ID);
				assertThat(row.getMunicipalityId()).isEqualTo("2281");
				assertThat(row.getNamespace()).isEqualTo("NAMESPACE-1");
				assertThat(row.getProcessService()).isEqualTo("pw-alkt");
				assertThat(row.getProcessKey()).isEqualTo("alkt-ansokan");
				assertThat(row.getEventType()).isEqualTo("UPDATE");
				assertThat(row.getEventSubType()).isEqualTo("MESSAGE");
				assertThat(row.getCreated()).isNotNull();
				assertThat(row.getDeliveredAt()).isNull();
			});

		// The label says nothing about the start mode, which reads as AUTOMATIC, and the errand has no process yet
		assertThat(outboxRepository.findAll()).extracting(ProcessEventOutboxEntity::isStartAllowed).containsExactly(true);

		verifyStubs();
	}
}
