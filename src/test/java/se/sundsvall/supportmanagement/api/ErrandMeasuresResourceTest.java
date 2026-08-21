package se.sundsvall.supportmanagement.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import se.sundsvall.supportmanagement.service.ErrandMeasureService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandMeasuresResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/measures";
	private static final String PATH_WITH_ID = "/{municipalityId}/{namespace}/errands/{errandId}/measures/{measureId}";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String MEASURE_ID = randomUUID().toString();

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private ErrandMeasureService serviceMock;

	@Test
	void createErrandMeasure() {

		// Arrange
		final var measure = new Measure()
			.withType("INTERVENTION")
			.withResponsibleUser("jo12doe")
			.withAddedByUser("jo12doe")
			.withAddedByRole("MANAGER")
			.withGoal("Improve response time");

		when(serviceMock.createErrandMeasure(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(Measure.class))).thenReturn(MEASURE_ID);

		// Act
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(measure)
			.exchange()
			.expectStatus().isCreated()
			.returnResult(Void.class);

		// Verify
		verify(serviceMock).createErrandMeasure(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(Measure.class));
		assertThat(response.getResponseHeaders().getLocation()).isNotNull();
		assertThat(response.getResponseHeaders().getLocation().getPath()).isEqualTo("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/measures/" + MEASURE_ID);
	}

	@Test
	void readErrandMeasure() {

		// Arrange
		final var measure = new Measure()
			.withId(MEASURE_ID)
			.withType("INTERVENTION")
			.withResponsibleUser("jo12doe")
			.withCreated(OffsetDateTime.now())
			.withModified(OffsetDateTime.now());

		when(serviceMock.readErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID)).thenReturn(measure);

		// Act
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_WITH_ID).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "measureId", MEASURE_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(Measure.class)
			.returnResult()
			.getResponseBody();

		// Verify
		verify(serviceMock).readErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID);
		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo(MEASURE_ID);
	}

	@Test
	void findErrandMeasures() {

		// Arrange
		final var measures = List.of(
			new Measure().withId(MEASURE_ID).withType("INTERVENTION"),
			new Measure().withId(randomUUID().toString()).withType("SUPPORT"));

		when(serviceMock.findErrandMeasures(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(measures);

		// Act
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(Measure.class)
			.returnResult()
			.getResponseBody();

		// Verify
		verify(serviceMock).findErrandMeasures(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
		assertThat(response).hasSize(2);
	}

	@Test
	void updateErrandMeasure() {

		// Arrange
		final var measure = new Measure()
			.withType("UPDATED_TYPE")
			.withGoal("Updated goal");

		final var updatedMeasure = new Measure()
			.withId(MEASURE_ID)
			.withType("UPDATED_TYPE")
			.withGoal("Updated goal");

		when(serviceMock.updateErrandMeasure(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(MEASURE_ID), any(Measure.class))).thenReturn(updatedMeasure);

		// Act
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH_WITH_ID).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "measureId", MEASURE_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(measure)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(Measure.class)
			.returnResult()
			.getResponseBody();

		// Verify
		verify(serviceMock).updateErrandMeasure(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(MEASURE_ID), any(Measure.class));
		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo(MEASURE_ID);
	}

	@Test
	void deleteErrandMeasure() {

		// Act
		webTestClient.delete()
			.uri(builder -> builder.path(PATH_WITH_ID).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "measureId", MEASURE_ID)))
			.exchange()
			.expectStatus().isNoContent();

		// Verify
		verify(serviceMock).deleteErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID);
	}
}
