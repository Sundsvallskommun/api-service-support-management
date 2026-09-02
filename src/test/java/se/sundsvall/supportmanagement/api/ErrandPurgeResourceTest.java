package se.sundsvall.supportmanagement.api;

import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.errand.purge.ErrandPurgeRequest;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus;
import se.sundsvall.supportmanagement.service.ErrandPurgeService;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.RUNNING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.STOPPED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobType.ERRAND_PURGE;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandPurgeResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/errands/purge";
	private static final String PATH_WITH_ID = "/{municipalityId}/{namespace}/errands/purge/{jobId}";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String JOB_ID = randomUUID().toString();

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private ErrandPurgeService serviceMock;

	@Test
	void startPurge() {

		// Arrange
		final var olderThan = now(systemDefault()).minusYears(3);
		final var request = ErrandPurgeRequest.create()
			.withOlderThan(olderThan)
			.withDryRun(true)
			.withMaxErrands(1000);

		when(serviceMock.startPurge(eq(NAMESPACE), eq(MUNICIPALITY_ID), any(ErrandPurgeRequest.class)))
			.thenReturn(job(RUNNING));

		// Act
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isAccepted()
			.expectHeader().valueEquals("Location", "/%s/%s/jobs/%s".formatted(MUNICIPALITY_ID, NAMESPACE, JOB_ID))
			.expectBody(JobResponse.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getJobId()).isEqualTo(JOB_ID);
		assertThat(response.getStatus()).isEqualTo(RUNNING);
		assertThat(response.getType()).isEqualTo(ERRAND_PURGE);

		verify(serviceMock).startPurge(eq(NAMESPACE), eq(MUNICIPALITY_ID), any(ErrandPurgeRequest.class));
		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void stopPurge() {

		// Arrange
		when(serviceMock.stopPurge(NAMESPACE, MUNICIPALITY_ID, JOB_ID)).thenReturn(job(STOPPED));

		// Act
		final var response = webTestClient.method(org.springframework.http.HttpMethod.DELETE)
			.uri(builder -> builder.path(PATH_WITH_ID).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "jobId", JOB_ID)))
			.exchange()
			.expectStatus().isAccepted()
			.expectBody(JobResponse.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(STOPPED);

		verify(serviceMock).stopPurge(NAMESPACE, MUNICIPALITY_ID, JOB_ID);
		verifyNoMoreInteractions(serviceMock);
	}

	private static JobResponse job(final JobStatus status) {
		return JobResponse.create()
			.withJobId(JOB_ID)
			.withType(ERRAND_PURGE)
			.withStatus(status)
			.withTotal(1000)
			.withProcessed(0)
			.withProgress(0)
			.withCreated(OffsetDateTime.now(systemDefault()));
	}
}
