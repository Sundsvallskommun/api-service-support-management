package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.flywaydb.core.internal.util.StringUtils.rightPad;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
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
import se.sundsvall.supportmanagement.api.model.errand.Classification;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ExternalTag;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.errand.Stakeholder;
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.ContactReason;
import se.sundsvall.supportmanagement.api.model.metadata.Role;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.service.ErrandService;
import se.sundsvall.supportmanagement.service.MetadataService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandsCreateResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/errands";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String INVALID = "#invalid#";

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private ErrandService errandServiceMock;

	@MockBean
	private MetadataService metadataServiceMock;

	private static Errand createErrandInstance() {
		return Errand.create()
			.withId(null)
			.withModified(null)
			.withCreated(null)
			.withAssignedGroupId("assignedGroupId")
			.withAssignedUserId("assignedUserId")
			.withStakeholders(List.of(Stakeholder.create().withExternalId("id").withExternalIdType("EMPLOYEE").withRole("ROLE_1")))
			.withClassification(Classification.create().withCategory("category_1").withType("TYPE_2"))
			.withExternalTags(List.of(ExternalTag.create().withKey("externalTagKey").withValue("externalTagValue")))
			.withPriority(Priority.HIGH)
			.withReporterUserId("reporterUserId")
			.withStatus("status_1")
			.withTitle("title")
			.withBusinessRelated(true)
			.withContactReason("contactReason");
	}

	@BeforeEach
	void setupMock() {
		when(metadataServiceMock.isValidated(any(), any(), any())).thenReturn(true);
		when(metadataServiceMock.findContactReasons(any(), any())).thenReturn(List.of(ContactReason.create().withReason("contactReason")));
		when(metadataServiceMock.findCategories(any(), any())).thenReturn(List.of(Category.create().withName("CATEGORY_1"), Category.create().withName("CATEGORY_2")));
		when(metadataServiceMock.findStatuses(any(), any())).thenReturn(List.of(Status.create().withName("STATUS_1"), Status.create().withName("STATUS_2")));
		when(metadataServiceMock.findTypes(any(), any(), any())).thenReturn(List.of(Type.create().withName("TYPE_1"), Type.create().withName("TYPE_2")));
		when(metadataServiceMock.findRoles(any(), any())).thenReturn(List.of(Role.create().withName("ROLE_1")));
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
		verify(metadataServiceMock, times(3)).isValidated(any(), any(), any());
		verify(metadataServiceMock).findCategories(any(), any());
		verify(metadataServiceMock).findStatuses(any(), any());
		verify(metadataServiceMock).findTypes(any(), any(), any());
		verify(metadataServiceMock).findRoles(INVALID, MUNICIPALITY_ID);
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
		verify(metadataServiceMock, times(3)).isValidated(any(), any(), any());
		verify(metadataServiceMock).findCategories(any(), any());
		verify(metadataServiceMock).findStatuses(any(), any());
		verify(metadataServiceMock).findTypes(any(), any(), any());
		verify(metadataServiceMock).findRoles(NAMESPACE, INVALID);
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
			Required request body is missing: org.springframework.http.ResponseEntity<java.lang.Void> \
			se.sundsvall.supportmanagement.api.ErrandsResource.createErrand(java.lang.String,java.lang.String,se.sundsvall.supportmanagement.api.model.errand.Errand)""");

		// Verification
		verifyNoInteractions(metadataServiceMock, errandServiceMock);
	}

	@Test
	void createErrandWithFullErrandInstance() {
		// Parameter values
		final var request = createErrandInstance().withId("not-null").withCreated(OffsetDateTime.now()).withModified(OffsetDateTime.now());

		when(metadataServiceMock.isValidated(any(), any(), any())).thenReturn(true);

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
		verify(metadataServiceMock, times(3)).isValidated(any(), any(), any());
		verify(metadataServiceMock).findCategories(any(), any());
		verify(metadataServiceMock).findStatuses(any(), any());
		verify(metadataServiceMock).findTypes(any(), any(), any());
		verify(metadataServiceMock).findRoles(NAMESPACE, MUNICIPALITY_ID);
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
			tuple("createErrand.errand.classification", "must not be null"),
			tuple("createErrand.errand.reporterUserId", "must not be blank"),
			tuple("createErrand.errand.priority", "must not be null"),
			tuple("createErrand.errand.status", "must not be blank"),
			tuple("createErrand.errand.title", "must not be blank"));

		// Verification
		verify(metadataServiceMock).findStatuses(any(), any());
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithBlankErrandInstance() {
		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(Errand.create().withTitle(" ").withReporterUserId(" ").withClassification(Classification.create().withCategory(" ").withType(" ")).withStatus(" ").withContactReason(" ").withBusinessRelated(null))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("createErrand.errand.classification.category", "must not be blank"),
			tuple("createErrand.errand.classification.type", "must not be blank"),
			tuple("createErrand.errand.title", "must not be blank"),
			tuple("createErrand.errand.reporterUserId", "must not be blank"),
			tuple("createErrand.errand.priority", "must not be null"),
			tuple("createErrand.errand.status", "must not be blank"),
			tuple("createErrand.errand.contactReason", "not a valid contact reason"));

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
		verify(metadataServiceMock).findStatuses(any(), any());
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithNonUniqueExternalTagKeys() {

		final var errand = createErrandInstance().withExternalTags(List.of(
			ExternalTag.create().withKey("key").withValue("value"),
			ExternalTag.create().withKey("key").withValue("other value")));

		System.out.println(errand.getId());
		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(errand)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactly(
			tuple("createErrand.errand.externalTags", "keys in the collection must be unique"));

		// Verification
		verify(metadataServiceMock).findStatuses(any(), any());
		verify(metadataServiceMock).findRoles(NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithInvalidTags() {
		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(createErrandInstance().withClassification(Classification.create().withCategory("invalid_category").withType("invalid_type")).withStatus("invalid_status"))
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
		verify(metadataServiceMock).findRoles(NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithInvalidType() {
		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(createErrandInstance().withClassification(Classification.create().withCategory("CATEGORY_1").withType("invalid_type")).withStatus("STATUS_1"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("classification", "value 'invalid_type' doesn't match any of [TYPE_1, TYPE_2]"));

		// Verification
		verify(metadataServiceMock).findCategories(any(), any());
		verify(metadataServiceMock).findStatuses(any(), any());
		verify(metadataServiceMock).findTypes(any(), any(), any());
		verify(metadataServiceMock).findRoles(NAMESPACE, MUNICIPALITY_ID);
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
		verify(metadataServiceMock).findStatuses(any(), any());
		verify(metadataServiceMock).findRoles(NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithTooLongChannel() {
		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(createErrandInstance().withChannel(rightPad("Test", 260, 'X')))
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
			.containsExactly(tuple("channel", "size must be between 0 and 255"));

		// Verification
		verify(metadataServiceMock).findStatuses(any(), any());
		verify(metadataServiceMock).findRoles(NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithTooLongContactReasonDescription() {
		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(createErrandInstance().withContactReasonDescription(rightPad("Test", 4097, 'X')))
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
			.containsExactly(tuple("contactReasonDescription", "size must be between 0 and 4096"));

		// Verification
		verify(metadataServiceMock).findStatuses(NAMESPACE, MUNICIPALITY_ID);
		verify(metadataServiceMock).findRoles(NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithEmptyParameterKey() {
		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(createErrandInstance().withId(null).withCreated(null).withModified(null)
				.withParameters(List.of(Parameter.create().withValues(List.of("value1", "value2")))))
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
			.containsExactly(tuple("parameters[0].key", "must not be blank"));

		// Verification
		verify(metadataServiceMock, times(3)).isValidated(any(), any(), any());
		verify(metadataServiceMock).findCategories(NAMESPACE, MUNICIPALITY_ID);
		verify(metadataServiceMock).findStatuses(NAMESPACE, MUNICIPALITY_ID);
		verify(metadataServiceMock).findTypes(eq(NAMESPACE), eq(MUNICIPALITY_ID), any());
		verify(metadataServiceMock).findRoles(NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithInvalidStakeholderRole() {
		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(createErrandInstance().withId(null).withCreated(null).withModified(null).withStakeholders(List.of(Stakeholder.create().withRole("INVALID"))))
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
			.containsExactly(tuple("stakeholders[0].role", "value 'INVALID' doesn't match any of [ROLE_1]"));

		// Verification
		verify(metadataServiceMock, times(3)).isValidated(any(), any(), any());
		verify(metadataServiceMock).findCategories(NAMESPACE, MUNICIPALITY_ID);
		verify(metadataServiceMock).findStatuses(NAMESPACE, MUNICIPALITY_ID);
		verify(metadataServiceMock).findTypes(eq(NAMESPACE), eq(MUNICIPALITY_ID), any());
		verify(metadataServiceMock).findRoles(NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithEmptyStakeholderParameterKey() {
		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(createErrandInstance().withId(null).withCreated(null).withModified(null)
				.withStakeholders(List.of(
					Stakeholder.create().withParameters(List.of(
						Parameter.create().withValues(List.of("value1", "value2")))))))
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
			.containsExactly(tuple("stakeholders[0].parameters[0].key", "must not be blank"));

		// Verification
		verify(metadataServiceMock, times(3)).isValidated(any(), any(), any());
		verify(metadataServiceMock).findCategories(NAMESPACE, MUNICIPALITY_ID);
		verify(metadataServiceMock).findStatuses(NAMESPACE, MUNICIPALITY_ID);
		verify(metadataServiceMock).findTypes(eq(NAMESPACE), eq(MUNICIPALITY_ID), any());
		verify(metadataServiceMock).findRoles(NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(errandServiceMock);
	}
}
