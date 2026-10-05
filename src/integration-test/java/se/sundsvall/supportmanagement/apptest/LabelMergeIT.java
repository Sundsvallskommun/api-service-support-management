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
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.STOPPED;

/**
 * Label merge IT tests.
 * <p>
 * A real merge is answered before it is carried out, so what comes back only says a run was accepted. Every test
 * that carries one out waits for the job to reach a state it cannot leave before looking at anything, then reads the
 * label tree and the errands under it straight from the database - mirrors {@link LabelMoveIT}.
 * <p>
 * The tree walked is the one in NAMESPACE-1, municipality 2281, that the shared test data already carries:
 * DEEPSUBTYPE-1 and DEEPSUBTYPE-2, both leaves under SUBTYPE-4, with errand 147d355f referencing both directly. A
 * third leaf, DEEPSUBTYPE-3, is added under the same parent by test01's own {@code @Sql} to serve as the merge
 * destination - nothing in the shared fixture is itself a spare leaf sibling.
 */
@WireMockAppTestSuite(files = "classpath:/LabelMergeIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
@SqlMergeMode(MERGE)
class LabelMergeIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/labels";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	// Leaves under SUBTYPE-4 (CATEGORY-1/TYPE-2/SUBTYPE-4)
	private static final String CATEGORY_1 = "a8fe832f-77a7-4906-9a97-ac5cbd73dbe7";
	private static final String TYPE_2 = "3273e374-855c-4525-b8fc-aeaa710b83c5";
	private static final String SUBTYPE_4 = "f4d6e210-633b-48a6-ad0a-7be839b28762";
	private static final String DEEPSUBTYPE_1 = "ffe5f120-6a3b-4404-ace8-8ea87b559907";
	private static final String DEEPSUBTYPE_2 = "0eb1f695-48b1-40fd-af8c-b277c37db2d4";
	// Added by @Sql on test01 only - a fresh leaf sibling to merge DEEPSUBTYPE-1/2 into
	private static final String DEEPSUBTYPE_3 = "5f6a7b8c-9d0e-41f2-a3b4-c5d6e7f80912";

	// Leaves under TYPE-1 (CATEGORY-1/TYPE-1) - neither referenced by any errand in the shared fixture
	private static final String TYPE_1 = "cd99569f-6b6d-4d7a-b04c-9ae528be8258";
	private static final String SUBTYPE_1 = "8d0ac81c-9c56-43b7-95cd-fa3c3592666d";
	private static final String SUBTYPE_2 = "6dd1f18b-3f45-4d5f-b38b-176bfb3329c8";

	// References DEEPSUBTYPE-1 and DEEPSUBTYPE-2 directly - merging them is what restows it
	private static final String AFFECTED_ERRAND = "147d355f-dc94-4fde-a4cb-9ddd16cb1946";

	private static final String ADD_MERGE_TARGET_LEAF = "INSERT INTO metadata_label(created, municipality_id, namespace, classification, display_name, id, parent_id, resource_name, resource_path, deprecated) "
		+ "VALUES (NOW(), '2281', 'NAMESPACE-1', 'DEEPSUBTYPE', 'DEEPSUBTYPE-DISPLAY-NAME-3', '5f6a7b8c-9d0e-41f2-a3b4-c5d6e7f80912', 'f4d6e210-633b-48a6-ad0a-7be839b28762', 'DEEPSUBTYPE-3', 'CATEGORY-1/TYPE-2/SUBTYPE-4/DEEPSUBTYPE-3', false)";

	private static final String RUNNING_MERGE_LABELS_JOB = "INSERT INTO job(id, municipality_id, namespace, type, status, progress, total, processed, label_id, created, modified) "
		+ "VALUES ('cccccccc-0000-0000-0000-000000000001', '2281', 'NAMESPACE-1', 'MERGE_LABELS', 'RUNNING', 10, 100, 10, '6dd1f18b-3f45-4d5f-b38b-176bfb3329c8', NOW(), NOW())";

	@Autowired
	private MetadataLabelRepository metadataLabelRepository;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("Verification that a merge is accepted, carried out asynchronously, restows every errand it reaches, and removes the source labels - without sending a single email or notification along the way")
	@Sql(statements = ADD_MERGE_TARGET_LEAF)
	void test01_mergeLabelsCompletesRestowsAffectedErrandsAndDeletesSources() throws Exception {
		final var job = startMerge(DEEPSUBTYPE_3, REQUEST_FILE);

		final var ended = awaitEndOf(job.getJobId());

		assertThat(ended.getStatus()).isEqualTo(COMPLETED);

		assertThat(metadataLabelRepository.findById(DEEPSUBTYPE_1)).isEmpty();
		assertThat(metadataLabelRepository.findById(DEEPSUBTYPE_2)).isEmpty();
		assertThat(metadataLabelRepository.findById(DEEPSUBTYPE_3)).isPresent();

		assertThat(labelIdsOf(AFFECTED_ERRAND)).containsExactlyInAnyOrder(DEEPSUBTYPE_3, SUBTYPE_4, TYPE_2, CATEGORY_1);

		// Restowing an errand's labels is not a communication - nothing should have gone out to Messaging
		wiremock.verify(0, postRequestedFor(urlPathMatching("/api-messaging/.*")));
	}

	@Test
	@DisplayName("Verification that a dry-run reports the affected count without making any changes")
	void test02_mergeLabelsDryRunReturnsCountWithoutChanges() throws Exception {
		setupCall()
			.withServicePath(PATH + "/" + SUBTYPE_2 + "/merge")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withContentType(APPLICATION_JSON)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jobRepository.findAll()).isEmpty();
		assertThat(metadataLabelRepository.findById(SUBTYPE_1)).isPresent();
		assertThat(metadataLabelRepository.findById(SUBTYPE_2)).isPresent();
	}

	@Test
	@DisplayName("Verification that a destination with children is refused before a job is ever created, leaving the tree exactly as it was")
	void test03_mergeLabelsTargetHasChildren_refused() {
		setupCall()
			.withServicePath(PATH + "/" + TYPE_1 + "/merge")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withContentType(APPLICATION_JSON)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jobRepository.findAll()).isEmpty();
		assertThat(metadataLabelRepository.findById(SUBTYPE_1)).isPresent();
	}

	@Test
	@DisplayName("Verification that a merge is refused while another job is already running for the namespace, whichever kind it is")
	@Sql(statements = RUNNING_MERGE_LABELS_JOB)
	void test04_mergeLabelsIsRefusedWhileAnotherJobIsRunning() {
		setupCall()
			.withServicePath(PATH + "/" + SUBTYPE_1 + "/merge")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withContentType(APPLICATION_JSON)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(metadataLabelRepository.findById(SUBTYPE_1)).isPresent();
	}

	@Test
	@DisplayName("Verification that the DB itself, not just the API's precheck, refuses a second active MERGE_LABELS row for the same namespace")
	@Sql(statements = RUNNING_MERGE_LABELS_JOB)
	void test05_dbRejectsSecondActiveMergeLabelsJobForSameNamespace() {
		final var secondActiveJob = "INSERT INTO job(id, municipality_id, namespace, type, status, progress, total, processed, label_id, created, modified) "
			+ "VALUES ('cccccccc-0000-0000-0000-000000000002', '2281', 'NAMESPACE-1', 'MERGE_LABELS', 'PENDING', 0, 0, 0, '8d0ac81c-9c56-43b7-95cd-fa3c3592666d', NOW(), NOW())";

		assertThatThrownBy(() -> jdbcTemplate.execute(secondActiveJob))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	/**
	 * Asks for a merge and hands back the job it was answered with. A run is carried out on a thread of its own, so
	 * what comes back says nothing yet about what it has done.
	 */
	private JobResponse startMerge(final String targetLabelId, final String requestFile) throws Exception {
		return setupCall()
			.withServicePath(PATH + "/" + targetLabelId + "/merge")
			.withHttpMethod(POST)
			.withRequest(requestFile)
			.withContentType(APPLICATION_JSON)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(JobResponse.class);
	}

	/**
	 * Waits for the run to reach a state it cannot leave and hands back the job as it ended.
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
	 * The label ids an errand carries, read straight from the join table rather than through the JPA entity.
	 */
	private List<String> labelIdsOf(final String errandId) {
		return jdbcTemplate.queryForList("SELECT metadata_label_id FROM errand_labels WHERE errand_id = ?", String.class, errandId);
	}
}
