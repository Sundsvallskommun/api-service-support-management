package se.sundsvall.supportmanagement.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachment;
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachmentLink;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.api.model.errand.Statement;
import se.sundsvall.supportmanagement.api.validation.ValidJsonParameter;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;
import se.sundsvall.supportmanagement.service.ErrandStatementService;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.ETAG;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;
import static se.sundsvall.supportmanagement.api.util.JsonParameterUtil.toUpsertResponse;
import static se.sundsvall.supportmanagement.api.util.JsonParameterUtil.verifyKeyMatchesPath;
import static se.sundsvall.supportmanagement.service.util.ETagUtil.formatOrNull;

/**
 * The statements of an errand, together with the attachments and the business content that belong to them.
 * <p>
 * The attachments have no read operation here on purpose. An attachment linked to a statement <em>is</em> an attachment
 * of the errand, and is read - content and all - through
 * {@code GET /{municipalityId}/{namespace}/errands/{errandId}/attachments/{attachmentId}}. What this resource adds is
 * which attachments belong to the statement and in what order they are shown.
 */
@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/{errandId}/statements")
@Tag(name = "Errand Statements", description = "Errand statement operations")
@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class ErrandStatementsResource {

	private final ErrandStatementService service;

	ErrandStatementsResource(final ErrandStatementService service) {
		this.service = service;
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Create errand statement", description = "Creates a new statement for the errand", responses = {
		@ApiResponse(responseCode = "201", description = "Successful operation", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> createErrandStatement(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final Statement statement) {

		return created(fromPath("/{municipalityId}/{namespace}/errands/{errandId}/statements/{statementId}")
			.buildAndExpand(municipalityId, namespace, errandId, service.createErrandStatement(namespace, municipalityId, errandId, statement)).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Find errand statements", description = "Fetches all statements for the errand", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<List<Statement>> findErrandStatements(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId) {

		return ok(service.findErrandStatements(namespace, municipalityId, errandId));
	}

	@GetMapping(path = "/{statementId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read errand statement", description = "Fetches the statement matching the provided errand id and statement id", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", headers = @Header(name = ETAG, schema = @Schema(type = "string")), useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Statement> readErrandStatement(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "statementId", description = "Statement id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String statementId) {

		return withETag(service.readErrandStatement(namespace, municipalityId, errandId, statementId));
	}

	@PatchMapping(path = "/{statementId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Update errand statement", description = "Updates the statement matching the provided errand id and statement id", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", headers = @Header(name = ETAG, schema = @Schema(type = "string")), useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "412", description = "Precondition Failed - If-Match version mismatch", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Statement> updateErrandStatement(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "statementId", description = "Statement id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String statementId,
		@Parameter(name = "If-Match", description = "Optional ETag of the statement for optimistic locking - omit to skip version check") @RequestHeader(value = "If-Match", required = false) final String ifMatch,
		@Validated(OnUpdate.class) @NotNull @RequestBody final Statement statement) {

		return withETag(service.updateErrandStatement(namespace, municipalityId, errandId, statementId, ifMatch, statement));
	}

	@DeleteMapping(path = "/{statementId}", produces = ALL_VALUE)
	@Operation(summary = "Delete errand statement", description = "Deletes the statement matching the provided errand id and statement id", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "412", description = "Precondition Failed - If-Match version mismatch", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> deleteErrandStatement(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "statementId", description = "Statement id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String statementId,
		@Parameter(name = "If-Match", description = "Optional ETag of the statement for optimistic locking - omit to skip version check") @RequestHeader(value = "If-Match", required = false) final String ifMatch) {

		service.deleteErrandStatement(namespace, municipalityId, errandId, statementId, ifMatch);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	@PostMapping(path = "/{statementId}/attachments", consumes = MULTIPART_FORM_DATA_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Upload and link statement attachment", description = "Stores the file as an attachment of the errand and links it to the statement", responses = {
		@ApiResponse(responseCode = "201", description = "Successful operation", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> createStatementAttachment(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "statementId", description = "Statement id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String statementId,
		@Parameter(name = "sortOrder", description = "Order the attachment is shown in") @RequestParam(required = false) final Integer sortOrder,
		@NotNull @RequestPart("attachment") final MultipartFile attachment) {

		final var attachmentId = service.createStatementAttachment(namespace, municipalityId, errandId, statementId, attachment, sortOrder);

		return created(fromPath("/{municipalityId}/{namespace}/errands/{errandId}/attachments/{attachmentId}")
			.buildAndExpand(municipalityId, namespace, errandId, attachmentId).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@PostMapping(path = "/{statementId}/attachments/{attachmentId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Link statement attachment", description = "Links an attachment already on the errand to the statement", responses = {
		@ApiResponse(responseCode = "201", description = "Successful operation", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "409", description = "Conflict - the attachment is already linked", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<ArtefactAttachment> linkStatementAttachment(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "statementId", description = "Statement id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String statementId,
		@Parameter(name = "attachmentId", description = "Attachment id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String attachmentId,
		@Valid @NotNull @RequestBody final ArtefactAttachmentLink link) {

		final var result = service.linkStatementAttachment(namespace, municipalityId, errandId, statementId, attachmentId, link);
		return created(fromPath("/{municipalityId}/{namespace}/errands/{errandId}/attachments/{attachmentId}")
			.buildAndExpand(municipalityId, namespace, errandId, attachmentId).toUri())
			.body(result);
	}

	@PatchMapping(path = "/{statementId}/attachments/{attachmentId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Update statement attachment link", description = "Updates the order a linked attachment is shown in", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<ArtefactAttachment> updateStatementAttachment(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "statementId", description = "Statement id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String statementId,
		@Parameter(name = "attachmentId", description = "Attachment id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String attachmentId,
		@Valid @NotNull @RequestBody final ArtefactAttachmentLink link) {

		return ok(service.updateStatementAttachment(namespace, municipalityId, errandId, statementId, attachmentId, link));
	}

	@DeleteMapping(path = "/{statementId}/attachments/{attachmentId}", produces = ALL_VALUE)
	@Operation(summary = "Unlink statement attachment", description = "Removes the link between the statement and the attachment. The attachment itself stays on the errand", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> unlinkStatementAttachment(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "statementId", description = "Statement id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String statementId,
		@Parameter(name = "attachmentId", description = "Attachment id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String attachmentId) {

		service.unlinkStatementAttachment(namespace, municipalityId, errandId, statementId, attachmentId);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	@GetMapping(path = "/{statementId}/json-parameters", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read statement JSON parameters", description = "Fetches all JSON parameters belonging to the statement", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<List<JsonParameter>> readStatementJsonParameters(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "statementId", description = "Statement id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String statementId) {

		return ok(service.readStatementJsonParameters(namespace, municipalityId, errandId, statementId));
	}

	@GetMapping(path = "/{statementId}/json-parameters/{key}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read statement JSON parameter", description = "Fetches the JSON parameter matching the provided key", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", headers = @Header(name = ETAG, schema = @Schema(type = "string")), useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<JsonParameter> readStatementJsonParameter(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "statementId", description = "Statement id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String statementId,
		@Parameter(name = "key", description = "Parameter key", example = "responseForm") @NotBlank @PathVariable final String key) {

		final var result = service.readStatementJsonParameter(namespace, municipalityId, errandId, statementId, key);
		return ok().header(ETAG, formatOrNull(result.getVersion())).body(result);
	}

	@PutMapping(path = "/{statementId}/json-parameters/{key}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Write statement JSON parameter", description = "Creates or replaces the JSON parameter matching the provided key", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", headers = @Header(name = ETAG, schema = @Schema(type = "string")), useReturnTypeSchema = true),
		@ApiResponse(responseCode = "201", description = "Successful operation", headers = @Header(name = ETAG, schema = @Schema(type = "string")), useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "409", description = "Conflict - the key belongs to something else on the errand", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "412", description = "Precondition Failed - If-Match version mismatch", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<JsonParameter> updateStatementJsonParameter(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "statementId", description = "Statement id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String statementId,
		@Parameter(name = "key", description = "Parameter key", example = "responseForm") @NotBlank @PathVariable final String key,
		@Parameter(name = "If-Match", description = "Optional ETag of the JSON parameter for optimistic locking - omit to skip version check") @RequestHeader(value = "If-Match", required = false) final String ifMatch,
		@Valid @ValidJsonParameter @NotNull @RequestBody final JsonParameter jsonParameter) {

		verifyKeyMatchesPath(jsonParameter, key);

		return toUpsertResponse(service.updateStatementJsonParameter(namespace, municipalityId, errandId, statementId, key, ifMatch, jsonParameter));
	}

	@DeleteMapping(path = "/{statementId}/json-parameters/{key}", produces = ALL_VALUE)
	@Operation(summary = "Delete statement JSON parameter", description = "Deletes the JSON parameter matching the provided key", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "412", description = "Precondition Failed - If-Match version mismatch", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> deleteStatementJsonParameter(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "statementId", description = "Statement id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String statementId,
		@Parameter(name = "key", description = "Parameter key", example = "responseForm") @NotBlank @PathVariable final String key,
		@Parameter(name = "If-Match", description = "Optional ETag of the JSON parameter for optimistic locking - omit to skip version check") @RequestHeader(value = "If-Match", required = false) final String ifMatch) {

		service.deleteStatementJsonParameter(namespace, municipalityId, errandId, statementId, key, ifMatch);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	private static ResponseEntity<Statement> withETag(final Statement statement) {
		return ok().header(ETAG, formatOrNull(statement.getVersion())).body(statement);
	}

}
