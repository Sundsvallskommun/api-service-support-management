package se.sundsvall.supportmanagement.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachmentHeader;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.service.ErrandAttachmentService;

import java.util.List;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_VALIDATON_MESSAGE;

@RestController
@Validated
@RequestMapping("/{namespace}/{municipalityId}/errands/{id}/attachments")
@Tag(name = "Errand attachments", description = "Errand attachments operations")
public class ErrandAttachmentsResource {

	@Autowired
	private ErrandAttachmentService errandAttachmentService;

	@PostMapping(consumes = MULTIPART_FORM_DATA_VALUE, produces = { ALL_VALUE, APPLICATION_PROBLEM_JSON_VALUE })
	@Operation(summary = "Create errand attachment", description = "Creates a new errand attachment based on the supplied attributes")
	@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful operation", content = @Content(mediaType = ALL_VALUE, schema = @Schema(implementation = Void.class)))
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class })))
	@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	@Validated(OnCreate.class)
	public ResponseEntity<Void> createErrandAttachment(
		final UriComponentsBuilder uriComponentsBuilder,
		@Parameter(name = "namespace", description = "Namespace", example = "my.namespace") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATON_MESSAGE, groups = OnCreate.class) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId(groups = OnCreate.class) @PathVariable final String municipalityId,
		@Parameter(name = "id", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid(groups = OnCreate.class) @PathVariable("id") final String id,
		@NotNull @RequestPart("errandAttachment") final MultipartFile errandAttachment) {

		var attachmentId = errandAttachmentService.createErrandAttachment(namespace, municipalityId, id, errandAttachment);

		return created(uriComponentsBuilder.path("/{namespace}/{municipalityId}/errands/{id}/attachments/{attachmentId}")
			.buildAndExpand(namespace, municipalityId, id, attachmentId).toUri()).header(CONTENT_TYPE, ALL_VALUE).build();
	}

	@GetMapping(path = "/{attachmentId}", produces = { ALL_VALUE, APPLICATION_PROBLEM_JSON_VALUE })
	@Operation(summary = "Read errand attachment", description = "Fetches the errand attachment that matches the provided errand id and attachment id")
	@ApiResponse(responseCode = "200", description = "Successful Operation", content = @Content(mediaType = ALL_VALUE))
	@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class })))
	@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	public void readErrandAttachment(
		final UriComponentsBuilder uriComponentsBuilder,
		@Parameter(name = "namespace", description = "Namespace", example = "my.namespace") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATON_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "id", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("id") final String id,
		@Parameter(name = "attachmentId", description = "Errand attachment id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable("attachmentId") final String attachmentId,
		HttpServletResponse response) {

		errandAttachmentService.readErrandAttachment(namespace, municipalityId, id, attachmentId, response);
	}

	@GetMapping(produces = { APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE })
	@Operation(summary = "Read errand attachments", description = "Fetches the errand attachments that matches the provided errand id")
	@ApiResponse(responseCode = "200", description = "Successful Operation", content = @Content(mediaType = APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = ErrandAttachmentHeader.class))))
	@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class })))
	@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	public ResponseEntity<List<ErrandAttachmentHeader>> readErrandAttachments(
		@Parameter(name = "namespace", description = "Namespace", example = "my.namespace") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATON_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "id", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("id") final String id) {

		return ok(errandAttachmentService.readErrandAttachmentHeaders(namespace, municipalityId, id));
	}

	@DeleteMapping(path = "/{attachmentId}")
	@Operation(summary = "Delete errand attachment", description = "Deletes the errand attachment that matches the provided errand id and attachment id")
	@ApiResponse(responseCode = "204", description = "Successful operation")
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class })))
	@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	public ResponseEntity<Void> deleteErrandAttachment(
		@Parameter(name = "namespace", description = "Namespace", example = "my.namespace") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATON_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "id", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("id") final String id,
		@Parameter(name = "attachmentId", description = "Errand attachment id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable("attachmentId") final String attachmentId) {

		errandAttachmentService.deleteErrandAttachment(namespace, municipalityId, id, attachmentId);
		return noContent().build();
	}
}
