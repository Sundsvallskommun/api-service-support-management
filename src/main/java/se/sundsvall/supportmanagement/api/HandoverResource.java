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
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverErrand;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverErrandRequest;
import se.sundsvall.supportmanagement.api.model.handover.HandoverPreview;
import se.sundsvall.supportmanagement.api.model.handover.HandoverPreviewRequest;
import se.sundsvall.supportmanagement.service.HandoverPreviewService;
import se.sundsvall.supportmanagement.service.HandoverService;

import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/{errandId}/handover")
@Tag(name = "Handover", description = "Errand handover operations")
class HandoverResource {

	private final HandoverPreviewService previewService;
	private final HandoverService handoverService;

	HandoverResource(final HandoverPreviewService previewService, final HandoverService handoverService) {
		this.previewService = previewService;
		this.handoverService = handoverService;
	}

	@PostMapping(path = "/preview", consumes = APPLICATION_JSON_VALUE, produces = {
		APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE
	})
	@Operation(summary = "Preview errand handover", description = "Builds a side-effect free preview describing how the errand would be handed over to another namespace, including auto-suggested mappings for namespace-bound fields", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
			Problem.class, ConstraintViolationProblem.class
		}))),
		@ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<HandoverPreview> previewHandover(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final HandoverPreviewRequest request) {

		return ok(previewService.previewHandover(namespace, municipalityId, errandId, request));
	}

	@PostMapping(path = "/execute", consumes = APPLICATION_JSON_VALUE, produces = {
		APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE
	})
	@Operation(summary = "Execute errand handover",
		description = "Hands over an errand to another namespace. Copies the errand with applied field mappings, optionally copies attachments, creates a HANDOVER relation, and optionally closes or suspends the source errand.",
		responses = {
			@ApiResponse(responseCode = "201",
				description = "Successful operation",
				headers = @Header(name = LOCATION, description = "Location of the newly created errand in the target namespace", schema = @Schema(type = "string")),
				useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
				Problem.class, ConstraintViolationProblem.class
			}))),
			@ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<HandoverErrand> executeHandover(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "Idempotency-Key",
			description = "Optional idempotency key (UUID). Repeated requests with the same key within 24 hours return the same result without creating a new errand.",
			example = "f0882f1d-06bc-47fd-b017-1d8307f5ce95") @RequestHeader(name = "Idempotency-Key", required = false) final String idempotencyKey,
		@Valid @NotNull @RequestBody final HandoverErrandRequest request) {

		final var result = handoverService.handover(namespace, municipalityId, errandId, idempotencyKey, request);
		final var location = UriComponentsBuilder.fromPath("/{municipalityId}/{namespace}/errands/{errandId}")
			.buildAndExpand(request.getTarget().getMunicipalityId(), request.getTarget().getNamespace(), result.getNewErrandId())
			.toUri();
		return ResponseEntity.status(CREATED).header(LOCATION, location.toString()).body(result);
	}
}
