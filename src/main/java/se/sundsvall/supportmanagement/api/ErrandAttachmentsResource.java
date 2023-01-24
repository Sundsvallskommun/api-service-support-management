package se.sundsvall.supportmanagement.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.service.ErrandAttachmentService;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;

@RestController
@Validated
@RequestMapping("/errands/{id}/attachments")
@Tag(name = "Errand attachments", description = "Errand attachments operations")
public class ErrandAttachmentsResource {

	@Autowired
	private ErrandAttachmentService errandAttachmentService;

	@PostMapping(consumes = APPLICATION_JSON_VALUE, produces = { ALL_VALUE, APPLICATION_PROBLEM_JSON_VALUE })
	@Operation(summary = "Create errand attachment", description = "Creates a new errand attachment based on the supplied attributes")
	@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful operation", content = @Content(mediaType = ALL_VALUE, schema = @Schema(implementation = Void.class)))
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class })))
	@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	@Validated(OnCreate.class)
	public ResponseEntity<Void> createErrandAttachment(
		final UriComponentsBuilder uriComponentsBuilder,
		@Parameter(name = "id", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid(groups = OnCreate.class) @PathVariable("id") final String id,
		@Valid @NotNull @RequestBody final ErrandAttachment errandAttachment) {

		final var attachmentId = errandAttachmentService.createErrandAttachment(id, errandAttachment);
		return created(uriComponentsBuilder.path("/errands/{id}/attachments/{attachmentId}").buildAndExpand(id, attachmentId).toUri()).header(CONTENT_TYPE, ALL_VALUE).build();
	}

	@GetMapping(path = "/{attachmentId}", produces = { APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE })
	@Operation(summary = "Read errand attachment", description = "Fetches the errand attachment that matches the provided errand id and attachment id")
	@ApiResponse(responseCode = "200", description = "Successful Operation", content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrandAttachment.class)))
	@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class })))
	@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	public ResponseEntity<ErrandAttachment> readErrandAttachment(
		@Parameter(name = "id", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("id") final String id,
		@Parameter(name = "attachmentId", description = "Errand attachment id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable("attachmentId") final String attachmentId) {

		return ResponseEntity.ok(errandAttachmentService.readErrandAttachment(id, attachmentId));
	}

	@GetMapping(produces = { APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE })
	@Operation(summary = "Read errand attachments", description = "Fetches the errand attachments that matches the provided errand id")
	@ApiResponse(responseCode = "200", description = "Successful Operation", content = @Content(mediaType = APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = ErrandAttachment.class))))
	@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class })))
	@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	public ResponseEntity<List<ErrandAttachment>> readErrandAttachments(
		@Parameter(name = "id", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("id") final String id) {

		return ResponseEntity.ok(errandAttachmentService.readErrandAttachments(id));
	}

	@DeleteMapping(path = "/{attachmentId}")
	@Operation(summary = "Delete errand attachment", description = "Deletes the errand attachment that matches the provided errand id and attachment id")
	@ApiResponse(responseCode = "204", description = "Successful operation")
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class })))
	@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	public ResponseEntity<Void> deleteErrandAttachment(
		@Parameter(name = "id", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("id") final String id,
		@Parameter(name = "attachmentId", description = "Errand attachment id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable("attachmentId") final String attachmentId) {

		errandAttachmentService.deleteErrandAttachment(id, attachmentId);
		return noContent().build();
	}
}
