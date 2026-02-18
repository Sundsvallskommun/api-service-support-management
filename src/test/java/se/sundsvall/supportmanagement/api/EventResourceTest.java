package se.sundsvall.supportmanagement.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.event.Event;
import se.sundsvall.supportmanagement.service.EventService;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class EventResourceTest {

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/events";

	@MockitoBean
	private EventService eventServiceMock;

	@Captor
	private ArgumentCaptor<Pageable> pageableCaptor;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getErrandEventsWithDefaultPageSettings() {
		// Parameter values
		final var errandId = randomUUID().toString();

		// Mock
		when(eventServiceMock.readEvents(eq(MUNICIPALITY_ID), eq(errandId), any(Pageable.class))).thenReturn(new RestResponsePage<>(List.of(Event.create()), PageRequest.of(0, 20), 1));

		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", errandId)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(new ParameterizedTypeReference<Page<Event>>() {

			})
			.returnResult()
			.getResponseBody();

		// Verification
		verify(eventServiceMock).readEvents(eq(MUNICIPALITY_ID), eq(errandId), pageableCaptor.capture());

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
		final var errandId = randomUUID().toString();

		// Mock
		when(eventServiceMock.readEvents(eq(MUNICIPALITY_ID), eq(errandId), any(Pageable.class))).thenReturn(new RestResponsePage<>(List.of(Event.create(), Event.create())));

		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(PATH)
			.queryParam("page", "10")
			.queryParam("size", "5")
			.queryParam("sort", "created,desc")
			.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", errandId)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(new ParameterizedTypeReference<Page<Event>>() {

			})
			.returnResult()
			.getResponseBody();

		// Verification
		verify(eventServiceMock).readEvents(eq(MUNICIPALITY_ID), eq(errandId), pageableCaptor.capture());
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

		@Serial
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
			super(content, PageRequest.of(0, 10), content.size());
		}

		public RestResponsePage() {
			super(new ArrayList<>());
		}
	}
}
