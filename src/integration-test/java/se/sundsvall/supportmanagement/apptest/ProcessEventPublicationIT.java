package se.sundsvall.supportmanagement.apptest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.service.scheduler.action.ActionScheduler;
import se.sundsvall.supportmanagement.service.scheduler.emailreader.EmailReaderScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

/**
 * Publication from the writes that reach an errand through neither the API nor the errand service: the email intake,
 * which writes no revision, and a scheduled action, which has no request behind it. Also the deletion of an errand
 * through the API, which is published whatever its labels say.
 */
@WireMockAppTestSuite(files = "classpath:/ProcessEventPublicationIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-process-event.sql"
})
@SqlMergeMode(MERGE)
class ProcessEventPublicationIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
	private static final String ERRAND_WITHOUT_LABELS_ID = "cc236cf1-c00f-4479-8341-ecf5dd90b5b9";
	private static final String PROCESS_KEY = "alkt-ansokan";
	private static final String ERRANDS_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands";

	@Autowired
	private EmailReaderScheduler emailReaderScheduler;

	@Autowired
	private ActionScheduler actionScheduler;

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
				assertThat(row.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
				assertThat(row.getNamespace()).isEqualTo(NAMESPACE);
				assertThat(row.getProcessService()).isEqualTo("pw-alkt");
				assertThat(row.getProcessKey()).isEqualTo(PROCESS_KEY);
				assertThat(row.getEventType()).isEqualTo("UPDATE");
				assertThat(row.getEventSubType()).isEqualTo("MESSAGE");
				assertThat(row.getCreated()).isNotNull();
				assertThat(row.getDeliveredAt()).isNull();
			});

		// The label says nothing about the start mode, which reads as AUTOMATIC, and the errand has no process yet
		assertThat(outboxRepository.findAll()).extracting(ProcessEventOutboxEntity::isStartAllowed).containsExactly(true);

		verifyStubs();
	}

	/**
	 * The label is added with no request behind it, and it is the label naming the process. The action records its
	 * change as a new revision, and the executed action is removed.
	 */
	@Test
	@DisplayName("Verification that the process label a scheduled action gives an errand reaches the process, with the permission to start it")
	@Sql("/db/scripts/testdata-process-event-action.sql")
	void test02_aScheduledLabelActionGivesAnOutboxRow() {
		setupCall();

		actionScheduler.processActions();

		assertThat(outboxRepository.findAll())
			.singleElement()
			.satisfies(row -> {
				assertThat(row.getErrandId()).isEqualTo(ERRAND_WITHOUT_LABELS_ID);
				assertThat(row.getProcessKey()).isEqualTo(PROCESS_KEY);
				assertThat(row.getEventType()).isEqualTo("UPDATE");
				assertThat(row.getEventSubType()).isEqualTo("ERRAND");
				assertThat(row.isStartAllowed()).isTrue();
				assertThat(row.getExecutedBy()).isNull();
			});

		setupCall()
			.withServicePath(ERRANDS_PATH + "/" + ERRAND_WITHOUT_LABELS_ID + "/revisions")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-revisions.json")
			.sendRequest();

		setupCall()
			.withServicePath(ERRANDS_PATH + "/" + ERRAND_WITHOUT_LABELS_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-errand.json")
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The errand is created without a label naming a process, so its creation reaches none. Its deletion is published all
	 * the same, without a process key, since the process consumer finds the instance by the errand and not by the key.
	 */
	@Test
	@DisplayName("Verification that deleting an errand whose labels name no process still reaches the process consumer, without a key")
	void test03_aDeletionWithoutAProcessKeyGivesAnOutboxRow() {
		final var location = setupCall()
			.withServicePath(ERRANDS_PATH)
			.withHttpMethod(POST)
			.withRequest("request-create.json")
			.withExpectedResponseStatus(CREATED)
			.sendRequest()
			.getResponseHeaders()
			.getLocation()
			.getPath();
		final var errandId = location.substring(location.lastIndexOf('/') + 1);

		setupCall()
			.withServicePath(location)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequest();

		setupCall()
			.withServicePath(location)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll())
			.singleElement()
			.satisfies(row -> {
				assertThat(row.getErrandId()).isEqualTo(errandId);
				assertThat(row.getProcessService()).isEqualTo("pw-alkt");
				assertThat(row.getProcessKey()).isNull();
				assertThat(row.getEventType()).isEqualTo("DELETE");
				assertThat(row.getEventSubType()).isEqualTo("ERRAND");
				assertThat(row.isStartAllowed()).isFalse();
			});
	}
}
