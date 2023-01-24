package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.errand.Customer;
import se.sundsvall.supportmanagement.api.model.errand.CustomerType;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ExternalTag;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.service.ErrandService;
import se.sundsvall.supportmanagement.service.TagService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandsCreateResourceFailureTest {

	private static final String PATH = "/errands";

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private ErrandService errandServiceMock;

	@MockBean
	private TagService tagServiceMock;

	@BeforeEach
	void setupMock() {
		when(tagServiceMock.findAllCategoryTags()).thenReturn(List.of("CATEGORY_1", "CATEGORY_2"));
		when(tagServiceMock.findAllClientIdTags()).thenReturn(List.of("CLIENT_ID_1", "CLIENT_ID_2"));
		when(tagServiceMock.findAllStatusTags()).thenReturn(List.of("STATUS_1", "STATUS_2"));
		when(tagServiceMock.findAllTypeTags()).thenReturn(List.of("TYPE_1", "TYPE_2"));
	}

	@Test
	void createErrandWithNullErrandInstance() {
		// Call
		final var response = webTestClient.post().uri(PATH)
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
			se.sundsvall.supportmanagement.api.model.errand.Errand)""");

		// Verification
		verifyNoInteractions(tagServiceMock, errandServiceMock);
	}

	@Test
	void createErrandWithFullErrandInstance() {
		// Parameter values
		final var request = createErrandInstance();

		// Call
		final var response = webTestClient.post().uri(PATH)
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
		verify(tagServiceMock).findAllCategoryTags();
		verify(tagServiceMock).findAllClientIdTags();
		verify(tagServiceMock).findAllStatusTags();
		verify(tagServiceMock).findAllTypeTags();
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithEmptyErrandInstance() {
		// Call
		final var response = webTestClient.post().uri(PATH)
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
			tuple("createErrand.errand.title", "must not be blank"),
			tuple("createErrand.errand.clientIdTag", "must not be blank"),
			tuple("createErrand.errand.reporterUserId", "must not be blank"),
			tuple("createErrand.errand.priority", "must not be null"),
			tuple("createErrand.errand.categoryTag", "must not be blank"),
			tuple("createErrand.errand.customer", "must not be null"),
			tuple("createErrand.errand.typeTag", "must not be blank"),
			tuple("createErrand.errand.statusTag", "must not be blank"));

		// Verification
		verify(tagServiceMock).findAllCategoryTags();
		verify(tagServiceMock).findAllClientIdTags();
		verify(tagServiceMock).findAllStatusTags();
		verify(tagServiceMock).findAllTypeTags();
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithBlankErrandInstance() {
		// Call
		final var response = webTestClient.post().uri(PATH)
			.contentType(APPLICATION_JSON)
			.bodyValue(Errand.create().withTitle(" ").withClientIdTag(" ").withReporterUserId(" ").withCategoryTag(" ").withTypeTag(" ").withStatusTag(" "))
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
			tuple("createErrand.errand.clientIdTag", "must not be blank"),
			tuple("createErrand.errand.reporterUserId", "must not be blank"),
			tuple("createErrand.errand.priority", "must not be null"),
			tuple("createErrand.errand.categoryTag", "must not be blank"),
			tuple("createErrand.errand.customer", "must not be null"),
			tuple("createErrand.errand.typeTag", "must not be blank"),
			tuple("createErrand.errand.statusTag", "must not be blank"));

		// Verification
		verify(tagServiceMock).findAllCategoryTags();
		verify(tagServiceMock).findAllClientIdTags();
		verify(tagServiceMock).findAllStatusTags();
		verify(tagServiceMock).findAllTypeTags();
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithNullExternalKeyAndValue() {
		// Call
		final var response = webTestClient.post().uri(PATH)
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
		verify(tagServiceMock).findAllCategoryTags();
		verify(tagServiceMock).findAllClientIdTags();
		verify(tagServiceMock).findAllStatusTags();
		verify(tagServiceMock).findAllTypeTags();
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithNonUniqueExternalTagKeys() {
		// Call
		final var response = webTestClient.post().uri(PATH)
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
		verify(tagServiceMock).findAllCategoryTags();
		verify(tagServiceMock).findAllClientIdTags();
		verify(tagServiceMock).findAllStatusTags();
		verify(tagServiceMock).findAllTypeTags();
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithEmptyCustomer() {
		// Call
		final var response = webTestClient.post().uri(PATH)
			.contentType(APPLICATION_JSON)
			.bodyValue(Errand.create().withCustomer(Customer.create()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("customer.id", "must not be blank"),
			tuple("customer.type", "must not be null"));

		// Verification
		verify(tagServiceMock).findAllCategoryTags();
		verify(tagServiceMock).findAllClientIdTags();
		verify(tagServiceMock).findAllStatusTags();
		verify(tagServiceMock).findAllTypeTags();
		verifyNoInteractions(errandServiceMock);
	}

	@ParameterizedTest
	@EnumSource(names = { "ENTERPRISE", "PRIVATE" })
	void createErrandWithInvalidCustomerId(final CustomerType type) {
		// Call
		final var response = webTestClient.post().uri(PATH)
			.contentType(APPLICATION_JSON)
			.bodyValue(Errand.create().withCustomer(Customer.create().withType(type).withId("id")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactly(
			tuple("customer", String.format("id must be a valid uuid when customer type is %s", type.name())));

		// Verification
		verify(tagServiceMock).findAllCategoryTags();
		verify(tagServiceMock).findAllClientIdTags();
		verify(tagServiceMock).findAllStatusTags();
		verify(tagServiceMock).findAllTypeTags();
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandWithInvalidTags() {
		// Call
		final var response = webTestClient.post().uri(PATH)
			.contentType(APPLICATION_JSON)
			.bodyValue(createErrandInstance().withCategoryTag("invalid_category").withClientIdTag("invalid_client_id").withStatusTag("invalid_status").withTypeTag("invalid_type"))
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
			tuple("clientIdTag", "value 'invalid_client_id' doesn't match any of [CLIENT_ID_1, CLIENT_ID_2]"),
			tuple("statusTag", "value 'invalid_status' doesn't match any of [STATUS_1, STATUS_2]"),
			tuple("typeTag", "value 'invalid_type' doesn't match any of [TYPE_1, TYPE_2]"));

		// Verification
		verify(tagServiceMock).findAllCategoryTags();
		verify(tagServiceMock).findAllClientIdTags();
		verify(tagServiceMock).findAllStatusTags();
		verify(tagServiceMock).findAllTypeTags();
		verifyNoInteractions(errandServiceMock);
	}

	private static Errand createErrandInstance() {
		return Errand.create()
			.withAssignedGroupId("assignedGroupId")
			.withAssignedUserId("assignedUserId")
			.withCustomer(Customer.create().withId("id").withType(CustomerType.EMPLOYEE))
			.withCategoryTag("category_1")
			.withCreated(OffsetDateTime.now())
			.withExternalTags(List.of(ExternalTag.create().withKey("externalTagKey").withValue("externalTagValue")))
			.withId("id")
			.withModified(OffsetDateTime.now())
			.withClientIdTag("CLIENT_ID_2")
			.withPriority(Priority.HIGH)
			.withReporterUserId("reporterUserId")
			.withStatusTag("status_1")
			.withTitle("title")
			.withTypeTag("TYPE_2");
	}
}
