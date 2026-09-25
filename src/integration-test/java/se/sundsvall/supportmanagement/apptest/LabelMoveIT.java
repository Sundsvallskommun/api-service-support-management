package se.sundsvall.supportmanagement.apptest;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.integration.db.JobRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.JobEntity;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.time.Duration.ofMillis;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.RUNNING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.STOPPED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobType.MOVE_LABEL;

/**
 * Label move IT tests.
 * <p>
 * A move is answered before it is carried out, so what comes back only says a run was accepted. Every test here waits
 * for the job to reach a state it cannot leave before looking at anything, then reads the label tree and the errands
 * under it straight from the database rather than through the API — what a caller following the move would read too.
 * <p>
 * The tree walked is the one in NAMESPACE-1, municipality 2281, that the shared test data already carries: CATEGORY-1
 * / TYPE-2 / SUBTYPE-4, with two DEEPSUBTYPE children under it, and errand 1be673c0 referencing both SUBTYPE-3 and
 * SUBTYPE-4 directly.
 */
@WireMockAppTestSuite(files = "classpath:/LabelMoveIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
@SqlMergeMode(MERGE)
class LabelMoveIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/labels";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	// CATEGORY-1 / TYPE-2 / SUBTYPE-4, with DEEPSUBTYPE-1 and DEEPSUBTYPE-2 under it
	private static final String CATEGORY_1 = "a8fe832f-77a7-4906-9a97-ac5cbd73dbe7";
	private static final String CATEGORY_2 = "13a6abf3-b5ed-4edc-b582-6cf58fa667e3";
	private static final String TYPE_1 = "cd99569f-6b6d-4d7a-b04c-9ae528be8258";
	private static final String TYPE_2 = "3273e374-855c-4525-b8fc-aeaa710b83c5";
	private static final String SUBTYPE_3 = "926fd3f9-f488-4ba4-93f6-2789dee0c0c3";
	private static final String SUBTYPE_4 = "f4d6e210-633b-48a6-ad0a-7be839b28762";
	private static final String DEEPSUBTYPE_1 = "ffe5f120-6a3b-4404-ace8-8ea87b559907";
	private static final String DEEPSUBTYPE_2 = "0eb1f695-48b1-40fd-af8c-b277c37db2d4";

	// References SUBTYPE-3 and SUBTYPE-4 directly - moving SUBTYPE-4 is what restows it
	private static final String AFFECTED_ERRAND = "1be673c0-6ba3-4fb0-af4a-43acf23389f6";

	private static final String RUNNING_MOVE_LABEL_JOB = "INSERT INTO job(id, municipality_id, namespace, type, status, progress, total, processed, label_id, created, modified) "
		+ "VALUES ('bbbbbbbb-0000-0000-0000-000000000001', '2281', 'NAMESPACE-1', 'MOVE_LABEL', 'RUNNING', 10, 100, 10, 'f4d6e210-633b-48a6-ad0a-7be839b28762', NOW(), NOW())";

	@Autowired
	private MetadataLabelRepository metadataLabelRepository;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("Verification that a move is accepted, carried out asynchronously, and restows every errand it reaches - without sending a single email or notification along the way")
	void test01_moveLabelCompletesAndRestowsAffectedErrands() throws Exception {
		final var job = startMove(SUBTYPE_4, REQUEST_FILE);

		final var ended = awaitEndOf(job.getJobId());

		assertThat(ended.getStatus()).isEqualTo(COMPLETED);

		assertThat(metadataLabelRepository.findById(SUBTYPE_4)).hasValueSatisfying(label -> assertThat(label.getResourcePath()).isEqualTo("CATEGORY-1/TYPE-1/SUBTYPE-4"));
		assertThat(metadataLabelRepository.findById(DEEPSUBTYPE_1)).hasValueSatisfying(label -> assertThat(label.getResourcePath()).isEqualTo("CATEGORY-1/TYPE-1/SUBTYPE-4/DEEPSUBTYPE-1"));
		assertThat(metadataLabelRepository.findById(DEEPSUBTYPE_2)).hasValueSatisfying(label -> assertThat(label.getResourcePath()).isEqualTo("CATEGORY-1/TYPE-1/SUBTYPE-4/DEEPSUBTYPE-2"));

		assertThat(labelIdsOf(AFFECTED_ERRAND)).containsExactlyInAnyOrder(SUBTYPE_3, TYPE_2, CATEGORY_1, SUBTYPE_4, TYPE_1);

		// Restowing an errand's labels is not a communication - nothing should have gone out to Messaging
		wiremock.verify(0, postRequestedFor(urlPathMatching("/api-messaging/.*")));
	}

	@Test
	@DisplayName("Verification that a move refreshes resourcePath for every node in a multi-level subtree, not just the one moved")
	void test02_moveLabelRefreshesResourcePathForWholeSubtree() throws Exception {
		final var job = startMove(CATEGORY_1, REQUEST_FILE);

		final var ended = awaitEndOf(job.getJobId());

		assertThat(ended.getStatus()).isEqualTo(COMPLETED);

		assertThat(metadataLabelRepository.findById(CATEGORY_1)).hasValueSatisfying(label -> assertThat(label.getResourcePath()).isEqualTo("CATEGORY-2/CATEGORY-1"));
		assertThat(metadataLabelRepository.findById(TYPE_2)).hasValueSatisfying(label -> assertThat(label.getResourcePath()).isEqualTo("CATEGORY-2/CATEGORY-1/TYPE-2"));
		assertThat(metadataLabelRepository.findById(SUBTYPE_4)).hasValueSatisfying(label -> assertThat(label.getResourcePath()).isEqualTo("CATEGORY-2/CATEGORY-1/TYPE-2/SUBTYPE-4"));
		// Four levels below the new root - read straight from the database, the way a caller following the move would
		assertThat(metadataLabelRepository.findById(DEEPSUBTYPE_1)).hasValueSatisfying(label -> assertThat(label.getResourcePath()).isEqualTo("CATEGORY-2/CATEGORY-1/TYPE-2/SUBTYPE-4/DEEPSUBTYPE-1"));
	}

	@Test
	@DisplayName("Verification that a cycle is refused before a job is ever created, leaving the tree exactly as it was")
	void test03_moveLabelCycleIsRefusedAndNothingChanges() {
		setupCall()
			.withServicePath(PATH + "/" + TYPE_2 + "/move")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withContentType(APPLICATION_JSON)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jobRepository.findAll()).isEmpty();
		assertThat(metadataLabelRepository.findById(TYPE_2)).hasValueSatisfying(label -> assertThat(label.getResourcePath()).isEqualTo("CATEGORY-1/TYPE-2"));
	}

	@Test
	@DisplayName("Verification that a move is refused while another job is already running for the namespace, whichever kind it is")
	void test04_moveLabelIsRefusedWhileAnotherJobIsRunning() {
		// Seeded through the repository, not @Sql's raw NOW() - stealStaleLease() judges this row by its actual
		// (Hibernate-normalized) modified/created instant, and a raw-SQL NOW() round-trips through
		// @TimeZoneStorage(NORMALIZE) off by whatever the JVM's local UTC offset happens to be (confirmed directly:
		// a row inserted with NOW() while the JVM read back "2 hours old" under this run's CEST offset), which is
		// wrong by more than enough to make this run look stale and get its lease stolen. A row this test creates
		// through the same repository the app itself writes through carries no such offset.
		//
		// No id set: JobEntity's id is @UuidGenerator-assigned, and giving it one of our own here would route the save
		// through merge() instead of persist() - Hibernate then looks for an existing row with that id before writing,
		// finds none (the table was just truncated), and raises exactly the StaleObjectStateException that generator
		// is there to prevent. Nothing downstream in this test needs the id back.
		jobRepository.saveAndFlush(JobEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withType(MOVE_LABEL)
			.withStatus(RUNNING)
			.withProgress(10)
			.withTotal(100)
			.withProcessed(10)
			.withLabelId(SUBTYPE_4));

		setupCall()
			.withServicePath(PATH + "/" + SUBTYPE_4 + "/move")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withContentType(APPLICATION_JSON)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(metadataLabelRepository.findById(SUBTYPE_4)).hasValueSatisfying(label -> assertThat(label.getResourcePath()).isEqualTo("CATEGORY-1/TYPE-2/SUBTYPE-4"));
	}

	@Test
	@DisplayName("Verification that the DB itself, not just the API's precheck, refuses a second active MOVE_LABEL row for the same namespace - the backstop a check-then-act guard alone cannot provide against two requests racing within the same window")
	@Sql(statements = RUNNING_MOVE_LABEL_JOB)
	void test05_dbRejectsSecondActiveMoveLabelJobForSameNamespaceRegardlessOfLabel() {
		// A different id and a different label than the seeded row - the constraint must still refuse, since by the
		// time either insert reaches the DB neither request's own precheck has any way left to catch the other.
		final var secondActiveJob = "INSERT INTO job(id, municipality_id, namespace, type, status, progress, total, processed, label_id, created, modified) "
			+ "VALUES ('bbbbbbbb-0000-0000-0000-000000000002', '2281', 'NAMESPACE-1', 'MOVE_LABEL', 'PENDING', 0, 0, 0, 'ffe5f120-6a3b-4404-ace8-8ea87b559907', NOW(), NOW())";

		assertThatThrownBy(() -> jdbcTemplate.execute(secondActiveJob))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	/**
	 * Asks for a move and hands back the job it was answered with. A run is carried out on a thread of its own, so what
	 * comes back says nothing yet about what it has done.
	 */
	private JobResponse startMove(final String labelId, final String requestFile) throws Exception {
		return setupCall()
			.withServicePath(PATH + "/" + labelId + "/move")
			.withHttpMethod(POST)
			.withRequest(requestFile)
			.withContentType(APPLICATION_JSON)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(JobResponse.class);
	}

	/**
	 * Waits for the run to reach a state it cannot leave and hands back the job as it ended. Answered by the job table
	 * rather than by anything held on this side, which is what a caller following the run would read as well.
	 */
	private JobEntity awaitEndOf(final String jobId) {
		await()
			.atMost(120, SECONDS)
			.pollDelay(ofMillis(0))
			.pollInterval(ofMillis(250))
			.until(() -> jobRepository.findById(jobId)
				.map(JobEntity::getStatus)
				.filter(List.of(COMPLETED, STOPPED, FAILED)::contains)
				.isPresent());

		return jobRepository.findById(jobId).orElseThrow();
	}

	/**
	 * The moved-label ids an errand carries, read straight from the join table rather than through the JPA entity - its
	 * {@code labels} collection is lazy, and the repository call that would fetch it has long since closed its session
	 * by the time a test gets to look.
	 */
	private List<String> labelIdsOf(final String errandId) {
		return jdbcTemplate.queryForList("SELECT metadata_label_id FROM errand_labels WHERE errand_id = ?", String.class, errandId);
	}
}
