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
import se.sundsvall.supportmanagement.service.CommunicationService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandCommunicationAttachmentFailuresResourceTest {

	private static final String NAMESPACE = "name.space";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String PATH = "/{municipalityId}/{namespace}/communication/attachments/{attachmentId}/streamed";
	private static final String INVALID = "#invalid#";

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private CommunicationService messageServiceMock;

	@Test
	void getMessageAttachmentStreamedWithInvalidAttachmentID() {

		// Call
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "attachmentId", INVALID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody()
			.returnResult();

		verifyNoInteractions(messageServiceMock);
	}
}
