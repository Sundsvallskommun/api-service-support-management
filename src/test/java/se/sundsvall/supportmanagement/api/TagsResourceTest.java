package se.sundsvall.supportmanagement.api;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.tag.TagsResponse;
import se.sundsvall.supportmanagement.service.TagService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class TagsResourceTest {

	@MockBean
	private TagService tagServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@LocalServerPort
	private int port;

	@Test
	void getTags() {

		// Setup
		final var tagsResponse = TagsResponse.create()
			.withCategoryTags(List.of("Category-1"))
			.withStatusTags(List.of("Status-1"))
			.withTypeTags(List.of("Type-1"));

		when(tagServiceMock.findAllTags()).thenReturn(tagsResponse);

		// Call
		final var response = webTestClient.get().uri("tags/")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(TagsResponse.class)
			.returnResult().getResponseBody();

		// Verification
		assertThat(response).isEqualTo(tagsResponse);

		verify(tagServiceMock).findAllTags();
	}

	@Test
	void getStatusTags() {

		webTestClient.get().uri("tags/statusTags")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(String[].class)
			.isEqualTo(EMPTY_STRING_ARRAY);

		verify(tagServiceMock).findAllStatusTags();
	}

	@Test
	void getCatgoryTags() {

		webTestClient.get().uri("tags/categoryTags")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(String[].class)
			.isEqualTo(EMPTY_STRING_ARRAY);

		verify(tagServiceMock).findAllCategoryTags();
	}

	@Test
	void getTypeTags() {

		webTestClient.get().uri("tags/typeTags")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(String[].class)
			.isEqualTo(EMPTY_STRING_ARRAY);

		verify(tagServiceMock).findAllTypeTags();
	}
}
