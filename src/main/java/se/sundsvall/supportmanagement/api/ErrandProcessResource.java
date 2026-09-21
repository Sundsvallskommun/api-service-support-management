package se.sundsvall.supportmanagement.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcess;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcessOverview;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcessReport;
import se.sundsvall.supportmanagement.api.model.process.ProcessActivity;
import se.sundsvall.supportmanagement.api.model.process.ProcessSignalRequest;
import se.sundsvall.supportmanagement.api.model.process.ProcessStartRequest;
import se.sundsvall.supportmanagement.service.ErrandProcessService;
import se.sundsvall.supportmanagement.service.ProcessCommandService;
import se.sundsvall.supportmanagement.service.model.ErrandProcessResult;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

/**
 * The process attached to an errand: what it reports about itself, what a handler reads of it, and the commands a
 * handler starts it and steps it on with.
 */
@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/{errandId}")
@Tag(name = "Errand processes", description = "Errand process operations")
@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class ErrandProcessResource {

	private final ErrandProcessService service;
	private final ProcessCommandService commandService;

	ErrandProcessResource(final ErrandProcessService service, final ProcessCommandService commandService) {
		this.service = service;
		this.commandService = commandService;
	}

	@PutMapping(path = "/processes/{processInstanceId}", consumes = APPLICATION_JSON_VALUE, produces = {
		APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE
	})
	@Operation(summary = "Report process state", description = """
		Takes the state of a process instance and the activities it performed, in one call. Creates the row for the \
		instance when this is the first report about it, which is what lets a work step report before the start of its \
		own process has been registered.""", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "201", description = "Successful operation", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "412",
			description = "Precondition Failed — the errand has changed since the version the report was read at",
			content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "409", description = "Conflict", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<ErrandProcess> reportProcess(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "processInstanceId", description = "Process instance id", example = "8f1c2b6e-1f4a-4d61-9a0e-2b7c1f0a5e33") @Size(max = 64) @PathVariable final String processInstanceId,
		@Valid @NotNull @RequestBody final ErrandProcessReport report) {

		return respond(service.reportProcess(namespace, municipalityId, errandId, processInstanceId, report), municipalityId, namespace, errandId);
	}

	@PostMapping(path = "/processes", consumes = APPLICATION_JSON_VALUE, produces = {
		APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE
	})
	@Operation(summary = "Register process start", description = """
		Registers that a process was started for the errand, or that starting one failed. Creates, and never updates: an \
		instance already reported on is answered with what it says right now, untouched, since that report came from the \
		process itself and is the newer word. A start that failed carries no process instance id.""", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "201", description = "Successful operation", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "412",
			description = "Precondition Failed — the errand has changed since the version the report was read at",
			content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "409", description = "Conflict", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<ErrandProcess> registerProcess(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final ErrandProcessReport report) {

		return respond(service.registerProcess(namespace, municipalityId, errandId, report), municipalityId, namespace, errandId);
	}

	@PostMapping(path = "/processes/start", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Start process", description = """
		Starts the handling of an errand by hand - the "start handling" button. Offer it when startable.status from GET \
		.../processes is AVAILABLE, in automatic start mode as well as in manual: in automatic mode it is how a start that \
		failed is tried again. The body may be left out when startable.processKeys holds one key; when it holds several, \
		send the key the user chose. Only a person may start a process, and the start is recorded with who sent it. \
		202 says the start is recorded and on its way, not that the process runs: the process shows in GET .../processes \
		once the process engine has registered it, normally within seconds. Until then startable still says AVAILABLE - \
		show the start as on its way rather than as not started. Sending the same start again while it is on its way \
		starts nothing more and is answered 202. \
		400 is answered when no label of the errand names a process it can be started with, when the labels name several \
		and the request chose none, when the request names a key that is not among them, and when the namespace runs no \
		processes.""", responses = {
		@ApiResponse(responseCode = "202", description = "Accepted — the start is recorded and handed on to the process", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "403",
			description = "Forbidden — the caller is not an ad account, or may not reach the errand",
			content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "404",
			description = "Not found — the errand does not exist in the namespace",
			content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "409",
			description = "Conflict — the errand already has a live process, its process has run to its end, or a start of another process is already on its way",
			content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> startProcess(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Valid @RequestBody(required = false) final ProcessStartRequest request) {

		commandService.startProcess(namespace, municipalityId, errandId, ofNullable(request).map(ProcessStartRequest::getProcessKey).orElse(null));
		return accepted().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	@PostMapping(path = "/processes/{processInstanceId}/signals", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Signal process", description = """
		Steps a process past the gate it waits at, by sending one of the signals listed in awaitingSignals of the process. \
		The signal is recorded and handed on to the process, which decides what it means where it stands - so 202 says the \
		signal is on its way, not that the process has moved on. Read the errand again to see where it went. Sending a \
		signal does not consume it: until the process reports where it went, the same signal is accepted again, so a \
		client should not offer it twice. Only a person may send a signal, and the request carries its name and nothing \
		else: a reason for the step belongs in a note on the errand.""", responses = {
		@ApiResponse(responseCode = "202", description = "Accepted — the signal is recorded and handed on to the process", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "403",
			description = "Forbidden — the caller is not an ad account, or may not reach the errand",
			content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "404",
			description = "Not found — the errand does not exist, or has no such process instance",
			content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "409",
			description = "Conflict — the process does not wait for the signal right now, or has ended. Read the errand again rather than retrying",
			content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> signalProcess(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "processInstanceId", description = "Process instance id", example = "8f1c2b6e-1f4a-4d61-9a0e-2b7c1f0a5e33") @Size(max = 64) @PathVariable final String processInstanceId,
		@Valid @NotNull @RequestBody final ProcessSignalRequest request) {

		commandService.signalProcess(namespace, municipalityId, errandId, processInstanceId, request.getSignal());
		return accepted().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	@GetMapping(path = "/processes", produces = {
		APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE
	})
	@Operation(summary = "Read errand processes", description = """
		Every process the errand has had, most recent first, together with whether a new one may be started right now \
		and why not when it cannot be - which is what a start button is lit, dimmed and explained by. An empty list is not \
		an error.""", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<ErrandProcessOverview> readErrandProcesses(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId) {

		return ok(service.readProcesses(namespace, municipalityId, errandId));
	}

	@GetMapping(path = "/process-activities", produces = {
		APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE
	})
	@Operation(summary = "Read errand process activities", description = """
		The activity log of the errand. Read per errand rather than per process instance, since the entries explaining \
		why no process ever started belong to no instance. Narrowing to one instance therefore leaves those entries \
		out.""", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Page<ProcessActivity>> readErrandProcessActivities(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "processInstanceId", description = "Narrows the log to one process instance", example = "8f1c2b6e-1f4a-4d61-9a0e-2b7c1f0a5e33") @RequestParam(required = false) final String processInstanceId,
		@PageableDefault(size = 50, sort = "occurredAt", direction = Direction.DESC) @ParameterObject final Pageable pageable) {

		return ok(service.readProcessActivities(namespace, municipalityId, errandId, processInstanceId, pageable));
	}

	/**
	 * Answers created for a row this write brought into being and ok for one that was already there, which is the whole
	 * difference between the two write paths as seen from outside.
	 */
	private static ResponseEntity<ErrandProcess> respond(final ErrandProcessResult result, final String municipalityId, final String namespace, final String errandId) {
		if (!result.created()) {
			return ok(result.process());
		}

		final var processInstanceId = result.process().getProcessInstanceId();

		if (isNull(processInstanceId)) {
			return status(CREATED).body(result.process());
		}

		return created(fromPath("/{municipalityId}/{namespace}/errands/{errandId}/processes/{processInstanceId}")
			.buildAndExpand(municipalityId, namespace, errandId, processInstanceId).toUri())
			.body(result.process());
	}
}
