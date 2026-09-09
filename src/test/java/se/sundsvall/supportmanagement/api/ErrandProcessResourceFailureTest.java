package se.sundsvall.supportmanagement.api;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcess;
import se.sundsvall.supportmanagement.api.model.process.ProcessActivity;
import se.sundsvall.supportmanagement.service.ErrandProcessService;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.RUNNING;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandProcessResourceFailureTest {

	private static final String PROCESSES_PATH = "/{municipalityId}/{namespace}/errands/{errandId}/processes";
	private static final String PROCESS_PATH = PROCESSES_PATH + "/{processInstanceId}";
	private static final String ACTIVITIES_PATH = "/{municipalityId}/{namespace}/errands/{errandId}/process-activities";
	private static final String NAMESPACE = "namespace";
	private static final String INVALID_NAMESPACE = "invalid,namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PROCESS_INSTANCE_ID = "8f1c2b6e-1f4a-4d61-9a0e-2b7c1f0a5e33";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private ErrandProcessService serviceMock;

	private static Map<String, Object> instanceVariables() {
		return instanceVariables(NAMESPACE);
	}

	private static Map<String, Object> instanceVariables(final String namespace) {
		return Map.of("namespace", namespace, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "processInstanceId", PROCESS_INSTANCE_ID);
	}

	private static Map<String, Object> errandVariables(final String namespace) {
		return Map.of("namespace", namespace, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID);
	}

	private static ErrandProcess validReport() {
		return ErrandProcess.create()
			.withProcessService("pw-alkt")
			.withProcessKey("alkt-ansokan")
			.withProcessStatus(RUNNING);
	}

	@Test
	void aReportWithoutTheFieldsIdentifyingTheProcessIsRejected() {
		final var response = webTestClient.put()
			.uri(builder -> builder.path(PROCESS_PATH).build(instanceVariables()))
			.contentType(APPLICATION_JSON)
			.bodyValue(ErrandProcess.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(
				tuple("processService", "must not be blank"),
				tuple("processKey", "must not be blank"),
				tuple("processStatus", "must not be blank"));

		verifyNoInteractions(serviceMock);
	}

	/**
	 * The batch is capped where a report is validated rather than where it is written, so a report too large to be one
	 * work step is refused before it reaches the database at all.
	 */
	@Test
	void aReportCarryingMoreThanAHundredActivitiesIsRejected() {
		final var report = validReport()
			.withActivities(IntStream.range(0, 101)
				.mapToObj(index -> ProcessActivity.create().withActivityType("PHASE").withActivityId("phase-" + index).withOccurredAt(now(systemDefault())))
				.toList());

		final var response = webTestClient.put()
			.uri(builder -> builder.path(PROCESS_PATH).build(instanceVariables()))
			.contentType(APPLICATION_JSON)
			.bodyValue(report)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("activities", "may contain at most 100 activities"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void anActivityWithoutAMomentIsRejected() {
		final var report = validReport()
			.withActivities(List.of(ProcessActivity.create().withActivityType("PHASE")));

		final var response = webTestClient.put()
			.uri(builder -> builder.path(PROCESS_PATH).build(instanceVariables()))
			.contentType(APPLICATION_JSON)
			.bodyValue(report)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("activities[0].occurredAt", "must not be null"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void anInvalidMunicipalityIsRejected() {
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PROCESSES_PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", "invalid", "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("readErrandProcesses.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void anInvalidErrandIdIsRejected() {
		final var response = webTestClient.get()
			.uri(builder -> builder.path(ACTIVITIES_PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", "invalid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("readErrandProcessActivities.errandId", "not a valid UUID"));

		verifyNoInteractions(serviceMock);
	}

	/**
	 * Every endpoint is enumerated because the constraint is written out once per method: dropping it from one of them
	 * leaves the other three green, and the write paths are the ones a process engine calls.
	 */
	@ParameterizedTest
	@MethodSource("anInvalidNamespaceArguments")
	void anInvalidNamespaceIsRejected(final HttpMethod method, final String path, final Map<String, Object> variables, final String expectedField) {
		final var uriSpec = webTestClient.method(method).uri(builder -> builder.path(path).build(variables));
		final WebTestClient.RequestHeadersSpec<?> request = GET.equals(method)
			? uriSpec
			: uriSpec.contentType(APPLICATION_JSON).bodyValue(validReport());

		final var response = request
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple(expectedField, "can only contain A-Z, a-z, 0-9, - and _"));

		verifyNoInteractions(serviceMock);
	}

	private static Stream<Arguments> anInvalidNamespaceArguments() {
		return Stream.of(
			Arguments.of(PUT, PROCESS_PATH, instanceVariables(INVALID_NAMESPACE), "reportProcess.namespace"),
			Arguments.of(POST, PROCESSES_PATH, errandVariables(INVALID_NAMESPACE), "registerProcess.namespace"),
			Arguments.of(GET, PROCESSES_PATH, errandVariables(INVALID_NAMESPACE), "readErrandProcesses.namespace"),
			Arguments.of(GET, ACTIVITIES_PATH, errandVariables(INVALID_NAMESPACE), "readErrandProcessActivities.namespace"));
	}
}
