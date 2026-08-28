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
import se.sundsvall.supportmanagement.api.model.errand.purge.ErrandPurgeStatus;
import se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState;
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
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.COMPLETED;
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.RUNNING;
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.STOPPED;

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
			.thenReturn(status(RUNNING).withStarted(now(systemDefault())));

		// Act
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isAccepted()
			.expectBody(ErrandPurgeStatus.class)
			.returnResult();

		// Verify
		verify(serviceMock).startPurge(eq(NAMESPACE), eq(MUNICIPALITY_ID), any(ErrandPurgeRequest.class));
		verifyNoMoreInteractions(serviceMock);

		assertThat(response.getResponseHeaders().getLocation()).isNotNull();
		assertThat(response.getResponseHeaders().getLocation().getPath())
			.isEqualTo("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/purge/" + JOB_ID);
		assertThat(response.getResponseBody()).isNotNull();
		assertThat(response.getResponseBody().getJobId()).isEqualTo(JOB_ID);
		assertThat(response.getResponseBody().getState()).isEqualTo(RUNNING);
	}

	@Test
	void readPurgeStatus() {

		// Arrange
		when(serviceMock.readPurgeStatus(NAMESPACE, MUNICIPALITY_ID, JOB_ID))
			.thenReturn(status(COMPLETED).withProcessed(250).withDeleted(248).withFailed(2));

		// Act
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_WITH_ID).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "jobId", JOB_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(ErrandPurgeStatus.class)
			.returnResult()
			.getResponseBody();

		// Verify
		verify(serviceMock).readPurgeStatus(NAMESPACE, MUNICIPALITY_ID, JOB_ID);
		verifyNoMoreInteractions(serviceMock);

		assertThat(response).isNotNull();
		assertThat(response.getState()).isEqualTo(COMPLETED);
		assertThat(response.getProcessed()).isEqualTo(250);
		assertThat(response.getDeleted()).isEqualTo(248);
		assertThat(response.getFailed()).isEqualTo(2);
	}

	@Test
	void stopPurge() {

		// Arrange
		when(serviceMock.stopPurge(NAMESPACE, MUNICIPALITY_ID, JOB_ID)).thenReturn(status(STOPPED));

		// Act
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH_WITH_ID).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "jobId", JOB_ID)))
			.exchange()
			.expectStatus().isAccepted()
			.expectBody(ErrandPurgeStatus.class)
			.returnResult()
			.getResponseBody();

		// Verify
		verify(serviceMock).stopPurge(NAMESPACE, MUNICIPALITY_ID, JOB_ID);
		verifyNoMoreInteractions(serviceMock);

		assertThat(response).isNotNull();
		assertThat(response.getState()).isEqualTo(STOPPED);
	}

	private static ErrandPurgeStatus status(final PurgeState state) {
		return ErrandPurgeStatus.create()
			.withJobId(JOB_ID)
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withOlderThan(OffsetDateTime.parse("2024-08-28T00:00:00+02:00"))
			.withDryRun(true)
			.withState(state);
	}
}
