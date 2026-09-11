package se.sundsvall.supportmanagement.api;

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
import se.sundsvall.supportmanagement.api.model.errand.Statement;
import se.sundsvall.supportmanagement.integration.jsonschema.JsonSchemaClient;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService.UpsertResult;
import se.sundsvall.supportmanagement.service.ErrandStatementService;
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
class ErrandStatementsResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/statements";
	private static final String PATH_WITH_ID = PATH + "/{statementId}";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String STATEMENT_ID = randomUUID().toString();
	private static final String ATTACHMENT_ID = randomUUID().toString();
	private static final String KEY = "responseForm";

	private static final Map<String, String> PATH_VARIABLES = Map.of(
		"namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "statementId", STATEMENT_ID, "attachmentId", ATTACHMENT_ID, "key", KEY);

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private ErrandStatementService serviceMock;

	@MockitoBean
	private JsonSchemaClient jsonSchemaClientMock;

	@Test
	void createErrandStatement() {

		// Arrange
		final var statement = Statement.create().withStatus("DRAFT").withCounterpartyName("Miljokontoret");
		when(serviceMock.createErrandStatement(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(Statement.class))).thenReturn(STATEMENT_ID);

		// Act
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(statement)
			.exchange()
			.expectStatus().isCreated()
			.returnResult(Void.class);

		// Verify
		verify(serviceMock).createErrandStatement(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(Statement.class));
		assertThat(response.getResponseHeaders().getLocation().getPath())
			.isEqualTo("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/statements/" + STATEMENT_ID);
	}

	@Test
	void readErrandStatement() {

		// Arrange
		when(serviceMock.readErrandStatement(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID))
			.thenReturn(Statement.create().withId(STATEMENT_ID).withVersion(3L));

		// Act
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_WITH_ID).build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().valueEquals("ETag", "\"3\"")
			.expectBody(Statement.class)
			.returnResult()
			.getResponseBody();

		// Verify
		assertThat(response.getId()).isEqualTo(STATEMENT_ID);
		verify(serviceMock).readErrandStatement(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID);
	}

	/**
	 * A statement that has never been written carries no version, and then there is no ETag to hand out either.
	 */
	@Test
	void readErrandStatementWithoutVersion() {

		// Arrange
		when(serviceMock.readErrandStatement(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID)).thenReturn(Statement.create().withId(STATEMENT_ID));

		// Act & Verify
		webTestClient.get()
			.uri(builder -> builder.path(PATH_WITH_ID).build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().doesNotExist("ETag");
	}

	@Test
	void findErrandStatements() {

		// Arrange
		when(serviceMock.findErrandStatements(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(List.of(Statement.create().withId(STATEMENT_ID)));

		// Act
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Statement.class)
			.returnResult()
			.getResponseBody();

		// Verify
		assertThat(response).hasSize(1);
		verify(serviceMock).findErrandStatements(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
	}

	@Test
	void updateErrandStatement() {

		// Arrange
		when(serviceMock.updateErrandStatement(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(STATEMENT_ID), eq("\"2\""), any(Statement.class)))
			.thenReturn(Statement.create().withId(STATEMENT_ID).withVersion(3L));

		// Act & Verify
		webTestClient.patch()
			.uri(builder -> builder.path(PATH_WITH_ID).build(PATH_VARIABLES))
			.header("If-Match", "\"2\"")
			.contentType(APPLICATION_JSON)
			.bodyValue(Statement.create().withTitle("title"))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().valueEquals("ETag", "\"3\"");

		verify(serviceMock).updateErrandStatement(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(STATEMENT_ID), eq("\"2\""), any(Statement.class));
	}

	@Test
	void deleteErrandStatement() {

		// Act & Verify
		webTestClient.delete()
			.uri(builder -> builder.path(PATH_WITH_ID).build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).deleteErrandStatement(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID, null);
	}

	@Test
	void createStatementAttachment() {

		// Arrange
		when(serviceMock.createStatementAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(STATEMENT_ID), any(MultipartFile.class), eq(1)))
			.thenReturn(ATTACHMENT_ID);

		final var body = new MultipartBodyBuilder();
		body.part("attachment", "content".getBytes()).filename("yttrande.txt");

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
	void linkStatementAttachment() {

		// Arrange
		when(serviceMock.linkStatementAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(STATEMENT_ID), eq(ATTACHMENT_ID), any(ArtefactAttachmentLink.class)))
			.thenReturn(ArtefactAttachment.create().withAttachmentId(ATTACHMENT_ID));

		// Act & Verify
		webTestClient.post()
			.uri(builder -> builder.path(PATH_WITH_ID + "/attachments/{attachmentId}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(ArtefactAttachmentLink.create().withSortOrder(1))
			.exchange()
			.expectStatus().isCreated()
			.expectBody(ArtefactAttachment.class);

		verify(serviceMock).linkStatementAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(STATEMENT_ID), eq(ATTACHMENT_ID), any(ArtefactAttachmentLink.class));
	}

	@Test
	void updateStatementAttachment() {

		// Arrange
		when(serviceMock.updateStatementAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(STATEMENT_ID), eq(ATTACHMENT_ID), any(ArtefactAttachmentLink.class)))
			.thenReturn(ArtefactAttachment.create().withAttachmentId(ATTACHMENT_ID).withSortOrder(1));

		// Act & Verify
		webTestClient.patch()
			.uri(builder -> builder.path(PATH_WITH_ID + "/attachments/{attachmentId}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(ArtefactAttachmentLink.create().withSortOrder(1))
			.exchange()
			.expectStatus().isOk()
			.expectBody(ArtefactAttachment.class);

		verify(serviceMock).updateStatementAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(STATEMENT_ID), eq(ATTACHMENT_ID), any(ArtefactAttachmentLink.class));
	}

	@Test
	void unlinkStatementAttachment() {

		// Act & Verify
		webTestClient.delete()
			.uri(builder -> builder.path(PATH_WITH_ID + "/attachments/{attachmentId}").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).unlinkStatementAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID, ATTACHMENT_ID);
	}

	@Test
	void readStatementJsonParameters() {

		// Arrange
		when(serviceMock.readStatementJsonParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID)).thenReturn(List.of(JsonParameter.create().withKey(KEY)));

		// Act & Verify
		webTestClient.get()
			.uri(builder -> builder.path(PATH_WITH_ID + "/json-parameters").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(JsonParameter.class);

		verify(serviceMock).readStatementJsonParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID);
	}

	@Test
	void readStatementJsonParameter() {

		// Arrange
		when(serviceMock.readStatementJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID, KEY))
			.thenReturn(JsonParameter.create().withKey(KEY).withVersion(1L));

		// Act & Verify
		webTestClient.get()
			.uri(builder -> builder.path(PATH_WITH_ID + "/json-parameters/{key}").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().valueEquals("ETag", "\"1\"");

		verify(serviceMock).readStatementJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID, KEY);
	}

	@Test
	void updateStatementJsonParameterCreated() {

		// Arrange
		final var created = JsonParameter.create().withKey(KEY).withSchemaId("test-schema-1.0").withValue(JsonNodeFactory.instance.objectNode()).withVersion(0L);
		when(serviceMock.updateStatementJsonParameter(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(STATEMENT_ID), eq(KEY), any(), any(JsonParameter.class)))
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
	void updateStatementJsonParameterReplaced() {

		// Arrange
		final var replaced = JsonParameter.create().withKey(KEY).withSchemaId("test-schema-1.0").withValue(JsonNodeFactory.instance.objectNode()).withVersion(2L);
		when(serviceMock.updateStatementJsonParameter(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(STATEMENT_ID), eq(KEY), any(), any(JsonParameter.class)))
			.thenReturn(new UpsertResult(replaced, false));

		// Act & Verify
		webTestClient.put()
			.uri(builder -> builder.path(PATH_WITH_ID + "/json-parameters/{key}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(replaced)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().valueEquals("ETag", "\"2\"");
	}

	/**
	 * The key is mandatory in the body even though the path carries it too - {@code JsonParameter.key} is
	 * {@code @NotBlank},
	 * so a body that omits it never reaches the check that compares the two.
	 */
	@Test
	void updateStatementJsonParameterWithoutAKeyInTheBody() {

		// Act & Verify
		webTestClient.put()
			.uri(builder -> builder.path(PATH_WITH_ID + "/json-parameters/{key}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(JsonParameter.create().withSchemaId("test-schema-1.0").withValue(JsonNodeFactory.instance.objectNode()))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	/**
	 * A key in the body that disagrees with the one in the path is a mistake worth naming rather than silently picking
	 * one of.
	 */
	@Test
	void updateStatementJsonParameterWithMismatchingKey() {

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
	void deleteStatementJsonParameter() {

		// Act & Verify
		webTestClient.delete()
			.uri(builder -> builder.path(PATH_WITH_ID + "/json-parameters/{key}").build(PATH_VARIABLES))
			.header(IF_MATCH, "\"3\"")
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).deleteStatementJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID, KEY, "\"3\"");
	}
}
