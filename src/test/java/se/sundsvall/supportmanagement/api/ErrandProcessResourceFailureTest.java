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
import se.sundsvall.supportmanagement.api.model.process.ErrandProcessReport;
import se.sundsvall.supportmanagement.api.model.process.ProcessActivity;
import se.sundsvall.supportmanagement.api.model.process.ProcessError;
import se.sundsvall.supportmanagement.api.model.process.ProcessSignal;
import se.sundsvall.supportmanagement.api.model.process.ProcessSignalRequest;
import se.sundsvall.supportmanagement.service.ErrandProcessService;
import se.sundsvall.supportmanagement.service.ProcessCommandService;

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
	private static final String SIGNALS_PATH = PROCESS_PATH + "/signals";
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

	@MockitoBean
	private ProcessCommandService commandServiceMock;

	private static Map<String, Object> instanceVariables() {
		return instanceVariables(NAMESPACE);
	}

	private static Map<String, Object> instanceVariables(final String namespace) {
		return Map.of("namespace", namespace, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "processInstanceId", PROCESS_INSTANCE_ID);
	}

	private static Map<String, Object> errandVariables(final String namespace) {
		return Map.of("namespace", namespace, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID);
	}

	private static ErrandProcessReport validReport() {
		return ErrandProcessReport.create()
			.withProcessService("pw-alkt")
			.withProcessKey("alkt-ansokan")
			.withProcessStatus(RUNNING);
	}

	@Test
	void aReportWithoutTheFieldsIdentifyingTheProcessIsRejected() {
		final var response = webTestClient.put()
			.uri(builder -> builder.path(PROCESS_PATH).build(instanceVariables()))
			.contentType(APPLICATION_JSON)
			.bodyValue(ErrandProcessReport.create())
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

		verifyNoInteractions(serviceMock, commandServiceMock);
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

		verifyNoInteractions(serviceMock, commandServiceMock);
	}

	/**
	 * The error of a failed process is often a stack trace, and one longer than its column has to be refused as a bad
	 * request rather than reach the insert and fail as a server error - which would leave the failure unreported.
	 */
	@Test
	void aReportWithAnErrorMessageLongerThanItsColumnIsRejected() {
		final var report = validReport()
			.withError(ProcessError.create().withCode("INCIDENT").withMessage("x".repeat(2049)));

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
			.containsExactly(tuple("error.message", "size must be between 0 and 2048"));

		verifyNoInteractions(serviceMock, commandServiceMock);
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

		verifyNoInteractions(serviceMock, commandServiceMock);
	}

	@Test
	void aReportWithASignalWithoutANameIsRejected() {
		final var report = validReport()
			.withAwaitingSignals(List.of(ProcessSignal.create().withLabel("Godkänn granskning")));

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
			.containsExactly(tuple("awaitingSignals[0].name", "must not be blank"));

		verifyNoInteractions(serviceMock, commandServiceMock);
	}

	/**
	 * Both columns are fed from the process model, and a name or label longer than its column has to be refused as a bad
	 * request rather than fail the insert as a server error.
	 */
	@Test
	void aReportWithASignalLongerThanItsColumnsIsRejected() {
		final var report = validReport()
			.withAwaitingSignals(List.of(ProcessSignal.create().withName("x".repeat(129)).withLabel("y".repeat(256))));

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
			.containsExactlyInAnyOrder(
				tuple("awaitingSignals[0].name", "size must be between 0 and 128"),
				tuple("awaitingSignals[0].label", "size must be between 0 and 255"));

		verifyNoInteractions(serviceMock, commandServiceMock);
	}

	@Test
	void aReportWaitingForMoreThanFiftySignalsIsRejected() {
		final var report = validReport()
			.withAwaitingSignals(IntStream.range(0, 51)
				.mapToObj(index -> ProcessSignal.create().withName("signal-" + index))
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
			.containsExactly(tuple("awaitingSignals", "may contain at most 50 signals"));

		verifyNoInteractions(serviceMock, commandServiceMock);
	}

	/**
	 * A null in the list would otherwise reach the service and fail there as a server error.
	 */
	@Test
	void aReportWithAnEmptySlotAmongItsSignalsIsRejected() {
		final var response = webTestClient.put()
			.uri(builder -> builder.path(PROCESS_PATH).build(instanceVariables()))
			.contentType(APPLICATION_JSON)
			.bodyValue("""
				{"processService": "pw-alkt", "processKey": "alkt-ansokan", "processStatus": "WAITING", "awaitingSignals": [null]}""")
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::message)
			.containsExactly("must not be null");
		assertThat(response.getViolations().getFirst().field()).startsWith("awaitingSignals[0]");

		verifyNoInteractions(serviceMock, commandServiceMock);
	}

	@ParameterizedTest
	@MethodSource("aSignalWithoutANameArguments")
	void aSignalWithoutANameIsRejected(final String body) {
		final var response = webTestClient.post()
			.uri(builder -> builder.path(SIGNALS_PATH).build(instanceVariables()))
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("signal", "must not be blank"));

		verifyNoInteractions(serviceMock, commandServiceMock);
	}

	private static Stream<Arguments> aSignalWithoutANameArguments() {
		return Stream.of(
			Arguments.of("{}"),
			Arguments.of("{\"signal\": null}"),
			Arguments.of("{\"signal\": \"\"}"),
			Arguments.of("{\"signal\": \"   \"}"));
	}

	/**
	 * The name travels all the way to the outbox, where a name that does not fit fails the signal rather than being cut
	 * to one that would correlate another gate. Refused here, it never gets that far.
	 */
	@Test
	void aSignalLongerThanTheNamesAProcessCanWaitForIsRejected() {
		final var response = webTestClient.post()
			.uri(builder -> builder.path(SIGNALS_PATH).build(instanceVariables()))
			.contentType(APPLICATION_JSON)
			.bodyValue(ProcessSignalRequest.create().withSignal("x".repeat(129)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("signal", "size must be between 0 and 128"));

		verifyNoInteractions(serviceMock, commandServiceMock);
	}

	@Test
	void aSignalToAnInstanceIdLongerThanItsColumnIsRejected() {
		final var response = webTestClient.post()
			.uri(builder -> builder.path(SIGNALS_PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "processInstanceId", "x".repeat(65))))
			.contentType(APPLICATION_JSON)
			.bodyValue(ProcessSignalRequest.create().withSignal("granskning-godkand"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("signalProcess.processInstanceId", "size must be between 0 and 64"));

		verifyNoInteractions(serviceMock, commandServiceMock);
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

		verifyNoInteractions(serviceMock, commandServiceMock);
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

		verifyNoInteractions(serviceMock, commandServiceMock);
	}

	/**
	 * Every endpoint is enumerated because the constraint is written out once per method: dropping it from one of them
	 * leaves the others green, and the write paths are the ones a process engine and a handler call. Each is sent a body
	 * that is valid for it, so the namespace is the only thing to object to.
	 */
	@ParameterizedTest
	@MethodSource("anInvalidNamespaceArguments")
	void anInvalidNamespaceIsRejected(final HttpMethod method, final String path, final Map<String, Object> variables, final Object body, final String expectedField) {
		final var uriSpec = webTestClient.method(method).uri(builder -> builder.path(path).build(variables));
		final WebTestClient.RequestHeadersSpec<?> request = GET.equals(method)
			? uriSpec
			: uriSpec.contentType(APPLICATION_JSON).bodyValue(body);

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

		verifyNoInteractions(serviceMock, commandServiceMock);
	}

	private static Stream<Arguments> anInvalidNamespaceArguments() {
		return Stream.of(
			Arguments.of(PUT, PROCESS_PATH, instanceVariables(INVALID_NAMESPACE), validReport(), "reportProcess.namespace"),
			Arguments.of(POST, PROCESSES_PATH, errandVariables(INVALID_NAMESPACE), validReport(), "registerProcess.namespace"),
			Arguments.of(POST, SIGNALS_PATH, instanceVariables(INVALID_NAMESPACE), ProcessSignalRequest.create().withSignal("granskning-godkand"), "signalProcess.namespace"),
			Arguments.of(GET, PROCESSES_PATH, errandVariables(INVALID_NAMESPACE), null, "readErrandProcesses.namespace"),
			Arguments.of(GET, ACTIVITIES_PATH, errandVariables(INVALID_NAMESPACE), null, "readErrandProcessActivities.namespace"));
	}
}
