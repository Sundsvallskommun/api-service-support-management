package se.sundsvall.supportmanagement.api;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.service.MessageService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MessageResourceFailuresTest {

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	private static final String ERRAND_ID = randomUUID().toString();

	private static final String MESSAGE_ID = randomUUID().toString();

	private static final boolean IS_VIEWED = true;


	private static final String PATH = "/{namespace}/{municipalityId}/errands/{id}/messages";

	private static final String INVALID = "#invalid#";


	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private MessageService messageServiceMock;

	@Test
	void getMessagesOnErrandWithInvalidNamespace() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody()
			.returnResult();

		verifyNoInteractions(messageServiceMock);
	}

	@Test
	void getMessagesOnErrandWithInvalidMunicipalityId() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "id", ERRAND_ID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody()
			.returnResult();

		verifyNoInteractions(messageServiceMock);

	}

	@Test
	void getMessagesOnErrandWithInvalidErrandId() {


		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", INVALID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody()
			.returnResult();
		// Verification

		verifyNoInteractions(messageServiceMock);

	}

	@Test
	void setViewedStatusForMessageWithInvalidNamespace() {

		// Call
		webTestClient.put()
			.uri(builder -> builder.path(PATH + "/{messageID}/viewed/{isViewed}").build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID, "messageID", MESSAGE_ID, "isViewed", IS_VIEWED)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody()
			.returnResult();

		// Verification
		verifyNoInteractions(messageServiceMock);
	}

	@Test
	void setViewedStatusForMessageWithInvalidMunicipalityId() {

		// Call
		webTestClient.put()
			.uri(builder -> builder.path(PATH + "/{messageID}/viewed/{isViewed}").build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "id", ERRAND_ID, "messageID", MESSAGE_ID, "isViewed", IS_VIEWED)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody()
			.returnResult();

		// Verification
		verifyNoInteractions(messageServiceMock);
	}

	@Test
	void setViewedStatusForMessageWithInvalidErrandId() {

		// Call
		webTestClient.put()
			.uri(builder -> builder.path(PATH + "/{messageID}/viewed/{isViewed}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", INVALID, "messageID", MESSAGE_ID, "isViewed", IS_VIEWED)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody()
			.returnResult();

		// Verification
		verifyNoInteractions(messageServiceMock);
	}

	@Test
	void setViewedStatusForMessageWithInvalidMessageId() {

		// Call
		webTestClient.put()
			.uri(builder -> builder.path(PATH + "/{messageID}/viewed/{isViewed}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID, "messageID", INVALID, "isViewed", IS_VIEWED)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody()
			.returnResult();

		// Verification
		verifyNoInteractions(messageServiceMock);
	}

	@Test
	void setViewedStatusForMessageWithInvalidIsViewed() {

		// Call
		webTestClient.put()
			.uri(builder -> builder.path(PATH + "/{messageID}/viewed/{isViewed}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID, "messageID", MESSAGE_ID, "isViewed", INVALID)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody()
			.returnResult();

		// Verification
		verifyNoInteractions(messageServiceMock);
	}

}
