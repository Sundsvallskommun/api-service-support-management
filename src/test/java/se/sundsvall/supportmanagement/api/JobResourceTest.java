package se.sundsvall.supportmanagement.api;

import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobType;
import se.sundsvall.supportmanagement.service.JobService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class JobResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final String JOB_ID = "6a5b8c9d-1234-5678-abcd-ef0123456789";
	private static final String PATH = "/{municipalityId}/{namespace}/jobs/{jobId}";

	@MockitoBean
	private JobService jobServiceMock;

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
		verify(jobServiceMock).get(NAMESPACE, MUNICIPALITY_ID, JOB_ID);
	}
}
