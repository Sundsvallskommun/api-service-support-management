package se.sundsvall.supportmanagement.api;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.errand.Customer;
import se.sundsvall.supportmanagement.api.model.errand.CustomerType;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ExternalTag;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.ErrandService;
import se.sundsvall.supportmanagement.service.TagService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandsResourceTest {

	private static final String PATH = "/{namespace}/{municipalityId}/errands";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = UUID.randomUUID().toString();

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private TagService tagServiceMock;

	@MockBean
	private ErrandService errandServiceMock;

	@LocalServerPort
	private int port;

	@BeforeEach
	void setupMock() {
		when(tagServiceMock.findAllCategoryTags()).thenReturn(List.of("CATEGORY_1", "CATEGORY_2"));
		when(tagServiceMock.findAllStatusTags()).thenReturn(List.of("STATUS_1", "STATUS_2"));
		when(tagServiceMock.findAllTypeTags()).thenReturn(List.of("TYPE_1", "TYPE_2"));
	}

	@Test
	void createErrand() {
		// Parameter values
		final var errandInstance = createErrandInstance("reporterUserId", true);

		// Mock
		when(errandServiceMock.createErrand(NAMESPACE, MUNICIPALITY_ID, errandInstance)).thenReturn(ERRAND_ID);

		// Call
		webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(errandInstance)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("http://localhost:".concat(String.valueOf(port)).concat(fromPath(PATH + "/{id}")
				.build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)).toString()))
			.expectBody().isEmpty();

		// Verification
		verify(tagServiceMock).findAllCategoryTags();
		verify(tagServiceMock).findAllStatusTags();
		verify(tagServiceMock).findAllTypeTags();
		verify(errandServiceMock).createErrand(NAMESPACE, MUNICIPALITY_ID, errandInstance);
	}

	@Test
	void readErrand() {
		// Parameter values
		final var errand = Errand.create().withId(ERRAND_ID);

		// Mock
		when(errandServiceMock.readErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(errand);

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(Errand.class)
			.returnResult()
			.getResponseBody();

		// Verification
		verify(errandServiceMock).readErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
		assertThat(response).isNotNull().isEqualTo(errand);
	}

	@Test
	void findErrandsWithNoFilter() {
		// Parameter values
		final var pageable = PageRequest.of(0, 20);
		final var matches = new RestResponsePage<>(List.of(Errand.create()), pageable, 1);

		// Mock
		when(errandServiceMock.findErrands(NAMESPACE, MUNICIPALITY_ID, null, pageable)).thenReturn(matches);

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.returnResult(new ParameterizedTypeReference<RestResponsePage<Errand>>() {
			})
			.getResponseBody()
			.blockFirst();

		// Verification
		verify(errandServiceMock).findErrands(NAMESPACE, MUNICIPALITY_ID, null, pageable);
		assertThat(response).isNotNull();
		assertThat(response.getContent()).hasSize(1);
	}

	@Test
	void findErrandsWithFilter() {
		// Parameter values
		final var page = 13;
		final var size = 37;
		final var pageable = PageRequest.of(page, size);
		final var matches = new PageImpl<>(List.of(Errand.create()), pageable, 1);
		final var filter = "categoryTag:'SUPPORT_CASE' and reporterUserId:'joe01doe'";

		// Mock
		when(errandServiceMock.findErrands(eq(NAMESPACE), eq(MUNICIPALITY_ID), ArgumentMatchers.<Specification<ErrandEntity>>any(), eq(pageable))).thenReturn(matches);

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH)
				.queryParam("filter", filter)
				.queryParam("page", page)
				.queryParam("size", size)
				.build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.returnResult(new ParameterizedTypeReference<RestResponsePage<Errand>>() {
			})
			.getResponseBody()
			.blockFirst();

		// Verification
		verify(errandServiceMock).findErrands(eq(NAMESPACE), eq(MUNICIPALITY_ID), ArgumentMatchers.<Specification<ErrandEntity>>any(), eq(pageable));
		assertThat(response).isNotNull();
		assertThat(response.getContent()).hasSize(1);
	}

	@Test
	void findErrandsWithDateFilter() {
		// Parameter values
		final var page = 13;
		final var size = 37;
		final var pageable = PageRequest.of(page, size);
		final var matches = new PageImpl<>(List.of(Errand.create()), pageable, 1);
		final var dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		final var from = OffsetDateTime.now().minusMinutes(1).format(dateFormat);
		final var to = OffsetDateTime.now().format(dateFormat);
		final var filter = "created > '" + from + "' and created < '" + to + "'";

		// Mock
		when(errandServiceMock.findErrands(eq(NAMESPACE), eq(MUNICIPALITY_ID), ArgumentMatchers.<Specification<ErrandEntity>>any(), eq(pageable))).thenReturn(matches);

		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(PATH)
				.queryParam("filter", filter)
				.queryParam("page", page)
			.queryParam("size", size)
			.build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.returnResult(new ParameterizedTypeReference<RestResponsePage<Errand>>() {
			})
			.getResponseBody()
			.blockFirst();

		// Verification
		verify(errandServiceMock).findErrands(eq(NAMESPACE), eq(MUNICIPALITY_ID), ArgumentMatchers.<Specification<ErrandEntity>>any(), eq(pageable));
		assertThat(response).isNotNull();
		assertThat(response.getContent()).hasSize(1);
	}

	@Test
	void updateErrandFullRequest() {
		// Parameter values
		final var errandInstance = createErrandInstance(null, true);
		final var updatedInstance = Errand.create().withId(ERRAND_ID);

		// Mock
		when(errandServiceMock.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, errandInstance)).thenReturn(updatedInstance);

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(errandInstance)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(Errand.class)
			.returnResult()
			.getResponseBody();

		// Verification
		verify(errandServiceMock).updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, errandInstance);
		assertThat(response).isEqualTo(updatedInstance);
	}

	@Test
	void updateErrandEmptyRequest() {
		// Parameter values
		final var emptyInstance = Errand.create();
		final var updatedInstance = Errand.create().withId(ERRAND_ID);

		// Mock
		when(errandServiceMock.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, emptyInstance)).thenReturn(updatedInstance);

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(Errand.create())
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(Errand.class)
			.returnResult()
			.getResponseBody();

		// Verification
		verify(errandServiceMock).updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, emptyInstance);
		assertThat(response).isEqualTo(updatedInstance);
	}

	@Test
	void updateErrandWithoutCustomer() {
		// Parameter values
		final var errandInstance = createErrandInstance(null, false);
		final var updatedInstance = Errand.create().withId(ERRAND_ID);

		// Mock
		when(errandServiceMock.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, errandInstance)).thenReturn(updatedInstance);

		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(errandInstance)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(Errand.class)
			.returnResult()
			.getResponseBody();

		// Verification
		verify(errandServiceMock).updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, errandInstance);
		assertThat(response).isEqualTo(updatedInstance);
	}

	@Test
	void deleteErrand() {
		webTestClient.delete()
			.uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		verify(errandServiceMock).deleteErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
	}

	private static Errand createErrandInstance(final String reporterUserId, final boolean withCustomer) {
		return Errand.create()
			.withAssignedGroupId("assignedGroupId")
			.withAssignedUserId("assignedUserId")
			.withCategoryTag("category_1")
			.withCustomer(withCustomer ? Customer.create().withId(randomUUID().toString()).withType(CustomerType.ENTERPRISE) : null)
			.withExternalTags(List.of(ExternalTag.create().withKey("externalTagKey").withValue("externalTagValue")))
			.withPriority(Priority.HIGH)
			.withReporterUserId(reporterUserId)
			.withStatusTag("status_2")
			.withTitle("title")
			.withTypeTag("type_1");
	}

	// Helper implementation of Page
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class RestResponsePage<T> extends PageImpl<T> {
		private static final long serialVersionUID = -7361702892303169935L;

		@JsonCreator(mode = PROPERTIES)
		public RestResponsePage(@JsonProperty("content") final List<T> content, @JsonProperty("number") final int number, @JsonProperty("size") final int size,
			@JsonProperty("totalElements") final Long totalElements, @JsonProperty("pageable") final JsonNode pageable, @JsonProperty("last") final boolean last,
			@JsonProperty("totalPages") final int totalPages, @JsonProperty("sort") final JsonNode sort, @JsonProperty("first") final boolean first,
			@JsonProperty("numberOfElements") final int numberOfElements) {
			super(content, PageRequest.of(number, size), totalElements);
		}

		public RestResponsePage(final List<T> content, final Pageable pageable, final long total) {
			super(content, pageable, total);
		}

		public RestResponsePage(final List<T> content) {
			super(content);
		}

		public RestResponsePage() {
			super(new ArrayList<>());
		}
	}
}
