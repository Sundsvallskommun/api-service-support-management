package se.sundsvall.supportmanagement.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.model.enums.JobStatus;
import se.sundsvall.supportmanagement.model.enums.JobType;
import se.sundsvall.supportmanagement.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.AccessControlService;
import se.sundsvall.supportmanagement.service.job.JobService;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ResourceTest
class JobResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final String JOB_ID = "6a5b8c9d-1234-5678-abcd-ef0123456789";
	private static final String PATH = "/{municipalityId}/{namespace}/jobs/{jobId}";

	private static final String JOBS_PATH = "/{municipalityId}/{namespace}/jobs";

	@Autowired
	private JobService jobServiceMock;

	@Autowired
	private AccessControlService accessControlServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@LocalServerPort
	private int port;

	@Test
	void getJob() {
		final var jobResponse = JobResponse.create()
			.withJobId(JOB_ID)
			.withType(JobType.MOVE_LABEL)
			.withStatus(JobStatus.RUNNING)
			.withProgress(50)
			.withTotal(100)
			.withProcessed(50)
			.withCreated(OffsetDateTime.now().minusMinutes(1))
			.withModified(OffsetDateTime.now());

		when(jobServiceMock.get(NAMESPACE, MUNICIPALITY_ID, JOB_ID)).thenReturn(jobResponse);

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "jobId", JOB_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(JobResponse.class)
			.returnResult().getResponseBody();

		assertThat(response).isEqualTo(jobResponse);
		verify(accessControlServiceMock).verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.NAMESPACE_CONFIG, R);
		verify(jobServiceMock).get(NAMESPACE, MUNICIPALITY_ID, JOB_ID);
	}

	@Test
	void getJobWhenNotAuthorized() {
		doThrow(Problem.valueOf(FORBIDDEN, "no")).when(accessControlServiceMock).verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.NAMESPACE_CONFIG, R);

		webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "jobId", JOB_ID)))
			.exchange()
			.expectStatus().isForbidden();

		verify(jobServiceMock, never()).get(any(), any(), any());
	}

	@Test
	void getJobs() {
		final var jobResponse = JobResponse.create()
			.withJobId(JOB_ID)
			.withType(JobType.SEARCH_REINDEX)
			.withStatus(JobStatus.COMPLETED);
		when(jobServiceMock.find(NAMESPACE, MUNICIPALITY_ID, null, null)).thenReturn(List.of(jobResponse));

		final var response = webTestClient.get()
			.uri(builder -> builder.path(JOBS_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(JobResponse.class)
			.returnResult().getResponseBody();

		assertThat(response).containsExactly(jobResponse);
		verify(accessControlServiceMock).verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.NAMESPACE_CONFIG, R);
		verify(jobServiceMock).find(NAMESPACE, MUNICIPALITY_ID, null, null);
	}

	@Test
	void getJobsOfAStatusAndAType() {
		when(jobServiceMock.find(NAMESPACE, MUNICIPALITY_ID, JobStatus.FAILED, JobType.ERRAND_PURGE)).thenReturn(List.of());

		webTestClient.get()
			.uri(builder -> builder.path(JOBS_PATH).queryParam("status", "FAILED").queryParam("type", "ERRAND_PURGE")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectBody().json("[]");

		verify(jobServiceMock).find(NAMESPACE, MUNICIPALITY_ID, JobStatus.FAILED, JobType.ERRAND_PURGE);
	}

	@Test
	void getJobsWithAnUnknownStatus() {
		webTestClient.get()
			.uri(builder -> builder.path(JOBS_PATH).queryParam("status", "SLEEPING")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(jobServiceMock);
	}

	@Test
	void getJobsWhenNotAuthorized() {
		doThrow(Problem.valueOf(FORBIDDEN, "no")).when(accessControlServiceMock).verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.NAMESPACE_CONFIG, R);

		webTestClient.get()
			.uri(builder -> builder.path(JOBS_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isForbidden();

		verifyNoInteractions(jobServiceMock);
	}
}
