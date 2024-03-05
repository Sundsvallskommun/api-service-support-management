package se.sundsvall.supportmanagement.api;

import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.noContent;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_VALIDATON_MESSAGE;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;

import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.supportmanagement.api.model.communication.Communication;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;
import se.sundsvall.supportmanagement.service.CommunicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Validated
@RequestMapping("/{namespace}/{municipalityId}/errands/{id}/communication")
@Tag(name = "Errand communication", description = "Errand communication operations")
@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {Problem.class, ConstraintViolationProblem.class})))
@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class ErrandCommunicationResource {

	private final CommunicationService service;

	ErrandCommunicationResource(final CommunicationService service) {
		this.service = service;
	}

	@Operation(description = "Get all communications for an errand.")
	@GetMapping(produces = {APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE})
	@ApiResponse(responseCode = "200", description = "OK - Successful operation", useReturnTypeSchema = true)
	public ResponseEntity<List<Communication>> getCommunications(
		@Parameter(name = "namespace", description = "Namespace", example = "my.namespace") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATON_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "id", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("id") final String id) {

		return ResponseEntity.ok(service.readCommunications(namespace, municipalityId, id));
	}

	@Operation(description = "Set viewed status for communication.")
	@PutMapping(path = "/{communicationID}/viewed/{isViewed}", produces = {APPLICATION_PROBLEM_JSON_VALUE})
	@ApiResponse(responseCode = "204", description = "No content - Successful operation")
	public ResponseEntity<Void> updateViewedStatus(
		@Parameter(name = "namespace", description = "Namespace", example = "my.namespace") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATON_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "id", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("id") final String id,
		@Parameter(name = "communicationID", description = "communication id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("communicationID") final String communicationID,
		@Parameter(name = "isViewed", description = "If a message is viewed", example = "true") @PathVariable final boolean isViewed) {

		service.updateViewedStatus(namespace, municipalityId, id, communicationID, isViewed);
		return ResponseEntity.noContent().build();
	}

	@PostMapping(path = "/email", consumes = APPLICATION_JSON_VALUE, produces = {ALL_VALUE, APPLICATION_PROBLEM_JSON_VALUE})
	@Operation(summary = "Send email to in context of an errand", description = "Sends an email message to the recipient specified in the request")
	@ApiResponse(responseCode = "204", description = "Successful operation")
	public ResponseEntity<Void> sendEmail(
		@Parameter(name = "namespace", description = "Namespace", example = "my.namespace") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATON_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "id", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("id") final String id,
		@Valid @NotNull @RequestBody final EmailRequest request) {

		service.sendEmail(namespace, municipalityId, id, request);
		return noContent().build();

	}

	@PostMapping(path = "/sms", consumes = APPLICATION_JSON_VALUE, produces = {ALL_VALUE, APPLICATION_PROBLEM_JSON_VALUE})
	@Operation(summary = "Send sms to in context of an errand", description = "Sends a sms message to the recipient specified in the request")
	@ApiResponse(responseCode = "204", description = "Successful operation")
	public ResponseEntity<Void> sendSms(
		@Parameter(name = "namespace", description = "Namespace", example = "my.namespace") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATON_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "id", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("id") final String id,
		@Valid @NotNull @RequestBody final SmsRequest request) {

		service.sendSms(namespace, municipalityId, id, request);
		return noContent().build();
	}
}
