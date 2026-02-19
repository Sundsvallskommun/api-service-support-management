package se.sundsvall.supportmanagement.api;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.metadata.ContactReason;
import se.sundsvall.supportmanagement.service.MetadataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.zalando.problem.Status.BAD_REQUEST;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataContactReasonResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/contactreasons";

	@MockitoBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	private static Stream<Arguments> createContactReasonArgumentProvider() {
		return Stream.of(
			Arguments.of(ContactReason.create().withReason(""), "2281", "namespace", "reason", "must not be blank"),
			Arguments.of(ContactReason.create().withReason(null), "2281", "namespace", "reason", "must not be blank"),
			Arguments.of(ContactReason.create().withReason("reason"), "2281", "#not-a-valid-namespace", "createContactReason.namespace", "can only contain A-Z, a-z, 0-9, - and _"),
			Arguments.of(ContactReason.create().withReason("reason"), "not-a-valid-municipalityId", "namespace", "createContactReason.municipalityId", "not a valid municipality ID"));
	}

	@ParameterizedTest
	@MethodSource("createContactReasonArgumentProvider")
	void createContactReason(ContactReason contactReason, String namespace, String municipalityId, String field, String message) {
		// Setup
		final var response = webTestClient.post().uri(builder -> builder.path(PATH).build(namespace, municipalityId))
			.bodyValue(contactReason)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactly(tuple(field, message));

		verifyNoInteractions(metadataServiceMock);
	}
}
