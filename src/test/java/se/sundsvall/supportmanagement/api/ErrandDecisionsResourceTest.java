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
import se.sundsvall.supportmanagement.api.model.errand.Decision;
import se.sundsvall.supportmanagement.api.model.errand.DecisionTerm;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.jsonschema.JsonSchemaClient;
import se.sundsvall.supportmanagement.service.ErrandDecisionService;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService.UpsertResult;
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
class ErrandDecisionsResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/decisions";
	private static final String PATH_WITH_ID = PATH + "/{decisionId}";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String DECISION_ID = randomUUID().toString();
	private static final String TERM_ID = randomUUID().toString();
	private static final String ATTACHMENT_ID = randomUUID().toString();
	private static final String KEY = "decisionData";

	private static final Map<String, String> PATH_VARIABLES = Map.of(
		"namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "decisionId", DECISION_ID, "termId", TERM_ID, "attachmentId", ATTACHMENT_ID, "key", KEY);

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private ErrandDecisionService serviceMock;

	@MockitoBean
	private JsonSchemaClient jsonSchemaClientMock;

	private static Decision validDecision() {
		return Decision.create()
			.withStatus("COMPLETED")
			.withOutcome("APPROVAL")
			.withMethod("MANUAL")
			.withDecidedBy("joe01doe")
			.withDecidedAt(OffsetDateTime.now());
	}

	@Test
	void createErrandDecision() {

		// Arrange
		when(serviceMock.createErrandDecision(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(Decision.class))).thenReturn(DECISION_ID);

		// Act
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(validDecision())
			.exchange()
			.expectStatus().isCreated()
			.returnResult(Void.class);

		// Verify
		assertThat(response.getResponseHeaders().getLocation().getPath())
			.isEqualTo("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/decisions/" + DECISION_ID);
		verify(serviceMock).createErrandDecision(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(Decision.class));
	}

	@Test
	void readErrandDecision() {

		// Arrange
		when(serviceMock.readErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID)).thenReturn(Decision.create().withId(DECISION_ID).withVersion(4L));

		// Act & Verify
		webTestClient.get()
			.uri(builder -> builder.path(PATH_WITH_ID).build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().valueEquals("ETag", "\"4\"")
			.expectBody(Decision.class);

		verify(serviceMock).readErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID);
	}

	@Test
	void findErrandDecisions() {

		// Arrange
		when(serviceMock.findErrandDecisions(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(List.of(Decision.create().withId(DECISION_ID)));

		// Act
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Decision.class)
			.returnResult()
			.getResponseBody();

		// Verify
		assertThat(response).hasSize(1);
		verify(serviceMock).findErrandDecisions(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
	}

	@Test
	void updateErrandDecision() {

		// Arrange
		when(serviceMock.updateErrandDecision(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(DECISION_ID), eq("\"1\""), any(Decision.class)))
			.thenReturn(Decision.create().withId(DECISION_ID).withVersion(2L));

		// Act & Verify
		webTestClient.patch()
			.uri(builder -> builder.path(PATH_WITH_ID).build(PATH_VARIABLES))
			.header("If-Match", "\"1\"")
			.contentType(APPLICATION_JSON)
			.bodyValue(Decision.create().withJustification("Justification"))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().valueEquals("ETag", "\"2\"");

		verify(serviceMock).updateErrandDecision(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(DECISION_ID), eq("\"1\""), any(Decision.class));
	}

	@Test
	void deleteErrandDecision() {

		// Act & Verify
		webTestClient.delete()
			.uri(builder -> builder.path(PATH_WITH_ID).build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).deleteErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, null);
	}

	@Test
	void createDecisionTerm() {

		// Arrange
		when(serviceMock.createDecisionTerm(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(DECISION_ID), any(DecisionTerm.class))).thenReturn(TERM_ID);

		// Act
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_WITH_ID + "/terms").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(DecisionTerm.create().withText("A term"))
			.exchange()
			.expectStatus().isCreated()
			.returnResult(Void.class);

		// Verify
		assertThat(response.getResponseHeaders().getLocation().getPath())
			.isEqualTo("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/decisions/" + DECISION_ID + "/terms/" + TERM_ID);
	}

	@Test
	void findDecisionTerms() {

		// Arrange
		when(serviceMock.findDecisionTerms(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID)).thenReturn(List.of(DecisionTerm.create().withId(TERM_ID)));

		// Act & Verify
		webTestClient.get()
			.uri(builder -> builder.path(PATH_WITH_ID + "/terms").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(DecisionTerm.class);

		verify(serviceMock).findDecisionTerms(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID);
	}

	@Test
	void readDecisionTerm() {

		// Arrange
		when(serviceMock.readDecisionTerm(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, TERM_ID)).thenReturn(DecisionTerm.create().withId(TERM_ID));

		// Act & Verify
		webTestClient.get()
			.uri(builder -> builder.path(PATH_WITH_ID + "/terms/{termId}").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBody(DecisionTerm.class);

		verify(serviceMock).readDecisionTerm(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, TERM_ID);
	}

	@Test
	void updateDecisionTerm() {

		// Arrange
		when(serviceMock.updateDecisionTerm(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(DECISION_ID), eq(TERM_ID), any(DecisionTerm.class)))
			.thenReturn(DecisionTerm.create().withId(TERM_ID).withText("A term"));

		// Act & Verify
		webTestClient.patch()
			.uri(builder -> builder.path(PATH_WITH_ID + "/terms/{termId}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(DecisionTerm.create().withText("A term"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(DecisionTerm.class);

		verify(serviceMock).updateDecisionTerm(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(DECISION_ID), eq(TERM_ID), any(DecisionTerm.class));
	}

	@Test
	void deleteDecisionTerm() {

		// Act & Verify
		webTestClient.delete()
			.uri(builder -> builder.path(PATH_WITH_ID + "/terms/{termId}").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).deleteDecisionTerm(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, TERM_ID);
	}

	@Test
	void createDecisionAttachment() {

		// Arrange
		when(serviceMock.createDecisionAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(DECISION_ID), any(MultipartFile.class), eq(0)))
			.thenReturn(ATTACHMENT_ID);

		final var body = new MultipartBodyBuilder();
		body.part("attachment", "content".getBytes()).filename("beslut.pdf");

		// Act
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_WITH_ID + "/attachments").queryParam("sortOrder", 0).build(PATH_VARIABLES))
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
	void linkDecisionAttachment() {

		// Arrange
		when(serviceMock.linkDecisionAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(DECISION_ID), eq(ATTACHMENT_ID), any(ArtefactAttachmentLink.class)))
			.thenReturn(ArtefactAttachment.create().withAttachmentId(ATTACHMENT_ID));

		// Act & Verify
		webTestClient.post()
			.uri(builder -> builder.path(PATH_WITH_ID + "/attachments/{attachmentId}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(ArtefactAttachmentLink.create().withSortOrder(1))
			.exchange()
			.expectStatus().isCreated()
			.expectBody(ArtefactAttachment.class);

		verify(serviceMock).linkDecisionAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(DECISION_ID), eq(ATTACHMENT_ID), any(ArtefactAttachmentLink.class));
	}

	@Test
	void updateDecisionAttachment() {

		// Arrange
		when(serviceMock.updateDecisionAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(DECISION_ID), eq(ATTACHMENT_ID), any(ArtefactAttachmentLink.class)))
			.thenReturn(ArtefactAttachment.create().withAttachmentId(ATTACHMENT_ID).withSortOrder(2));

		// Act & Verify
		webTestClient.patch()
			.uri(builder -> builder.path(PATH_WITH_ID + "/attachments/{attachmentId}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(ArtefactAttachmentLink.create().withSortOrder(2))
			.exchange()
			.expectStatus().isOk()
			.expectBody(ArtefactAttachment.class);

		verify(serviceMock).updateDecisionAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(DECISION_ID), eq(ATTACHMENT_ID), any(ArtefactAttachmentLink.class));
	}

	@Test
	void unlinkDecisionAttachment() {

		// Act & Verify
		webTestClient.delete()
			.uri(builder -> builder.path(PATH_WITH_ID + "/attachments/{attachmentId}").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).unlinkDecisionAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, ATTACHMENT_ID);
	}

	@Test
	void readDecisionJsonParameters() {

		// Arrange
		when(serviceMock.readDecisionJsonParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID)).thenReturn(List.of(JsonParameter.create().withKey(KEY)));

		// Act & Verify
		webTestClient.get()
			.uri(builder -> builder.path(PATH_WITH_ID + "/json-parameters").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(JsonParameter.class);

		verify(serviceMock).readDecisionJsonParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID);
	}

	@Test
	void readDecisionJsonParameter() {

		// Arrange
		when(serviceMock.readDecisionJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, KEY)).thenReturn(JsonParameter.create().withKey(KEY).withVersion(5L));

		// Act & Verify
		webTestClient.get()
			.uri(builder -> builder.path(PATH_WITH_ID + "/json-parameters/{key}").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().valueEquals("ETag", "\"5\"");

		verify(serviceMock).readDecisionJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, KEY);
	}

	@Test
	void updateDecisionJsonParameterCreated() {

		// Arrange
		final var created = JsonParameter.create().withKey(KEY).withSchemaId("test-schema-1.0").withValue(JsonNodeFactory.instance.objectNode()).withVersion(0L);
		when(serviceMock.updateDecisionJsonParameter(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(DECISION_ID), eq(KEY), any(), any(JsonParameter.class)))
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
	void updateDecisionJsonParameterReplaced() {

		// Arrange
		final var replaced = JsonParameter.create().withKey(KEY).withSchemaId("test-schema-1.0").withValue(JsonNodeFactory.instance.objectNode()).withVersion(7L);
		when(serviceMock.updateDecisionJsonParameter(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(DECISION_ID), eq(KEY), any(), any(JsonParameter.class)))
			.thenReturn(new UpsertResult(replaced, false));

		// Act & Verify
		webTestClient.put()
			.uri(builder -> builder.path(PATH_WITH_ID + "/json-parameters/{key}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(replaced)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().valueEquals("ETag", "\"7\"");
	}

	@Test
	void updateDecisionJsonParameterWithMismatchingKey() {

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
	void deleteDecisionJsonParameter() {

		// Act & Verify
		webTestClient.delete()
			.uri(builder -> builder.path(PATH_WITH_ID + "/json-parameters/{key}").build(PATH_VARIABLES))
			.header(IF_MATCH, "\"3\"")
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).deleteDecisionJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, KEY, "\"3\"");
	}
}
