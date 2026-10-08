package se.sundsvall.supportmanagement.apptest;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.integration.db.JobRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.JobEntity;
import se.sundsvall.supportmanagement.service.scheduler.action.ActionScheduler;

import static java.time.Duration.ofMillis;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.STOPPED;

/**
 * Verifies the process key guard over the wire: a label change that would leave an errand naming a process it does not
 * belong to.
 * <p>
 * The refusal reaches the caller as a 400 whose detail names what is wrong, and is decided by the process rows in the
 * database - a live process, one that has run to its end, and none at all.
 * <p>
 * A label change made by a scheduled action or a label move is refused as well, and the refusal is written on the errand,
 * since there is no caller to answer.
 */
@WireMockAppTestSuite(files = "classpath:/ProcessKeyGuardIT/", classes = Application.class, sharedContext = true)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-process-key-guard.sql"
})
@SqlMergeMode(MERGE)
class ProcessKeyGuardIT extends AbstractAppTest {

	private static final String NAMESPACE = "PROCESS-NAMESPACE";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_WITH_LIVE_PROCESS = "aa000000-0000-0000-0000-0000000000a1";
	private static final String ERRAND_WITHOUT_PROCESS = "aa000000-0000-0000-0000-0000000000a2";
	private static final String ERRAND_WITH_FINISHED_PROCESS = "aa000000-0000-0000-0000-0000000000a3";
	private static final String MOVED_LABEL = "bb000000-0000-0000-0000-0000000000b6";
	private static final String APPLICATION_LABEL = "bb000000-0000-0000-0000-0000000000b1";
	private static final String SUPERVISION_LABEL = "bb000000-0000-0000-0000-0000000000b2";

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	@Autowired
	private ActionScheduler actionScheduler;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private MetadataLabelRepository metadataLabelRepository;

	private static String errandPath(final String errandId) {
		return "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + errandId;
	}

