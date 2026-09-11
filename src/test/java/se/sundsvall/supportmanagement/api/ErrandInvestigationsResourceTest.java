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
import se.sundsvall.supportmanagement.api.model.errand.Investigation;
import se.sundsvall.supportmanagement.api.model.errand.InvestigationSection;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.jsonschema.JsonSchemaClient;
import se.sundsvall.supportmanagement.service.ErrandInvestigationService;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService.UpsertResult;
import tools.jackson.databind.node.JsonNodeFactory;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
class ErrandInvestigationsResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/investigations";
	private static final String PATH_WITH_ID = PATH + "/{investigationId}";
	private static final String SECTION_PATH = PATH_WITH_ID + "/sections/{sectionId}";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String INVESTIGATION_ID = randomUUID().toString();
	private static final String SECTION_ID = randomUUID().toString();
	private static final String ATTACHMENT_ID = randomUUID().toString();
	private static final String KEY = "investigationData";

	private static final Map<String, String> PATH_VARIABLES = Map.of(
		"namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "investigationId", INVESTIGATION_ID, "sectionId", SECTION_ID, "attachmentId", ATTACHMENT_ID, "key", KEY);

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private ErrandInvestigationService serviceMock;

	@MockitoBean
	private JsonSchemaClient jsonSchemaClientMock;

	@Test
	void createErrandInvestigation() {

		// Arrange
		when(serviceMock.createErrandInvestigation(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(Investigation.class))).thenReturn(INVESTIGATION_ID);

		// Act
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(Investigation.create().withStatus("ACTIVE").withTitle("Investigation"))
			.exchange()
			.expectStatus().isCreated()
			.returnResult(Void.class);

		// Verify
		assertThat(response.getResponseHeaders().getLocation().getPath())
			.isEqualTo("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/investigations/" + INVESTIGATION_ID);
		verify(serviceMock).createErrandInvestigation(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(Investigation.class));
	}

	@Test
	void readErrandInvestigation() {

		// Arrange
		when(serviceMock.readErrandInvestigation(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID))
			.thenReturn(Investigation.create().withId(INVESTIGATION_ID).withVersion(2L));

		// Act & Verify
		webTestClient.get()
			.uri(builder -> builder.path(PATH_WITH_ID).build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().valueEquals("ETag", "\"2\"")
			.expectBody(Investigation.class);

		verify(serviceMock).readErrandInvestigation(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID);
	}

	@Test
	void findErrandInvestigations() {

		// Arrange
		when(serviceMock.findErrandInvestigations(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(List.of(Investigation.create().withId(INVESTIGATION_ID)));

		// Act
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Investigation.class)
			.returnResult()
			.getResponseBody();

		// Verify
		assertThat(response).hasSize(1);
		verify(serviceMock).findErrandInvestigations(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
	}

	@Test
	void updateErrandInvestigation() {

		// Arrange
		when(serviceMock.updateErrandInvestigation(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(INVESTIGATION_ID), eq("\"3\""), any(Investigation.class)))
			.thenReturn(Investigation.create().withId(INVESTIGATION_ID).withVersion(4L));

		// Act & Verify
		webTestClient.patch()
			.uri(builder -> builder.path(PATH_WITH_ID).build(PATH_VARIABLES))
			.header("If-Match", "\"3\"")
			.contentType(APPLICATION_JSON)
			.bodyValue(Investigation.create().withSummary("Summary"))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().valueEquals("ETag", "\"4\"");

		verify(serviceMock).updateErrandInvestigation(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(INVESTIGATION_ID), eq("\"3\""), any(Investigation.class));
	}

	@Test
	void deleteErrandInvestigation() {

		// Act & Verify
		webTestClient.delete()
			.uri(builder -> builder.path(PATH_WITH_ID).build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).deleteErrandInvestigation(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, null);
	}

	@Test
	void createInvestigationSection() {

		// Arrange
		when(serviceMock.createInvestigationSection(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(INVESTIGATION_ID), any(InvestigationSection.class))).thenReturn(SECTION_ID);

		// Act
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_WITH_ID + "/sections").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(InvestigationSection.create().withSectionKey("financial").withAssessment("APPROVED"))
			.exchange()
			.expectStatus().isCreated()
			.returnResult(Void.class);

		// Verify
		assertThat(response.getResponseHeaders().getLocation().getPath())
			.isEqualTo("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/investigations/" + INVESTIGATION_ID + "/sections/" + SECTION_ID);
	}

	@Test
	void findInvestigationSections() {

		// Arrange
		when(serviceMock.findInvestigationSections(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID))
			.thenReturn(List.of(InvestigationSection.create().withId(SECTION_ID)));

		// Act & Verify
		webTestClient.get()
			.uri(builder -> builder.path(PATH_WITH_ID + "/sections").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(InvestigationSection.class);

		verify(serviceMock).findInvestigationSections(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID);
	}

	@Test
	void readInvestigationSection() {

		// Arrange
		when(serviceMock.readInvestigationSection(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID))
			.thenReturn(InvestigationSection.create().withId(SECTION_ID));

		// Act & Verify
		webTestClient.get()
			.uri(builder -> builder.path(SECTION_PATH).build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBody(InvestigationSection.class);

		verify(serviceMock).readInvestigationSection(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID);
	}

	@Test
	void updateInvestigationSection() {

		// Arrange
		when(serviceMock.updateInvestigationSection(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(INVESTIGATION_ID), eq(SECTION_ID), any(InvestigationSection.class)))
			.thenReturn(InvestigationSection.create().withId(SECTION_ID).withAssessment("DEFICIENCY"));

		// Act & Verify
		webTestClient.patch()
			.uri(builder -> builder.path(SECTION_PATH).build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(InvestigationSection.create().withAssessment("DEFICIENCY"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(InvestigationSection.class);

		verify(serviceMock).updateInvestigationSection(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(INVESTIGATION_ID), eq(SECTION_ID), any(InvestigationSection.class));
	}

	@Test
	void deleteInvestigationSection() {

		// Act & Verify
		webTestClient.delete()
			.uri(builder -> builder.path(SECTION_PATH).build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).deleteInvestigationSection(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID);
	}

	@Test
	void createInvestigationAttachment() {

		// Arrange
		when(serviceMock.createInvestigationAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(INVESTIGATION_ID), any(MultipartFile.class), eq(1)))
			.thenReturn(ATTACHMENT_ID);

		final var body = new MultipartBodyBuilder();
		body.part("attachment", "content".getBytes()).filename("utredning.pdf");

		// Act
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_WITH_ID + "/attachments").queryParam("sortOrder", 1).build(PATH_VARIABLES))
			.contentType(MULTIPART_FORM_DATA)
			.body(BodyInserters.fromMultipartData(body.build()))
			.exchange()
			.expectStatus().isCreated()
			.returnResult(Void.class);

		// Verify
		assertThat(response.getResponseHeaders().getLocation().getPath())
			.isEqualTo("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/attachments/" + ATTACHMENT_ID);
	}

	@Test
	void linkInvestigationAttachment() {

		// Arrange
		when(serviceMock.linkInvestigationAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(INVESTIGATION_ID), eq(ATTACHMENT_ID), any(ArtefactAttachmentLink.class)))
			.thenReturn(ArtefactAttachment.create().withAttachmentId(ATTACHMENT_ID));

		// Act & Verify
		webTestClient.post()
			.uri(builder -> builder.path(PATH_WITH_ID + "/attachments/{attachmentId}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(ArtefactAttachmentLink.create().withSortOrder(1))
			.exchange()
			.expectStatus().isCreated()
			.expectBody(ArtefactAttachment.class);

		verify(serviceMock).linkInvestigationAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(INVESTIGATION_ID), eq(ATTACHMENT_ID), any(ArtefactAttachmentLink.class));
	}

	@Test
	void updateInvestigationAttachment() {

		// Arrange
		when(serviceMock.updateInvestigationAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(INVESTIGATION_ID), eq(ATTACHMENT_ID), any(ArtefactAttachmentLink.class)))
			.thenReturn(ArtefactAttachment.create().withAttachmentId(ATTACHMENT_ID).withSortOrder(1));

		// Act & Verify
		webTestClient.patch()
			.uri(builder -> builder.path(PATH_WITH_ID + "/attachments/{attachmentId}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(ArtefactAttachmentLink.create().withSortOrder(1))
			.exchange()
			.expectStatus().isOk()
			.expectBody(ArtefactAttachment.class);

		verify(serviceMock).updateInvestigationAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(INVESTIGATION_ID), eq(ATTACHMENT_ID), any(ArtefactAttachmentLink.class));
	}

	@Test
	void unlinkInvestigationAttachment() {

		// Act & Verify
		webTestClient.delete()
			.uri(builder -> builder.path(PATH_WITH_ID + "/attachments/{attachmentId}").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).unlinkInvestigationAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, ATTACHMENT_ID);
	}

	@Test
	void readInvestigationJsonParameters() {

		// Arrange
		when(serviceMock.readInvestigationJsonParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID)).thenReturn(List.of(JsonParameter.create().withKey(KEY)));

		// Act & Verify
		webTestClient.get()
			.uri(builder -> builder.path(PATH_WITH_ID + "/json-parameters").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(JsonParameter.class);

		verify(serviceMock).readInvestigationJsonParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID);
	}

	@Test
	void readInvestigationJsonParameter() {

		// Arrange
		when(serviceMock.readInvestigationJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, KEY))
			.thenReturn(JsonParameter.create().withKey(KEY).withVersion(1L));

		// Act & Verify
		webTestClient.get()
			.uri(builder -> builder.path(PATH_WITH_ID + "/json-parameters/{key}").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().valueEquals("ETag", "\"1\"");

		verify(serviceMock).readInvestigationJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, KEY);
	}

	@Test
	void updateInvestigationJsonParameterCreated() {

		// Arrange
		final var created = JsonParameter.create().withKey(KEY).withSchemaId("test-schema-1.0").withValue(JsonNodeFactory.instance.objectNode()).withVersion(0L);
		when(serviceMock.updateInvestigationJsonParameter(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(INVESTIGATION_ID), any(), argThat(parameter -> KEY.equals(parameter.getKey()))))
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
	void updateInvestigationJsonParameterReplaced() {

		// Arrange
		final var replaced = JsonParameter.create().withKey(KEY).withSchemaId("test-schema-1.0").withValue(JsonNodeFactory.instance.objectNode()).withVersion(3L);
		when(serviceMock.updateInvestigationJsonParameter(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(INVESTIGATION_ID), any(), argThat(parameter -> KEY.equals(parameter.getKey()))))
			.thenReturn(new UpsertResult(replaced, false));

		// Act & Verify
		webTestClient.put()
			.uri(builder -> builder.path(PATH_WITH_ID + "/json-parameters/{key}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(replaced)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().valueEquals("ETag", "\"3\"");
	}

	@Test
	void updateInvestigationJsonParameterWithMismatchingKey() {

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
	void deleteInvestigationJsonParameter() {

		// Act & Verify
		webTestClient.delete()
			.uri(builder -> builder.path(PATH_WITH_ID + "/json-parameters/{key}").build(PATH_VARIABLES))
			.header(IF_MATCH, "\"3\"")
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).deleteInvestigationJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, KEY, "\"3\"");
	}

	@Test
	void readSectionJsonParameters() {

		// Arrange
		when(serviceMock.readSectionJsonParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID)).thenReturn(List.of(JsonParameter.create().withKey(KEY)));

		// Act & Verify
		webTestClient.get()
			.uri(builder -> builder.path(SECTION_PATH + "/json-parameters").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(JsonParameter.class);

		verify(serviceMock).readSectionJsonParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID);
	}

	@Test
	void readSectionJsonParameter() {

		// Arrange
		when(serviceMock.readSectionJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID, KEY))
			.thenReturn(JsonParameter.create().withKey(KEY).withVersion(6L));

		// Act & Verify
		webTestClient.get()
			.uri(builder -> builder.path(SECTION_PATH + "/json-parameters/{key}").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().valueEquals("ETag", "\"6\"");

		verify(serviceMock).readSectionJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID, KEY);
	}

	@Test
	void updateSectionJsonParameterCreated() {

		// Arrange
		final var created = JsonParameter.create().withKey(KEY).withSchemaId("test-schema-1.0").withValue(JsonNodeFactory.instance.objectNode()).withVersion(0L);
		when(serviceMock.updateSectionJsonParameter(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(INVESTIGATION_ID), eq(SECTION_ID), any(), argThat(parameter -> KEY.equals(parameter.getKey()))))
			.thenReturn(new UpsertResult(created, true));

		// Act & Verify
		webTestClient.put()
			.uri(builder -> builder.path(SECTION_PATH + "/json-parameters/{key}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(created)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().valueEquals("ETag", "\"0\"");
	}

	@Test
	void updateSectionJsonParameterReplaced() {

		// Arrange
		final var replaced = JsonParameter.create().withKey(KEY).withSchemaId("test-schema-1.0").withValue(JsonNodeFactory.instance.objectNode()).withVersion(9L);
		when(serviceMock.updateSectionJsonParameter(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(INVESTIGATION_ID), eq(SECTION_ID), any(), argThat(parameter -> KEY.equals(parameter.getKey()))))
			.thenReturn(new UpsertResult(replaced, false));

		// Act & Verify
		webTestClient.put()
			.uri(builder -> builder.path(SECTION_PATH + "/json-parameters/{key}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(replaced)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().valueEquals("ETag", "\"9\"");
	}

	@Test
	void updateSectionJsonParameterWithMismatchingKey() {

		// Act & Verify
		webTestClient.put()
			.uri(builder -> builder.path(SECTION_PATH + "/json-parameters/{key}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(JsonParameter.create().withKey("somethingElse").withSchemaId("test-schema-1.0").withValue(JsonNodeFactory.instance.objectNode()))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteSectionJsonParameter() {

		// Act & Verify
		webTestClient.delete()
			.uri(builder -> builder.path(SECTION_PATH + "/json-parameters/{key}").build(PATH_VARIABLES))
			.header(IF_MATCH, "\"3\"")
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).deleteSectionJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID, KEY, "\"3\"");
	}
}
