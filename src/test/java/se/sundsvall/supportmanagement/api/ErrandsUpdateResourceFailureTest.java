package se.sundsvall.supportmanagement.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.errand.Classification;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ExternalTag;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.errand.Stakeholder;
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.service.ErrandService;
import se.sundsvall.supportmanagement.service.MetadataService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.problem.Status.BAD_REQUEST;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandsUpdateResourceFailureTest {

	private static final String PATH = "/{namespace}/{municipalityId}/errands";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = UUID.randomUUID().toString();
	private static final String INVALID = "#invalid#";

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private ErrandService errandServiceMock;

	@MockBean
	private MetadataService metadataServiceMock;

	@BeforeEach
	void setupMock() {
		when(metadataServiceMock.isValidated(any(), any(), any())).thenReturn(true);
		when(metadataServiceMock.findCategories(any(), any())).thenReturn(List.of(Category.create().withName("CATEGORY_1"), Category.create().withName("CATEGORY_2")));
		when(metadataServiceMock.findStatuses(any(), any())).thenReturn(List.of(Status.create().withName("STATUS_1"), Status.create().withName("STATUS_2")));
		when(metadataServiceMock.findTypes(any(), any(), any())).thenReturn(List.of(Type.create().withName("TYPE_1"), Type.create().withName("TYPE_2")));
	}

	@Test
	void updateErrandWithInvalidNamespace() {
		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(Errand.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("updateErrand.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		// Verification
		verify(metadataServiceMock).findStatuses(any(), any());
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void updateErrandWithInvalidMunicipalityId() {
		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(Errand.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("updateErrand.municipalityId", "not a valid municipality ID"));

		// Verification
		verify(metadataServiceMock).findStatuses(any(), any());
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void updateErrandWithInvalidErrandId() {
		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", INVALID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(Errand.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("updateErrand.id", "not a valid UUID"));

		// Verification
		verify(metadataServiceMock).findStatuses(any(), any());
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void updateErrandWithNullBody() {

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Bad Request");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getDetail()).isEqualTo("""
			Required request body is missing: public org.springframework.http.ResponseEntity<se.sundsvall.supportmanagement.api.model.errand.Errand>\s\
			se.sundsvall.supportmanagement.api.ErrandsResource.updateErrand(java.lang.String,java.lang.String,java.lang.String,\
			se.sundsvall.supportmanagement.api.model.errand.Errand)""");

		// Verification
		verifyNoInteractions(metadataServiceMock, errandServiceMock);
	}

	@Test
	void updateErrandWithFullErrandInstance() {

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(createErrandInstance())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("updateErrand.errand.id", "must be null"),
			tuple("updateErrand.errand.created", "must be null"),
			tuple("updateErrand.errand.modified", "must be null"),
			tuple("updateErrand.errand.reporterUserId", "must be null"));

		// Verification
		verify(metadataServiceMock).findCategories(any(), any());
		verify(metadataServiceMock).findStatuses(any(), any());
		verify(metadataServiceMock).findTypes(any(), any(), any());
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void updateErrandWithBlankErrandInstance() {

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(Errand.create().withTitle(" ").withReporterUserId(" ").withClassification(Classification.create().withCategory(" ").withType(" ")).withStatus(" "))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("updateErrand.errand.classification.category", "must not be blank"),
			tuple("updateErrand.errand.classification.type", "must not be blank"),
			tuple("updateErrand.errand.reporterUserId", "must be null"));

		// Verification
		verify(metadataServiceMock).findCategories(any(), any());
		verify(metadataServiceMock).findStatuses(any(), any());
		verify(metadataServiceMock).findTypes(any(), any(), any());
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void updateErrandWithNullExternalKeyAndValue() {

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(Errand.create().withExternalTags(List.of(
				ExternalTag.create(),
				ExternalTag.create().withKey(" ").withValue(" "))))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("externalTags[0].key", "must not be blank"),
			tuple("externalTags[0].value", "must not be blank"),
			tuple("externalTags[1].key", "must not be blank"),
			tuple("externalTags[1].value", "must not be blank"));

		// Verification
		verify(metadataServiceMock).findStatuses(any(), any());
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void updateErrandWithInvalidTags() {

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(Errand.create().withClassification(Classification.create().withCategory("invalid_category").withType("invalid_type")).withStatus("invalid_status"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("classification", "value 'invalid_category' doesn't match any of [CATEGORY_1, CATEGORY_2]"),
			tuple("status", "value 'invalid_status' doesn't match any of [STATUS_1, STATUS_2]"));

		// Verification
		verify(metadataServiceMock).findCategories(any(), any());
		verify(metadataServiceMock).findStatuses(any(), any());
		verifyNoInteractions(errandServiceMock);
	}

	private static Errand createErrandInstance() {
		return Errand.create()
			.withAssignedGroupId("assignedGroupId")
			.withAssignedUserId("assignedUserId")
			.withStakeholders(List.of(Stakeholder.create().withExternalId("id").withExternalIdType("EMPLOYEE")))
			.withClassification(Classification.create().withCategory("CATEGORY_2").withType("type_1"))
			.withCreated(OffsetDateTime.now())
			.withExternalTags(List.of(ExternalTag.create().withKey("externalTagKey").withValue("externalTagValue")))
			.withId("id")
			.withModified(OffsetDateTime.now())
			.withPriority(Priority.HIGH)
			.withReporterUserId("reporterUserId")
			.withStatus("STATUS_2")
			.withTitle("title");
	}
}
