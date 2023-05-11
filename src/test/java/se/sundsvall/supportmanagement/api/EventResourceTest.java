package se.sundsvall.supportmanagement.api;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.event.Event;
import se.sundsvall.supportmanagement.service.EventService;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("junit")
class EventResourceTest {

	private static final String PATH = "/errands/{id}/events";

	@MockBean
	private EventService eventServiceMock;

	@Captor
	private ArgumentCaptor<Pageable> pageableCaptor;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void testAPIDocumentationClass() {
		assertThat(new EventResource.PagedEvent()).isInstanceOf(Page.class);
	}

	@Test
	void getErrandEventsWithDefaultPageSettings() {
		// Parameter values
		final var id = UUID.randomUUID().toString();

		// Mock
		when(eventServiceMock.readEvents(eq(id), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(Event.create())));

		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(PATH).build(Map.of("id", id)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(new ParameterizedTypeReference<RestResponsePage<Event>>() {})
			.returnResult()
			.getResponseBody();

		// Verification
		verify(eventServiceMock).readEvents(eq(id), pageableCaptor.capture());

		assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
		assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.unsorted());
		assertThat(response).isNotNull();
		assertThat(response.getContent()).hasSize(1);
		assertThat(response.getTotalElements()).isEqualTo(1);
	}

	@Test
	void getErrandEventsWithCustomPageSettings() {
		// Parameter values
		final var id = UUID.randomUUID().toString();

		// Mock
		when(eventServiceMock.readEvents(eq(id), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(Event.create(), Event.create())));

		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(PATH)
			.queryParam("page", "10")
			.queryParam("size", "5")
			.queryParam("sort", "created,desc")
			.build(Map.of("id", id)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(new ParameterizedTypeReference<RestResponsePage<Event>>() {})
			.returnResult()
			.getResponseBody();

		// Verification
		verify(eventServiceMock).readEvents(eq(id), pageableCaptor.capture());
		assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(10);
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
		assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by(Direction.DESC, "created"));
		assertThat(response).isNotNull();
		assertThat(response.getContent()).hasSize(2);
		assertThat(response.getTotalElements()).isEqualTo(2);
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
