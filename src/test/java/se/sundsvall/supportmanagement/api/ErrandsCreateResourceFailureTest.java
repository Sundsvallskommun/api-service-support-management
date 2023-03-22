package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

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
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ExternalTag;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.errand.Stakeholder;
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.service.ErrandService;
import se.sundsvall.supportmanagement.service.MetadataService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandsCreateResourceFailureTest {

	private static final String PATH = "/{namespace}/{municipalityId}/errands";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String INVALID = "#invalid#";

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private ErrandService errandServiceMock;

	@MockBean
	private MetadataService metadataServiceMock;

	@BeforeEach
	void setupMock() {
		when(metadataServiceMock.findCategories(any(), any())).thenReturn(List.of(Category.create().withName("CATEGORY_1"), Category.create().withName("CATEGORY_2")));
		when(metadataServiceMock.findStatuses(any(), any())).thenReturn(List.of(Status.create().withName("STATUS_1"), Status.create().withName("STATUS_2")));
		when(metadataServiceMock.findTypes(any(), any(), any())).thenReturn(List.of(Type.create().withName("TYPE_1"), Type.create().withName("TYPE_2")));
	}

	@Test
	void createErrandWithInvalidNamespace() {
		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(createErrandInstance().withId(null).withCreated(null).withModified(null))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("createErrand.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		// Verification
		verify(metadataServiceMock).findCategories(any(), any());
		verify(metadataServiceMock).findStatuses(any(), any());
		verify(metadataServiceMock).findTypes(any(), any(), any());
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithInvalidMunicipalityId() {
		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(createErrandInstance().withId(null).withCreated(null).withModified(null))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("createErrand.municipalityId", "not a valid municipality ID"));

		// Verification
		verify(metadataServiceMock).findCategories(any(), any());
		verify(metadataServiceMock).findStatuses(any(), any());
		verify(metadataServiceMock).findTypes(any(), any(), any());
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithNullErrandInstance() {
		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
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
			Required request body is missing: public org.springframework.http.ResponseEntity<java.lang.Void>\s\
			se.sundsvall.supportmanagement.api.ErrandsResource.createErrand(org.springframework.web.util.UriComponentsBuilder,\
			java.lang.String,java.lang.String,se.sundsvall.supportmanagement.api.model.errand.Errand)""");

		// Verification
		verifyNoInteractions(metadataServiceMock, errandServiceMock);
	}

	@Test
	void createErrandWithFullErrandInstance() {
		// Parameter values
		final var request = createErrandInstance();

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("createErrand.errand.id", "must be null"),
			tuple("createErrand.errand.created", "must be null"),
			tuple("createErrand.errand.modified", "must be null"));

		// Verification
		verify(metadataServiceMock).findCategories(any(), any());
		verify(metadataServiceMock).findStatuses(any(), any());
		verify(metadataServiceMock).findTypes(any(), any(), any());
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithEmptyErrandInstance() {
		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
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
			tuple("createErrand.errand.categoryTag", "must not be blank"),
			tuple("createErrand.errand.reporterUserId", "must not be blank"),
			tuple("createErrand.errand.priority", "must not be null"),
			tuple("createErrand.errand.statusTag", "must not be blank"),
			tuple("createErrand.errand.title", "must not be blank"),
			tuple("createErrand.errand.typeTag", "must not be blank"));

		// Verification
		verify(metadataServiceMock).findCategories(any(), any());
		verify(metadataServiceMock).findStatuses(any(), any());
		verify(metadataServiceMock).findTypes(any(), any(), any());
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithBlankErrandInstance() {
		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(Errand.create().withTitle(" ").withReporterUserId(" ").withCategoryTag(" ").withTypeTag(" ").withStatusTag(" "))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("createErrand.errand.title", "must not be blank"),
			tuple("createErrand.errand.reporterUserId", "must not be blank"),
			tuple("createErrand.errand.priority", "must not be null"),
			tuple("createErrand.errand.categoryTag", "must not be blank"),
			tuple("createErrand.errand.typeTag", "must not be blank"),
			tuple("createErrand.errand.statusTag", "must not be blank"));

		// Verification
		verify(metadataServiceMock).findCategories(any(), any());
		verify(metadataServiceMock).findStatuses(any(), any());
		verify(metadataServiceMock).findTypes(any(), any(), any());
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithNullExternalKeyAndValue() {
		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
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
		verify(metadataServiceMock).findCategories(any(), any());
		verify(metadataServiceMock).findStatuses(any(), any());
		verify(metadataServiceMock).findTypes(any(), any(), any());
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithNonUniqueExternalTagKeys() {
		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(Errand.create().withExternalTags(List.of(
				ExternalTag.create().withKey("key").withValue("value"),
				ExternalTag.create().withKey("key").withValue("other value"))))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactly(
			tuple("externalTags", "keys in the collection must be unique"));

		// Verification
		verify(metadataServiceMock).findCategories(any(), any());
		verify(metadataServiceMock).findStatuses(any(), any());
		verify(metadataServiceMock).findTypes(any(), any(), any());
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithInvalidTags() {
		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(createErrandInstance().withCategoryTag("invalid_category").withStatusTag("invalid_status").withTypeTag("invalid_type"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("categoryTag", "value 'invalid_category' doesn't match any of [CATEGORY_1, CATEGORY_2]"),
			tuple("statusTag", "value 'invalid_status' doesn't match any of [STATUS_1, STATUS_2]"),
			tuple("typeTag", "value 'invalid_type' doesn't match any of [TYPE_1, TYPE_2]"));

		// Verification
		verify(metadataServiceMock).findCategories(any(), any());
		verify(metadataServiceMock).findStatuses(any(), any());
		verify(metadataServiceMock).findTypes(any(), any(), any());
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithInvalidEscalationEmail() {
		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(createErrandInstance().withId(null).withCreated(null).withModified(null).withEscalationEmail("invalid"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("escalationEmail", "must be a well-formed email address"));

		// Verification
		verify(metadataServiceMock).findCategories(any(), any());
		verify(metadataServiceMock).findStatuses(any(), any());
		verify(metadataServiceMock).findTypes(any(), any(), any());
		verifyNoInteractions(errandServiceMock);
	}

	private static Errand createErrandInstance() {
		return Errand.create()
			.withAssignedGroupId("assignedGroupId")
			.withAssignedUserId("assignedUserId")
			.withStakeholders(List.of(Stakeholder.create().withExternalId("id").withExternalIdTypeTag("EMPLOYEE")))
			.withCategoryTag("category_1")
			.withCreated(OffsetDateTime.now())
			.withExternalTags(List.of(ExternalTag.create().withKey("externalTagKey").withValue("externalTagValue")))
			.withId("id")
			.withModified(OffsetDateTime.now())
			.withPriority(Priority.HIGH)
			.withReporterUserId("reporterUserId")
			.withStatusTag("status_1")
			.withTitle("title")
			.withTypeTag("TYPE_2");
	}
}
