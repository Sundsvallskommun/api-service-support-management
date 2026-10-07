package se.sundsvall.supportmanagement.apptest;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.integration.db.JobRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.JobEntity;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.service.scheduler.emailreader.EmailReaderScheduler;

import static java.time.Duration.ofMillis;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.STOPPED;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.TRIGGER_PROCESS_HEADER;

/**
 * A label carrying processBlocked=true over the wire: nothing about an errand wearing it reaches its process, whatever
 * the event, whoever writes it and whatever the header says; the start and the signal endpoints answer 409; and an ad
 * account cannot take the label off, while a service identity can.
 * <p>
 * The errand running a process (aa..a1) and the errand that has never had one (aa..a2) wear the application label,
 * which names the process, and a label under the blocking one (testdata-process-block.sql). The namespace wakes its
 * process on a changed errand, an attachment and a decision, so what holds an event back is the block and not the
 * triggers. The errand aa..a4 wears the application label alone, and is published as before.
 */
@WireMockAppTestSuite(files = "classpath:/ProcessBlockIT/", classes = Application.class, sharedContext = true)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-process-loop-guard.sql",
	"/db/scripts/testdata-process-block.sql"
})
@SqlMergeMode(MERGE)
class ProcessBlockIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "PROCESS-NAMESPACE";
	private static final String ERRANDS_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands";
	private static final String LABELS_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/labels";

	private static final String ERRAND_RUNNING_A_PROCESS = "aa000000-0000-0000-0000-0000000000a1";
	private static final String ERRAND_WITHOUT_A_PROCESS = "aa000000-0000-0000-0000-0000000000a2";
	private static final String ERRAND_NOT_BLOCKED = "aa000000-0000-0000-0000-0000000000a4";
	private static final String PROCESS_INSTANCE_ID = "pi-it-live";
	private static final String APPLICATION = "alkt-ansokan";

	private static final String APPLICATION_LABEL = "dd000000-0000-0000-0000-0000000000d2";
	private static final String BLOCKING_LABEL = "cb000000-0000-0000-0000-0000000000c1";
	private static final String LABEL_UNDER_THE_BLOCK = "cb000000-0000-0000-0000-0000000000c2";

	private static final String HANDLER_IDENTITY = "joe01doe; type=adAccount";
	private static final String PROCESS_ENGINE_IDENTITY = "pw-alkt; type=processEngine";
	private static final String WAKE = "true";
	private static final String DO_NOT_WAKE = "false";

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String PROCESSES_RESPONSE_FILE = "response-processes.json";
	private static final String ERRAND_RESPONSE_FILE = "response-errand.json";
	private static final String ACTIVITIES_RESPONSE_FILE = "response-activities.json";

	@Autowired
	private ProcessEventOutboxRepository outboxRepository;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private EmailReaderScheduler emailReaderScheduler;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static String errandPath(final String errandId) {
		return ERRANDS_PATH + "/" + errandId;
	}

	@Test
	@DisplayName("Verification that an errand created by a handler wearing a label under the blocking one reaches no process, though the header asks to wake it")
	void test01_aCreationOfABlockedErrandReachesNoProcess() {
		setupCall()
			.withServicePath(ERRANDS_PATH)
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withHeader(TRIGGER_PROCESS_HEADER, WAKE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("Verification that a change to a blocked errand reaches no process, whether a handler or the process engine makes it, and whatever the header says")
	void test02_aChangeToABlockedErrandReachesNoProcessWhoeverMakesIt() {
		setupCall()
			.withServicePath(errandPath(ERRAND_WITHOUT_A_PROCESS))
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withHeader(TRIGGER_PROCESS_HEADER, DO_NOT_WAKE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequest();

		setupCall()
			.withServicePath(errandPath(ERRAND_WITHOUT_A_PROCESS))
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE_IDENTITY)
			.withHeader(TRIGGER_PROCESS_HEADER, WAKE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("Verification that an attachment added to a blocked errand reaches no process, though attachments wake it")
	void test03_anAttachmentOnABlockedErrandReachesNoProcess() throws Exception {
		setupCall()
			.withServicePath(errandPath(ERRAND_RUNNING_A_PROCESS) + "/attachments")
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("errandAttachment", "test.txt")
			.withExpectedResponseStatus(CREATED)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("Verification that a decision made by a handler on a blocked errand reaches no process, though it is the kind of event that passes the emergency brake")
	void test04_aDecisionOnABlockedErrandReachesNoProcess() {
		setupCall()
			.withServicePath(errandPath(ERRAND_RUNNING_A_PROCESS) + "/decisions")
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	/**
	 * The intake writes to an errand of NAMESPACE-1, which testdata-process-event.sql turns into a namespace waking its
	 * process on a message, and which testdata-process-block-intake.sql blocks.
	 */
	@Test
	@DisplayName("Verification that an email arriving on a blocked errand reaches no process, though messages wake it")
	@Sql({
		"/db/scripts/testdata-process-event.sql", "/db/scripts/testdata-process-block-intake.sql"
	})
	void test05_aMessageOnABlockedErrandReachesNoProcess() {
		setupCall();

		emailReaderScheduler.getAndProcessEmails();

		assertThat(outboxRepository.findAll()).isEmpty();
		verifyStubs();
	}

	@Test
	@DisplayName("Verification that a blocked errand is offered no start, and that a start is refused with 409 without anything written")
	void test06_aStartOfABlockedErrandIsRefused() {
		setupCall()
			.withServicePath(errandPath(ERRAND_WITHOUT_A_PROCESS) + "/processes")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(PROCESSES_RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(errandPath(ERRAND_WITHOUT_A_PROCESS) + "/processes/start")
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
		assertThat(activitiesOf(ERRAND_WITHOUT_A_PROCESS)).isZero();
	}

	@Test
	@DisplayName("Verification that the process of a blocked errand is shown as blocked, and that a signal it waits for is refused with 409 without anything written")
	void test07_aSignalToABlockedErrandIsRefused() {
		setupCall()
			.withServicePath(errandPath(ERRAND_RUNNING_A_PROCESS) + "/processes")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(PROCESSES_RESPONSE_FILE)
			.sendRequest();

		final var activitiesBefore = activitiesOf(ERRAND_RUNNING_A_PROCESS);

		setupCall()
			.withServicePath(errandPath(ERRAND_RUNNING_A_PROCESS) + "/processes/" + PROCESS_INSTANCE_ID + "/signals")
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
		assertThat(activitiesOf(ERRAND_RUNNING_A_PROCESS)).isEqualTo(activitiesBefore);
	}

	@Test
	@DisplayName("Verification that the deletion of a blocked errand reaches no process, though a deletion is otherwise published whatever its labels say")
	void test08_aDeletionOfABlockedErrandReachesNoProcess() {
		setupCall()
			.withServicePath(errandPath(ERRAND_WITHOUT_A_PROCESS))
			.withHttpMethod(DELETE)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequest();

		setupCall()
			.withServicePath(errandPath(ERRAND_WITHOUT_A_PROCESS))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("Verification that a handler taking the blocking label off an errand is refused with 409, and that the errand keeps its labels")
	void test09_anAdAccountCannotTakeTheBlockOff() {
		setupCall()
			.withServicePath(errandPath(ERRAND_WITHOUT_A_PROCESS))
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(errandPath(ERRAND_WITHOUT_A_PROCESS))
			.withHttpMethod(GET)
			.withJsonAssertOptions(null)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(ERRAND_RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("Verification that the process engine may take the blocking label off an errand, and that the process is told about the errand from then on")
	void test10_aServiceIdentityTakesTheBlockOff() {
		setupCall()
			.withServicePath(errandPath(ERRAND_WITHOUT_A_PROCESS))
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, PROCESS_ENGINE_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequest();

		setupCall()
			.withServicePath(errandPath(ERRAND_WITHOUT_A_PROCESS))
			.withHttpMethod(GET)
			.withJsonAssertOptions(null)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(ERRAND_RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).singleElement().satisfies(row -> {
			assertThat(row.getErrandId()).isEqualTo(ERRAND_WITHOUT_A_PROCESS);
			assertThat(row.getEventType()).isEqualTo("UPDATE");
			assertThat(row.getEventSubType()).isEqualTo("ERRAND");
			assertThat(row.getProcessKey()).isEqualTo(APPLICATION);
		});
	}

	/**
	 * Moving the label under the blocking one to the root would leave both errands without the blocking label. The move
	 * is asked for by a handler, so both keep the labels they had, the moved label at its new place, and the refusal is
	 * written on each of them.
	 */
	@Test
	@DisplayName("Verification that a label move a handler asks for leaves the blocking label on the errands it would take it off, and writes the refusal on them")
	void test11_aLabelMoveStartedByAnAdAccountLeavesTheBlockOn() throws Exception {
		awaitCompletion(startMove(HANDLER_IDENTITY));

		assertThat(labelIdsOf(ERRAND_RUNNING_A_PROCESS)).containsExactlyInAnyOrder(APPLICATION_LABEL, BLOCKING_LABEL, LABEL_UNDER_THE_BLOCK);
		assertThat(labelIdsOf(ERRAND_WITHOUT_A_PROCESS)).containsExactlyInAnyOrder(APPLICATION_LABEL, BLOCKING_LABEL, LABEL_UNDER_THE_BLOCK);

		setupCall()
			.withServicePath(errandPath(ERRAND_WITHOUT_A_PROCESS) + "/process-activities")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(ACTIVITIES_RESPONSE_FILE)
			.sendRequest();

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("Verification that a label move the process engine asks for takes the blocking label off the errands, and that their processes are told about them from then on")
	void test12_aLabelMoveStartedByAServiceTakesTheBlockOff() throws Exception {
		awaitCompletion(startMove(PROCESS_ENGINE_IDENTITY));

		assertThat(labelIdsOf(ERRAND_RUNNING_A_PROCESS)).containsExactlyInAnyOrder(APPLICATION_LABEL, LABEL_UNDER_THE_BLOCK);
		assertThat(labelIdsOf(ERRAND_WITHOUT_A_PROCESS)).containsExactlyInAnyOrder(APPLICATION_LABEL, LABEL_UNDER_THE_BLOCK);

		assertThat(outboxRepository.findAll())
			.extracting(ProcessEventOutboxEntity::getErrandId, ProcessEventOutboxEntity::getEventType, ProcessEventOutboxEntity::getEventSubType, ProcessEventOutboxEntity::getProcessKey)
			.containsExactlyInAnyOrder(
				tuple(ERRAND_RUNNING_A_PROCESS, "UPDATE", "ERRAND", APPLICATION),
				tuple(ERRAND_WITHOUT_A_PROCESS, "UPDATE", "ERRAND", APPLICATION));
	}

	@Test
	@DisplayName("Verification that an errand without a blocking label is published as before")
	void test13_anErrandWithoutABlockingLabelIsPublishedAsBefore() {
		setupCall()
			.withServicePath(errandPath(ERRAND_NOT_BLOCKED))
			.withHttpMethod(PATCH)
			.withHeader(SENT_BY_HEADER, HANDLER_IDENTITY)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		assertThat(outboxRepository.findAll()).singleElement().satisfies(row -> {
			assertThat(row.getErrandId()).isEqualTo(ERRAND_NOT_BLOCKED);
			assertThat(row.getEventType()).isEqualTo("UPDATE");
			assertThat(row.getEventSubType()).isEqualTo("ERRAND");
			assertThat(row.getProcessKey()).isEqualTo(APPLICATION);
		});
	}

	/**
	 * Asks for the label under the blocking one to be moved to the root, and hands back the job it was answered with.
	 */
	private JobResponse startMove(final String identity) throws Exception {
		return setupCall()
			.withServicePath(LABELS_PATH + "/" + LABEL_UNDER_THE_BLOCK + "/move")
			.withHttpMethod(POST)
			.withHeader(SENT_BY_HEADER, identity)
			.withRequest(REQUEST_FILE)
			.withContentType(APPLICATION_JSON)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(JobResponse.class);
	}

	private void awaitCompletion(final JobResponse job) {
		await()
			.atMost(60, SECONDS)
			.pollInterval(ofMillis(250))
			.until(() -> jobRepository.findById(job.getJobId())
				.map(JobEntity::getStatus)
				.filter(List.of(COMPLETED, STOPPED, FAILED)::contains)
				.isPresent());

		assertThat(jobRepository.findById(job.getJobId())).hasValueSatisfying(ended -> assertThat(ended.getStatus()).isEqualTo(COMPLETED));
	}

	/**
	 * The label ids an errand wears, read from the join table, since the labels of an errand are lazy and the repository
	 * call fetching it has closed its session by the time a test looks.
	 */
	private List<String> labelIdsOf(final String errandId) {
		return jdbcTemplate.queryForList("SELECT metadata_label_id FROM errand_labels WHERE errand_id = ?", String.class, errandId);
	}

	private Integer activitiesOf(final String errandId) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM errand_process_activity WHERE errand_id = ?", Integer.class, errandId);
	}
}
