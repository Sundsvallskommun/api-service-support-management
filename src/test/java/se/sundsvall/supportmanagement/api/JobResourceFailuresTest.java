package se.sundsvall.supportmanagement.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.service.JobService;

import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class JobResourceFailuresTest {

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
	void getJobNotFound() {
		when(jobServiceMock.get(NAMESPACE, MUNICIPALITY_ID, JOB_ID))
			.thenThrow(Problem.valueOf(NOT_FOUND, "Job with id '%s' not found".formatted(JOB_ID)));

		webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "jobId", JOB_ID)))
			.exchange()
			.expectStatus().isNotFound()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON);
	}

	@Test
	void getJobInvalidMunicipalityId() {
		webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("municipalityId", "invalid", "namespace", NAMESPACE, "jobId", JOB_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON);
	}

	@Test
	void getJobInvalidNamespace() {
		webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "invalid namespace!", "jobId", JOB_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON);
	}
}
