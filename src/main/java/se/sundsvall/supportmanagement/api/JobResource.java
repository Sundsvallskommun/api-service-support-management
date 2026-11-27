package se.sundsvall.supportmanagement.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.model.enums.JobStatus;
import se.sundsvall.supportmanagement.model.enums.JobType;
import se.sundsvall.supportmanagement.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.AccessControlService;
import se.sundsvall.supportmanagement.service.job.JobService;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/jobs")
@Tag(name = "Jobs", description = "Job operations")
class JobResource {

	private final JobService jobService;
	private final AccessControlService accessControlService;

	JobResource(final JobService jobService, final AccessControlService accessControlService) {
		this.jobService = jobService;
		this.accessControlService = accessControlService;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Get jobs",
		description = "Get the background jobs of the namespace, newest first, optionally narrowed to a status and a type. " +
			"Jobs that have ended are kept for a period of time and then removed.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
				Problem.class, ConstraintViolationProblem.class
			}))),
			@ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<List<JobResponse>> getJobs(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "status", description = "Status of the jobs to get, every status when left out") @RequestParam(required = false) final JobStatus status,
		@Parameter(name = "type", description = "Type of the jobs to get, every type when left out") @RequestParam(required = false) final JobType type) {

		accessControlService.verifyNamespaceAuthorization(namespace, municipalityId, ProtectedResource.NAMESPACE_CONFIG, R);
		return ok(jobService.find(namespace, municipalityId, status, type));
	}

	@GetMapping(path = "/{jobId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Get job status", description = "Get status and progress for a background job", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
			Problem.class, ConstraintViolationProblem.class
		}))),
		@ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<JobResponse> getJob(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "jobId", description = "Job id") @ValidUuid @PathVariable final String jobId) {

		accessControlService.verifyNamespaceAuthorization(namespace, municipalityId, ProtectedResource.NAMESPACE_CONFIG, R);
		return ok(jobService.get(namespace, municipalityId, jobId));
	}
}
