package se.sundsvall.supportmanagement.api;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.service.CommunicationService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandCommunicationAttachmentResourceTest {

	private static final String NAMESPACE = "name.space";

	private static final String MUNICIPALITY_ID = "2281";

	private static final String PATH = "/{municipalityId}/{namespace}/communication/attachments/{attachmentID}/streamed";

	private static final String ATTACHMENT_ID = randomUUID().toString();

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private CommunicationService messageServiceMock;

	@Test
	void getMessageAttachmentStreamed() {

		// ACT
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "attachmentID", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.returnResult();

		verify(messageServiceMock).getMessageAttachmentStreamed(any(String.class), any(String.class), any(String.class), any(HttpServletResponse.class));
	}

}
