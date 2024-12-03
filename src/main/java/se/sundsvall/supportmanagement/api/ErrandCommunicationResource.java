package se.sundsvall.supportmanagement.api;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.noContent;
import static org.zalando.problem.Status.TOO_MANY_REQUESTS;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.concurrent.Semaphore;
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
import se.sundsvall.supportmanagement.api.model.communication.WebMessageRequest;
import se.sundsvall.supportmanagement.service.CommunicationService;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/{errandId}/communication")
@Tag(name = "Errand communication", description = "Errand communication operations")
@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class ErrandCommunicationResource {

	private final Semaphore semaphore;

	private final CommunicationService service;

	ErrandCommunicationResource(final Semaphore semaphore, final CommunicationService service) {
		this.semaphore = semaphore;
		this.service = service;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(description = "Get all communications for an errand.", responses = {
		@ApiResponse(responseCode = "200", description = "OK - Successful operation", useReturnTypeSchema = true)
	})
	ResponseEntity<List<Communication>> getCommunications(
		@Parameter(name = "namespace", description = "Namespace", example = "my.namespace") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("errandId") final String errandId) {

		return ResponseEntity.ok(service.readCommunications(namespace, municipalityId, errandId));
	}

	@PutMapping(path = "/{communicationId}/viewed/{isViewed}", produces = ALL_VALUE)
	@Operation(description = "Set viewed status for communication.", responses = {
		@ApiResponse(responseCode = "204", description = "No content - Successful operation", useReturnTypeSchema = true)
	})
	ResponseEntity<Void> updateViewedStatus(
		@Parameter(name = "namespace", description = "Namespace", example = "my.namespace") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("errandId") final String errandId,
		@Parameter(name = "communicationId", description = "communication ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("communicationId") final String communicationId,
		@Parameter(name = "isViewed", description = "If a message is viewed", example = "true") @PathVariable final boolean isViewed) {

		service.updateViewedStatus(namespace, municipalityId, errandId, communicationId, isViewed);
		return noContent()
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@PostMapping(path = "/email", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Send email to in context of an errand", description = "Sends an email message to the recipient specified in the request", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true)
	})
	ResponseEntity<Void> sendEmail(
		@Parameter(name = "namespace", description = "Namespace", example = "my.namespace") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("errandId") final String errandId,
		@Valid @NotNull @RequestBody final EmailRequest request) {

		service.sendEmail(namespace, municipalityId, errandId, request);
		return noContent()
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@PostMapping(path = "/sms", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Send sms to in context of an errand", description = "Sends a sms message to the recipient specified in the request", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true)
	})
	ResponseEntity<Void> sendSms(
		@Parameter(name = "namespace", description = "Namespace", example = "my.namespace") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("errandId") final String errandId,
		@Valid @NotNull @RequestBody final SmsRequest request) {

		service.sendSms(namespace, municipalityId, errandId, request);
		return noContent()
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@PostMapping(path = "/webmessage", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Send email to in context of an errand", description = "Sends an email message to the recipient specified in the request", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true)
	})
	ResponseEntity<Void> sendWebMessage(
		@Parameter(name = "namespace", description = "Namespace", example = "my.namespace") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("errandId") final String errandId,
		@Valid @NotNull @RequestBody final WebMessageRequest request) {

		service.sendWebMessage(namespace, municipalityId, errandId, request);
		return noContent()
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@GetMapping(path = "/{communicationId}/attachments/{attachmentId}/streamed", produces = ALL_VALUE)
	@Operation(summary = "Get a streamed communication attachment.", description = "Fetches the communication attachment that matches the provided id in a streamed manner", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	void getMessageAttachmentStreamed(
		@Parameter(name = "namespace", description = "Namespace", example = "my.namespace") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("errandId") final String errandId,
		@Parameter(name = "communicationId", description = "communication ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("communicationId") final String communicationId,
		@Parameter(name = "attachmentId", description = "Message attachment ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String attachmentId,
		final HttpServletResponse response) {

		if (!semaphore.tryAcquire()) {
			throw Problem.valueOf(TOO_MANY_REQUESTS, "Too many files being read. Try again later.");
		}
		try {
			service.getMessageAttachmentStreamed(namespace, municipalityId, errandId, communicationId, attachmentId, response);
		} finally {
			semaphore.release();
		}
	}
}
