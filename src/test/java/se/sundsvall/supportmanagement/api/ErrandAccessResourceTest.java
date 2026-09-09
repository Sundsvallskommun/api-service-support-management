package se.sundsvall.supportmanagement.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.access.ErrandAccess;
import se.sundsvall.supportmanagement.api.model.access.ErrandFieldAccess;
import se.sundsvall.supportmanagement.api.model.access.ErrandFieldKeyAccess;
import se.sundsvall.supportmanagement.api.model.access.ErrandResourceAccess;
import se.sundsvall.supportmanagement.api.model.config.AccessLevel;
import se.sundsvall.supportmanagement.service.ErrandAccessService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandAccessResourceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/access";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private ErrandAccessService errandAccessServiceMock;

	@Test
	void readErrandAccess() {

		final var errandAccess = ErrandAccess.create()
			.withLevel(AccessLevel.RW)
			.withFields(List.of(
				ErrandFieldAccess.create().withField("title"),
				ErrandFieldAccess.create().withField("parameters").withAllKeys(false)
					.withKeys(List.of(ErrandFieldKeyAccess.create().withKey("granted-key").withLevel(AccessLevel.RW)))))
			.withResources(List.of(ErrandResourceAccess.create().withResource("errand/communication").withLevel(AccessLevel.R)));

		when(errandAccessServiceMock.readErrandAccess(any(), any(), any())).thenReturn(errandAccess);

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(ErrandAccess.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(errandAccess);
		verify(errandAccessServiceMock).readErrandAccess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
		verifyNoMoreInteractions(errandAccessServiceMock);
	}
}
