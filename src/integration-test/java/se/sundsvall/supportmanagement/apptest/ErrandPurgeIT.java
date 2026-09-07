package se.sundsvall.supportmanagement.apptest;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import se.sundsvall.dept44.scheduling.health.Dept44CompositeHealthContributor;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.JobRepository;
import se.sundsvall.supportmanagement.integration.db.RevisionRepository;
import se.sundsvall.supportmanagement.integration.db.model.JobEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus;
import se.sundsvall.supportmanagement.service.scheduler.job.JobScheduler;

import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.time.Duration.ofMillis;
import static java.time.ZoneOffset.UTC;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.STOPPED;

/**
 * Errand purge IT tests.
 * <p>
 * A purge is answered before it is carried out, so what comes back says only that a run was accepted. What each test is
 * really about is what the run left behind once it ended, which is why every one of them waits for the job to reach a
 * state it cannot leave before looking at anything.
 * <p>
 * The errands walked are the ones in PURGE-NAMESPACE, which exist for these tests alone. Six of the nine are reached by
 * the cutoff the tests use and three are not, among them the one lying exactly on it. The errands of every other
 * namespace in the shared test data - several of them older still - are what a run has to leave where they are.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandPurgeIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-it-purge.sql"
})
@SqlMergeMode(MERGE)
class ErrandPurgeIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "PURGE-NAMESPACE";
	private static final String PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/purge";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String SENT_BY = "joe01doe; type=adAccount";

	/**
	 * The cutoff the tests are run with, in UTC because that is the wall clock the timestamps in the test data are
	 * written against: the database runs in UTC and the entities store their times normalized to it. The two errands
	 * either side of the cutoff are one millisecond apart, so reading it in the zone the build happens to run in would
	 * put both of them on the same side of it.
	 */
	private static final OffsetDateTime CUTOFF = LocalDateTime.of(2023, 1, 1, 0, 0).atOffset(UTC);
	private static final String CUTOFF_PLACEHOLDER = "<CUTOFF>";

	// Last touched before the cutoff, and reached by a run in the order their ids sort in. The first of them is the one
	// carrying an attachment, a stakeholder, a notification, revisions, a communication and a conversation.
	private static final String FIRST_ERRAND_REACHED = "aaaa1111-0000-0000-0000-000000000001";
	private static final String ERRAND_A_MILLISECOND_BEFORE_THE_CUTOFF = "aaaa1111-0000-0000-0000-000000000005";
	private static final String ERRAND_DATED_BY_MODIFIED = "aaaa1111-0000-0000-0000-000000000007";
	private static final String ERRAND_DATED_BY_CREATED = "aaaa1111-0000-0000-0000-000000000008";
	private static final List<String> ERRANDS_REACHED = List.of(
		FIRST_ERRAND_REACHED,
		"aaaa1111-0000-0000-0000-000000000002",
		"aaaa1111-0000-0000-0000-000000000003",
		ERRAND_A_MILLISECOND_BEFORE_THE_CUTOFF,
		ERRAND_DATED_BY_MODIFIED,
		ERRAND_DATED_BY_CREATED);

	// Not reached however often a run is asked for: one touched after the cutoff, one touched exactly on it since the
	// cutoff is exclusive, and one carrying no date at all and therefore nothing to show it is old enough
	private static final String ERRAND_TOUCHED_AFTER_THE_CUTOFF = "aaaa1111-0000-0000-0000-000000000004";
	private static final String ERRAND_EXACTLY_AT_THE_CUTOFF = "aaaa1111-0000-0000-0000-000000000006";
	private static final String ERRAND_WITH_NO_DATE = "aaaa1111-0000-0000-0000-000000000009";
	private static final List<String> ERRANDS_SPARED_BY_THE_CUTOFF = List.of(
		ERRAND_TOUCHED_AFTER_THE_CUTOFF,
		ERRAND_EXACTLY_AT_THE_CUTOFF,
		ERRAND_WITH_NO_DATE);

	// Older than the cutoff, but in namespaces and municipalities the run was not pointed at
	private static final List<String> ERRANDS_OUTSIDE_THE_NAMESPACE = List.of(
		"ec677eb3-604c-4935-bff7-f8f0b500c8f4",
		"cc236cf1-c00f-4479-8341-ecf5dd90b5b9",
		"1be673c0-6ba3-4fb0-af4a-43acf23389f6",
		"f4a7a771-bb75-487b-b7d8-2684a0c3512c",
		"e29906af-3083-4dcf-bb8a-d787ccf2dcc4");

	private static final String ACCESS_CONTROLLED_ERRAND = "58c41b44-0b9f-413d-bd46-406d24bf5ca8";

	private static final String RUNNING_PURGE_JOB_ID = "aaaaaaaa-0000-0000-0000-000000000001";
	private static final String MOVE_LABEL_JOB_ID = "aaaaaaaa-0000-0000-0000-000000000002";
	private static final String ABANDONED_PURGE_JOB_ID = "aaaaaaaa-0000-0000-0000-000000000003";

	// Kept on one line each: an inlined statement carrying a line break is taken for two statements
	private static final String RUNNING_PURGE_JOB = "INSERT INTO job(id, municipality_id, namespace, type, status, progress, total, processed, created, modified) VALUES ('aaaaaaaa-0000-0000-0000-000000000001', '2281', 'PURGE-NAMESPACE', 'ERRAND_PURGE', 'RUNNING', 10, 100, 10, NOW(), NOW())";

	private static final String RUNNING_MOVE_LABEL_JOB = "INSERT INTO job(id, municipality_id, namespace, type, status, progress, total, processed, created, modified) VALUES ('aaaaaaaa-0000-0000-0000-000000000002', '2281', 'PURGE-NAMESPACE', 'MOVE_LABEL', 'RUNNING', 10, 100, 10, NOW(), NOW())";

	// A run whose instance went away days ago: still reading as running, and still holding the namespace against every
	// run that comes after it
	private static final String ABANDONED_PURGE_JOB = "INSERT INTO job(id, municipality_id, namespace, type, status, progress, total, processed, created, modified) VALUES ('aaaaaaaa-0000-0000-0000-000000000003', '2281', 'PURGE-NAMESPACE', 'ERRAND_PURGE', 'RUNNING', 10, 100, 10, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 7 DAY)";

	@Autowired
	private ErrandsRepository errandsRepository;

	@Autowired
	private RevisionRepository revisionRepository;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private JobScheduler jobScheduler;

	@Autowired
	private Dept44CompositeHealthContributor healthContributor;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("Verification that a dry run counts what a purge would reach and removes none of it, which is what makes it safe to ask before asking for the real thing")
	void test01_dryRunReportsWhatWouldBeRemoved() throws Exception {
		final var job = startPurge(PATH);

		final var ended = awaitEndOf(job.getJobId());

		assertThat(ended.getStatus()).isEqualTo(COMPLETED);
		assertThat(ended.getTotal()).isEqualTo(6);
		assertThat(ended.getProcessed()).isEqualTo(6);
		assertThat(ended.getMessage()).isEqualTo("Dry run over 6 errands, none of which were removed");

		assertThat(errandsRepository.findAllById(ERRANDS_REACHED)).hasSize(6);
		assertThat(rowsIn("attachment", FIRST_ERRAND_REACHED)).isEqualTo(2);
	}

	@Test
	@DisplayName("Verification that a run removes the errands past the cutoff along with everything hanging off them, and leaves every errand it was not pointed at where it is")
	void test02_purgeRemovesErrandsPastTheCutoff() throws Exception {
		assertThat(revisionRepository.findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(NAMESPACE, MUNICIPALITY_ID, FIRST_ERRAND_REACHED)).hasSize(2);

		final var job = startPurge(PATH);

		final var ended = awaitEndOf(job.getJobId());

		assertThat(ended.getStatus()).isEqualTo(COMPLETED);
		assertThat(ended.getProcessed()).isEqualTo(6);
		assertThat(ended.getMessage()).isEqualTo("Removed 6 of 6 errands reached, 0 could not be removed");

		assertThat(errandsRepository.findAllById(ERRANDS_REACHED)).isEmpty();
		assertThat(errandsRepository.findAllById(ERRANDS_SPARED_BY_THE_CUTOFF)).hasSize(3);
		assertThat(errandsRepository.findAllById(ERRANDS_OUTSIDE_THE_NAMESPACE)).hasSize(5);

		// The cutoff is exclusive, and an errand is dated by touched, then modified, then created
		assertThat(errandsRepository.existsById(ERRAND_A_MILLISECOND_BEFORE_THE_CUTOFF)).isFalse();
		assertThat(errandsRepository.existsById(ERRAND_EXACTLY_AT_THE_CUTOFF)).isTrue();
		assertThat(errandsRepository.existsById(ERRAND_DATED_BY_MODIFIED)).isFalse();
		assertThat(errandsRepository.existsById(ERRAND_DATED_BY_CREATED)).isFalse();
		assertThat(errandsRepository.existsById(ERRAND_WITH_NO_DATE)).isTrue();

		// Everything that hung off the errand goes with it, blobs included
		assertThat(rowsIn("attachment", FIRST_ERRAND_REACHED)).isZero();
		assertThat(rowsIn("stakeholder", FIRST_ERRAND_REACHED)).isZero();
		assertThat(rowsIn("notification", FIRST_ERRAND_REACHED)).isZero();
		assertThat(rowsIn("conversation", FIRST_ERRAND_REACHED)).isZero();
		assertThat(communicationsFor("PU-23020001")).isZero();
		assertThat(blobsOfThePurgedErrand()).isZero();

		// The revisions go with the errand as well, since each of them holds a full snapshot of what the run set out to remove
		assertThat(revisionRepository.findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(NAMESPACE, MUNICIPALITY_ID, FIRST_ERRAND_REACHED)).isEmpty();

		// What the errands reached into out in the neighbouring services is gone too: notes are asked for once per
		// errand, and the one conversation takes its relation and its counterpart in MessageExchange with it
		wiremock.verify(6, getRequestedFor(urlPathMatching("/api-notes/2281/notes")));
		wiremock.verify(1, deleteRequestedFor(urlPathMatching("/api-relation/2281/relations/PURGE-RELATION-[0-9]")));
		wiremock.verify(1, deleteRequestedFor(urlPathMatching("/api-messageexchange/2281/draken/conversations/[0-9a-f-]+")));
		verifyStubs();
	}

	@Test
	@DisplayName("Verification that a run given a limit stops at it, so that a first purge of a namespace can be asked for in an amount someone is prepared to lose")
	void test03_purgeStopsAtTheErrandLimit() throws Exception {
		final var job = startPurge(PATH);

		final var ended = awaitEndOf(job.getJobId());

		assertThat(ended.getStatus()).isEqualTo(COMPLETED);
		assertThat(ended.getProcessed()).isEqualTo(1);
		assertThat(ended.getMessage()).isEqualTo("Removed 1 of 1 errands reached, 0 could not be removed");

		// A walk goes in id order, so the limit falls on the first of the three reached rather than on any of them
		assertThat(errandsRepository.existsById(FIRST_ERRAND_REACHED)).isFalse();
		assertThat(errandsRepository.findAllById(ERRANDS_REACHED)).hasSize(5);
		assertThat(errandsRepository.existsById(ERRAND_TOUCHED_AFTER_THE_CUTOFF)).isTrue();
		verifyStubs();
	}

	@Test
	@DisplayName("Verification that a namespace already being purged is refused, since two runs walking it at once would do each other's work twice over")
	@Sql(statements = RUNNING_PURGE_JOB)
	void test04_purgeIsRefusedWhileAnotherIsRunning() {
		setupCall()
			.withHeader(SENT_BY_HEADER, SENT_BY)
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withRequestReplacement(CUTOFF_PLACEHOLDER, CUTOFF.toString())
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(errandsRepository.findAllById(ERRANDS_REACHED)).hasSize(6);
	}

	@Test
	@DisplayName("Verification that a namespace under access control is refused outright, so that a purge does not become the way around the guard on who may reach its errands")
	void test05_purgeIsRefusedForNamespaceUnderAccessControl() {
		setupCall()
			.withHeader(SENT_BY_HEADER, SENT_BY)
			.withServicePath("/2506/NAMESPACE-2506/errands/purge")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withRequestReplacement(CUTOFF_PLACEHOLDER, CUTOFF.toString())
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(errandsRepository.existsById(ACCESS_CONTROLLED_ERRAND)).isTrue();
	}

	@Test
	@DisplayName("Verification that a cutoff too close to now is refused, which is what stands between a mistyped timestamp and an emptied namespace")
	void test06_purgeIsRefusedForACutoffTooCloseToNow() {
		setupCall()
			.withHeader(SENT_BY_HEADER, SENT_BY)
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withRequestReplacement(CUTOFF_PLACEHOLDER, CUTOFF.toString())
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		// Refused before a job was created, so nothing is left behind for a run that was never accepted
		assertThat(jobRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("Verification that a run is marked as stopped where the request lands, which is what lets it be stopped from an instance other than the one carrying it out")
	@Sql(statements = RUNNING_PURGE_JOB)
	void test07_stopPurge() {
		setupCall()
			.withHeader(SENT_BY_HEADER, SENT_BY)
			.withServicePath(PATH + "/" + RUNNING_PURGE_JOB_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest();

		assertThat(jobRepository.findById(RUNNING_PURGE_JOB_ID))
			.map(JobEntity::getStatus)
			.contains(STOPPED);
	}

	@Test
	@DisplayName("Verification that a job of another kind is not stopped through the purge resource, since the job table is shared by every long running piece of work")
	@Sql(statements = RUNNING_MOVE_LABEL_JOB)
	void test08_stopIsRefusedForAJobOfAnotherKind() {
		setupCall()
			.withHeader(SENT_BY_HEADER, SENT_BY)
			.withServicePath(PATH + "/" + MOVE_LABEL_JOB_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		// The job it does not belong to is left exactly as it was
		assertThat(jobRepository.findById(MOVE_LABEL_JOB_ID))
			.map(JobEntity::getStatus)
			.contains(JobStatus.RUNNING);
	}

	@Test
	@DisplayName("Verification that a run whose instance went away is ended rather than left holding the namespace for good, and that the service answers for it where its state is read")
	@Sql(statements = ABANDONED_PURGE_JOB)
	void test09_abandonedRunIsEndedSoTheNamespaceIsNotBlockedForGood() throws Exception {
		// Nothing has moved the job since the instance carrying it out went away, so the namespace is closed to new runs
		setupCall()
			.withHeader(SENT_BY_HEADER, SENT_BY)
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withRequestReplacement(CUTOFF_PLACEHOLDER, CUTOFF.toString())
			.withExpectedResponseStatus(CONFLICT)
			.sendRequest();

		jobScheduler.maintainJobs();

		assertThat(jobRepository.findById(ABANDONED_PURGE_JOB_ID))
			.map(JobEntity::getStatus)
			.contains(FAILED);

		// Work left half done is answered for where the state of the service is read, not only in a log nobody watches -
		// and it says which run stopped, since a service marked restricted without a reason tells a reader nothing
		final var health = healthContributor.getOrCreateIndicator("maintain_jobs").health();

		assertThat(health.getStatus().getCode()).isEqualTo("RESTRICTED");
		assertThat(health.getDetails()).hasEntrySatisfying("Reason", reason -> assertThat(reason.toString())
			.contains("1 run(s) stopped being reported on")
			.contains("ERRAND_PURGE " + ABANDONED_PURGE_JOB_ID)
			.contains("in namespace " + NAMESPACE + " for municipality " + MUNICIPALITY_ID)
			.contains("last written to"));

		// And the namespace takes runs again
		final var job = startPurge(PATH);

		assertThat(awaitEndOf(job.getJobId()).getStatus()).isEqualTo(COMPLETED);
	}

	/**
	 * Asks for a purge and hands back the job it was answered with. A run is carried out on a thread of its own, so what
	 * comes back says nothing yet about what it has done.
	 */
	private JobResponse startPurge(final String servicePath) throws Exception {
		return setupCall()
			.withHeader(SENT_BY_HEADER, SENT_BY)
			.withServicePath(servicePath)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withRequestReplacement(CUTOFF_PLACEHOLDER, CUTOFF.toString())
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

	private int rowsIn(final String table, final String errandId) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table + " WHERE errand_id = ?", Integer.class, errandId);
	}

	private int communicationsFor(final String errandNumber) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM communication WHERE errand_number = ?", Integer.class, errandNumber);
	}

	/**
	 * The blobs the purged errand held: one belonging to an attachment of its own, and one shared between a
	 * communication attachment and the copy of it kept on the errand. Removing the errand has to leave neither of them,
	 * and has to reach the shared one in an order that does not take it out from under something still pointing at it.
	 */
	private int blobsOfThePurgedErrand() {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM attachment_data WHERE id IN (101, 102)", Integer.class);
	}
}
