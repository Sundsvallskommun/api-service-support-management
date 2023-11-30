package se.sundsvall.supportmanagement.api;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.message.Message;
import se.sundsvall.supportmanagement.service.MessageService;


@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MessageResourceTest {

	private static final String NAMESPACE = "namespace";

	private static final String PATH = "/" + NAMESPACE + "/{municipalityId}/errands/{id}/messages";

	private static final String MUNICIPALITY_ID = "2281";

	private static final String ERRAND_ID = randomUUID().toString();

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private MessageService messageServiceMock;

	@Test
	void getMessagesOnErrand() {

		// Mock
		when(messageServiceMock.readMessages(anyString(), anyString(), anyString()))
			.thenReturn(List.of(Message.create()));

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(Message.class)
			.returnResult();

		// Verification
		assertThat(response).isNotNull();
		assertThat(response.getResponseBody()).isNotNull().hasSize(1);
		verify(messageServiceMock).readMessages(anyString(), anyString(), anyString());
	}

	@Test
	void setViewedStatusForMessage() {
		// Parameter values
		final var messageID = randomUUID().toString();
		final var isViewed = true;

		// Call
		webTestClient.put()
			.uri(builder -> builder.path(PATH + "/{messageID}/viewed/{isViewed}").build(Map.of("municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID, "messageID", messageID, "isViewed", isViewed)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		// Verification
		verify(messageServiceMock).updateViewedStatus(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, messageID, isViewed);
	}


}
