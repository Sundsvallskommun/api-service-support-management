package se.sundsvall.supportmanagement.api;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import jakarta.servlet.http.HttpServletResponse;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.service.CommunicationService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandCommunicationAttachmentResourceTest {

	private final static String PATH = "/communication/attachments/{attachmentID}/streamed";

	private final static String ATTACHMENT_ID = randomUUID().toString();

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private CommunicationService messageServiceMock;

	@Test
	void getMessageAttachmentStreamed() {

		// ACT
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.build(Map.of("attachmentID", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.returnResult();

		verify(messageServiceMock).getMessageAttachmentStreamed(any(String.class), any(HttpServletResponse.class));
	}

}