	@Test
	@DisplayName("Verification that relabelling an errand which runs a process into another process is refused")
	void test01_relabellingAnErrandRunningAProcessIsRefused() {
		setupCall()
			.withServicePath(errandPath(ERRAND_WITH_LIVE_PROCESS))
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@DisplayName("Verification that a process which has run to its end holds the labels just as still: the process life of the errand is over")
	void test02_relabellingAnErrandWhoseProcessHasFinishedIsRefused() {
		setupCall()
			.withServicePath(errandPath(ERRAND_WITH_FINISHED_PROCESS))
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@DisplayName("Verification that an errand which has never had a process is not held by the guard at all")
	void test03_relabellingAnErrandWithoutAProcessGoesThrough() {
		setupCall()
			.withServicePath(errandPath(ERRAND_WITHOUT_PROCESS))
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequest();

		setupCall()
			.withServicePath(errandPath(ERRAND_WITHOUT_PROCESS))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withJsonAssertOptions(null)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@DisplayName("Verification that a label saying nothing about a process goes onto an errand running one as any other label would")
	void test04_addingALabelThatNamesNoProcessGoesThrough() {
		setupCall()
			.withServicePath(errandPath(ERRAND_WITH_LIVE_PROCESS))
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequest();

		setupCall()
			.withServicePath(errandPath(ERRAND_WITH_LIVE_PROCESS))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withJsonAssertOptions(null)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@DisplayName("Verification that an errand may not be labelled into two processes at once, whether it runs one or not")
	void test05_labellingAnErrandWithTwoProcessesIsRefused() {
		setupCall()
			.withServicePath(errandPath(ERRAND_WITHOUT_PROCESS))
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The label put on names no process itself, but its parent does, and the parent is put on with it.
	 */
	@Test
	@DisplayName("Verification that relabelling an errand which runs a process with a label whose parent names another process is refused")
	void test06_relabellingThroughAParentNamingAnotherProcessIsRefused() {
		setupCall()
			.withServicePath(errandPath(ERRAND_WITH_LIVE_PROCESS))
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * A scheduled action has no caller to answer, so the refusal is written on the errand instead, and the label is left
	 * off. The action is due twice, and the second refusal writes no second entry within the window.
	 */
	@Test
	@DisplayName("Verification that a scheduled label naming another process is left off the errand, and that the refusal is written on the errand once per window")
	@Sql("/db/scripts/testdata-process-key-guard-action.sql")
	void test07_aScheduledLabelNamingAnotherProcessIsLeftOffOnce() {
		setupCall();

		actionScheduler.processActions();

		setupCall()
			.withServicePath(errandPath(ERRAND_WITH_LIVE_PROCESS) + "/process-activities")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-activities.json")
			.sendRequest();

		setupCall()
			.withServicePath(errandPath(ERRAND_WITH_LIVE_PROCESS))
			.withHttpMethod(GET)
			.withJsonAssertOptions(null)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-errand.json")
			.sendRequestAndVerifyResponse();
	}

	/**
	 * A label move has no caller to answer either. The label is moved from under the application process to under the
	 * supervision process, which takes both errands wearing it there: the errand running the application process keeps the
	 * labels it had and the refusal is written on it, while the errand that has never had a process follows the label.
	 */
	@Test
	@DisplayName("Verification that a label move taking an errand off the process it runs leaves that errand as it is and writes the refusal on it, while an errand without a process follows the label")
	@Sql("/db/scripts/testdata-process-key-guard-move.sql")
	void test08_aMovedLabelNamingAnotherProcessLeavesTheErrandRunningOneAsItIs() throws Exception {
		final var job = setupCall()
			.withServicePath("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/labels/" + MOVED_LABEL + "/move")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withContentType(APPLICATION_JSON)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(JobResponse.class);

		await()
			.atMost(60, SECONDS)
			.pollInterval(ofMillis(250))
			.until(() -> jobRepository.findById(job.getJobId())
				.map(JobEntity::getStatus)
				.filter(List.of(COMPLETED, STOPPED, FAILED)::contains)
				.isPresent());

		assertThat(jobRepository.findById(job.getJobId())).hasValueSatisfying(ended -> assertThat(ended.getStatus()).isEqualTo(COMPLETED));

		setupCall()
			.withServicePath(errandPath(ERRAND_WITH_LIVE_PROCESS) + "/process-activities")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-activities.json")
			.sendRequest();

		setupCall()
			.withServicePath(errandPath(ERRAND_WITH_LIVE_PROCESS))
			.withHttpMethod(GET)
			.withJsonAssertOptions(null)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-errand-with-process.json")
			.sendRequest();

		setupCall()
			.withServicePath(errandPath(ERRAND_WITHOUT_PROCESS))
			.withHttpMethod(GET)
			.withJsonAssertOptions(null)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-errand-without-process.json")
			.sendRequestAndVerifyResponse();
	}

	/**
	 * A label merge has no caller to answer either. The label naming the application process is merged into the one
	 * naming the supervision process: the errand running the application process keeps the label and the refusal is
	 * written on it, the errand that has never had a process is given the supervision label, and the errand whose process
	 * has finished has no access labels to rebuild its labels from and is left as it is. The merged label is kept, since
	 * errands still wear it, and the merge completes.
	 */
	@Test
	@DisplayName("Verification that a label merge taking an errand off the process it runs leaves that errand as it is, keeps the merged label the errand still wears, and completes")
	@Sql("/db/scripts/testdata-process-key-guard-merge.sql")
	void test09_aMergedLabelNamingAnotherProcessIsKeptOnTheErrandRunningOne() throws Exception {
		final var job = setupCall()
			.withServicePath("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/labels/" + SUPERVISION_LABEL + "/merge")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withContentType(APPLICATION_JSON)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(JobResponse.class);

		await()
			.atMost(60, SECONDS)
			.pollInterval(ofMillis(250))
			.until(() -> jobRepository.findById(job.getJobId())
				.map(JobEntity::getStatus)
				.filter(List.of(COMPLETED, STOPPED, FAILED)::contains)
				.isPresent());

		assertThat(jobRepository.findById(job.getJobId())).hasValueSatisfying(ended -> {
			assertThat(ended.getStatus()).isEqualTo(COMPLETED);
			assertThat(ended.getMessage()).isEqualTo("Labels [%s] merged into %s, 1 errand(s) restowed, 2 kept their labels, labels [%s] kept since errands still wear them"
				.formatted(APPLICATION_LABEL, SUPERVISION_LABEL, APPLICATION_LABEL));
		});

		setupCall()
			.withServicePath(errandPath(ERRAND_WITH_LIVE_PROCESS) + "/process-activities")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-activities.json")
			.sendRequest();

		setupCall()
			.withServicePath(errandPath(ERRAND_WITH_LIVE_PROCESS))
			.withHttpMethod(GET)
			.withJsonAssertOptions(null)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-errand-with-process.json")
			.sendRequest();

		setupCall()
			.withServicePath(errandPath(ERRAND_WITHOUT_PROCESS))
			.withHttpMethod(GET)
			.withJsonAssertOptions(null)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-errand-without-process.json")
			.sendRequestAndVerifyResponse();

		assertThat(metadataLabelRepository.existsById(APPLICATION_LABEL)).isTrue();
	}
}
