package se.sundsvall.supportmanagement.api;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import se.sundsvall.supportmanagement.api.model.process.ProcessStartRequest;
import se.sundsvall.supportmanagement.service.ErrandProcessService;
import se.sundsvall.supportmanagement.service.ProcessCommandService;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
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
	private static final String START_PATH = PROCESSES_PATH + "/start";
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

	private static Stream<Arguments> invalidReports() {
		return Stream.of(
			argumentSet("a report without the fields identifying the process", PUT, ErrandProcessReport.create(), List.of(
				tuple("processService", "must not be blank"),
				tuple("processKey", "must not be blank"),
				tuple("processStatus", "must not be blank"))),
			argumentSet("a report carrying more than a hundred activities", PUT, validReport().withActivities(IntStream.range(0, 101)
				.mapToObj(index -> ProcessActivity.create().withActivityType("PHASE").withActivityId("phase-" + index).withOccurredAt(now(systemDefault())))
				.toList()), List.of(
					tuple("activities", "may contain at most 100 activities"))),
			argumentSet("a registration carrying more than a hundred activities", POST, validReport().withProcessInstanceId(PROCESS_INSTANCE_ID).withActivities(IntStream.range(0, 101)
				.mapToObj(index -> ProcessActivity.create().withActivityType("PHASE").withActivityId("phase-" + index).withOccurredAt(now(systemDefault())))
				.toList()), List.of(
					tuple("activities", "may contain at most 100 activities"))),
			argumentSet("a report with an error message longer than its column", PUT, validReport().withError(ProcessError.create().withCode("INCIDENT").withMessage("x".repeat(2049))), List.of(
				tuple("error.message", "size must be between 0 and 2048"))),
			argumentSet("a report with an activity without a moment", PUT, validReport().withActivities(List.of(ProcessActivity.create().withActivityType("PHASE"))), List.of(
				tuple("activities[0].occurredAt", "must not be null"))),
			argumentSet("a report with a signal without a name", PUT, validReport().withAwaitingSignals(List.of(ProcessSignal.create().withLabel("Godkänn granskning"))), List.of(
				tuple("awaitingSignals[0].name", "must not be blank"))),
			argumentSet("a report with a signal longer than its columns", PUT, validReport().withAwaitingSignals(List.of(ProcessSignal.create().withName("x".repeat(129)).withLabel("y".repeat(256)))), List.of(
				tuple("awaitingSignals[0].name", "size must be between 0 and 128"),
				tuple("awaitingSignals[0].label", "size must be between 0 and 255"))),
			argumentSet("a report waiting for more than fifty signals", PUT, validReport().withAwaitingSignals(IntStream.range(0, 51)
				.mapToObj(index -> ProcessSignal.create().withName("signal-" + index))
				.toList()), List.of(
					tuple("awaitingSignals", "may contain at most 50 signals"))),
			argumentSet("a registration with a blank process instance id", POST, validReport().withProcessInstanceId(""), List.of(
				tuple("processInstanceId", "must be an id, without blanks"))),
			argumentSet("a registration with a process instance id holding a blank", POST, validReport().withProcessInstanceId("8f1c 2b6e"), List.of(
				tuple("processInstanceId", "must be an id, without blanks"))));
	}

	@ParameterizedTest
	@MethodSource("invalidReports")
	void anInvalidReportIsRejected(final HttpMethod method, final ErrandProcessReport report, final List<Tuple> violations) {
		final var response = webTestClient.method(method)
			.uri(builder -> builder.path(PUT == method ? PROCESS_PATH : PROCESSES_PATH).build(instanceVariables()))
			.contentType(APPLICATION_JSON)
			.bodyValue(report)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrderElementsOf(violations);

		verifyNoInteractions(serviceMock, commandServiceMock);
	}

	/**
	 * An empty slot is refused in either list, rather than handed to the service to trip over.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"activities", "awaitingSignals"
	})
	void aReportWithAnEmptySlotInAListIsRejected(final String list) {
		final var response = webTestClient.put()
			.uri(builder -> builder.path(PROCESS_PATH).build(instanceVariables()))
			.contentType(APPLICATION_JSON)
			.bodyValue("""
				{"processService": "pw-alkt", "processKey": "alkt-ansokan", "processStatus": "WAITING", "%s": [null]}""".formatted(list))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::message)
			.containsExactly("must not be null");
		assertThat(response.getViolations().getFirst().field()).startsWith(list + "[0]");

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

	@Test
	void aStartNamingAKeyLongerThanTheProcessKeysCanBeIsRejected() {
		final var response = webTestClient.post()
			.uri(builder -> builder.path(START_PATH).build(errandVariables(NAMESPACE)))
			.contentType(APPLICATION_JSON)
			.bodyValue(ProcessStartRequest.create().withProcessKey("k".repeat(129)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("processKey", "size must be between 0 and 128"));

		verifyNoInteractions(serviceMock, commandServiceMock);
	}

	@Test
	void aStartOfAnErrandWithAnInvalidIdIsRejected() {
		final var response = webTestClient.post()
			.uri(builder -> builder.path(START_PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("startProcess.errandId", "not a valid UUID"));

		verifyNoInteractions(serviceMock, commandServiceMock);
	}

	private static Stream<Arguments> aSignalWithoutANameArguments() {
		return Stream.of(
			Arguments.of("{}"),
			Arguments.of("{\"signal\": null}"),
			Arguments.of("{\"signal\": \"\"}"),
			Arguments.of("{\"signal\": \"   \"}"));
	}

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

	@ParameterizedTest
	@ValueSource(strings = {
		" ", "a b"
	})
	void aReportToAnInstanceIdWithBlanksIsRejected(final String processInstanceId) {
		final var response = webTestClient.put()
			.uri(builder -> builder.path(PROCESS_PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "processInstanceId", processInstanceId)))
			.contentType(APPLICATION_JSON)
			.bodyValue(validReport())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("reportProcess.processInstanceId", "must be an id, without blanks"));

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
	 * Every endpoint is sent an invalid namespace together with a body that is valid for it, so the namespace is the only
	 * thing to object to.
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
			Arguments.of(POST, START_PATH, errandVariables(INVALID_NAMESPACE), ProcessStartRequest.create(), "startProcess.namespace"),
			Arguments.of(GET, PROCESSES_PATH, errandVariables(INVALID_NAMESPACE), null, "readErrandProcesses.namespace"),
			Arguments.of(GET, ACTIVITIES_PATH, errandVariables(INVALID_NAMESPACE), null, "readErrandProcessActivities.namespace"));
	}
}
