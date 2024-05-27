package se.sundsvall.supportmanagement.api;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.zalando.problem.Status.NOT_FOUND;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.Problem;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.errand.TimeMeasurement;
import se.sundsvall.supportmanagement.service.TimeMeasurementService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandTimeMeasurementsResourceTest {

	private static final String PATH = "/{namespace}/{municipalityId}/errands/{errandId}/time-measure";

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	private static final String ERRAND_ID = UUID.randomUUID().toString();

	private static final String ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";


	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private TimeMeasurementService serviceMock;

	@Test
	void getErrandTimeMeasure() {

		// Arrange
		final var timeMeasurements = List.of(new TimeMeasurement()
			.withStartTime(OffsetDateTime.now())
			.withStopTime(OffsetDateTime.now().plusHours(1))
			.withDescription("description")
			.withAdministrator("administrator")
			.withStatus("SUSPENDED"));

		when(serviceMock.getErrandTimeMeasurements(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(timeMeasurements);

		// Act
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(TimeMeasurement.class)
			.returnResult()
			.getResponseBody();

		// Verify
		verify(serviceMock).getErrandTimeMeasurements(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
		assertThat(response).isNotNull().isEqualTo(timeMeasurements);
	}

	@Test
	void getErrandTimeMeasureNoTimeMeasurementsFound() {

		// Arrange
		when(serviceMock.getErrandTimeMeasurements(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(emptyList());

		// Act
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(TimeMeasurement.class)
			.returnResult()
			.getResponseBody();

		// Verify
		verify(serviceMock).getErrandTimeMeasurements(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
		assertThat(response).isEmpty();
	}

	@Test
	void getErrandTimeMeasureErrandNotFound() {

		// Arrange
		when(serviceMock.getErrandTimeMeasurements(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID))
			.thenThrow(Problem.valueOf(NOT_FOUND, String.format(ENTITY_NOT_FOUND, ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)));

		// Act
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isNotFound()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON_VALUE)
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		// Verify
		verify(serviceMock).getErrandTimeMeasurements(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
		assertThat(response).isNotNull().satisfies(p -> {
			assertThat(p.getStatus()).isEqualTo(NOT_FOUND);
			assertThat(p.getTitle()).isEqualTo("Not Found");
			assertThat(p.getDetail()).isEqualTo(String.format(ENTITY_NOT_FOUND, ERRAND_ID, NAMESPACE, MUNICIPALITY_ID));
		});

	}

}
