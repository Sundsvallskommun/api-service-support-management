package se.sundsvall.supportmanagement.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.api.validation.ValidJsonParameter;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.ETAG;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.dept44.problem.Problem.valueOf;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;
import static se.sundsvall.supportmanagement.service.util.ETagUtil.format;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/{errandId}/json-parameters")
@Tag(name = "Errand JSON Parameters", description = "Errand JSON parameter operations")
@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class ErrandJsonParameterResource {

	private final ErrandJsonParameterService service;

	ErrandJsonParameterResource(final ErrandJsonParameterService service) {
		this.service = service;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read all JSON parameters", description = "Fetches all JSON parameters for the provided errand id", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<List<JsonParameter>> readJsonParameters(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId) {

		return ok(service.readJsonParameters(namespace, municipalityId, errandId));
	}

	@GetMapping(path = "/{key}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read JSON parameter", description = "Fetches the JSON parameter matching the provided errand id and key", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<JsonParameter> readJsonParameter(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "key", description = "JSON parameter key", example = "formData") @NotBlank @PathVariable final String key) {

		final var result = service.readJsonParameter(namespace, municipalityId, errandId, key);
		return ok()
			.header(ETAG, result.getVersion() != null ? format(result.getVersion()) : null)
			.body(result);
	}

	@PutMapping(path = "/{key}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Create or update JSON parameter", description = "Creates or updates the JSON parameter matching the provided errand id and key", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation — parameter updated", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "201", description = "Created — parameter created", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "409", description = "Conflict — resource modified concurrently", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "412", description = "Precondition Failed — If-Match version mismatch", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<JsonParameter> updateJsonParameter(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "key", description = "JSON parameter key", example = "formData") @NotBlank @PathVariable final String key,
		@Parameter(name = "If-Match", description = "Optional ETag of the JSON parameter for optimistic locking — omit to skip version check") @RequestHeader(value = "If-Match", required = false) final String ifMatch,
		@Valid @ValidJsonParameter @NotNull @RequestBody final JsonParameter jsonParameter) {

		if (jsonParameter.getKey() != null && !key.equals(jsonParameter.getKey())) {
			throw valueOf(BAD_REQUEST, "Key in request body '%s' does not match key in path '%s'".formatted(jsonParameter.getKey(), key));
		}
		final var result = service.updateJsonParameter(namespace, municipalityId, errandId, key, ifMatch, jsonParameter);
		final var eTag = result.jsonParameter().getVersion() != null ? format(result.jsonParameter().getVersion()) : null;
		if (result.created()) {
			final var location = URI.create(ServletUriComponentsBuilder.fromCurrentRequest().build().getPath());
			return ResponseEntity.created(location).header(ETAG, eTag).body(result.jsonParameter());
		}
		return ok().header(ETAG, eTag).body(result.jsonParameter());
	}

	@DeleteMapping(path = "/{key}", produces = ALL_VALUE)
	@Operation(summary = "Delete JSON parameter", description = "Deletes the JSON parameter matching the provided errand id and key", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "409", description = "Conflict — resource modified concurrently", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "412", description = "Precondition Failed — If-Match version mismatch", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> deleteJsonParameter(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "key", description = "JSON parameter key", example = "formData") @NotBlank @PathVariable final String key,
		@Parameter(name = "If-Match", description = "Optional ETag of the JSON parameter for optimistic locking — omit to skip version check") @RequestHeader(value = "If-Match", required = false) final String ifMatch) {

		service.deleteJsonParameter(namespace, municipalityId, errandId, key, ifMatch);
		return noContent()
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}
}
