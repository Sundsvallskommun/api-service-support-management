package se.sundsvall.supportmanagement.api;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

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
class MessageAttachmentFailuresResourceTest {

	private final static String PATH = "/messageattachments/{attachmentID}/streamed";

	private static final String INVALID = "#invalid#";

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private MessageService messageServiceMock;


	@Test
	void getMessageAttachmentStreamedWithInvalidAttachmentID() {

		// Call
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.build(Map.of("attachmentID", INVALID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody()
			.returnResult();

		verifyNoInteractions(messageServiceMock);
	}

}
