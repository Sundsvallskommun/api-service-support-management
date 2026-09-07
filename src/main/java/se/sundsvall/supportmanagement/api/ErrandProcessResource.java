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
import se.sundsvall.supportmanagement.api.model.process.ErrandProcesses;
import se.sundsvall.supportmanagement.api.model.process.ProcessActivity;
import se.sundsvall.supportmanagement.service.ErrandProcessService;
import se.sundsvall.supportmanagement.service.model.ErrandProcessResult;

import static java.util.Objects.isNull;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

/**
 * The process attached to an errand: what it reports about itself, and what a handler reads of it.
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

	private static final String PROCESS_PATH = "/{municipalityId}/{namespace}/errands/{errandId}/processes/{processInstanceId}";

	private final ErrandProcessService service;

	ErrandProcessResource(final ErrandProcessService service) {
		this.service = service;
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
		@ApiResponse(responseCode = "409", description = "Conflict", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<ErrandProcess> reportProcess(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "processInstanceId", description = "Process instance id", example = "8f1c2b6e-1f4a-4d61-9a0e-2b7c1f0a5e33") @PathVariable final String processInstanceId,
		@Valid @NotNull @RequestBody final ErrandProcess report) {

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
		@ApiResponse(responseCode = "409", description = "Conflict", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<ErrandProcess> registerProcess(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final ErrandProcess report) {

		return respond(service.registerProcess(namespace, municipalityId, errandId, report), municipalityId, namespace, errandId);
	}

	@GetMapping(path = "/processes", produces = {
		APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE
	})
	@Operation(summary = "Read errand processes", description = """
		Every process the errand has had, most recent first, in an envelope that also has room for whether a new one may \
		be started. An empty list is not an error.""", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<ErrandProcesses> readErrandProcesses(
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

		// A start that failed left no instance behind, and so nothing to point a location at.
		if (isNull(processInstanceId)) {
			return status(CREATED).body(result.process());
		}

		return created(fromPath(PROCESS_PATH)
			.buildAndExpand(municipalityId, namespace, errandId, processInstanceId).toUri())
			.body(result.process());
	}
}
