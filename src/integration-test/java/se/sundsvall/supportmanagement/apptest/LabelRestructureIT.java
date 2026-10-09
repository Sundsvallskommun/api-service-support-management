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
 * Label-tree restructure IT tests.
 * <p>
 * A real run is answered before it is carried out, so what comes back only says a run was accepted - mirrors
 * {@link LabelMoveIT}/{@link LabelMergeIT}. Every test that carries one out waits for the job to reach a state it
 * cannot leave, then reads the label tree and the errands under it straight from the database.
 * <p>
 * Unlike {@link LabelMoveIT}/{@link LabelMergeIT}, this suite does not address the shared fixture's own
 * {@code CATEGORY-1}/{@code TYPE-2}/... tree by path: those resourceNames carry hyphens, which is exactly what the
 * fixture rows have always been able to get away with (they are inserted straight into the database, bypassing the
 * create/update API's {@code [A-Z0-9_]+} resourceName validation entirely) - but a restructure step's {@code path}
 * segments are validated against that same charset, since addressing a label by path reuses the identical field a
 * client would otherwise submit to create one. So {@link #SEED_LABELS} plants a small, dedicated, validly-named tree
 * of its own under NAMESPACE-1/2281 instead, and {@link #SEED_ERRAND_LABELS}/{@link #SEED_ERRAND_ACCESS_LABELS} wire
 * two of the shared fixture's existing errands onto leaves in it - reusing those two errand rows (with their JSON
 * history, notes etc. already seeded) without needing to reference any of the shared tree's own hyphenated labels.
 * <p>
 * The main scenario (test01/test02) deliberately chains an ADD and a MOVE together: the MOVE's
 * {@code destinationParentPath} names the label the ADD step just created, by path, so it also proves a step can
 * resolve a label an earlier step in the very same request added - the one piece of behaviour that has no equivalent
 * in the standalone {@code /move}/{@code /merge} endpoints.
 */
@WireMockAppTestSuite(files = "classpath:/LabelRestructureIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
@SqlMergeMode(MERGE)
class LabelRestructureIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/labels/restructure";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	// TEST_CATEGORY/TEST_TYPE/TEST_SUBTYPE_OLD (moved), TEST_CATEGORY/TEST_SUBTYPE_DELETE (deleted),
	// TEST_CATEGORY/TEST_RENAME_ME (renamed), TEST_CATEGORY/{TEST_MERGE_TARGET,TEST_MERGE_SOURCE} (merged)
	private static final String TEST_CATEGORY = "eeeeeeee-0000-0000-0000-000000000001";
	private static final String TEST_TYPE = "eeeeeeee-0000-0000-0000-000000000002";
	private static final String TEST_SUBTYPE_OLD = "eeeeeeee-0000-0000-0000-000000000003";
	private static final String TEST_SUBTYPE_DELETE = "eeeeeeee-0000-0000-0000-000000000004";
	private static final String TEST_RENAME_ME = "eeeeeeee-0000-0000-0000-000000000005";
	private static final String TEST_MERGE_TARGET = "eeeeeeee-0000-0000-0000-000000000006";
	private static final String TEST_MERGE_SOURCE = "eeeeeeee-0000-0000-0000-000000000007";

	// Two of the shared fixture's own errands, re-pointed at leaves in this suite's own tree instead of the shared
	// tree's hyphenated labels - kept for their already-seeded JSON history/notes, not for their original labels.
	private static final String ERRAND_TO_RESTOW_ON_MOVE = "1be673c0-6ba3-4fb0-af4a-43acf23389f6";
	private static final String ERRAND_TO_RESTOW_ON_MERGE = "147d355f-dc94-4fde-a4cb-9ddd16cb1946";

	private static final String SEED_LABELS = "INSERT INTO metadata_label(created, municipality_id, namespace, classification, display_name, id, parent_id, resource_name, resource_path, deprecated) VALUES "
		+ "(NOW(), '2281', 'NAMESPACE-1', 'CATEGORY', 'Test category', 'eeeeeeee-0000-0000-0000-000000000001', NULL, 'TEST_CATEGORY', 'TEST_CATEGORY', false), "
		+ "(NOW(), '2281', 'NAMESPACE-1', 'TYPE', 'Test type', 'eeeeeeee-0000-0000-0000-000000000002', 'eeeeeeee-0000-0000-0000-000000000001', 'TEST_TYPE', 'TEST_CATEGORY/TEST_TYPE', false), "
		+ "(NOW(), '2281', 'NAMESPACE-1', 'SUBTYPE', 'Old subtype', 'eeeeeeee-0000-0000-0000-000000000003', 'eeeeeeee-0000-0000-0000-000000000002', 'TEST_SUBTYPE_OLD', 'TEST_CATEGORY/TEST_TYPE/TEST_SUBTYPE_OLD', false), "
		+ "(NOW(), '2281', 'NAMESPACE-1', 'SUBTYPE', 'To delete', 'eeeeeeee-0000-0000-0000-000000000004', 'eeeeeeee-0000-0000-0000-000000000001', 'TEST_SUBTYPE_DELETE', 'TEST_CATEGORY/TEST_SUBTYPE_DELETE', false), "
		+ "(NOW(), '2281', 'NAMESPACE-1', 'TYPE', 'Rename me', 'eeeeeeee-0000-0000-0000-000000000005', 'eeeeeeee-0000-0000-0000-000000000001', 'TEST_RENAME_ME', 'TEST_CATEGORY/TEST_RENAME_ME', false), "
		+ "(NOW(), '2281', 'NAMESPACE-1', 'TYPE', 'Merge target', 'eeeeeeee-0000-0000-0000-000000000006', 'eeeeeeee-0000-0000-0000-000000000001', 'TEST_MERGE_TARGET', 'TEST_CATEGORY/TEST_MERGE_TARGET', false), "
		+ "(NOW(), '2281', 'NAMESPACE-1', 'TYPE', 'Merge source', 'eeeeeeee-0000-0000-0000-000000000007', 'eeeeeeee-0000-0000-0000-000000000001', 'TEST_MERGE_SOURCE', 'TEST_CATEGORY/TEST_MERGE_SOURCE', false)";

	// The two errands come with the shared fixture's own hyphenated labels already attached (see class javadoc) -
	// cleared first so this suite's assertions only ever see the labels its own steps touch.
	private static final String CLEAR_ERRAND_LABELS = "DELETE FROM errand_labels WHERE errand_id IN ('1be673c0-6ba3-4fb0-af4a-43acf23389f6', '147d355f-dc94-4fde-a4cb-9ddd16cb1946')";
	private static final String CLEAR_ERRAND_ACCESS_LABELS = "DELETE FROM errand_access_labels WHERE errand_id IN ('1be673c0-6ba3-4fb0-af4a-43acf23389f6', '147d355f-dc94-4fde-a4cb-9ddd16cb1946')";

	private static final String SEED_ERRAND_LABELS = "INSERT INTO errand_labels(errand_id, metadata_label_id) VALUES "
		+ "('1be673c0-6ba3-4fb0-af4a-43acf23389f6', 'eeeeeeee-0000-0000-0000-000000000003'), "
		+ "('147d355f-dc94-4fde-a4cb-9ddd16cb1946', 'eeeeeeee-0000-0000-0000-000000000007')";

	private static final String SEED_ERRAND_ACCESS_LABELS = "INSERT INTO errand_access_labels(errand_id, metadata_label_id) VALUES "
		+ "('1be673c0-6ba3-4fb0-af4a-43acf23389f6', 'eeeeeeee-0000-0000-0000-000000000003'), "
		+ "('147d355f-dc94-4fde-a4cb-9ddd16cb1946', 'eeeeeeee-0000-0000-0000-000000000007')";

	private static final String RUNNING_MOVE_LABEL_JOB = "INSERT INTO job(id, municipality_id, namespace, type, status, progress, total, processed, label_id, created, modified) "
		+ "VALUES ('dddddddd-0000-0000-0000-000000000001', '2281', 'NAMESPACE-1', 'MOVE_LABEL', 'RUNNING', 10, 100, 10, 'eeeeeeee-0000-0000-0000-000000000005', NOW(), NOW())";

	private static final String RUNNING_RESTRUCTURE_JOB = "INSERT INTO job(id, municipality_id, namespace, type, status, progress, total, created, modified) "
		+ "VALUES ('dddddddd-0000-0000-0000-000000000002', '2281', 'NAMESPACE-1', 'RESTRUCTURE_LABEL_TREE', 'PENDING', 0, 0, NOW(), NOW())";

	@Autowired
	private MetadataLabelRepository metadataLabelRepository;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("Verification that a restructure applies every step in order, restows every errand it reaches, and a MOVE step resolves a destination an earlier ADD step in the same request just created - without sending a single email or notification along the way")
	@Sql(statements = {
		SEED_LABELS, CLEAR_ERRAND_LABELS, CLEAR_ERRAND_ACCESS_LABELS, SEED_ERRAND_LABELS, SEED_ERRAND_ACCESS_LABELS
	})
	void test01_restructureCompletesAllStepsAndRestowsAffectedErrands() throws Exception {
		final var job = startRestructure(REQUEST_FILE);

		final var ended = awaitEndOf(job.getJobId());

		assertThat(ended.getStatus()).isEqualTo(COMPLETED);

		final var newSubtype = metadataLabelRepository.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "TEST_CATEGORY/TEST_TYPE/NEW_SUBTYPE")
			.orElseThrow();
		assertThat(newSubtype.getDisplayName()).isEqualTo("New subtype");

		// The MOVE step's destinationParentPath named NEW_SUBTYPE by path, before this run ever gave it an id
		assertThat(metadataLabelRepository.findById(TEST_SUBTYPE_OLD)).hasValueSatisfying(label -> assertThat(label.getResourcePath()).isEqualTo("TEST_CATEGORY/TEST_TYPE/NEW_SUBTYPE/TEST_SUBTYPE_OLD"));

		assertThat(metadataLabelRepository.findById(TEST_RENAME_ME)).hasValueSatisfying(label -> {
			assertThat(label.getDisplayName()).isEqualTo("Renamed");
			assertThat(label.getResourcePath()).isEqualTo("TEST_CATEGORY/TEST_RENAME_ME");
		});

		assertThat(metadataLabelRepository.findById(TEST_SUBTYPE_DELETE)).isEmpty();

		assertThat(metadataLabelRepository.findById(TEST_MERGE_SOURCE)).isEmpty();
		assertThat(metadataLabelRepository.findById(TEST_MERGE_TARGET)).isPresent();

		assertThat(labelIdsOf(ERRAND_TO_RESTOW_ON_MOVE)).containsExactlyInAnyOrder(TEST_SUBTYPE_OLD, newSubtype.getId(), TEST_TYPE, TEST_CATEGORY);
		assertThat(labelIdsOf(ERRAND_TO_RESTOW_ON_MERGE)).containsExactlyInAnyOrder(TEST_MERGE_TARGET, TEST_CATEGORY);

		// Restowing an errand's labels is not a communication - nothing should have gone out to Messaging
		wiremock.verify(0, postRequestedFor(urlPathMatching("/api-messaging/.*")));
	}

	@Test
	@DisplayName("Verification that a dry-run reports every step's affected count without making any change")
	@Sql(statements = {
		SEED_LABELS, CLEAR_ERRAND_LABELS, CLEAR_ERRAND_ACCESS_LABELS, SEED_ERRAND_LABELS, SEED_ERRAND_ACCESS_LABELS
	})
	void test02_restructureDryRunReturnsPerStepCountsWithoutChanges() throws Exception {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withContentType(APPLICATION_JSON)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jobRepository.findAll()).isEmpty();
		assertThat(metadataLabelRepository.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "TEST_CATEGORY/TEST_TYPE/NEW_SUBTYPE")).isEmpty();
		assertThat(metadataLabelRepository.findById(TEST_SUBTYPE_OLD)).hasValueSatisfying(label -> assertThat(label.getResourcePath()).isEqualTo("TEST_CATEGORY/TEST_TYPE/TEST_SUBTYPE_OLD"));
		assertThat(metadataLabelRepository.findById(TEST_SUBTYPE_DELETE)).isPresent();
		assertThat(metadataLabelRepository.findById(TEST_MERGE_SOURCE)).isPresent();
	}

	@Test
	@DisplayName("Verification that a step naming a path that does not exist is refused before a job is ever created, leaving the tree exactly as it was")
	void test03_restructureStepValidationFailure_refusedBeforeJobCreated() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withContentType(APPLICATION_JSON)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jobRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("Verification that a restructure is refused while another job is already running for the namespace, whichever kind it is - a restructure must not race a lone move/merge job either")
	@Sql(statements = {
		SEED_LABELS, RUNNING_MOVE_LABEL_JOB
	})
	void test04_restructureIsRefusedWhileAnotherJobIsRunning() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withContentType(APPLICATION_JSON)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(metadataLabelRepository.findById(TEST_RENAME_ME)).hasValueSatisfying(label -> assertThat(label.getDisplayName()).isEqualTo("Rename me"));
	}

	@Test
	@DisplayName("Verification that the DB itself, not just the API's precheck, refuses a second active RESTRUCTURE_LABEL_TREE row for the same namespace")
	@Sql(statements = RUNNING_RESTRUCTURE_JOB)
	void test05_dbRejectsSecondActiveRestructureLabelTreeJobForSameNamespace() {
		final var secondActiveJob = "INSERT INTO job(id, municipality_id, namespace, type, status, progress, total, created, modified) "
			+ "VALUES ('dddddddd-0000-0000-0000-000000000003', '2281', 'NAMESPACE-1', 'RESTRUCTURE_LABEL_TREE', 'RUNNING', 0, 0, NOW(), NOW())";

		assertThatThrownBy(() -> jdbcTemplate.execute(secondActiveJob))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	/**
	 * Asks for a restructure and hands back the job it was answered with. A run is carried out on a thread of its own,
	 * so what comes back says nothing yet about what it has done.
	 */
	private JobResponse startRestructure(final String requestFile) throws Exception {
		return setupCall()
			.withServicePath(PATH)
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
	 * The label ids an errand carries, read straight from the join table rather than through the JPA entity - its
	 * {@code labels} collection is lazy, and the repository call that would fetch it has long since closed its session
	 * by the time a test gets to look.
	 */
	private List<String> labelIdsOf(final String errandId) {
		return jdbcTemplate.queryForList("SELECT metadata_label_id FROM errand_labels WHERE errand_id = ?", String.class, errandId);
	}
}
