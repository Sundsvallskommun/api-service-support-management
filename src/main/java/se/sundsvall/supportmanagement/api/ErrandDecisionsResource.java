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
import se.sundsvall.supportmanagement.api.model.errand.Decision;
import se.sundsvall.supportmanagement.api.model.errand.DecisionTerm;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.api.validation.ValidJsonParameter;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;
import se.sundsvall.supportmanagement.service.ErrandDecisionService;

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
 * The decisions of an errand, together with the attachments and the business content that belong to them.
 * <p>
 * The attachments have no read operation here on purpose. An attachment linked to a decision <em>is</em> an attachment
 * of the errand, and is read - content and all - through
 * {@code GET /{municipalityId}/{namespace}/errands/{errandId}/attachments/{attachmentId}}. What this resource adds is
 * which attachments belong to the decision and in what order they are shown.
 */
@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/{errandId}/decisions")
@Tag(name = "Errand Decisions", description = "Errand decision operations")
@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class ErrandDecisionsResource {

	private final ErrandDecisionService service;

	ErrandDecisionsResource(final ErrandDecisionService service) {
		this.service = service;
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Create errand decision", description = "Creates a new decision for the errand", responses = {
		@ApiResponse(responseCode = "201", description = "Successful operation", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> createErrandDecision(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final Decision decision) {

		return created(fromPath("/{municipalityId}/{namespace}/errands/{errandId}/decisions/{decisionId}")
			.buildAndExpand(municipalityId, namespace, errandId, service.createErrandDecision(namespace, municipalityId, errandId, decision)).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Find errand decisions", description = "Fetches all decisions for the errand", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<List<Decision>> findErrandDecisions(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId) {

		return ok(service.findErrandDecisions(namespace, municipalityId, errandId));
	}

	@GetMapping(path = "/{decisionId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read errand decision", description = "Fetches the decision matching the provided errand id and decision id", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", headers = @Header(name = ETAG, schema = @Schema(type = "string")), useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Decision> readErrandDecision(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "decisionId", description = "Decision id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String decisionId) {

		return withETag(service.readErrandDecision(namespace, municipalityId, errandId, decisionId));
	}

	@PatchMapping(path = "/{decisionId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Update errand decision", description = "Updates the decision matching the provided errand id and decision id", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", headers = @Header(name = ETAG, schema = @Schema(type = "string")), useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "412", description = "Precondition Failed - If-Match version mismatch", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Decision> updateErrandDecision(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "decisionId", description = "Decision id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String decisionId,
		@Parameter(name = "If-Match", description = "Optional ETag of the decision for optimistic locking - omit to skip version check") @RequestHeader(value = "If-Match", required = false) final String ifMatch,
		@Validated(OnUpdate.class) @NotNull @RequestBody final Decision decision) {

		return withETag(service.updateErrandDecision(namespace, municipalityId, errandId, decisionId, ifMatch, decision));
	}

	@DeleteMapping(path = "/{decisionId}", produces = ALL_VALUE)
	@Operation(summary = "Delete errand decision", description = "Deletes the decision matching the provided errand id and decision id", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "412", description = "Precondition Failed - If-Match version mismatch", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> deleteErrandDecision(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "decisionId", description = "Decision id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String decisionId,
		@Parameter(name = "If-Match", description = "Optional ETag of the decision for optimistic locking - omit to skip version check") @RequestHeader(value = "If-Match", required = false) final String ifMatch) {

		service.deleteErrandDecision(namespace, municipalityId, errandId, decisionId, ifMatch);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	@PostMapping(path = "/{decisionId}/attachments", consumes = MULTIPART_FORM_DATA_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Upload and link decision attachment", description = "Stores the file as an attachment of the errand and links it to the decision", responses = {
		@ApiResponse(responseCode = "201", description = "Successful operation", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> createDecisionAttachment(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "decisionId", description = "Decision id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String decisionId,
		@Parameter(name = "sortOrder", description = "Order the attachment is shown in") @RequestParam(required = false) final Integer sortOrder,
		@NotNull @RequestPart("attachment") final MultipartFile attachment) {

		final var attachmentId = service.createDecisionAttachment(namespace, municipalityId, errandId, decisionId, attachment, sortOrder);

		return created(fromPath("/{municipalityId}/{namespace}/errands/{errandId}/attachments/{attachmentId}")
			.buildAndExpand(municipalityId, namespace, errandId, attachmentId).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@PostMapping(path = "/{decisionId}/attachments/{attachmentId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Link decision attachment", description = "Links an attachment already on the errand to the decision", responses = {
		@ApiResponse(responseCode = "201", description = "Successful operation", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "409", description = "Conflict - the attachment is already linked", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<ArtefactAttachment> linkDecisionAttachment(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "decisionId", description = "Decision id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String decisionId,
		@Parameter(name = "attachmentId", description = "Attachment id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String attachmentId,
		@Valid @NotNull @RequestBody final ArtefactAttachmentLink link) {

		final var result = service.linkDecisionAttachment(namespace, municipalityId, errandId, decisionId, attachmentId, link);
		return created(fromPath("/{municipalityId}/{namespace}/errands/{errandId}/attachments/{attachmentId}")
			.buildAndExpand(municipalityId, namespace, errandId, attachmentId).toUri())
			.body(result);
	}

	@PatchMapping(path = "/{decisionId}/attachments/{attachmentId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Update decision attachment link", description = "Updates the order a linked attachment is shown in", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<ArtefactAttachment> updateDecisionAttachment(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "decisionId", description = "Decision id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String decisionId,
		@Parameter(name = "attachmentId", description = "Attachment id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String attachmentId,
		@Valid @NotNull @RequestBody final ArtefactAttachmentLink link) {

		return ok(service.updateDecisionAttachment(namespace, municipalityId, errandId, decisionId, attachmentId, link));
	}

	@DeleteMapping(path = "/{decisionId}/attachments/{attachmentId}", produces = ALL_VALUE)
	@Operation(summary = "Unlink decision attachment", description = "Removes the link between the decision and the attachment. The attachment itself stays on the errand", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> unlinkDecisionAttachment(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "decisionId", description = "Decision id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String decisionId,
		@Parameter(name = "attachmentId", description = "Attachment id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String attachmentId) {

		service.unlinkDecisionAttachment(namespace, municipalityId, errandId, decisionId, attachmentId);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	@GetMapping(path = "/{decisionId}/json-parameters", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read decision JSON parameters", description = "Fetches all JSON parameters belonging to the decision", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<List<JsonParameter>> readDecisionJsonParameters(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "decisionId", description = "Decision id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String decisionId) {

		return ok(service.readDecisionJsonParameters(namespace, municipalityId, errandId, decisionId));
	}

	@GetMapping(path = "/{decisionId}/json-parameters/{key}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read decision JSON parameter", description = "Fetches the JSON parameter matching the provided key", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", headers = @Header(name = ETAG, schema = @Schema(type = "string")), useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<JsonParameter> readDecisionJsonParameter(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "decisionId", description = "Decision id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String decisionId,
		@Parameter(name = "key", description = "Parameter key", example = "decisionForm") @NotBlank @PathVariable final String key) {

		final var result = service.readDecisionJsonParameter(namespace, municipalityId, errandId, decisionId, key);
		return ok().header(ETAG, formatOrNull(result.getVersion())).body(result);
	}

	@PutMapping(path = "/{decisionId}/json-parameters/{key}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Write decision JSON parameter", description = "Creates or replaces the JSON parameter matching the provided key", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", headers = @Header(name = ETAG, schema = @Schema(type = "string")), useReturnTypeSchema = true),
		@ApiResponse(responseCode = "201", description = "Successful operation", headers = @Header(name = ETAG, schema = @Schema(type = "string")), useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "409", description = "Conflict - the key belongs to something else on the errand", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "412", description = "Precondition Failed - If-Match version mismatch", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<JsonParameter> updateDecisionJsonParameter(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "decisionId", description = "Decision id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String decisionId,
		@Parameter(name = "key", description = "Parameter key", example = "decisionForm") @NotBlank @PathVariable final String key,
		@Parameter(name = "If-Match", description = "Optional ETag of the JSON parameter for optimistic locking - omit to skip version check") @RequestHeader(value = "If-Match", required = false) final String ifMatch,
		@Valid @ValidJsonParameter @NotNull @RequestBody final JsonParameter jsonParameter) {

		verifyKeyMatchesPath(jsonParameter, key);

		return toUpsertResponse(service.updateDecisionJsonParameter(namespace, municipalityId, errandId, decisionId, key, ifMatch, jsonParameter));
	}

	@DeleteMapping(path = "/{decisionId}/json-parameters/{key}", produces = ALL_VALUE)
	@Operation(summary = "Delete decision JSON parameter", description = "Deletes the JSON parameter matching the provided key", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "412", description = "Precondition Failed - If-Match version mismatch", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> deleteDecisionJsonParameter(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "decisionId", description = "Decision id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String decisionId,
		@Parameter(name = "key", description = "Parameter key", example = "decisionForm") @NotBlank @PathVariable final String key,
		@Parameter(name = "If-Match", description = "Optional ETag of the JSON parameter for optimistic locking - omit to skip version check") @RequestHeader(value = "If-Match", required = false) final String ifMatch) {

		service.deleteDecisionJsonParameter(namespace, municipalityId, errandId, decisionId, key, ifMatch);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	@PostMapping(path = "/{decisionId}/terms", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Create decision term", description = "Creates a new term on the decision", responses = {
		@ApiResponse(responseCode = "201", description = "Successful operation", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> createDecisionTerm(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "decisionId", description = "Decision id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String decisionId,
		@Valid @NotNull @RequestBody final DecisionTerm term) {

		return created(fromPath("/{municipalityId}/{namespace}/errands/{errandId}/decisions/{decisionId}/terms/{termId}")
			.buildAndExpand(municipalityId, namespace, errandId, decisionId, service.createDecisionTerm(namespace, municipalityId, errandId, decisionId, term)).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@GetMapping(path = "/{decisionId}/terms", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Find decision terms", description = "Fetches all terms of the decision", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<List<DecisionTerm>> findDecisionTerms(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "decisionId", description = "Decision id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String decisionId) {

		return ok(service.findDecisionTerms(namespace, municipalityId, errandId, decisionId));
	}

	@GetMapping(path = "/{decisionId}/terms/{termId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read decision term", description = "Fetches the term matching the provided decision id and term id", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<DecisionTerm> readDecisionTerm(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "decisionId", description = "Decision id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String decisionId,
		@Parameter(name = "termId", description = "Term id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String termId) {

		return ok(service.readDecisionTerm(namespace, municipalityId, errandId, decisionId, termId));
	}

	@PatchMapping(path = "/{decisionId}/terms/{termId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Update decision term", description = "Updates the term matching the provided decision id and term id", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<DecisionTerm> updateDecisionTerm(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "decisionId", description = "Decision id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String decisionId,
		@Parameter(name = "termId", description = "Term id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String termId,
		@Validated(OnUpdate.class) @NotNull @RequestBody final DecisionTerm term) {

		return ok(service.updateDecisionTerm(namespace, municipalityId, errandId, decisionId, termId, term));
	}

	@DeleteMapping(path = "/{decisionId}/terms/{termId}", produces = ALL_VALUE)
	@Operation(summary = "Delete decision term", description = "Deletes the term matching the provided decision id and term id", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> deleteDecisionTerm(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "decisionId", description = "Decision id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String decisionId,
		@Parameter(name = "termId", description = "Term id", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7") @ValidUuid @PathVariable final String termId) {

		service.deleteDecisionTerm(namespace, municipalityId, errandId, decisionId, termId);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	private static ResponseEntity<Decision> withETag(final Decision decision) {
		return ok().header(ETAG, formatOrNull(decision.getVersion())).body(decision);
	}

}
