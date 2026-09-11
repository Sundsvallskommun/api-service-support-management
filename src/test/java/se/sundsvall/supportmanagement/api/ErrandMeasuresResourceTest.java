package se.sundsvall.supportmanagement.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachment;
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachmentLink;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import se.sundsvall.supportmanagement.integration.jsonschema.JsonSchemaClient;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService.UpsertResult;
import se.sundsvall.supportmanagement.service.ErrandMeasureService;
import tools.jackson.databind.node.JsonNodeFactory;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.IF_MATCH;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

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
	private static final String ATTACHMENT_ID = randomUUID().toString();
	private static final String KEY = "measureData";

	private static final Map<String, String> PATH_VARIABLES = Map.of(
		"namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "measureId", MEASURE_ID, "attachmentId", ATTACHMENT_ID, "key", KEY);

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private ErrandMeasureService serviceMock;

	@MockitoBean
	private JsonSchemaClient jsonSchemaClientMock;

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

	@Test
	void createMeasureAttachment() {

		// Arrange
		when(serviceMock.createMeasureAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(MEASURE_ID), any(MultipartFile.class), eq(1)))
			.thenReturn(ATTACHMENT_ID);

		final var body = new MultipartBodyBuilder();
		body.part("attachment", "content".getBytes()).filename("protokoll.pdf");

		// Act
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_WITH_ID + "/attachments").queryParam("sortOrder", 1).build(PATH_VARIABLES))
			.contentType(MULTIPART_FORM_DATA)
			.body(BodyInserters.fromMultipartData(body.build()))
			.exchange()
			.expectStatus().isCreated()
			.returnResult(Void.class);

		// Verify - the location names the attachment resource of the errand, since that is where the file is read
		assertThat(response.getResponseHeaders().getLocation().getPath())
			.isEqualTo("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/attachments/" + ATTACHMENT_ID);
	}

	@Test
	void linkMeasureAttachment() {

		// Arrange
		when(serviceMock.linkMeasureAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(MEASURE_ID), eq(ATTACHMENT_ID), any(ArtefactAttachmentLink.class)))
			.thenReturn(ArtefactAttachment.create().withAttachmentId(ATTACHMENT_ID));

		// Act & Verify
		webTestClient.post()
			.uri(builder -> builder.path(PATH_WITH_ID + "/attachments/{attachmentId}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(ArtefactAttachmentLink.create().withSortOrder(1))
			.exchange()
			.expectStatus().isCreated()
			.expectBody(ArtefactAttachment.class);

		verify(serviceMock).linkMeasureAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(MEASURE_ID), eq(ATTACHMENT_ID), any(ArtefactAttachmentLink.class));
	}

	@Test
	void updateMeasureAttachment() {

		// Arrange
		when(serviceMock.updateMeasureAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(MEASURE_ID), eq(ATTACHMENT_ID), any(ArtefactAttachmentLink.class)))
			.thenReturn(ArtefactAttachment.create().withAttachmentId(ATTACHMENT_ID).withSortOrder(3));

		// Act & Verify
		webTestClient.patch()
			.uri(builder -> builder.path(PATH_WITH_ID + "/attachments/{attachmentId}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(ArtefactAttachmentLink.create().withSortOrder(3))
			.exchange()
			.expectStatus().isOk()
			.expectBody(ArtefactAttachment.class);

		verify(serviceMock).updateMeasureAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(MEASURE_ID), eq(ATTACHMENT_ID), any(ArtefactAttachmentLink.class));
	}

	@Test
	void unlinkMeasureAttachment() {

		// Act & Verify
		webTestClient.delete()
			.uri(builder -> builder.path(PATH_WITH_ID + "/attachments/{attachmentId}").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).unlinkMeasureAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, ATTACHMENT_ID);
	}

	@Test
	void readMeasureJsonParameters() {

		// Arrange
		when(serviceMock.readMeasureJsonParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID)).thenReturn(List.of(JsonParameter.create().withKey(KEY)));

		// Act & Verify
		webTestClient.get()
			.uri(builder -> builder.path(PATH_WITH_ID + "/json-parameters").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(JsonParameter.class);

		verify(serviceMock).readMeasureJsonParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID);
	}

	@Test
	void readMeasureJsonParameter() {

		// Arrange
		when(serviceMock.readMeasureJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, KEY)).thenReturn(JsonParameter.create().withKey(KEY).withVersion(2L));

		// Act & Verify
		webTestClient.get()
			.uri(builder -> builder.path(PATH_WITH_ID + "/json-parameters/{key}").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().valueEquals("ETag", "\"2\"");

		verify(serviceMock).readMeasureJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, KEY);
	}

	@Test
	void updateMeasureJsonParameterCreated() {

		// Arrange
		final var created = JsonParameter.create().withKey(KEY).withSchemaId("test-schema-1.0").withValue(JsonNodeFactory.instance.objectNode()).withVersion(0L);
		when(serviceMock.updateMeasureJsonParameter(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(MEASURE_ID), eq(KEY), any(), any(JsonParameter.class)))
			.thenReturn(new UpsertResult(created, true));

		// Act & Verify
		webTestClient.put()
			.uri(builder -> builder.path(PATH_WITH_ID + "/json-parameters/{key}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(created)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().valueEquals("ETag", "\"0\"");
	}

	@Test
	void updateMeasureJsonParameterReplaced() {

		// Arrange
		final var replaced = JsonParameter.create().withKey(KEY).withSchemaId("test-schema-1.0").withValue(JsonNodeFactory.instance.objectNode()).withVersion(8L);
		when(serviceMock.updateMeasureJsonParameter(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(MEASURE_ID), eq(KEY), any(), any(JsonParameter.class)))
			.thenReturn(new UpsertResult(replaced, false));

		// Act & Verify
		webTestClient.put()
			.uri(builder -> builder.path(PATH_WITH_ID + "/json-parameters/{key}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(replaced)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().valueEquals("ETag", "\"8\"");
	}

	@Test
	void updateMeasureJsonParameterWithMismatchingKey() {

		// Act & Verify
		webTestClient.put()
			.uri(builder -> builder.path(PATH_WITH_ID + "/json-parameters/{key}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(JsonParameter.create().withKey("somethingElse").withSchemaId("test-schema-1.0").withValue(JsonNodeFactory.instance.objectNode()))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteMeasureJsonParameter() {

		// Act & Verify
		webTestClient.delete()
			.uri(builder -> builder.path(PATH_WITH_ID + "/json-parameters/{key}").build(PATH_VARIABLES))
			.header(IF_MATCH, "\"3\"")
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).deleteMeasureJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, KEY, "\"3\"");
	}
}
